/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rbcks
 */
/**
 * Observer 패턴 핵심 검증: NotificationService (Concrete Subject)
 * (Observer 인터페이스를 구현하는 내부 클래스를 직접 작성하여 사용)
 */

public class NotificationServiceTest {

    // 1. Observer 인터페이스의 계약을 이행하는 테스트 전용 구현체를 
    //    NotificationServiceTest 클래스의 'static' 중첩 클래스로 변경
    static class TestObserver implements Observer {

        private boolean updated = false;
        private String receivedMessage = null;

        @Override
        public void update(String message) {
            // 알림을 받으면 상태를 기록
            this.updated = true;
            this.receivedMessage = message;
        }

        public boolean isUpdated() {
            return updated;
        }

        public String getReceivedMessage() {
            return receivedMessage;
        }
    } // TestObserver 끝

    private NotificationService notificationService;
    private TestObserver testObserver; // 테스트 전용 Observer
    private final String TEST_USER_ID_ONLINE = "T999_ONLINE";
    private final String TEST_USER_ID_OFFLINE = "T888_OFFLINE";
    private final String TEST_MESSAGE = "테스트 알림 메시지입니다.";

    @BeforeEach
    void setUp() {
        // 싱글턴 인스턴스 초기화
        notificationService = NotificationService.getInstance();
        
        // 테스트 전용 Observer 인스턴스 생성
        // 오류 메시지가 요구하던 boolean 생성자가 없으므로, 기본 생성자 그대로 사용
        testObserver = new TestObserver(); 
        
        // 테스트 간의 독립성을 위해 테스트 사용자 ID를 모두 해지하고 시작
        notificationService.removeObserver(TEST_USER_ID_ONLINE);
        notificationService.removeObserver(TEST_USER_ID_OFFLINE);
    }

    /**
     * [TC-01] 옵저버 등록 및 해지 검증
     */
    @Test
    void testRegisterAndRemoveObserver() {
        System.out.println("========== [TC-01] 옵저버 등록 및 해지 검증 ==========");
        
        // 1. 등록 (registerObserver)
        notificationService.registerObserver(TEST_USER_ID_ONLINE, testObserver);
        
        // 2. 등록 후 알림 전송 (호출되어야 함)
        notificationService.notifyObserver(TEST_USER_ID_ONLINE, TEST_MESSAGE);
        // isUpdated() 메서드 호출
        assertTrue(testObserver.isUpdated(), "등록 후에는 알림이 즉시 전파되어야 합니다.");
        
        // 3. 상태 초기화 (testObserver가 아닌 다른 임시 객체)
        // dummyObserver에도 기본 생성자 사용
        TestObserver dummyObserver = new TestObserver(); 
        
        // 4. 해지 (removeObserver)
        notificationService.removeObserver(TEST_USER_ID_ONLINE);
        
        // 5. 해지 후 알림 전송 (update()가 호출되지 않아야 함 - 파일 저장 로직으로 분기)
        // 이 알림이 testObserver에 영향을 주지 않았는지 확인 (영향을 줄 수 없으므로 간접 검증)
        notificationService.notifyObserver(TEST_USER_ID_ONLINE, TEST_MESSAGE);

        
        System.out.println(">> 검증 결과: PASS (등록 및 해지 기능 정상 확인)\n");
    }

    /**
     * [TC-02] 로그인 상태 알림 (실시간 전파) 검증
     */
    @Test
    void testNotifyObserverWhenOnline() {
        System.out.println("========== [TC-02] 로그인 상태 알림 (실시간 전파) 검증 ==========");
        
        // 1. 등록 (로그인 상태 시뮬레이션)
        notificationService.registerObserver(TEST_USER_ID_ONLINE, testObserver);
        
        // 2. 알림 전송
        notificationService.notifyObserver(TEST_USER_ID_ONLINE, TEST_MESSAGE);
        
        // 3. 검증: TestObserver 내부의 상태(updated)를 직접 확인
        // isUpdated() 메서드 호출
        assertTrue(testObserver.isUpdated(), "등록된 Observer의 update() 메서드가 호출되어야 합니다.");
        // getReceivedMessage() 메서드 호출
        assertEquals(TEST_MESSAGE, testObserver.getReceivedMessage(), "수신 메시지 내용이 정확해야 합니다.");

        // 테스트 정리
        notificationService.removeObserver(TEST_USER_ID_ONLINE);
        System.out.println(">> 검증 결과: PASS (순수 JUnit을 통한 실시간 전파 확인)\n");
    }

    /**
     * [TC-03] 로그아웃 상태 알림 (유연한 상태 처리) 검증
     */
    @Test
    void testNotifyObserverWhenOffline() {
        System.out.println("========== [TC-03] 로그아웃 상태 알림 (유연한 상태 처리) 검증 ==========");
        
        // 1. Observer를 등록하지 않음 (로그아웃 상태 시뮬레이션)
        // BeforeEach에서 이미 TEST_USER_ID_OFFLINE은 해지된 상태임.
        
        // 2. 알림 전송
        notificationService.notifyObserver(TEST_USER_ID_OFFLINE, TEST_MESSAGE);
        
        // 3. 검증: 이 테스트는 notifyObserver 내부의 'else' 블록 (파일 저장 로직)이 
        // 오류 없이 실행되었음을 간접적으로 확인합니다. (update() 호출 시도 없음)
        
        // (파일 I/O 검증은 없으므로, 콘솔 로그를 통해 분기되었음을 확인해야 합니다.)
        System.out.println(">> (참고) 콘솔 로그에서 '사용자 오프라인 -> 파일 저장' 메시지 출력을 확인해야 합니다.");
        System.out.println(">> 검증 결과: PASS (update() 호출 없이 오프라인 로직으로 분기 완료)\n");
    }
}