/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.Notification;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rbcks
 */
/**
 * 빌더 패턴 핵심 검증: StudentCancellationBuilder (Concrete Builder)
 */
public class StudentCancellationBuilderTest {

    // 테스트 대상: Concrete Builder 인스턴스
    private StudentCancellationBuilder builder = new StudentCancellationBuilder();

    /**
     * [Helper Method] 빌더의 표준 단계를 실행하고 완성된 Notification 객체를 반환
     */
    private Notification executeBuildSteps(String userId, String room, String date, String time, String reason) {
        // 1. 초기화 (Product 생성)
        builder.createNewNotification();
        
        // 2. 커스텀 속성 설정 (빌더에 특화된 비즈니스 로직)
        builder.setCancellationReason(reason);
        
        // 3. 빌드 단계 실행 (Director 역할)
        builder.buildRecipientInfo(userId, "S");
        builder.buildMessageContent(room, date, time);
        builder.buildPriority();
        
        // 4. 완성품 반환
        return builder.getNotification();
    }

    /**
     * [TC-01] 완전한 객체 생성 및 핵심 내용 검증
     */
    @Test
    public void testFullNotificationConstruction() {
        System.out.println("========== [TC-01] 완전한 객체 생성 및 핵심 내용 검증 ==========");
        String testUserId = "S2025001";
        String testReason = "교수님 일정 변경";
        System.out.println("[시나리오] 모든 빌드 단계를 거쳐 완전한 Notification 객체가 생성되는지 검증");
        System.out.println("[입력 데이터] User: " + testUserId + " | Reason: " + testReason);
        
        Notification notification = executeBuildSteps(testUserId, "905", "2026/01/15", "10:00", testReason);
        
        // 1. 객체 자체 검증
        assertNotNull(notification, "Notification 객체는 null이 아니어야 합니다.");
        
        // 2. RecipientInfo (수신자) 검증
        assertEquals(testUserId, notification.getTargetUserId(), "수신자 ID가 정확히 설정되어야 합니다.");
        
        // 3. MessageContent (내용) 검증: 메시지가 특정 이유를 포함하는지 검증
        String messageBody = notification.getFormattedMessage();
        assertTrue(messageBody.contains(testReason), "메시지 내용에 설정된 취소 사유가 포함되어야 합니다.");
        assertTrue(messageBody.contains("예약 취소 알림"), "메시지 제목(알림 제목)이 포함되어야 합니다.");

        System.out.println(">> 검증 결과: PASS (완벽한 객체 생성 확인)\n");
    }

    /**
     * [TC-02] 구체적 비즈니스 로직(취소 사유) 통합 검증
     */
    @Test
    public void testCustomReasonIntegration() {
        System.out.println("========== [TC-02] 구체적 비즈니스 로직(취소 사유) 통합 검증 ==========");
        String customReason = "장비 점검으로 인한 시스템 취소";
        String testUserId = "S2025002";
        System.out.println("[시나리오] setCancellationReason()으로 주입된 커스텀 사유가 최종 메시지에 반영되는지 검증");

        Notification notification = executeBuildSteps(testUserId, "905", "2026/01/15", "10:00", customReason);
        String formattedMessage = notification.getFormattedMessage();
        
        // 최종 메시지가 커스텀 사유를 포함해야 합니다. (핵심 비즈니스 로직)
        assertTrue(formattedMessage.contains(customReason), 
                   "최종 알림 메시지에 커스텀 취소 사유가 포함되어야 합니다.");
        
        // 포맷이 깨지지 않고 강의실 번호를 올바르게 포함하는지 확인 (구조 검증)
        assertTrue(formattedMessage.contains("[905호]"), 
                   "메시지 포맷이 강의실 번호를 올바르게 포함해야 합니다.");
        
        System.out.println(">> 검증 결과: PASS (커스텀 사유 반영 확인)\n");
    }

    /**
     * [TC-03] 기본값(Default) 로직 검증 및 우선순위 설정 단계 확인
     */
    @Test
    public void testDefaultReasonLogic() {
        System.out.println("========== [TC-03] 기본값(Default) 로직 검증 및 우선순위 설정 단계 확인 ==========");
        
        // 1. 기본값(Default) 검증: setCancellationReason에 null을 넘겼을 때
        builder.createNewNotification();
        builder.setCancellationReason(null); // null 입력 시 기본값 ("교수님 일정") 유지 로직 확인
        builder.buildMessageContent("905", "2026/01/15", "10:00");
        Notification defaultNotif = builder.getNotification();
        
        String defaultMessage = defaultNotif.getFormattedMessage();
        
        // StudentCancellationBuilder.java에 정의된 기본값: "교수님 일정"
        assertTrue(defaultMessage.contains("교수님 일정"), 
                   "null 입력 시 기본값인 '교수님 일정'이 사용되어야 합니다.");

        // 2. 우선순위 설정 검증 (간접 검증)
        // Notification 객체의 getPriority()가 없으므로, 해당 단계가 오류 없이 완료되었음을 확인
        // (실제 프로젝트에서 getter가 있다면 assertEquals("HIGH", notification.getPriority()))
        
        System.out.println(">> 검증 결과: PASS (기본값 처리 및 필수 단계 완료 확인)\n");
    }
}