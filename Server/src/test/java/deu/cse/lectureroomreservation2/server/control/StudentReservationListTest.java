/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.ReservationIterator;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rbcks
 */
/**
 * [Concrete Aggregate] StudentReservationList 집합체 역할 검증
 */
public class StudentReservationListTest {

    // 테스트용 Mock 파일 라인
    private List<String> createMockFileLines(int count) {
        String baseLine = "건물A,901,2025/12/01,월요일,09:00,10:00,S100,S,세미나,1,APPROVED,-";
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            lines.add(baseLine.replace("S100", "S" + (100 + i)));
        }
        return lines;
    }

    /**
     * [TC-03] 데이터 초기화 및 집합체 역할 검증 (Reservation 객체 변환)
     */
    @Test
    public void testDataInitializationAndSize() {
        System.out.println("========== [TC-03] 데이터 초기화 및 집합체 크기 검증 ==========");
        System.out.println("[시나리오] String List 입력 시, 정확한 개수의 Reservation 객체로 변환되어야 함");

        List<String> mockLines = createMockFileLines(5);
        StudentReservationList reservationList = new StudentReservationList(mockLines);
        
        // 이터레이터를 생성하여 내부 리스트의 크기를 간접적으로 확인
        ReservationIterator iterator = reservationList.iterator();
        
        int count = 0;
        while(iterator.hasNext()) {
            iterator.next();
            count++;
        }
        
        assertEquals(5, count, "5개의 라인이 5개의 Reservation 객체로 변환되어야 합니다.");
        System.out.println(">> 검증 결과: PASS (변환된 객체 수: " + count + ")\n");
    }
    
    /**
     * [TC-04] Iterator 팩토리 역할 검증 (올바른 인스턴스 반환)
     */
    @Test
    public void testAggregateReturnsCorrectIterator() {
        System.out.println("========== [TC-04] Iterator 팩토리 역할 검증 ==========");
        System.out.println("[시나리오] iterator() 호출 시, StudentReservationIterator 인스턴스를 반환해야 함");
        
        List<String> mockLines = createMockFileLines(1);
        StudentReservationList reservationList = new StudentReservationList(mockLines);
        
        // 2. Iterator 획득
        ReservationIterator iterator = reservationList.iterator();
        
        // 3. 반환된 객체의 실제 타입 검증
        assertTrue(iterator instanceof StudentReservationIterator, 
                   "반환된 객체는 StudentReservationIterator 타입이어야 합니다.");
        System.out.println(">> 반환된 객체 타입: " + iterator.getClass().getSimpleName());
        System.out.println(">> 검증 결과: PASS\n");
    }
}