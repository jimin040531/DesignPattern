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

// [Observer 패턴] 1. Observer 임포트
import deu.cse.lectureroomreservation2.server.control.Observer;
import deu.cse.lectureroomreservation2.server.control.NotificationService;
import deu.cse.lectureroomreservation2.server.control.ResourceCheckStrategy;
import deu.cse.lectureroomreservation2.server.control.SystemMonitor;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

public class ClientHandler implements Runnable, Observer {

    private final Socket socket;
    private final Server server;
    private final BuildingManager buildingManager;
   
    private String userId;
    // [Observer 패턴] 3. 출력 스트림을 멤버 변수로 승격 (update 메서드에서 쓰기 위해)
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.buildingManager = BuildingManager.getInstance();
    }
    private final List<String> pendingNotices = new ArrayList<>(); // 알림 대기열
    // 2. update 메서드 수정
    @Override
    public void update(String message) {
        synchronized (pendingNotices) {
            pendingNotices.add(message); // 즉시 전송하지 않고 큐에 쌓음
        }
        System.out.println(">> [ClientHandler] 알림 큐에 저장됨: " + message);
    }
    
    // [Observer 패턴] 4. 알림 수신 시 실행될 메서드 구현
    /*@Override
    public void update(String message) {
        try {
            if (out != null) {
                // 클라이언트(Client.java)의 checkAndShowNotices 메서드가 "NOTICE" 헤더를 기다림
                out.writeUTF("NOTICE");
                out.flush();
                out.writeUTF(message);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("알림 전송 실패: " + e.getMessage());
        }
    }*/

    @Override
    public void run() {
        boolean acquired = false;
        String id = null;

        try {
            System.out.println("Client Connection request received: " + socket.getInetAddress());

            // 멤버 변수 out 초기화
            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 사용자 정보 먼저 받음
            id = in.readUTF();
            String password = in.readUTF();
            String role = in.readUTF();

            // 중복로그인 체크
            synchronized (server.getLoggedInUsers()) {
                if (server.getLoggedInUsers().contains(id)) {
                    System.out.println("Connection refused (account already log-in): " + id);
                    out.writeObject(new LoginStatus(false, "DUPLICATE", "이미 로그인 중인 계정입니다."));
                    out.flush();
                    return;
                }
            }

            LoginStatus status = server.requestAuth(id, password, role); // 인증
            if (status.isLoginSuccess()) {
                acquired = true;
            }
            if (status.isLoginSuccess()) {
                synchronized (server.getLoggedInUsers()) {
                    server.getLoggedInUsers().add(id); // 로그인 성공한 사용자 등록
                }
                this.userId = id;
                // [Observer 패턴] 5. 로그인 성공 시 알림 서비스에 등록 (구독 시작)
                NotificationService.getInstance().registerObserver(userId, this);
            }

            out.writeObject(status);
            out.flush();

            // 로그인 성공한 경우 명령 수신 루프
            if (status.isLoginSuccess()) {
                // 쌓여있던(오프라인) 공지사항 전송
                System.out.println("로그인 성공 하여 역할 " + status.getRole() + "를 가집니다.");
                if ("STUDENT".equals(status.getRole())) {
                    List<String> notices = noticeController.getNotices(id);
                    synchronized (pendingNotices) { // 리스트 접근 동기화
                        for (String notice : notices) {
                            // 소켓으로 바로 보내지 말고, 대기열에 추가!
                            pendingNotices.add(notice); 
                            
                            // (선택) 파일에서 읽은 공지는 삭제 처리
                            noticeController.removeNotice(id, notice);
                        }
                    }
                }

                while (true) {
                    try {
                        String command = in.readUTF();

                        System.out.println(">> 수신 명령: " + command); // 여기 추가
                        
                        // 클라이언트가 주기적으로 알림을 확인하러 올 때 처리
                        if ("CHECK_NOTICES".equals(command)) {
                            synchronized (pendingNotices) {
                                // 쌓인 알림 개수 전송
                                out.writeInt(pendingNotices.size());

                                // 알림 내용 전송
                                for (String notice : pendingNotices) {
                                    out.writeUTF(notice);
                                }
                                out.flush();

                                // 전송 후 비우기
                                pendingNotices.clear();
                            }
                            continue; // 중요: 아래 다른 로직을 실행하지 않고 루프 처음으로 돌아감
                        }
                        
                        if ("CHECK_SYSTEM_STATUS".equals(command)) {
                            SystemMonitor monitor = new SystemMonitor();
                        
                            // 1. 파일 검사 (기본)
                            String fileResult = monitor.checkSystem();

                            // 2. 메모리 검사 (전략 교체!)
                            monitor.setStrategy(new ResourceCheckStrategy());
                            String resourceResult = monitor.checkSystem();

                            String finalResult = fileResult + "\n" + resourceResult;

                            // 서버 터미널 로그 출력
                            System.out.println(">> [관리자 요청] 시스템 상태 점검 결과:");
                            System.out.println(finalResult);

                            // 클라이언트로 결과 전송
                            out.writeUTF(finalResult);
                            out.flush();
                        }
                        
                        // 예약 현황 통계 요청 처리
                        if ("GET_RESERVATION_STATS".equals(command)) {
                            String buildingName = in.readUTF();
                            String room = in.readUTF();
                            String date = in.readUTF();
                            String startTime = in.readUTF();
                            
                            // 2. 건물 이름을 포함하여 통계 요청 (자연관/공학관 구분)
                            int[] stats = ReserveManager.getReservationStats(buildingName, room, date, startTime);

                            // 결과 전송 (int 배열: [확정수, 대기수])
                            synchronized (this) {
                                out.writeObject(stats);
                                out.flush();
                            }
                        }

                        if ("LOGOUT".equalsIgnoreCase(command)) {
                            System.out.println("User has log-out: " + id);
                            break;
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
                            // 클라이언트로부터 예약 요청 객체를 받음
                            ReserveRequest req = (ReserveRequest) in.readObject();
                            // 예약 처리 결과를 받아옴
                            ReserveResult result = new receiveController().handleReserve(req);
                            // 결과를 클라이언트에 전송
                            out.writeObject(result);
                            out.flush();
                        }
                        // CHECK_MAX_TIME 명령 처리 추가
                        if ("CHECK_MAX_TIME".equals(command)) {
                            CheckMaxTimeRequest req = (CheckMaxTimeRequest) in.readObject();
                            boolean exceeded = new CheckMaxTime(req.getId()).check();

                            String reason = exceeded ? "최대 예약 가능 개수를 초과했습니다." : "예약 가능";

                            CheckMaxTimeResult result = new CheckMaxTimeResult(exceeded, reason);
                            out.writeObject(result);
                            out.flush();
                        }
                        // 클라이언트 요청 - id 또는 강의실 또는 날짜로 예약 정보 조회 요청 받는 부분
                        if ("RETRIEVE_MY_RESERVE_ADVANCED".equals(command)) {
                            String userid = (String) in.readObject();
                            String room = (String) in.readObject();
                            String date = (String) in.readObject();

                            if (Objects.isNull(room) && Objects.isNull(date)) {
                                List<String> reserves = ReserveManager.getReserveInfoById(userid);
                                out.writeObject(reserves);
                                out.flush();
                            } else {
                                List<String> result = ReserveManager.getReserveInfoAdvanced(userid, room, date);
                                out.writeObject(result);
                                out.flush();
                            }
                        }
                        // 클라이언트 요청 - 예약 정보로 총 예약자 수 조회 요청 받는 부분
                        if ("COUNT_RESERVE_USERS".equals(command)) {
                            String reserveInfo = in.readUTF();
                            int userCount = ReserveManager.countUsersByReserveInfo(reserveInfo);
                            out.writeInt(userCount);
                            out.flush();
                        }
                        // 클라이언트 요청 - 예약 정보로 예약자 id 목록 조회 (6번 기능)
                        if ("GET_USER_IDS_BY_RESERVE".equals(command)) {
                            String reserveInfo = in.readUTF();
                            List<String> userIds = ReserveManager.getUserIdsByReserveInfo(reserveInfo);
                            out.writeObject(userIds);
                            out.flush();
                        }
                        // 클라이언트 요청 - 예약 취소 요청 받는 부분
                        if ("CANCEL_RESERVE".equals(command)) {
                            String userId = in.readUTF();
                            String reserveInfo = in.readUTF();
                            ReserveResult result = ReserveManager.cancelReserve(userId, reserveInfo);
                            out.writeObject(result);
                            out.flush();
                        }
                        // 클라이언트 요청 - 기존 예약 정보를 새 예약 정보로 변경
                        if ("MODIFY_RESERVE".equals(command)) {
                            // 1. 파라미터 읽기 (순서 중요!)
                            String userId = in.readUTF();
                            String oldReserveInfo = in.readUTF();
                            String buildingName = in.readUTF(); // [추가] 읽기
                            String newRoomNumber = in.readUTF();
                            String newDate = in.readUTF();
                            String newDay = in.readUTF();
                            String purpose = in.readUTF();      // [추가] 읽기
                            int userCount = in.readInt();       // [추가] 읽기
                            String giverole = in.readUTF();

                            // 2. Builder에 모든 정보 담기
                            ReservationDetails details = new ReservationDetails.Builder(userId, giverole)
                                    .oldReserveInfo(oldReserveInfo)
                                    .buildingName(buildingName) // [설정]
                                    .newRoomNumber(newRoomNumber)
                                    .newDate(newDate)
                                    .newDay(newDay)
                                    .purpose(purpose)           // [설정]
                                    .userCount(userCount)       // [설정]
                                    .build();

                            ReserveResult reserveResult = ReserveManager.updateReserve(details);
                            synchronized (this) {
                                out.writeObject(reserveResult);
                                out.flush();
                            }
                        }
                        // 클라이언트 요청 - 예약 정보로 교수 예약 여부 조회 요청 받는 부분 - 교수 예약O true, 교수 예약X false
                        if ("FIND_PROFESSOR_BY_RESERVE".equals(command)) {
                            String reserveInfo = in.readUTF();
                            boolean found = ReserveManager.hasProfessorReserve(reserveInfo);
                            out.writeBoolean(found);
                            out.flush();
                        }
                        // [신규 API] 건물 목록 요청
                        if ("GET_BUILDINGS".equals(command)) {
                            List<String> buildings = buildingManager.getBuildingList();
                            out.writeObject(buildings);
                            out.flush();
                        }

                        // [신규 API] 층 목록 요청
                        if ("GET_FLOORS".equals(command)) {
                            String buildingName = in.readUTF();
                            List<String> floors = buildingManager.getFloorList(buildingName);
                            out.writeObject(floors);
                            out.flush();
                        }

                        // [신규 API] 강의실 목록 요청
                        if ("GET_ROOMS".equals(command)) {
                            String buildingName = in.readUTF();
                            String floorName = in.readUTF();
                            List<String[]> rooms = buildingManager.getRoomList(buildingName, floorName);
                            out.writeObject(rooms);
                            out.flush();
                        }

                        // 클라이언트가 이 요청을 보내고 오류가 났으므로, 응답을 추가하여 연결을 유지합니다.
                        if ("GET_WEEKLY_SCHEDULE".equals(command)) {
                            String roomNum = in.readUTF();
                            // 클라이언트가 LocalDate 객체를 보내는지 확인 (주간 현황은 보통 주 시작 날짜를 보냅니다)
                            try {
                                @SuppressWarnings("unchecked")
                                LocalDate monday = (LocalDate) in.readObject(); // 주 시작일 (LocalDate)
                                // ReserveManager.getWeeklySchedule(roomNum, monday) 호출 (ClassCastException 방지 위해 Map 전송)
                                Map<String, List<String[]>> weeklySchedule = ReserveManager.getWeeklySchedule(roomNum, monday);
                                out.writeObject(weeklySchedule);
                            } catch (Exception e) {
                                // 파라미터가 잘못되거나 ReserveManager의 메서드가 없으면 빈 Map 응답
                                System.err.println("GET_WEEKLY_SCHEDULE 처리 중 오류: " + e.getMessage());
                                out.writeObject(new HashMap<String, List<String[]>>());
                            }
                            out.flush();
                        }

                        // "월별 현황 조회" 요청 처리
                        if ("GET_MONTHLY_STATUS".equals(command) || "GET_MONTHLY_RESERVED_DATES".equals(command)) { // <-- 명령 추가
                            System.out.println(">> 월별 현황 조회 명령 수신됨: " + command);

                            // 클라이언트가 보내는 파라미터는 Room, Year, Month 순서여야 합니다.
                            String room = in.readUTF();
                            int year = in.readInt(); // 이 부분에서 int 대신 String(915)을 읽으려다 오류날 수 있음
                            int month = in.readInt();
                            String startTime = in.readUTF();

                            // 템플릿 메서드 호출: "월별로 예약 상태를 조회한다"
                            List<String> result = ReserveManager.getReservationStatusForMonth(room, year, month, startTime);

                            out.writeObject(result);
                            out.flush();
                        }

                        // 클라이언트 요청 - 강의실 조회 state 요청 받는 부분
                        if ("GET_ROOM_STATE".equals(command)) {
                            String room = in.readUTF();
                            String day = in.readUTF();
                            String start = in.readUTF();
                            String end = in.readUTF();
                            String date = in.readUTF(); // "yyyy / MM / dd / HH:mm HH:mm" 형식
                            String state = ReserveManager.getRoomState(room, day, start, end, date);
                            out.writeUTF(state);
                            out.flush();
                        }
                        // 클라이언트 요청 - 강의실 예약 시간대 조회 요청 받는 부분
                        if ("GET_ROOM_SLOTS".equals(command)) {
                            String room = in.readUTF();
                            String day = in.readUTF();
                            List<String[]> slots = ReserveManager.getRoomSlots(room, day);
                            out.writeInt(slots.size());
                            for (String[] slot : slots) {
                                out.writeUTF(slot[0]); // start
                                out.writeUTF(slot[1]); // end
                            }
                            out.flush();
                        }

                        if ("SCHEDULE".equals(command)) {
                            System.out.println(">> SCHEDULE 명령 수신됨");

                            // 클라이언트로부터 ScheduleRequest 객체 수신
                            ScheduleRequest req = (ScheduleRequest) in.readObject();

                            ScheduleResult result; // 클라이언트에게 보낼 응답 객체
                            TimeTableController controller = new TimeTableController(); // 시간표 처리 로직

                            // 클라이언트가 요청한 명령에 따라 분기 처리
                            switch (req.getCommand()) {
                                case "LOAD" -> {
                                    // (1) 해당 년도/학기/건물만 메모리에 로드
                                    controller.loadSchedulesFromFile(
                                            req.getYear(),
                                            req.getSemester(),
                                            req.getBuilding()
                                    );

                                    // (2) 메모리에 올라간 것 중에서 강의실/요일/타입만 골라서 반환
                                    Map<String, String> schedule = controller.getScheduleForRoom(
                                            req.getRoom(),
                                            req.getDay(),
                                            req.getType()
                                    );
                                    result = new ScheduleResult(true, "조회 성공", schedule);
                                }

                                case "ADD" -> {
                                    // 시간표 추가
                                    try {
                                        controller.addScheduleToFile(
                                                req.getYear(), 
                                                req.getSemester(), 
                                                req.getBuilding(), 
                                                req.getRoom(),
                                                req.getDay(),
                                                req.getStart(),
                                                req.getEnd(),
                                                req.getSubject(),
                                                req.getProfessor(), 
                                                req.getType()
                                        );
                                        result = new ScheduleResult(true, "등록 성공", null);
                                    } catch (Exception e) {
                                        result = new ScheduleResult(false, "등록 실패: " + e.getMessage(), null);
                                    }
                                }

                                case "DELETE" -> {
                                    // 시간표 삭제
                                    boolean deleted = controller.deleteScheduleFromFile(
                                            req.getYear(),  
                                            req.getSemester(), 
                                            req.getBuilding(), 
                                            req.getRoom(),
                                            req.getDay(),
                                            req.getStart(),
                                            req.getEnd()
                                    );
                                    result = new ScheduleResult(deleted, deleted ? "삭제 성공" : "삭제 실패", null);
                                }

                                case "UPDATE" -> {
                                    // 시간표 수정
                                    boolean updated = controller.updateSchedule(
                                            req.getYear(),        // 🚨 추가됨
                                            req.getSemester(),    // 🚨 추가됨
                                            req.getBuilding(),    // 🚨 추가됨
                                            req.getRoom(),
                                            req.getDay(),
                                            req.getStart(),
                                            req.getEnd(),
                                            req.getSubject(),
                                            req.getProfessor(),   // 🚨 추가됨
                                            req.getType()
                                    );
                                    result = new ScheduleResult(updated, updated ? "수정 성공" : "수정 실패", null);
                                }

                                default ->
                                    result = new ScheduleResult(false, "알 수 없는 명령입니다", null);
                            }

                            // 처리 결과를 클라이언트로 전송
                            out.writeObject(result);
                            out.flush();
                        }

                        if ("USER".equals(command)) {
                            System.out.println(">> USER 명령 수신됨");

                            try {
                                // 1. 클라이언트로부터 UserRequest 객체 수신
                                UserRequest req = (UserRequest) in.readObject();
                                UserResult result;
                                UserRequestController controller = new UserRequestController();

                                // 2. 명령(command)에 따라 분기 처리
                                String cmd = req.getCommand();

                                if (null == cmd) {
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
                                        default ->
                                            result = new UserResult(false, "알 수 없는 사용자 명령입니다", null);
                                    }
                                }

                                // 3. 결과 전송
                                out.writeObject(result);
                                out.flush();

                            } catch (Exception e) {
                                System.err.println(">> USER 명령 처리 중 오류: " + e.getMessage());
                                e.printStackTrace();

                                // 예외 발생 시 실패 결과 전송
                                UserResult errorResult = new UserResult(false, "서버 처리 오류 발생", null);
                                out.writeObject(errorResult);
                                out.flush();
                            }
                        }

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

                        if ("RESERVE_MANAGE".equals(command)) {
                            System.out.println(">> RESERVE_MANAGE 명령 수신됨");

                            try {
                                // 클라이언트로부터 요청 객체 수신
                                ReserveManageRequest req = (ReserveManageRequest) in.readObject();
                                ReserveManageResult result = null;

                                String cmd = req.getCommand();

                                switch (cmd) {
                                    case "SEARCH" ->
                                        result = ReserveManager.searchUserAndReservations(
                                                req.getUserId(), req.getBuilding(), req.getRoom(), req.getDate()
                                        );
                                    
                                    /* 수정 기능 삭제
                                    case "UPDATE" -> {
                                        // [수정] Builder 패턴 적용
                                        ReservationDetails details = new ReservationDetails.Builder(req.getUserId(), req.getRole())
                                                .oldReserveInfo(req.getOldReserveInfo())
                                                .newRoomNumber(req.getNewRoom())
                                                .newDate(req.getNewDate())
                                                .newDay(req.getNewDay())
                                                .build();
                                        ReserveResult updateRes = ReserveManager.updateReserve(details);
                                        result = new ReserveManageResult(updateRes.getResult(), updateRes.getReason(), null);
                                    }
                                    */

                                    case "DELETE" -> {
                                        ReserveResult deleteRes = ReserveManager.cancelReserve(
                                                req.getUserId(), req.getReserveInfo()
                                        );
                                        result = new ReserveManageResult(deleteRes.getResult(), deleteRes.getReason(), null);
                                    }

                                    // 승인(APPROVE) 및 거절(REJECT) 기능 추가
                                    case "APPROVE" -> {
                                        result = ReserveManager.approveOrReject(
                                                "APPROVE",
                                                req.getUserId(),
                                                req.getOldReserveInfo(),
                                                null
                                        );
                                    }

                                    case "REJECT" -> {
                                        result = ReserveManager.approveOrReject(
                                                "REJECT",
                                                req.getUserId(),
                                                req.getOldReserveInfo(),
                                                req.getReserveInfo()
                                        );
                                    }

                                    default ->
                                        result = new ReserveManageResult(false, "알 수 없는 명령입니다", null);
                                }

                                // 결과 전송 (SEARCH / UPDATE / DELETE)
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

                        // ===========================
                        // 📁 강의실 시간표 백업 요청
                        // ===========================
                        if ("SCHEDULE_BACKUP".equals(command)) {
                            System.out.println(">> SCHEDULE_BACKUP 명령 수신됨");

                            // 클라이언트에서 보낸 백업 파일 이름 받기
                            String backupName = in.readUTF();    // 예: "ScheduleInfo_backup.txt"

                            TimeTableController controller = new TimeTableController();
                            boolean ok = controller.backupSchedule(backupName);

                            ScheduleResult result = new ScheduleResult(
                                    ok,
                                    ok ? "백업 성공" : "백업 실패",
                                    null
                            );
                            out.writeObject(result);
                            out.flush();
                        }

                        // ===========================
                        // 🔄 강의실 시간표 복원 요청
                        // ===========================
                        if ("SCHEDULE_RESTORE".equals(command)) {
                            System.out.println(">> SCHEDULE_RESTORE 명령 수신됨");

                            // 클라이언트에서 보낸 백업 파일 이름 받기
                            String backupName = in.readUTF();    // 예: "ScheduleInfo_backup.txt"

                            TimeTableController controller = new TimeTableController();
                            boolean ok = controller.restoreSchedule(backupName);

                            ScheduleResult result = new ScheduleResult(
                                    ok,
                                    ok ? "복원 성공" : "복원 실패",
                                    null
                            );
                            out.writeObject(result);
                            out.flush();
                        }

                    } catch (IOException e) {
                        System.out.println("Client Connection Error or Terminated. " + e.getMessage());
                        e.printStackTrace();
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Deserialization Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (acquired) {
                server.getConnectionLimiter().release();
            }

            if (id != null) {
                synchronized (server.getLoggedInUsers()) {
                    server.getLoggedInUsers().remove(id); // 로그아웃 처리
                }
                //연결 종료시 알림 구독 해지
                NotificationService.getInstance().removeObserver(this.userId);
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /*
     * private void handleStudent(ObjectInputStream in, ObjectOutputStream out,
     * String id) {
     * System.out.println("학생 기능 처리: " + id);
     * }
     * * private void handleProfessor(ObjectInputStream in, ObjectOutputStream out,
     * String id) {
     * System.out.println("교수 기능 처리: " + id);
     * }
     * * private void handleAdmin(ObjectInputStream in, ObjectOutputStream out, String
     * id) {
     * System.out.println("관리자 기능 처리: " + id);
     * }
     */
}