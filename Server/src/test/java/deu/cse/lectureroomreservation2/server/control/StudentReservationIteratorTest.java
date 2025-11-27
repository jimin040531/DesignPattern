/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.Reservation;
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
 * Iterator 패턴 핵심 검증: StudentReservationIterator 및 StudentReservationList
 */
/**
 * [Concrete Iterator] StudentReservationIterator 순회 로직 검증
 */
public class StudentReservationIteratorTest {

    // 테스트용 Reservation 객체 리스트 생성 헬퍼 함수
    private List<Reservation> createMockReservations(int count) {
        String baseLine = "건물A,901,2025/12/01,월요일,09:00,10:00,S100,S,세미나,1,APPROVED,-";
        List<Reservation> reservations = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            // ID와 강의실을 변경하여 다른 객체임을 표시하고 Reservation 객체로 변환
            String line = baseLine.replace("S100", "S" + (100 + i)).replace("901", "90" + i);
            reservations.add(new Reservation(line));
        }
        return reservations;
    }

    /**
     * [TC-01] 순회(Traversal)의 정확성 검증 (3개 항목)
     */
    @Test
    public void testIteratorAccurateTraversal() {
        System.out.println("========== [TC-01] Iterator 순회의 정확성 검증 (3개 항목) ==========");
        System.out.println("[시나리오] 3개의 항목을 생성하고, hasNext()와 next()가 정확히 3번 동작하는지 검증");

        List<Reservation> mockReservations = createMockReservations(3);
        // StudentReservationIterator는 List<Reservation>을 인자로 받습니다.
        ReservationIterator iterator = new StudentReservationIterator(mockReservations);
        
        int count = 0;
        while (iterator.hasNext()) {
            Reservation currentRes = iterator.next();
            System.out.println(">> 순회 중 ID 확인: " + currentRes.getUserId());
            count++;
        }
        
        assertEquals(3, count, "Iterator는 정확히 3개의 항목을 순회해야 합니다.");
        assertFalse(iterator.hasNext(), "순회 완료 후에는 hasNext()가 false여야 합니다.");
        System.out.println(">> 검증 결과: PASS (총 순회 횟수: " + count + ")\n");
    }

    /**
     * [TC-02] 빈 리스트(Empty List) 처리 검증
     */
    @Test
    public void testIteratorEmptyList() {
        System.out.println("========== [TC-02] 빈 리스트 처리 검증 ==========");
        System.out.println("[시나리오] 데이터가 없는 빈 리스트에 대한 순회 시도시 hasNext()가 즉시 false여야 함");
        
        List<Reservation> emptyReservations = createMockReservations(0);
        ReservationIterator iterator = new StudentReservationIterator(emptyReservations);
        
        assertFalse(iterator.hasNext(), "빈 리스트는 hasNext()가 false여야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }
}