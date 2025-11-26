package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult; // 실제 common 패키지의 클래스를 import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// -----------------------------------------------------
// 1. ReservationDetails 클래스 정의 (Builder Pattern Product)
//    - 테스트를 위해 ReserveManager와 같은 패키지에 있다고 가정합니다.
// -----------------------------------------------------
class ReservationDetails {
    private final String role;
    private final String id;
    private final String roomNumber;
    private final String date;
    private final String day;
    private final String purpose;
    private final int userCount;
    private final String buildingName = "공학관"; // 테스트용 더미

    private ReservationDetails(Builder builder) {
        this.role = builder.role;
        this.id = builder.id;
        this.roomNumber = builder.roomNumber;
        this.date = builder.date;
        this.day = builder.day;
        this.purpose = builder.purpose;
        this.userCount = builder.userCount;
    }

    public String getRole() { return role; }
    public String getRoomNumber() { return roomNumber; }
    public String getBuildingName() { return buildingName; }
    public String getId() { return id; }
    public String getDate() { return date; }
    public String getDay() { return day; }
    public String getPurpose() { return purpose; }
    public int getUserCount() { return userCount; }

    public static class Builder {
        private final String id;
        private final String role;
        private String roomNumber;
        private String date;
        private String day;
        private int userCount = 1;
        private String purpose = "-";

        public Builder(String id, String role) {
            this.id = id;
            this.role = role;
        }

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
        public Builder userCount(int userCount) {
            this.userCount = userCount;
            return this;
        }
        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        public ReservationDetails build() {
            return new ReservationDetails(this);
        }
    }
}

// -----------------------------------------------------
// 2. ReserveManager (Context) 전략 패턴 테스트
// -----------------------------------------------------

/**
 * Strategy Pattern의 Context 역할을 하는 ReserveManager의 전략 선택 및 위임 로직을 검증합니다.
 */
public class ReserveManagerStrategyTest {

    private ReservationDetails professorDetails;
    private ReservationDetails studentDetails;
    private ReservationDetails unknownDetails;

    @BeforeEach
    void setUp() {
        // [수정] Strategy 내부 로직이 기대하는 포맷으로 데이터를 설정하여 IndexOutOfBounds 오류 해결
        
        // 교수 역할 (P) - ProfessorReservation 전략을 기대
        professorDetails = new ReservationDetails.Builder("P12345", "P")
            .roomNumber("908")
            .date("2025 / 12 / 01 / 10:00 10:50") // 예상되는 포맷: yyyy / MM / dd / HH:mm HH:mm
            .day("월요일")
            .purpose("세미나")
            .userCount(10)
            .build();
        
        // 학생 역할 (S) - StudentReservation 전략을 기대
        studentDetails = new ReservationDetails.Builder("S20230001", "S")
            .roomNumber("915")
            .date("2025 / 12 / 01 / 11:00 11:50") // 예상되는 포맷
            .day("월요일")
            .purpose("스터디")
            .userCount(5)
            .build();

        // 알 수 없는 역할 (X) - 오류 처리를 기대
        unknownDetails = new ReservationDetails.Builder("X999", "X")
            .roomNumber("101")
            .date("2025 / 12 / 01 / 12:00 12:50") // 포맷 통일
            .day("월요일")
            .purpose("테스트")
            .userCount(1)
            .build();
    }

    /**
     * [TC-Strategy-01] 교수 역할(P) 선택 및 실행 위임 검증
     */
    @Test
    void testReserve_SelectsProfessorStrategy() {
        // Context 메서드 호출
        ReserveResult result = ReserveManager.reserve(professorDetails);
        
        System.out.println("[TC-Strategy-01] Context: 교수 전략 선택 테스트 (Role: P)");
        System.out.println(">> Context가 ProfessorReservation 전략을 선택하고 실행을 위임했습니다.");
        System.out.println(">> 최종 결과 메시지: " + result.getReason()); 
        
        // Strategy가 성공했는지 검증
        assertTrue(result.getResult(), "교수 예약 전략 실행은 성공해야 합니다.");
        assertTrue(result.getReason().contains("예약 요청이 완료되었습니다."), "Context는 전략으로부터 성공 메시지를 전달받아야 합니다.");
        System.out.println("검증 완료: Context가 올바른 전략을 선택하고 성공적으로 위임했습니다.");
    }

    /**
     * [TC-Strategy-02] 학생 역할(S) 선택 및 실행 위임 검증
     */
    @Test
    void testReserve_SelectsStudentStrategy() {
        // Context 메서드 호출
        ReserveResult result = ReserveManager.reserve(studentDetails);

        System.out.println("[TC-Strategy-02] Context: 학생 전략 선택 테스트 (Role: S)");
        System.out.println(">> Context가 StudentReservation 전략을 선택하고 실행을 위임했습니다.");
        System.out.println(">> 최종 결과 메시지: " + result.getReason());

        // Strategy가 성공했는지 검증
        assertTrue(result.getResult(), "학생 예약 전략 실행은 성공해야 합니다.");
        assertTrue(result.getReason().contains("예약 요청이 완료되었습니다."), "Context는 전략으로부터 성공 메시지를 전달받아야 합니다.");
        System.out.println("검증 완료: Context가 올바른 전략을 선택하고 성공적으로 위임했습니다.");
    }
    
    /**
     * [TC-Strategy-03] 알 수 없는 역할에 대한 오류 처리 검증
     */
    @Test
    void testReserve_UnknownRoleReturnsError() {
        // Context 메서드 호출
        ReserveResult result = ReserveManager.reserve(unknownDetails);

        System.out.println("[TC-Strategy-03] Context: 알 수 없는 역할 오류 처리 테스트 (Role: X)");
        System.out.println(">> Context가 전략 선택 없이 오류를 반환했습니다.");
        System.out.println(">> 최종 결과 메시지: " + result.getReason());

        // 오류 반환 검증
        assertFalse(result.getResult(), "알 수 없는 역할은 예약 실패로 처리되어야 합니다.");
        assertEquals("알 수 없는 역할", result.getReason(), "오류 메시지가 '알 수 없는 역할'이어야 합니다.");
        System.out.println("✅ 검증 완료: Context는 알 수 없는 역할에 대해 올바른 오류 처리를 수행했습니다.");
    }
}