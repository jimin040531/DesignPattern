/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Jimin
 */

/**
 * 서버에서 클라이언트로 예약 관리 요청 처리 결과를 반환하는 객체
 * 성공 여부, 메시지, 예약 내역 리스트를 포함 함
 */
public class ReserveManageResult implements Serializable {

    private final boolean success;          // 요청 성공 여부
    private final String message;           // 결과 메시지
    private final List<String[]> reserveList; // 예약 내역 리스트 (검색 결과 등)

    public ReserveManageResult(boolean success, String message) {
        this(success, message, null);
    }

    public ReserveManageResult(boolean success, String message, List<String[]> reserveList) {
        this.success = success;
        this.message = message;
        this.reserveList = reserveList;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String[]> getReserveList() {
        return reserveList;
    }
}
