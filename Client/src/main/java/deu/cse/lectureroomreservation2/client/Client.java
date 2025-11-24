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
    public synchronized ReserveResult sendReserveRequest(String id, String role, String roomNumber, 
            String date, String day, 
            String purpose, int userCount) // <--- 여기 파라미터가 변경되었습니다!
            throws IOException, ClassNotFoundException {
        
        // [수정] 변경된 ReserveRequest 생성자 호출 (7개 파라미터)
        ReserveRequest req = new ReserveRequest(id, role, roomNumber, date, day, purpose, userCount);
        
        // 서버에 예약 명령 전송
        out.writeUTF("RESERVE");
        out.flush();
        out.writeObject(req);
        out.flush();
        // 서버로부터 결과 수신
        return (ReserveResult) in.readObject();
    }

    /**
     * [신규] 특정 강의실/월/시간대에 예약된 날짜(day) 목록을 서버에 요청합니다. (ObjectStream 버전에 맞게 수정됨)
     */
    public synchronized List<String> getMonthlyReservedDates(String roomNum, int year, int month, String startTime)
            throws IOException, ClassNotFoundException {

        // 1. 서버에 보낼 요청 프로토콜 (명령어)
        out.writeUTF("GET_MONTHLY_RESERVED_DATES");
        out.flush();

        // 2. 파라미터 전송
        out.writeUTF(roomNum);
        out.flush();
        out.writeInt(year);
        out.flush();
        out.writeInt(month);
        out.flush();
        out.writeUTF(startTime);
        out.flush();

        // 3. 서버로부터 Set<Integer> 객체 자체를 수신
        // (서버는 {3, 5, 10} 같은 Set 객체를 new HashSet<>()으로 만들어
        //  out.writeObject()로 보내야 합니다.)
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

    // 클라이언트에서 예약 취소 요청 사용 예시, 응답 예시
    /*
     * String id = "20212991";
     * String reserveInfo = "915 / 2025 / 06 / 03 / 09:00 10:00 / 화요일";
     * 
     * ReserveResult result = client.sendCancelReserveRequest(id, reserveInfo);
     * 
     * if (result.getResult()) {
     * System.out.println("예약 취소 성공: " + result.getReason());
     * } else {
     * System.out.println("예약 취소 실패: " + result.getReason());
     * }
     * // 응답 예시
     * //예약 취소 성공: 예약이 취소되었습니다.
     * //예약 취소 실패: 해당 예약 정보를 찾을 수 없습니다.
     */
    // 예약 변경 요청 처리(사용자 id, 기존 예약 정보, 새로운 강의실 번호, 새로운 날짜, 새로운 요일)
    public synchronized ReserveResult sendModifyReserveRequest(String id, String oldReserveInfo, String newRoomNumber,
            String newDate, String newDay, String role)
            throws IOException, ClassNotFoundException {
        out.writeUTF("MODIFY_RESERVE");
        out.flush();
        out.writeUTF(id);
        out.flush();
        out.writeUTF(oldReserveInfo);
        out.flush();
        out.writeUTF(newRoomNumber);
        out.flush();
        out.writeUTF(newDate);
        out.flush();
        out.writeUTF(newDay);
        out.flush();
        out.writeUTF(role);
        out.flush();
        return (ReserveResult) in.readObject();
    }

    // 클라이언트에서 예약 변경 요청 사용 예시, 응답 예시
    /*
     * // 예약 변경 요청 예시
     * String id = "20212991";
     * String oldReserveInfo = "915 / 2025 / 06 / 03 / 09:00 10:00 / 화요일";
     * String newRoomNumber = "916";
     * String newDate = "2025 / 06 / 04 / 10:00 11:00";
     * String newDay = "수요일";
     * String role = "S"; // 학생이면 "S", 교수면 "P"
     * 
     * ReserveResult result = client.sendModifyReserveRequest(id, oldReserveInfo,
     * newRoomNumber, newDate, newDay, role);
     * 
     * if (result.getResult()) {
     * System.out.println("예약 변경 성공: " + result.getReason());
     * } else {
     * System.out.println("예약 변경 실패: " + result.getReason());
     * }
     * // 응답 예시
     * //예약 변경 성공: 예약 성공
     * //예약 변경 실패: 해당 예약 정보를 찾을 수 없습니다.
     */
    // 공지사항 수신 및 확인 처리
    public synchronized void checkAndShowNotices(javax.swing.JFrame parentFrame) throws IOException {
        while (true) {
            String msgType = in.readUTF();
            if ("NOTICE_END".equals(msgType)) {
                break;
            }
            if ("NOTICE".equals(msgType)) {
                String noticeText = in.readUTF();
                javax.swing.JOptionPane.showMessageDialog(parentFrame, noticeText, "공지사항",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
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

    // 클라이언트에서 사용예시, 응답예시
    /*
     * // id만 지정
     * List<String> myReserves = client.retrieveMyReserveInfo("20212991", null,
     * null);
     * // room만 지정
     * List<String> roomReserves = client.retrieveMyReserveInfo(null, "915", null);
     * // date만 지정
     * List<String> dateReserves = client.retrieveMyReserveInfo(null, null,
     * "2025 / 06 / 03");
     * 
     * for (String reserve : myReserves) {
     * System.out.println(reserve);
     * }
     * // 예시 출력
     * // id만 지정하고 나머지는 null인 경우
     *  915 / 2025 / 06 / 03 / 09:00 10:00 / 화요일
     * 
     * // room만 지정하거나 date만 지정하고 나머지는 null인 경우
     * // 20212991 / 915 / 2025 / 06 / 03 / 09:00 10:00 / 화요일
     * // 20212991 / 916 / 2025 / 06 / 04 / 10:00 11:00 / 수요일
     */
    // 예약 정보로 예약한 총 사용자 수 요청 처리
    public synchronized int requestReserveUserCount(String reserveInfo) throws IOException {
        out.writeUTF("COUNT_RESERVE_USERS");
        out.flush();
        out.writeUTF(reserveInfo);
        out.flush();
        return in.readInt();
    }

    // 클라이언트에서 사용예시, 응답예시
    /*
     * String reserveInfo = "915 / 2025 / 05 / 21 / 00:00 01:00 / 화요일";
     * int userCount = client.requestReserveUserCount(reserveInfo);
     * System.out.println("해당 예약 정보로 예약한 사용자 수: " + userCount);
     */

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

    // 사용 예시
    /*
     * String reserveInfo = "915 / 2025 / 05 / 21 / 00:00 01:00 / 화요일";
     * List<String> userIds = client.getUserIdsByReserveInfo(reserveInfo);
     * for (String userId : userIds) {
     * System.out.println("예약자 ID: " + userId);
     * }
     */

    // 예약 정보로 교수 예약 여부 조회 요청 처리
    public synchronized boolean hasProfessorReserve(String reserveInfo) throws IOException {
        out.writeUTF("FIND_PROFESSOR_BY_RESERVE");
        out.flush();
        out.writeUTF(reserveInfo);
        out.flush();
        return in.readBoolean();
    }

    // 클라이언트에서 사용예시, 응답예시
    /*
     * String reserveInfo = "915 / 2025 / 06 / 03 / 00:00 01:00 / 화요일";
     * boolean isReservedByProfessor = client.hasProfessorReserve(reserveInfo);
     * 
     * if (isReservedByProfessor) {
     * System.out.println("해당 시간대에 교수 예약이 있습니다.");
     * } else {
     * System.out.println("해당 시간대에 교수 예약이 없습니다.");
     * }
     */
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
     * [신규] 건물 목록 조회
     */
    public synchronized List<String> getBuildingList() throws IOException, ClassNotFoundException {
        out.writeUTF("GET_BUILDINGS");
        out.flush();
        return (List<String>) in.readObject();
    }

    /**
     * [신규] 층 목록 조회
     */
    public synchronized List<String> getFloorList(String buildingName) throws IOException, ClassNotFoundException {
        out.writeUTF("GET_FLOORS");
        out.flush();
        out.writeUTF(buildingName);
        out.flush();
        return (List<String>) in.readObject();
    }

    /**
     * [신규] 강의실 목록 조회 (반환 타입: List<String[]>)
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
     * [신규] 주별 현황 API 호출
     */
    public synchronized Map<String, List<String[]>> getWeeklySchedule(String roomNum, LocalDate monday)
            throws IOException, ClassNotFoundException {

        out.writeUTF("GET_WEEKLY_SCHEDULE");
        out.flush();
        out.writeUTF(roomNum);
        out.flush();
        out.writeObject(monday); // LocalDate 객체 전송
        out.flush();

        return (Map<String, List<String[]>>) in.readObject();
    }

    /**
     * [신규] 월별 현황 API 호출
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
    public synchronized String getRoomState(String room, String day, String start, String end, String date)
            throws IOException {
        out.writeUTF("GET_ROOM_STATE");
        out.flush();
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

    // 클라이언트에서 사용예시, 응답예시
    /*
     * String room = "908";
     * String day = "월";
     * String start = "09:00";
     * String end = "09:50";
     * String date = "2025 / 05 / 21 / 09:00 09:50";
     * 
     * String state = client.getRoomState(room, day, start, end, date);
     * 
     * System.out.println("해당 시간대 상태: " + state);
     * 
     * // 응답 예시
     * // 정규수업, 교수예약, 예약 가능, 예약 초과
     */

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
    
    // ---------------------------------------------------------
    // 예약 현황 통계 요청 (강의실, 날짜, 시작시간)
    // ---------------------------------------------------------
    public synchronized int[] getReservationStats(String room, String date, String startTime) {
        try {
            // 1. 서버와 약속된 명령어 전송
            out.writeUTF("GET_RESERVATION_STATS"); 
            out.flush();

            // 2. 파라미터 전송 (순서 중요: 방 -> 날짜 -> 시간)
            out.writeUTF(room);
            out.flush();
            out.writeUTF(date);
            out.flush();
            out.writeUTF(startTime);
            out.flush();
            
            // 3. 서버로부터 결과 수신 (int배열: [현재인원, 최대인원])
            // readObject()로 받은 뒤 캐스팅합니다.
            return (int[]) in.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            // 통신 오류 시 기본값 [0, 0] 반환하여 에러 방지
            return new int[]{0, 0}; 
        }
    }

    // 클라이언트에서 사용예시, 응답예시
    /*
     * java.util.List<String[]> slots = client.getRoomSlots(selectedRoom,
     * dayOfWeek);
     * for (String[] slot : slots) {
     * String start = slot[0];
     * String end = slot[1];
     * // ...
     * }
     */
    
    
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

}

