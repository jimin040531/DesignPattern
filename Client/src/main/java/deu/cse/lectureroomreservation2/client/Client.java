/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package deu.cse.lectureroomreservation2.client;

import deu.cse.lectureroomreservation2.common.ReserveRequest;
import deu.cse.lectureroomreservation2.common.CheckMaxTimeResult;
import deu.cse.lectureroomreservation2.common.CheckMaxTimeRequest;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.LoginStatus;
import deu.cse.lectureroomreservation2.common.ReserveManageRequest;
import deu.cse.lectureroomreservation2.common.ReserveManageResult;
import deu.cse.lectureroomreservation2.common.ScheduleRequest;
import deu.cse.lectureroomreservation2.common.ScheduleResult;
import deu.cse.lectureroomreservation2.common.UserRequest;
import deu.cse.lectureroomreservation2.common.UserResult;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Client는 강의실 예약 시스템에서 뷰와 관련된 행위를 다룬다. 필요시 Server에 요청하여 필요한 작업을 수행할 수 있다.
 *
 * @author Prof.Jong Min Lee
 */
public class Client {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private LoginStatus status;

    public Client(String serverAddress, int serverPort) {
        try {
            this.socket = new Socket(serverAddress, serverPort);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println(" Server Connection Error: " + e.getMessage());
        }
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }

    public ObjectInputStream getInputStream() {
        return in;
    }

    public synchronized void sendLoginRequest(String id, String password, String role) throws IOException {
        out.writeUTF(id);
        out.writeUTF(password);
        out.writeUTF(role);
        out.flush();
    }

    public synchronized LoginStatus receiveLoginStatus() throws IOException, ClassNotFoundException {
        status = (LoginStatus) in.readObject();
        return status;
    }

    public synchronized void logout() {
        try {
            out.writeUTF("LOGOUT");
            out.flush();
            socket.close();
        } catch (IOException e) {
            System.err.println(" logout Error : " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // 예약 요청 처리
    public synchronized ReserveResult sendReserveRequest(String id, String role,
            String buildingName,
            String roomNumber, String date, String day,
            String startTime, String endTime,
            String purpose, int userCount)
            throws IOException, ClassNotFoundException {

        // ReserveRequest 생성자 변경 반영
        ReserveRequest req = new ReserveRequest(id, role,
                buildingName,
                roomNumber,
                date, day,
                startTime, endTime,
                purpose, userCount);
        out.writeUTF("RESERVE");
        out.flush();
        out.writeObject(req);
        out.flush();
        return (ReserveResult) in.readObject();
    }

    /**
     * 특정 강의실/월/시간대에 예약된 날짜(day) 목록을 서버에 요청합니다. 
     */
    @SuppressWarnings("unchecked")
    public synchronized List<String> getMonthlyReservedDates(String buildingName, String roomNum, int year, int month, String startTime)
            throws IOException, ClassNotFoundException {

        out.writeUTF("GET_MONTHLY_RESERVED_DATES");
        out.flush();
        out.writeUTF(buildingName);
        out.writeUTF(roomNum);
        out.flush();
        out.writeInt(year);
        out.flush();
        out.writeInt(month);
        out.flush();
        out.writeUTF(startTime);
        out.flush();

        return (List<String>) in.readObject();
    }

    // 최대 예약 시간 체크 요청 처리
    public synchronized CheckMaxTimeResult sendCheckMaxTimeRequest(String id)
            throws IOException, ClassNotFoundException {
        out.writeUTF("CHECK_MAX_TIME");
        out.flush();
        out.writeObject(new CheckMaxTimeRequest(id));
        out.flush();
        return (CheckMaxTimeResult) in.readObject();
    }

    // 기존 예약 취소 요청 처리
    public synchronized ReserveResult sendCancelReserveRequest(String id, String reserveInfo)
            throws IOException, ClassNotFoundException {
        out.writeUTF("CANCEL_RESERVE");
        out.flush();
        out.writeUTF(id);
        out.flush();
        out.writeUTF(reserveInfo);
        out.flush();
        return (ReserveResult) in.readObject();
    }

    // 예약 변경 요청 처리(사용자 id, 기존 예약 정보, 새로운 강의실 번호, 새로운 날짜, 새로운 요일)
    public synchronized ReserveResult sendModifyReserveRequest(
            String id,
            String oldReserveInfo,
            String buildingName,
            String newRoomNumber,
            String newDate, // "2025 / 06 / 04 / 10:00 11:00" 형태 유지 가정
            String newDay,
            String purpose,
            int userCount,
            String role)
            throws IOException, ClassNotFoundException {

        out.writeUTF("MODIFY_RESERVE");
        out.flush();

        out.writeUTF(id);
        out.flush();
        out.writeUTF(oldReserveInfo);
        out.flush();

        out.writeUTF(buildingName);
        out.flush();

        out.writeUTF(newRoomNumber);
        out.flush();
        out.writeUTF(newDate);
        out.flush();
        out.writeUTF(newDay);
        out.flush();

        out.writeUTF(purpose);
        out.flush();
        out.writeInt(userCount);
        out.flush();

        out.writeUTF(role);
        out.flush();

        return (ReserveResult) in.readObject();
    }

    // 공지사항 수신 및 확인 처리
    public void checkAndShowNotices(javax.swing.JFrame parentFrame) {
        while (true) {
            try {
                List<String> notices = new java.util.ArrayList<>();

                // 1. 서버에 요청 보내기 (이 부분만 동기화하여 다른 요청과 겹치지 않게 함)
                synchronized (this) {
                    try {
                        out.writeUTF("CHECK_NOTICES"); // 서버에 "알림 내놔" 요청
                        out.flush();

                        int count = in.readInt(); // 몇 개나 있는지 개수 수신

                        for (int i = 0; i < count; i++) {
                            notices.add(in.readUTF()); // 개수만큼 알림 내용 수신
                        }
                    } catch (java.net.SocketTimeoutException ste) {
                        // 타임아웃은 무시하고 계속 진행
                    }
                }

                // 2. 받은 알림이 있으면 화면에 표시 (동기화 블록 밖에서 처리)
                for (String noticeText : notices) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JOptionPane.showMessageDialog(
                                parentFrame,
                                noticeText,
                                "실시간 알림",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE
                        );
                    });
                }

                // 3. 잠시 대기 (1~2초) - 너무 자주 물어보면 서버 부하 발생
                Thread.sleep(2000);

            } catch (InterruptedException ie) {
                break; // 스레드 종료 신호 시 루프 탈출
            } catch (IOException e) {
                System.err.println("알림 확인 중 연결 종료됨: " + e.getMessage());
                break;
            }
        }
    }

