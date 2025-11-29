/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

/**
 *
 * @author rbcks
 */
public class Reservation {
    private String room;
    private String date;     // yyyy/MM/dd
    private String start;    // HH:mm
    private String end;
    private String userId;
    private String role;
    private String status;   // WAIT, APPROVED, REJECTED
    private String rawLine;  // 파일 원본 라인

    public Reservation(String line) {
        this.rawLine = line;
        String[] parts = line.split(",");
        // 파일 포맷: 건물(0),강의실(1),날짜(2),요일(3),시작(4),종료(5),ID(6),역할(7),목적(8),인원(9),상태(10),사유(11)
        if (parts.length >= 11) {
            this.room = parts[1].trim();
            this.date = parts[2].trim();
            this.start = parts[4].trim();
            this.end = parts[5].trim();
            this.userId = parts[6].trim();
            this.role = parts[7].trim();
            this.status = parts[10].trim();
        }
    }

    // 학생이면서 거절되지 않은 유효한 예약인지 확인
    public boolean isValidStudentReservation() {
        return "S".equals(role) && !"REJECTED".equals(status);
    }

    // Getters
    public String getRoom() { return room; }
    public String getDate() { return date; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getUserId() { return userId; }
    public String getRawLine() { return rawLine; }
}
