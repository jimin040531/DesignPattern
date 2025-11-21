/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.MessageContent;
import deu.cse.lectureroomreservation2.server.model.RecipientInfo;

/**
 *
 * @author rbcks
 */
// 학생 예약 취소 알림 전용 빌더
public class StudentCancellationBuilder extends NotificationBuilder {
    @Override
    public void buildRecipientInfo(String userId, String role) {
        // 작은 객체 1 생성 및 조립
        RecipientInfo info = new RecipientInfo(userId, role);
        notification.setRecipientInfo(info);
    }

    @Override
    public void buildMessageContent(String room, String date, String time) {
        // 작은 객체 2 생성 및 조립 (취소 알림 메시지 포맷팅)
        String title = "예약 취소 알림";
        String body = String.format("교수님 일정으로 인해 [%s]호 [%s %s] 예약이 취소되었습니다.", room, date, time);
        
        MessageContent content = new MessageContent(title, body);
        notification.setMessageContent(content);
    }

    @Override
    public void buildPriority() {
        // 취소 알림은 중요하므로 높음(HIGH)으로 설정
        notification.setPriority("HIGH");
    }
}
