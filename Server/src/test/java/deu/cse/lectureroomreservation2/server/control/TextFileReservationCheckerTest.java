/*
package deu.cse.lectureroomreservation2.server.control;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TextFileReservationCheckerTest {
    
  // 템플릿 메서드 테스트를 위한 구체 클래스 객체 생성
    AbstractReservationChecker checker = new TextFileReservationChecker();

    /**
     * [TC-01] 정규 수업(RegularClass) 우선순위 테스트
     */
  /* @Test
   public void testCheckStatusRegularClass() {
        System.out.println("========== [TC-01] 정규 수업 우선순위 검증 ==========");
        System.out.println("[시나리오] 정규 수업이 있는 시간에는 예약이 있더라도 'CLASS' 반환");
        System.out.println("[입력 데이터] 강의실: 908 | 날짜: 2025/06/02(월) | 시간: 09:00");
        
        String result = checker.checkStatus("908", "2025/06/02", "월요일", "09:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("CLASS", result, "정규 수업이 있으면 CLASS를 반환해야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }

    /**
     * [TC-02] 예약 승인(APPROVED) 상태 테스트
     */
    /*@Test
    public void testCheckStatusActiveReservationApproved() {
        System.out.println("========== [TC-02] 예약 승인(APPROVED) 검증 ==========");
        System.out.println("[시나리오] 수업이 없고 승인된 예약이 있는 경우 'APPROVED' 반환");
        System.out.println("[입력 데이터] 강의실: 101 | 날짜: 2025/06/12(월) | 시간: 15:00");
        
        String result = checker.checkStatus("101", "2025/06/12", "월요일", "15:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("APPROVED", result, "승인된 예약은 APPROVED 상태여야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }

    /**
     * [TC-03] 예약 거절(REJECTED) 상태 테스트 (핵심 로직)
     */
    /*@Test
    public void testCheckStatusActiveReservationRejected() {
        System.out.println("========== [TC-03] 예약 거절(REJECTED) 검증 ==========");
        System.out.println("[시나리오] 거절된 예약은 '없는 예약'으로 간주하여 'AVAILABLE' 반환");
        System.out.println("[입력 데이터] 강의실: 912 | 날짜: 2025/06/10(화) | 시간: 09:00");
        
        String result = checker.checkStatus("912", "2025/06/10", "화요일", "09:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("AVAILABLE", result, "거절된 예약은 AVAILABLE로 처리되어야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }
    
    /**
     * [TC-04] 빈 강의실 테스트
     */
    /*@Test
    public void testCheckStatusAvailable() {
        System.out.println("========== [TC-04] 빈 강의실 검증 ==========");
        System.out.println("[시나리오] 수업도 없고 예약도 없는 경우 'AVAILABLE' 반환");
        System.out.println("[입력 데이터] 강의실: 908 | 날짜: 2025/12/25(목) | 시간: 12:00");
        
        String result = checker.checkStatus("908", "2025/12/25", "목요일", "12:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("AVAILABLE", result, "빈 강의실은 AVAILABLE이어야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }

    /**
     * [TC-05] 예약 대기(WAIT) 상태 테스트
     */
   /* @Test
    public void testCheckStatusActiveReservationWait() {
        System.out.println("========== [TC-05] 예약 대기(WAIT) 검증 ==========");
        System.out.println("[시나리오] 대기 중인 예약도 유효한 예약 상태('WAIT')를 반환");
        System.out.println("[입력 데이터] 강의실: 915 | 날짜: 2025/11/27(목) | 시간: 14:00");
        
        String result = checker.checkStatus("915", "2025/11/27", "목요일", "14:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("WAIT", result, "대기 중인 예약은 WAIT 상태여야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }
}*/
