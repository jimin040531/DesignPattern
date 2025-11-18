package deu.cse.lectureroomreservation2.server.control;

/**
 * Builder Pattern: 예약/예약 변경에 필요한 모든 데이터를 캡슐화하는 클래스
 * (SFR-206: 교수는 '목적', 학생은 '인원' 등 옵션이 다르므로 Builder가 유용함)
 */
public class ReservationDetails {

    // --- 필수 ---
    private final String id;
    private final String role;

    // --- '신규 예약'용 ---
    private final String roomNumber;
    private final String date;
    private final String day;

    // --- '예약 변경'용 ---
    private final String oldReserveInfo;
    private final String newRoomNumber;
    private final String newDate;
    private final String newDay;
    
    // (향후 SFR-204, 206에 따라 추가될 필드)
    // private final int numberOfStudents; // SFR-204
    // private final String purpose;         // SFR-206

    // private 생성자 (Builder를 통해서만 생성)
    private ReservationDetails(Builder builder) {
        this.id = builder.id;
        this.role = builder.role;
        this.roomNumber = builder.roomNumber;
        this.date = builder.date;
        this.day = builder.day;
        this.oldReserveInfo = builder.oldReserveInfo;
        this.newRoomNumber = builder.newRoomNumber;
        this.newDate = builder.newDate;
        this.newDay = builder.newDay;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getRole() { return role; }
    public String getRoomNumber() { return roomNumber; }
    public String getDate() { return date; }
    public String getDay() { return day; }
    public String getOldReserveInfo() { return oldReserveInfo; }
    public String getNewRoomNumber() { return newRoomNumber; }
    public String getNewDate() { return newDate; }
    public String getNewDay() { return newDay; }

    
    /**
     * The Builder Class
     */
    public static class Builder {
        // 필수 파라미터
        private final String id;
        private final String role;

        // 선택적 파라미터
        private String roomNumber;
        private String date;
        private String day;
        private String oldReserveInfo;
        private String newRoomNumber;
        private String newDate;
        private String newDay;

        // 필수 파라미터는 Builder 생성자에서 받음
        public Builder(String id, String role) {
            this.id = id;
            this.role = role;
        }

        // --- '신규 예약'용 메서드 ---
        public Builder roomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public Builder day(String day) {
            this.day = day;
            return this;
        }

        // --- '예약 변경'용 메서드 ---
        public Builder oldReserveInfo(String oldReserveInfo) {
            this.oldReserveInfo = oldReserveInfo;
            return this;
        }
        
        public Builder newRoomNumber(String newRoomNumber) {
            this.newRoomNumber = newRoomNumber;
            return this;
        }

        public Builder newDate(String newDate) {
            this.newDate = newDate;
            return this;
        }

        public Builder newDay(String newDay) {
            this.newDay = newDay;
            return this;
        }
        
        // --- (향후 SFR-204, 206에 따라 추가될 메서드) ---
        // public Builder numberOfStudents(int num) { ... }
        // public Builder purpose(String p) { ... }

        // build() 메서드
        public ReservationDetails build() {
            return new ReservationDetails(this);
        }
    }
}