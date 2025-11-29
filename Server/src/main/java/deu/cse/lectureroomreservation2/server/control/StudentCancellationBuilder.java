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
    
    // 취소 사유 저장 (기본값)
    private String cancellationReason = "교수님 일정"; 

    // Setter
    public void setCancellationReason(String reason) {
        if (reason != null && !reason.trim().isEmpty()) {
            this.cancellationReason = reason;
        }
    }

    @Override
    public void buildRecipientInfo(String userId, String role) {
        RecipientInfo info = new RecipientInfo(userId, role);
        notification.setRecipientInfo(info);
    }

    @Override
    public void buildMessageContent(String room, String date, String time) {
        String title = "예약 취소 알림";
        
        String body = String.format("[%s호] [%s %s] 예약이 다음 사유로 취소되었습니다: %s", 
                room, date, time, cancellationReason);
        
        MessageContent content = new MessageContent(title, body);
        notification.setMessageContent(content);
    }

    @Override
    public void buildPriority() {
        notification.setPriority("HIGH");
    }
}