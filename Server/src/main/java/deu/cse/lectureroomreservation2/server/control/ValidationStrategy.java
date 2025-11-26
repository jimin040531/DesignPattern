/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author rbcks
 */
public interface ValidationStrategy {
    /**
     * 시스템 상태를 검사하고 결과를 문자열로 반환
     * @return 정상 작동 시 "SUCCESS", 문제 발생 시 에러 메시지
     */
    String validate();
}
