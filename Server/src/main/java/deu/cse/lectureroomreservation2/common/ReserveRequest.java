package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;

public class ReserveRequest implements Serializable {
    private String id;
    private String role;
    private String buildingName;
    private String roomNumber;
    private String date;
    private String day;
    private String purpose; 
    private int userCount;
    private String startTime;
    private String endTime;
    
    public ReserveRequest(String id, String role, String buildingName, String roomNumber, String date, String day, String startTime, String endTime, String purpose, int userCount) {
        this.id = id;
        this.role = role;
        this.buildingName = buildingName;
        this.roomNumber = roomNumber;
        this.date = date;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.userCount = userCount;

        System.out.println("클라이언트 ReserveRequest : " + id + " " + role + " " + roomNumber + " " + date + " " + day + " " + startTime + " " + endTime + " " + purpose + " " + userCount + " 보냄");
    }
    
    public String getBuildingName() {
        return buildingName;
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

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
    
    public String getPurpose() {
        return purpose;
    }

    public int getUserCount() {
        return userCount;
    }
}