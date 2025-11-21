/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 *
 * @author rbcks
 */
public class MessageContent {
    private String title;
    private String body;
    private LocalDateTime timestamp;

    public MessageContent(String title, String body) {
        this.title = title;
        this.body = body;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        // 알림 포맷 예: "[2025-05-22 14:00] 제목: 내용"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("[%s] %s: %s", timestamp.format(formatter), title, body);
    }
}
