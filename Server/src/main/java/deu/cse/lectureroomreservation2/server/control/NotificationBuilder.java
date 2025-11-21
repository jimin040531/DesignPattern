/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;
import deu.cse.lectureroomreservation2.server.model.Notification;
/**
 *
 * @author rbcks
 */
public abstract class NotificationBuilder {
    // 만들어질 복합 객체 (Product)
    protected Notification notification;

    // 빈 객체 생성 (공통 로직)
    public void createNewNotification() {
        notification = new Notification();
    }

    // 완성된 객체 반환 (공통 로직)
    public Notification getNotification() {
        return notification;
    }

    // [추상 메서드] 구상 빌더가 반드시 구현해야 할 단계들
    // 작은 객체 1(RecipientInfo) 생성 및 조립
    public abstract void buildRecipientInfo(String userId, String role);

    // 작은 객체 2(MessageContent) 생성 및 조립
    public abstract void buildMessageContent(String room, String date, String time);

    // 추가 속성(Priority) 설정
    public abstract void buildPriority();
}
