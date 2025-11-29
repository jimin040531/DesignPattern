package deu.cse.lectureroomreservation2.server.control;

/**
 * Builder Pattern: 예약/예약 변경에 필요한 모든 데이터를 캡슐화하는 클래스
 * 
 */
public class ReservationDetails {
    // --- 사용자 확인 용 ---
    private final String id;
    private final String role;
    // --- 신규 예약 용 ---
    private String buildingName;
    private String roomNumber;
    private String date;    
    private String day;
    private String startTime;
    private String endTime;
    private int userCount;    
    private String purpose;       
    // --- 예약 변경 용 ---
    private String oldReserveInfo; 
    private String newRoomNumber;
    private String newDate;
    private String newDay;
    
    // 필수 파라미터만 받는 생성자
    public ReservationDetails(String id, String role) {
        this.id = id;
        this.role = role;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setDate(String date) {
        this.date = date; 
    }

    public void setDay(String day) {
        this.day = day;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public void setOldReserveInfo(String oldReserveInfo) {
        this.oldReserveInfo = oldReserveInfo;
    }

    public void setNewRoomNumber(String newRoomNumber) {
        this.newRoomNumber = newRoomNumber;
    }

    public void setNewDate(String newDate) {
        this.newDate = newDate;
    }

    public void setNewDay(String newDay) {
        this.newDay = newDay;
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getBuildingName() {
        return buildingName;
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
    
    public int getUserCount() {
        return userCount;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getOldReserveInfo() {
        return oldReserveInfo;
    }

    public String getNewRoomNumber() {
        return newRoomNumber;
    }

    public String getNewDate() {
        return newDate;
    }

    public String getNewDay() {
        return newDay;
    }
}