    // 클라이언트의 예약 정보 조회 요청 처리 (id, room, date 중 하나 이상 조건으로 조회)
    // id: 사용자 ID, room: 강의실 번호, date: 예약 날짜("년 / 월 / 일" 형식, 예: "2025 / 05 / 24")
    // id만 지정하면 해당 사용자의 모든 예약정보 반환
    // room만 지정하면 해당 강의실에 예약한 모든 사용자id/예약정보 반환
    // date만 지정하면 해당 날짜에 예약한 모든 사용자id/예약정보 반환
    // 세 파라미터 중 null이 아닌 조건만 적용됨
    @SuppressWarnings("unchecked")
    public synchronized List<String> retrieveMyReserveInfo(String id, String room, String date)
            throws IOException, ClassNotFoundException {
        out.writeUTF("RETRIEVE_MY_RESERVE_ADVANCED");
        out.flush();
        out.writeObject(id);
        out.flush();
        out.writeObject(room);
        out.flush();
        out.writeObject(date);
        out.flush();
        return (List<String>) in.readObject();
    }

    // 예약 정보로 예약한 총 사용자 수 요청 처리
    public synchronized int requestReserveUserCount(String reserveInfo) throws IOException {
        out.writeUTF("COUNT_RESERVE_USERS");
        out.flush();
        out.writeUTF(reserveInfo);
        out.flush();
        return in.readInt();
    }

    // 예약 정보로 예약한 사용자 id 목록 요청 처리 (6번 기능)
    @SuppressWarnings("unchecked")

    public synchronized List<String> getUserIdsByReserveInfo(String reserveInfo)
            throws IOException, ClassNotFoundException {
        out.writeUTF("GET_USER_IDS_BY_RESERVE");
        out.flush();
        out.writeUTF(reserveInfo);
        out.flush();
        return (List<String>) in.readObject();
    }

    // 예약 정보로 교수 예약 여부 조회 요청 처리
    public synchronized boolean hasProfessorReserve(String reserveInfo) throws IOException {
        out.writeUTF("FIND_PROFESSOR_BY_RESERVE");
        out.flush();
        out.writeUTF(reserveInfo);
        out.flush();
        return in.readBoolean();
    }

