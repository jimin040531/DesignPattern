/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.LoginStatus;
import java.util.concurrent.Semaphore;
/**
 *
 * @author rbcks
 */
public class QueuedLoginProxy implements AuthService {
    private final AuthService realSubject; // 진짜 로그인 담당자 (LoginController)
    private final Semaphore limiter;       // 인원 제한기

    // 생성자: 진짜 담당자와 세마포어를 받아서 저장함
    public QueuedLoginProxy(AuthService realSubject, Semaphore limiter) {
        this.realSubject = realSubject;
        this.limiter = limiter;
    }

    @Override
    public LoginStatus authenticate(String id, String password, String selectedRole) {
        try {
            System.out.println("[Proxy] " + id + " 님이 로그인 대기열에 진입했습니다. (현재 인원 초과 시 대기)");
            
            // 1. 입장권 받기 (자리가 없으면 여기서 자리가 날 때까지 멈춤!)
            limiter.acquire(); 

            System.out.println("[Proxy] " + id + " 님의 입장권 확보 완료! 로그인을 진행합니다.");

            // 2. 자리가 확보되면 진짜 담당자에게 로그인 처리를 맡김
            LoginStatus status = realSubject.authenticate(id, password, selectedRole);

            // 3. 만약 비밀번호가 틀려서 로그인이 실패했다면? -> 확보한 자리를 다시 반납해야 함
            if (!status.isLoginSuccess()) {
                limiter.release();
            }
            
            return status;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return new LoginStatus(false, "ERROR", "서버 대기 중 오류가 발생했습니다.");
        }
    }
}
