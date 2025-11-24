/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.client.view;

/**
 * 학기별 시간표 검증 전략을 정의하는 인터페이스
 * 
 * - 학기마다 수업 시간 규칙(길이, 허용 범위 등)이 달라질 수 있으므로
 *   이 인터페이스를 통해 검증 로직을 교체 가능하게 만든다.
 */
public interface SemesterStrategy {

    /**
     * 시간 범위 검증
     *
     * @param start "HH:mm" 형식 시작 시간 (예: "09:00")
     * @param end   "HH:mm" 형식 종료 시간 (예: "09:50")
     * @return null 이면 "검증 통과" (에러 없음),
     *         문자열이면 "에러 메시지" (JOptionPane 등에 띄우면 됨)
     */
    String validateTimeRange(String start, String end);
}
