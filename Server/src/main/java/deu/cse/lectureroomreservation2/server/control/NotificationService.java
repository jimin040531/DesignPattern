/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package deu.cse.lectureroomreservation2.server.control;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
/**
 *
 * @author rbcks
 */
public class NotificationService implements NotificationSubject {
    private static NotificationService instance;
    private final Map<String, Observer> activeUsers = new ConcurrentHashMap<>();
    // 로그인 중인 사용자(Observer) 목록

    private NotificationService(){}

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    @Override
    public void registerObserver(String userId, Observer observer) {
        activeUsers.put(userId, observer);
        System.out.println(">> [알림 서비스] 사용자 등록됨: " + userId);
    }

    @Override
    public void removeObserver(String userId) {
        activeUsers.remove(userId);
        System.out.println(">> [알림 서비스] 사용자 해지됨: " + userId);
    }

    @Override
    public void notifyObserver(String userId, String message) {
        Observer observer = activeUsers.get(userId);
                
        if (observer != null) {
            // 1. 로그인 상태: 즉시 전송 (Observer 패턴 동작)
            System.out.println(">> [알림 전송] 실시간 알림 -> " + userId);
            observer.update(message);
        } else {
            // 2. 로그아웃 상태: 파일 저장 (기존 noticeController 활용)
            System.out.println(">> [알림 전송] 사용자 오프라인 -> 파일 저장 (" + userId + ")");
            List<String> target = new ArrayList<>();
            target.add(userId);
            noticeController.addNotice(target, message);
        }
    }
}
