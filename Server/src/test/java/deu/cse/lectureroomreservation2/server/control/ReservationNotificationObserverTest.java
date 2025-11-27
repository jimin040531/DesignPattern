/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author dadad
 */
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;


// 테스트용(Mock) Observer 생성
class TestObserver implements Observer {

    private final boolean online;       // true이면 update() 실행됨
    public final List<String> receivedMessages = new ArrayList<>();

    public TestObserver(boolean online) {
        this.online = online;
    }
    
    public boolean isOnline() {
        return online;
    }

    @Override
    public void update(String msg) {
        //온라인 사용자만 메세지 수신이 가능하다.
        if (online) {
            receivedMessages.add(msg);
            System.out.println("[Observer 수신] → " + msg);
        }
    }
}

//테스트용(Mock) Subject 클래스, activeUsers 등록 여부로 온라인/오프라인을 판단한다.
class TestableNotificationService {
    
    private final Map<String, Observer> activeUsers = new HashMap<>();
    private final Map<String, List<String>> offlineMessages = new HashMap<>();

    public void registerObserver(String userId, Observer observer) {
        activeUsers.put(userId, observer);
        System.out.println("[Observer 등록] " + userId + " (온라인 상태)");
    }

    public void removeObserver(String userId) {
        activeUsers.remove(userId);
        System.out.println("[Observer 제거] " + userId + " (오프라인 처리됨)");
    }

    public void notifyObserver(String userId, String message) {
        Observer observer = activeUsers.get(userId);
        if (observer != null) {
            System.out.println("[알림 전송] userId=" + userId + " (온라인) → 즉시 update() 호출");
            observer.update(message);
        } else {
            System.out.println("[알림 전송] userId=" + userId + " (오프라인) → 메시지 저장");
            offlineMessages
                .computeIfAbsent(userId, k -> new ArrayList<>())
                .add(message);
        }
    }

    public List<String> getOfflineMessages(String userId) {
        return offlineMessages.getOrDefault(userId, new ArrayList<>());
    }
}

public class ReservationNotificationObserverTest {
    private TestableNotificationService notifier;

    @BeforeEach
    void setup() {
        notifier = new TestableNotificationService();
    }

    // 온라인 사용자 → 즉시 알림 전달 테스트
    @Test
    void testOnlineUserReceivesNotification() {

        System.out.println("\n======== [온라인 사용자 알림 테스트] ========");
        
        TestObserver onlineUser = new TestObserver(true);       // 온라인 사용자 생성

        notifier.registerObserver("user1", onlineUser);         //등록 → 온라인으로 간주됨

        System.out.println("[알림 전송 시도] '예약이 승인되었습니다'");
        notifier.notifyObserver("user1", "예약이 승인되었습니다");

        assertEquals(1, onlineUser.receivedMessages.size());
        assertEquals("예약이 승인되었습니다", onlineUser.receivedMessages.get(0));

        System.out.println("[검증 결과] user1이 정상적으로 알림을 수신함");
    }

    
    // 오프라인 사용자 → 메시지 저장 테스트
    @Test
    void testOfflineUserStoresMessage() {
        System.out.println("\n======== [오프라인 사용자 메시지 저장 테스트 시작] ========");
        System.out.println("[상태] user2는 activeUsers에 없으므로 오프라인 상태");
        
        notifier.notifyObserver("user2", "예약이 거절되었습니다");
        List<String> stored = notifier.getOfflineMessages("user2");
        
        assertEquals(1, stored.size());
        assertEquals("예약이 거절되었습니다", stored.get(0));

        System.out.println("[검증 결과] user2 오프라인 메시지 저장 정상");
    }


    // 오프라인 사용자 → 여러 메시지 저장 테스트
    @Test
    void testMultipleOfflineMessages() {

        System.out.println("\n======== [오프라인 사용자 다중 메시지 저장 테스트 시작] ========");
        System.out.println("[상태] user3는 오프라인 상태 → 메시지가 누적 저장되어야 함");

        notifier.notifyObserver("user3", "예약이 승인되었습니다");
        notifier.notifyObserver("user3", "예약이 거절되었습니다");

        List<String> stored = notifier.getOfflineMessages("user3");

        assertEquals(2, stored.size());
        assertEquals("예약이 승인되었습니다", stored.get(0));
        assertEquals("예약이 거절되었습니다", stored.get(1));

        System.out.println("[검증 결과] user3 오프라인 메시지 2개 정상 저장");
    }


    // Observer 제거 후 알림이 전달되지 않는지 테스트
    @Test
    void testRemoveObserver() {

        System.out.println("\n======== [오프라인 사용자 알림 저장 테스트] ========");

        TestObserver obs = new TestObserver(true);
        
        notifier.registerObserver("user4", obs);
        notifier.removeObserver("user4");

        System.out.println("[알림 전송 시도] '삭제 후 메시지'");
        notifier.notifyObserver("user4", "삭제 후 메시지");

        // 제거 후 알림이 전달되면 안 된다.
        assertEquals(0, obs.receivedMessages.size());

        System.out.println("[검증 결과] 제거된 user4에게 알림이 전달되지 않음");
    }
}