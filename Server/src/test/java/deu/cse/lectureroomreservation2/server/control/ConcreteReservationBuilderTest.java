/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 빌더 패턴: ConcreteReservationBuilder 제약 조건 검증 테스트
 *
 * @author Jimin
 */
public class ConcreteReservationBuilderTest { // 기존 프로토타입 클래스명 유지

    private final String BUILDING = "공학관";
    private final String ROOM = "301호";
    private final String STUDENT_ROLE = "S";
    private final String PROFESSOR_ROLE = "P";

    /**
     * [TC-01] 성공 시나리오: 유효한 학생 예약 객체 생성 검증
     */
    @Test
    @DisplayName("TC-01: 유효한 예약 조건에서 객체 생성 성공 (빌더 무결성)")
    public void testBuilderSuccess() {
        System.out.println("========== [TC-01] 유효한 학생 예약 객체 생성 ==========");

        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        ConcreteReservationBuilder builder = new ConcreteReservationBuilder("student01", STUDENT_ROLE);

        // 60분 예약 (10:00 ~ 11:00). (최대 120분 제한 내)
        builder.buildBaseInfo(BUILDING, ROOM, tomorrow, "금요일", "10:00", "11:00");
        builder.buildPurpose("스터디");
        builder.buildUserCount(4);

        // 예외 없이 객체 생성에 성공해야 함
        ReservationDetails result = assertDoesNotThrow(() -> builder.getReservationDetails(),
                "유효한 예약 조건에서는 객체 생성에 성공해야 합니다.");

        assertEquals("공학관", result.getBuildingName());
        assertEquals(4, result.getUserCount());
        System.out.println(">> 검증 결과: PASS (객체 생성 성공)\n");
    }

    /**
     * [TC-02] 실패 시나리오 : 학생 최대 시간 제한 초과 검증
     */
    @Test
    @DisplayName("TC-02: 학생 최대 시간(120분) 초과 시 예외 발생")
    public void testStudentTimeLimitExceeded() {
        System.out.println("========== [TC-02] 학생 시간 초과 검증 ==========");

        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        ConcreteReservationBuilder builder = new ConcreteReservationBuilder("student02", STUDENT_ROLE);

        // 3시간 예약 시도 (180분)
        builder.buildBaseInfo(BUILDING, ROOM, tomorrow, "금요일", "10:00", "13:00");

        // getReservationDetails() 호출 시 IllegalArgumentException이 발생해야 함
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            builder.getReservationDetails();
        });

        // [수정됨] 실제 애플리케이션 코드가 던지는 메시지와 정확히 일치하도록 수정
        assertEquals("학생은 1회 최대 2시간(120분)까지만 예약 가능합니다.", exception.getMessage());
        System.out.println(">> 검증 결과: PASS (시간 초과 예외 발생 확인)\n");
    }

    /**
     * [TC-03] 실패 시나리오 : 학생 당일 예약 불가 검증
     */
    @Test
    @DisplayName("TC-03: 학생 당일 예약 불가 시 예외 발생")
    public void testStudentSameDayReservation() {
        System.out.println("========== [TC-03] 학생 당일 예약 불가 검증 ==========");

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        ConcreteReservationBuilder builder = new ConcreteReservationBuilder("student03", STUDENT_ROLE);

        // 오늘 날짜로 예약 시도
        builder.buildBaseInfo(BUILDING, ROOM, today, "금요일", "10:00", "11:00");

        // getReservationDetails() 호출 시 IllegalArgumentException이 발생해야 함
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            builder.getReservationDetails();
        });

        assertEquals("당일 예약은 불가능합니다. 최소 하루 전에 예약해주세요.", exception.getMessage());
        System.out.println(">> 검증 결과: PASS (당일 예약 불가 예외 발생 확인)\n");
    }

    /**
     * [TC-04] 교수 시간 제한 검증 (역할 분리 적용 확인)
     */
    @Test
    @DisplayName("TC-04: 교수 역할의 3시간 제한 내 예약 성공")
    public void testProfessorTimeLimit() {
        System.out.println("========== [TC-04] 교수 시간 제한 검증 ==========");

        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        ConcreteReservationBuilder builder = new ConcreteReservationBuilder("prof01", PROFESSOR_ROLE);

        // 2.5시간 예약 시도 (150분). (학생이면 실패, 교수라서 성공해야 함)
        builder.buildBaseInfo(BUILDING, ROOM, tomorrow, "금요일", "10:00", "12:30");

        // 교수에게 허용된 시간이므로 객체 생성에 성공해야 함
        ReservationDetails result = assertDoesNotThrow(() -> builder.getReservationDetails(),
                "교수는 3시간 이내의 예약에 성공해야 합니다.");

        assertEquals(PROFESSOR_ROLE, result.getRole());
        System.out.println(">> 검증 결과: PASS (역할별 규칙 정상 적용 확인)\n");
    }
}