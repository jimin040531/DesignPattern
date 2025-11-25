/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author rbcks
 */
/**
 * Template Method Pattern
 * 강의실 시간표 상태 확인의 전체 흐름(알고리즘)을 정의하는 추상 클래스
 */
public abstract class AbstractReservationChecker {
    /**
     * [Template Method]
     * 상태 확인 순서를 결정합니다. (수정 불가 final)
     * 1. 정규 수업이 있는가? -> "CLASS" (과목명 등 상세 정보는 구현체에서 처리 가능)
     * 2. 승인/대기 중인 예약이 있는가? -> "RESERVED" (예약자 정보 등은 구현체에서 처리 가능)
     * 3. 둘 다 없으면 -> "AVAILABLE"
     */
    public final String checkStatus(String room, String date, String day, String time) {
        
        // 1. 정규 수업 체크 (ScheduleInfo.txt)
        // 날짜(date) 정보를 추가로 전달하여 학기를 구분할 수 있게 함
        if (hasRegularClass(room, day, time, date)) {
            return "CLASS"; 
        }

        // 2. 예약 현황 체크 (ReservationInfo.txt)
        // 거절(REJECTED)된 건 무시하고, 승인(APPROVED)이나 대기(WAIT)만 체크
        if (hasActiveReservation(room, date, time)) {
            return "RESERVED"; // 예약 있음 (노란색/빨간색 등)
        }

        // 3. 아무것도 없음
        return "AVAILABLE"; // 예약 가능 (초록색)
    }

    // 하위 클래스에서 구체적으로 구현할 메서드 (Hooks)
    protected abstract boolean hasRegularClass(String room, String day, String time, String date);
    protected abstract boolean hasActiveReservation(String room, String date, String time);
}
