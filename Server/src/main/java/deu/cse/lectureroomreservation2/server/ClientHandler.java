/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server;

/**
 *
 * @author SAMSUNG
 */
import deu.cse.lectureroomreservation2.common.LoginStatus;
import deu.cse.lectureroomreservation2.server.control.noticeController;
import deu.cse.lectureroomreservation2.server.control.receiveController;
import deu.cse.lectureroomreservation2.server.control.CheckMaxTime;
import deu.cse.lectureroomreservation2.server.control.ReserveManager;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.CheckMaxTimeResult;
import deu.cse.lectureroomreservation2.common.ReserveRequest;
import deu.cse.lectureroomreservation2.common.CheckMaxTimeRequest;
import deu.cse.lectureroomreservation2.common.ReserveManageRequest;
import deu.cse.lectureroomreservation2.common.ReserveManageResult;
import deu.cse.lectureroomreservation2.common.ScheduleRequest;
import deu.cse.lectureroomreservation2.common.ScheduleResult;
import deu.cse.lectureroomreservation2.common.UserRequest;
import deu.cse.lectureroomreservation2.common.UserResult;
import deu.cse.lectureroomreservation2.server.control.TimeTableController;
import deu.cse.lectureroomreservation2.server.control.UserRequestController;
import deu.cse.lectureroomreservation2.server.control.ChangePassController;
import deu.cse.lectureroomreservation2.server.control.BuildingManager;
import deu.cse.lectureroomreservation2.server.control.ReservationDetails;
import deu.cse.lectureroomreservation2.server.control.LoginQueueProxy;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;
    private final BuildingManager buildingManager;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.buildingManager = new BuildingManager();
    }

    @Override
    public void run() {
        String id = null;   // 현재 클라이언트의 ID (finally에서 큐/로그인 상태 정리용)

        try {
            System.out.println("Client Connection request received: " + socket.getInetAddress());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 사용자 정보 먼저 받음
            id = in.readUTF();
            String password = in.readUTF();
            String role = in.readUTF();

            // ===== 로그인 처리 (3명 제한 + 대기열 Proxy 적용) =====

            // 중복로그인 체크
            synchronized (server.getLoggedInUsers()) {
                if (server.getLoggedInUsers().contains(id)) {
                    System.out.println("Connection refused (account already log-in): " + id);
                    out.writeObject(new LoginStatus(false, "DUPLICATE", "이미 로그인 중인 계정입니다."));
                    out.flush();
                    return;
                }
            }

            // 1) ID/PW/Role 검증은 기존 서버 로직 사용
            LoginStatus status = server.requestAuth(id, password, role);

            // 2) 인증 성공한 경우에만, Proxy를 통해 ACTIVE / WAITING 결정
            if (status.isLoginSuccess()) {
                String queueState = LoginQueueProxy.register(id); // "ACTIVE" 또는 "WAITING:n"
                status.setMessage(queueState);

                // 같은 계정 중복 접속 방지를 위해 서버에 등록
                synchronized (server.getLoggedInUsers()) {
                    server.getLoggedInUsers().add(id);
                }
            }

            // 3) 로그인 결과 전송
            out.writeObject(status);
            out.flush();

            // ===== 로그인 성공한 경우 명령 수신 루프 =====
            if (status.isLoginSuccess()) {
                System.out.println("로그인 성공 하여 역할 " + status.getRole() + "를 가집니다.");

                // 학생이면 공지사항 전송
                if ("STUDENT".equals(status.getRole())) {
                    List<String> notices = noticeController.getNotices(id);
                    for (String notice : notices) {
                        out.writeUTF("NOTICE");
                        out.flush();
                        out.writeUTF(notice);
                        out.flush();
                        noticeController.removeNotice(id, notice);
                    }
                    out.writeUTF("NOTICE_END");
                    out.flush();
                }

                while (true) {
                    try {
                        String command = in.readUTF();
                        System.out.println(">> 수신 명령: " + command);

                        if ("LOGOUT".equalsIgnoreCase(command)) {
                            System.out.println("User has log-out: " + id);
                            break;
                        }

                        // 대기열 상태 확인 (클라이언트 타이머용)
                        if ("CHECK_QUEUE".equals(command)) {
                            String state = LoginQueueProxy.getStatus(id); // ACTIVE / WAITING:n / NONE
                            out.writeUTF(state);
                            out.flush();
                            continue;
                        }

                        // 비밀번호 변경 요청 처리
                        if ("CHANGE_PASS".equals(command)) {
                            String userId = in.readUTF();
                            String currentPass = in.readUTF();
                            String newPass = in.readUTF();

                            ChangePassController controller = new ChangePassController();
                            String result = controller.changePassword(userId, currentPass, newPass);

                            if ("SUCCESS".equals(result)) {
                                System.out.println("비밀번호 변경 성공: " + userId);
                            } else {
                                System.out.println("비밀번호 변경 실패 " + userId + result);
                            }

                            out.writeUTF(result); // 예: "SUCCESS" 또는 오류 메시지
                            out.flush();
                        }

                        // 예약 요청 처리
                        if ("RESERVE".equals(command)) {
                            ReserveRequest req = (ReserveRequest) in.readObject();
                            ReserveResult result = new receiveController().handleReserve(req);
                            out.writeObject(result);
                            out.flush();
                        }

                        // CHECK_MAX_TIME 명령 처리
                        if ("CHECK_MAX_TIME".equals(command)) {
                            CheckMaxTimeRequest req = (CheckMaxTimeRequest) in.readObject();
                            boolean exceeded = new CheckMaxTime(req.getId()).check();

                            String reason = exceeded ? "최대 예약 가능 개수를 초과했습니다." : "예약 가능";

                            CheckMaxTimeResult result = new CheckMaxTimeResult(exceeded, reason);
                            out.writeObject(result);
                            out.flush();
                        }

                        // 예약 정보 조회 (고급)
                        if ("RETRIEVE_MY_RESERVE_ADVANCED".equals(command)) {
                            String userid = (String) in.readObject();
                            String roomNum = (String) in.readObject();
                            String date = (String) in.readObject();

                            if (Objects.isNull(roomNum) && Objects.isNull(date)) {
                                List<String> reserves = ReserveManager.getReserveInfoById(userid);
                                out.writeObject(reserves);
                                out.flush();
                            } else {
                                List<String> result = ReserveManager.getReserveInfoAdvanced(userid, roomNum, date);
                                out.writeObject(result);
                                out.flush();
                            }
                        }

                        // 예약 정보로 총 예약자 수 조회
                        if ("COUNT_RESERVE_USERS".equals(command)) {
                            String reserveInfo = in.readUTF();
                            int userCount = ReserveManager.countUsersByReserveInfo(reserveInfo);
                            out.writeInt(userCount);
                            out.flush();
                        }

                        // 예약 정보로 예약자 id 목록 조회
                        if ("GET_USER_IDS_BY_RESERVE".equals(command)) {
                            String reserveInfo = in.readUTF();
                            List<String> userIds = ReserveManager.getUserIdsByReserveInfo(reserveInfo);
                            out.writeObject(userIds);
                            out.flush();
                        }

                        // 예약 취소
                        if ("CANCEL_RESERVE".equals(command)) {
                            String userId = in.readUTF();
                            String reserveInfo = in.readUTF();
                            ReserveResult result = ReserveManager.cancelReserve(userId, reserveInfo);
                            out.writeObject(result);
                            out.flush();
                        }

                        // 기존 예약 정보를 새 예약 정보로 변경
                        if ("MODIFY_RESERVE".equals(command)) {
                            String userId = in.readUTF();
                            String oldReserveInfo = in.readUTF();
                            String newRoomNumber = in.readUTF();
                            String newDate = in.readUTF();
                            String newDay = in.readUTF();
                            String giverole = in.readUTF(); // 역할

                            ReservationDetails details = new ReservationDetails.Builder(userId, giverole)
                                    .oldReserveInfo(oldReserveInfo)
                                    .newRoomNumber(newRoomNumber)
                                    .newDate(newDate)
                                    .newDay(newDay)
                                    .build();

                            ReserveResult reserveResult = ReserveManager.updateReserve(details);

                            out.writeObject(reserveResult);
                            out.flush();
                        }

                        // 예약 정보로 교수 예약 여부 조회
                        if ("FIND_PROFESSOR_BY_RESERVE".equals(command)) {
                            String reserveInfo = in.readUTF();
                            boolean found = ReserveManager.hasProfessorReserve(reserveInfo);
                            out.writeBoolean(found);
                            out.flush();
                        }

                        // 건물 목록 요청
                        if ("GET_BUILDINGS".equals(command)) {
                            List<String> buildings = buildingManager.getBuildingList();
                            out.writeObject(buildings);
                            out.flush();
                        }

                        // 층 목록 요청
                        if ("GET_FLOORS".equals(command)) {
                            String buildingName = in.readUTF();
                            List<String> floors = buildingManager.getFloorList(buildingName);
                            out.writeObject(floors);
                            out.flush();
                        }

                        // 강의실 목록 요청
                        if ("GET_ROOMS".equals(command)) {
                            String buildingName = in.readUTF();
                            String floorName = in.readUTF();
                            List<String[]> rooms = buildingManager.getRoomList(buildingName, floorName);
                            out.writeObject(rooms);
                            out.flush();
                        }

                        // 주간 시간표 조회
                        if ("GET_WEEKLY_SCHEDULE".equals(command)) {
                            String roomNum = in.readUTF();
                            try {
                                LocalDate monday = (LocalDate) in.readObject(); // 주 시작일
                                Map<String, List<String[]>> weeklySchedule = ReserveManager.getWeeklySchedule(roomNum, monday);
                                out.writeObject(weeklySchedule);
                            } catch (Exception e) {
                                System.err.println("GET_WEEKLY_SCHEDULE 처리 중 오류: " + e.getMessage());
                                out.writeObject(new HashMap<String, List<String[]>>());
                            }
                            out.flush();
                        }

                        // 월별 현황 조회
                        if ("GET_MONTHLY_STATUS".equals(command) || "GET_MONTHLY_RESERVED_DATES".equals(command)) {
                            System.out.println(">> 월별 현황 조회 명령 수신됨: " + command);

                            String roomNum = in.readUTF();
                            int year = in.readInt();
                            int month = in.readInt();
                            String startTime = in.readUTF();

                            List<String> result = ReserveManager.getReservationStatusForMonth(roomNum, year, month, startTime);

                            out.writeObject(result);
                            out.flush();
                        }

                        // 강의실 상태 조회
                        if ("GET_ROOM_STATE".equals(command)) {
                            String roomNum = in.readUTF();
                            String day = in.readUTF();
                            String start = in.readUTF();
                            String end = in.readUTF();
                            String date = in.readUTF(); // "yyyy / MM / dd / HH:mm HH:mm"
                            String state = ReserveManager.getRoomState(roomNum, day, start, end, date);
                            out.writeUTF(state);
                            out.flush();
                        }

                        // 강의실 예약 시간대 조회
                        if ("GET_ROOM_SLOTS".equals(command)) {
                            String roomNum = in.readUTF();
                            String day = in.readUTF();
                            List<String[]> slots = ReserveManager.getRoomSlots(roomNum, day);
                            out.writeInt(slots.size());
                            for (String[] slot : slots) {
                                out.writeUTF(slot[0]); // start
                                out.writeUTF(slot[1]); // end
                            }
                            out.flush();
                        }

                        // 시간표 관련 명령
                        if ("SCHEDULE".equals(command)) {
                            System.out.println(">> SCHEDULE 명령 수신됨");

                            ScheduleRequest req = (ScheduleRequest) in.readObject();
                            ScheduleResult result;
                            TimeTableController controller = new TimeTableController();

                            switch (req.getCommand()) {
                                case "LOAD" -> {
                                    Map<String, String> schedule = controller.getScheduleForRoom(
                                            req.getRoom(), req.getDay(), req.getType());
                                    result = new ScheduleResult(true, "조회 성공", schedule);
                                }
                                case "ADD" -> {
                                    try {
                                        controller.addScheduleToFile(req.getRoom(), req.getDay(), req.getStart(), req.getEnd(), req.getSubject(), req.getType());
                                        result = new ScheduleResult(true, "등록 성공", null);
                                    } catch (Exception e) {
                                        result = new ScheduleResult(false, "등록 실패: " + e.getMessage(), null);
                                    }
                                }
                                case "DELETE" -> {
                                    boolean deleted = controller.deleteScheduleFromFile(req.getRoom(), req.getDay(), req.getStart(), req.getEnd());
                                    result = new ScheduleResult(deleted, deleted ? "삭제 성공" : "삭제 실패", null);
                                }
                                case "UPDATE" -> {
                                    boolean updated = controller.updateSchedule(req.getRoom(), req.getDay(), req.getStart(), req.getEnd(), req.getSubject(), req.getType());
                                    result = new ScheduleResult(updated, updated ? "수정 성공" : "수정 실패", null);
                                }
                                default -> result = new ScheduleResult(false, "알 수 없는 명령입니다", null);
                            }

                            out.writeObject(result);
                            out.flush();
                        }

                        // 사용자 관리
                        if ("USER".equals(command)) {
                            System.out.println(">> USER 명령 수신됨");

                            try {
                                UserRequest req = (UserRequest) in.readObject();
                                UserResult result;
                                UserRequestController controller = new UserRequestController();

                                String cmd = req.getCommand();

                                if (cmd == null) {
                                    result = new UserResult(false, "알 수 없는 사용자 명령입니다", null);
                                } else {
                                    switch (cmd) {
                                        case "ADD" -> {
                                            try {
                                                List<String[]> added = controller.saveUserAndGetSingleUser(
                                                        new String[]{req.getRole(), req.getName(), req.getId(), req.getPassword()}
                                                );
                                                result = new UserResult(true, "사용자 등록 성공", added);
                                            } catch (Exception e) {
                                                result = new UserResult(false, "등록 실패: " + e.getMessage(), null);
                                            }
                                        }
                                        case "DELETE" -> {
                                            boolean deleted = controller.deleteUser(req.getRole(), req.getId());
                                            result = new UserResult(deleted, deleted ? "사용자 삭제 성공" : "삭제 실패", null);
                                        }
                                        case "SEARCH" -> {
                                            List<String[]> users = controller.handleSearchRequest(req.getRole(), req.getNameFilter());
                                            result = new UserResult(true, "사용자 검색 성공", users);
                                        }
                                        default -> result = new UserResult(false, "알 수 없는 사용자 명령입니다", null);
                                    }
                                }

                                out.writeObject(result);
                                out.flush();

                            } catch (Exception e) {
                                System.err.println(">> USER 명령 처리 중 오류: " + e.getMessage());
                                e.printStackTrace();

                                UserResult errorResult = new UserResult(false, "서버 처리 오류 발생", null);
                                out.writeObject(errorResult);
                                out.flush();
                            }
                        }

                        // ID로 role 찾기
                        if ("FIND_ROLE".equals(command)) {
                            String userId = in.readUTF();
                            String foundRole = null;
                            try (BufferedReader br = new BufferedReader(new FileReader(receiveController.getUserFileName()))) {
                                String line;
                                while ((line = br.readLine()) != null) {
                                    String[] parts = line.split(",");
                                    if (parts.length >= 3 && parts[2].trim().equals(userId)) {
                                        foundRole = parts[0].trim();
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            out.writeObject(foundRole != null ? foundRole : "");
                            out.flush();
                            continue;
                        }

                        // 예약 관리
                        if ("RESERVE_MANAGE".equals(command)) {
                            System.out.println(">> RESERVE_MANAGE 명령 수신됨");

                            try {
                                ReserveManageRequest req = (ReserveManageRequest) in.readObject();
                                ReserveManageResult result;

                                String cmd = req.getCommand();

                                switch (cmd) {
                                    case "SEARCH" -> result = ReserveManager.searchUserAndReservations(
                                            req.getUserId(), req.getRoom(), req.getDate()
                                    );
                                    case "UPDATE" -> {
                                        ReservationDetails details = new ReservationDetails.Builder(req.getUserId(), req.getRole())
                                                .oldReserveInfo(req.getOldReserveInfo())
                                                .newRoomNumber(req.getNewRoom())
                                                .newDate(req.getNewDate())
                                                .newDay(req.getNewDay())
                                                .build();
                                        ReserveResult updateRes = ReserveManager.updateReserve(details);
                                        result = new ReserveManageResult(updateRes.getResult(), updateRes.getReason(), null);
                                    }
                                    case "DELETE" -> {
                                        ReserveResult deleteRes = ReserveManager.cancelReserve(
                                                req.getUserId(), req.getReserveInfo()
                                        );
                                        result = new ReserveManageResult(deleteRes.getResult(), deleteRes.getReason(), null);
                                    }
                                    default -> result = new ReserveManageResult(false, "알 수 없는 명령입니다", null);
                                }

                                out.writeObject(result);
                                out.flush();

                            } catch (Exception e) {
                                System.err.println(">> RESERVE_MANAGE 처리 중 오류: " + e.getMessage());
                                e.printStackTrace();
                                ReserveManageResult errorResult = new ReserveManageResult(false, "서버 처리 오류", null);
                                out.writeObject(errorResult);
                                out.flush();
                            }
                        }

                    } catch (IOException e) {
                        System.out.println("Client Connection Error or Terminated. " + e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 큐/ACTIVE에서 제거
            if (id != null) {
                LoginQueueProxy.leave(id);

                synchronized (server.getLoggedInUsers()) {
                    server.getLoggedInUsers().remove(id); // 로그인 목록에서 제거
                }
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
