/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

/**
 *
 * @author rbcks
 */
public class Notification {
    // 작은 객체들을 부품으로 가짐
    private RecipientInfo recipientInfo;
    private MessageContent messageContent;
    private String priority; // 추가 속성 (우선순위)

    // Setter 메서드들 (빌더가 사용함)
    public void setRecipientInfo(RecipientInfo recipientInfo) {
        this.recipientInfo = recipientInfo;
    }

    public void setMessageContent(MessageContent messageContent) {
        this.messageContent = messageContent;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    // 사용 편의를 위한 Getter
    public String getTargetUserId() {
        return recipientInfo != null ? recipientInfo.getUserId() : null;
    }

    public String getFormattedMessage() {
        return messageContent != null ? messageContent.toString() : "";
    }
}