    public synchronized ScheduleResult sendScheduleRequest(ScheduleRequest req)
            throws IOException, ClassNotFoundException {
        // 1. 명령 문자열 "SCHEDULE"을 먼저 전송하여 서버 측에서 시간표 관리 관련 요청임을 알림
        out.writeUTF("SCHEDULE");
        out.flush();

        // 2. ScheduleRequest 객체를 직렬화하여 서버로 전송
        out.writeObject(req);
        out.flush();

        // 3. 서버로부터 ScheduleResult 응답 객체를 수신
        return (ScheduleResult) in.readObject();
    }

    // 사용자 관리 요청 전송
    public synchronized UserResult sendUserRequest(UserRequest req) throws IOException, ClassNotFoundException {
        out.writeUTF("USER"); // 사용자 관리 명령 전송
        out.flush();
        out.writeObject(req); // UserRequest 객체 전송
        out.flush();
        return (UserResult) in.readObject(); // 결과 수신
    }

    public synchronized ReserveManageResult sendReserveManageRequest(ReserveManageRequest req)
            throws IOException, ClassNotFoundException {
        out.writeUTF("RESERVE_MANAGE"); // 명령 전송
        out.flush();
        out.writeObject(req); // 객체 직렬화 전송
        out.flush();
        return (ReserveManageResult) in.readObject(); // 결과 수신
    }

    public synchronized String findUserRole(String userId) throws IOException, ClassNotFoundException {
        out.writeUTF("FIND_ROLE");
        out.flush();
        out.writeUTF(userId);
        out.flush();
        return (String) in.readObject();  // 올바른 반환값 타입
    }

    /**
     * 건물 목록 조회
     */
    public synchronized List<String> getBuildingList() throws IOException, ClassNotFoundException {
        out.writeUTF("GET_BUILDINGS");
        out.flush();
        return (List<String>) in.readObject();
    }

    /**
     * 층 목록 조회
     */
    public synchronized List<String> getFloorList(String buildingName) throws IOException, ClassNotFoundException {
        out.writeUTF("GET_FLOORS");
        out.flush();
        out.writeUTF(buildingName);
        out.flush();
        return (List<String>) in.readObject();
    }

    /**
     * 강의실 목록 조회
     */
    public synchronized List<String[]> getRoomList(String buildingName, String floorName) throws IOException, ClassNotFoundException {
        out.writeUTF("GET_ROOMS");
        out.flush();
        out.writeUTF(buildingName);
        out.flush();
        out.writeUTF(floorName);
        out.flush();
        return (List<String[]>) in.readObject();
    }

    /**
     * 주별 현황 API 호출
     */
    public synchronized Map<String, List<String[]>> getWeeklySchedule(String buildingName, String roomNum, LocalDate monday)
            throws IOException, ClassNotFoundException {

        out.writeUTF("GET_WEEKLY_SCHEDULE");
        out.flush();
        out.writeUTF(buildingName);
        out.writeUTF(roomNum);
        out.flush();
        out.writeObject(monday); // LocalDate 객체 전송
        out.flush();

        return (Map<String, List<String[]>>) in.readObject();
    }

    /**
     * 월별 현황 API 호출
     */
    public synchronized Map<Integer, String> getMonthlySchedule(String roomNum, int year, int month)
            throws IOException, ClassNotFoundException {
        out.writeUTF("GET_MONTHLY_SCHEDULE");
        out.flush();
        out.writeUTF(roomNum);
        out.flush();
        out.writeInt(year);
        out.flush();
        out.writeInt(month);
        out.flush();
        return (Map<Integer, String>) in.readObject();
    }

    // 강의실 조회 state 요청 처리
    public synchronized String getRoomState(String buildingName, String room, String day, String start, String end, String date)
            throws IOException {
        out.writeUTF("GET_ROOM_STATE");
        out.flush();
        out.writeUTF(buildingName);
        out.writeUTF(room);
        out.flush();
        out.writeUTF(day);
        out.flush();
        out.writeUTF(start);
        out.flush();
        out.writeUTF(end);
        out.flush();
        out.writeUTF(date);
        out.flush();
        return in.readUTF();
    }

