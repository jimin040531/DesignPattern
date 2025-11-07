/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author Jimin
 */

/**
 * 서버가 시간표 요청에 대해 클라이언트에 응답할 때 사용하는 클래스
 * Serializable을 구현하여 네트워크 통신에 사용 가능 
 * 결과 상태 및 상세 데이터를 담음
 * 
 *
 * 주요 필드:
 * - success: 요청 성공 여부
 * - message: 응답 메시지 (성공/실패 사유 포함)
 * - data: 시간표 데이터 (key: 시간대 문자열, value: 과목명 또는 '예약불가' 등 상태)
 *
 * 사용 예 : 특정 강의실의 시간표 조회 결과 반환 시 사용
 *         ex : {"09:00~09:50": "자료구조", "10:00~10:50": "예약불가"}
 * 
 * RoomScheduleManagementView나 TimeTableController에서 처리 결과를 확인하고 UI에 반영
 * 
 */
public class ScheduleResult implements Serializable {

    private final boolean success;
    private final String message;
    private final Map<String, String> data; // 시간:과목명

    public ScheduleResult(boolean success, String message, Map<String, String> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getData() {
        return data;
    }
}
