package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;

public class ReserveRequest implements Serializable {
    private String id;
    private String role;
    private String roomNumber;
    private String date;
    private String day;
    private String notice;

    public ReserveRequest(String id, String role, String roomNumber, String date, String day, String notice) {
        this.id = id;
        this.role = role;
        this.roomNumber = roomNumber;
        this.date = date;
        this.day = day;
        this.notice = notice;

        System.out.println("클라이언트 ReserveRequest : " + id + " " + role + " " + roomNumber + " " + date + " " + day + " " + notice + " 보냄");
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
    public String getNotice() {
        return notice;
    }
}