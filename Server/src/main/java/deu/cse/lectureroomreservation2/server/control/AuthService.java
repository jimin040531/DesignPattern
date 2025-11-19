/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.LoginStatus;

/**
 * Subject (인터페이스)
 * : RealSubject(실제 로그인)와 Proxy(대기열 처리)가 공통으로 구현할 인터페이스
 */
/**
 *
 * @author rbcks
 */
public interface AuthService {
    LoginStatus authenticate(String id, String password, String selectedRole);
}
