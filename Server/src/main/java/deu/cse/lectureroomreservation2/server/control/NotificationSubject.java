/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author rbcks
 */
public interface NotificationSubject {
    // 관찰자 등록 (로그인 시)
    void registerObserver(String userId, Observer observer);
    
    // 관찰자 해지 (로그아웃 시)
    void removeObserver(String userId);
    
    // 알림 전송
    void notifyObserver(String userId, String message);
}
