package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;

public class ReserveRequest implements Serializable {
    private String id;
    private String role;
    private String roomNumber;
    private String date;
    private String day;
    
    // [수정] 기존 notice 삭제 -> purpose, userCount 추가
    private String purpose; 
    private int userCount;

    // [수정] 생성자 파라미터 변경 (7개)
    public ReserveRequest(String id, String role, String roomNumber, String date, String day, String purpose, int userCount) {
        this.id = id;
        this.role = role;
        this.roomNumber = roomNumber;
        this.date = date;
        this.day = day;
        this.purpose = purpose;
        this.userCount = userCount;

        System.out.println("클라이언트 ReserveRequest : " + id + " " + role + " " + roomNumber + " " + date + " " + day + " " + purpose + " " + userCount + " 보냄");
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }   

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getDate() {
        return date;
    }

    public String getDay() {
        return day;
    }

    // [수정] Getter 변경
    public String getPurpose() {
        return purpose;
    }

    public int getUserCount() {
        return userCount;
    }
}