    // 강의실 예약 가능 시간대 조회 요청 처리
    public synchronized java.util.List<String[]> getRoomSlots(String room, String day) throws IOException {
        out.writeUTF("GET_ROOM_SLOTS");
        out.flush();
        out.writeUTF(room);
        out.flush();
        out.writeUTF(day);
        out.flush();
        int size = in.readInt();
        java.util.List<String[]> slots = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            String start = in.readUTF();
            String end = in.readUTF();
            slots.add(new String[]{start, end});
        }
        return slots;
    }

    public synchronized int[] getReservationStats(String buildingName, String room, String date, String startTime) {
        try {
            out.writeUTF("GET_RESERVATION_STATS");
            out.flush();

            // 건물 이름 전송
            out.writeUTF(buildingName);
            out.flush();

            out.writeUTF(room);
            out.flush();
            out.writeUTF(date);
            out.flush();
            out.writeUTF(startTime);
            out.flush();

            return (int[]) in.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new int[]{0, 0};
        }
    }

    /**
     * [신규] 시간표 파일 백업 요청 서버에 "SCHEDULE_BACKUP" 명령과 백업 파일 이름을 보내고, ScheduleResult
     * 로 성공/실패 메시지를 돌려받는다.
     *
     * @param backupName 생성할 백업 파일 이름 (예: "ScheduleInfo_backup.txt")
     */
    public synchronized ScheduleResult sendScheduleBackupRequest(String backupName)
            throws IOException, ClassNotFoundException {

        // 1. 백업 명령 문자열 전송
        out.writeUTF("SCHEDULE_BACKUP");
        out.flush();

        // 2. 백업 파일 이름 전송
        out.writeUTF(backupName);
        out.flush();

        // 3. 서버에서 ScheduleResult 응답 수신
        return (ScheduleResult) in.readObject();
    }

    /**
     * [신규] 시간표 파일 복원 요청 서버에 "SCHEDULE_RESTORE" 명령과 사용할 백업 파일 이름을 보내고,
     * ScheduleResult 로 성공/실패 메시지를 돌려받는다.
     *
     * @param backupName 사용할 백업 파일 이름
     */
    public synchronized ScheduleResult sendScheduleRestoreRequest(String backupName)
            throws IOException, ClassNotFoundException {

        // 1. 복원 명령 문자열 전송
        out.writeUTF("SCHEDULE_RESTORE");
        out.flush();

        // 2. 백업 파일 이름 전송
        out.writeUTF(backupName);
        out.flush();

        // 3. 서버에서 ScheduleResult 응답 수신
        return (ScheduleResult) in.readObject();
    }

    public static void main(String[] args) {
        try {
            Client c = new Client("localhost", 5000);  // 서버 컴퓨터의 IP 주소
            if (c.isConnected()) {
                LoginStatus status = c.receiveLoginStatus();
                c.logout();
            } else {
                System.err.println("Cannot Connect Server.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ReserveManageResult backupReservation(String reservationInfo_backuptxt) {
        try {
            // 1) 서버에 명령 전송
            out.writeUTF("RESERVE_BACKUP");
            out.flush();

            // 2) 백업 파일 이름 전송 (예: "ReservationInfo_backup.txt")
            out.writeUTF(reservationInfo_backuptxt);
            out.flush();

            // 3) 서버에서 결과 객체 수신
            Object obj = in.readObject();
            if (obj instanceof ReserveManageResult) {
                return (ReserveManageResult) obj;
            } else {
                // 예상치 못한 응답 형식
                return new ReserveManageResult(false, "예약 백업: 서버 응답 형식 오류", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 예외가 나도 NPE 안 나게 실패 결과 리턴
            return new ReserveManageResult(false, "예약 백업 중 오류 발생: " + e.getMessage(), null);
        }
    }

    public ReserveManageResult restoreReservation(String reservationInfo_backuptxt) {
        try {
            // 1) 서버에 명령 전송
            out.writeUTF("RESERVE_RESTORE");
            out.flush();

            // 2) 복원에 사용할 백업 파일 이름 전송
            out.writeUTF(reservationInfo_backuptxt);
            out.flush();

            // 3) 서버에서 결과 객체 수신
            Object obj = in.readObject();
            if (obj instanceof ReserveManageResult) {
                return (ReserveManageResult) obj;
            } else {
                return new ReserveManageResult(false, "예약 복원: 서버 응답 형식 오류", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ReserveManageResult(false, "예약 복원 중 오류 발생: " + e.getMessage(), null);
        }
    }
    
    
    public String getServerIp() {
        if (socket != null && socket.getInetAddress() != null) {
            return socket.getInetAddress().getHostAddress();
        }
        return "localhost"; // 기본값
    }
    
}
