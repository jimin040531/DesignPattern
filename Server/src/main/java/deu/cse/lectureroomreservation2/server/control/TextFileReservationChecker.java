/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
/**
 *
 * @author rbcks
 */
public class TextFileReservationChecker extends AbstractReservationChecker {
    private final String scheduleFile = receiveController.getScheduleInfoFileName();
    private final String reservationFile = receiveController.getReservationInfoFileName();

    /**
     * 1. 정규 수업 확인 (ScheduleInfo.txt)
     * 포맷: 년도(0), 학기(1), 건물(2), 강의실(3), 요일(4), 시작(5), 종료(6), 과목(7), 교수(8), 유형(9)
     */
    @Override
    protected boolean hasRegularClass(String room, String day, String time, String date) {
        String shortDay = day.length() > 1 ? day.substring(0, 1) : day;

        // [추가] 날짜를 기반으로 학기 계산 (1~6월: 1학기, 7~12월: 2학기)
        String targetSemester = "1";
        try {
            // date 형식: yyyy/MM/dd 또는 yyyy-MM-dd 호환 처리
            String normalizedDate = date.replace(" ", "").replace("/", "-");
            LocalDate localDate = LocalDate.parse(normalizedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int month = localDate.getMonthValue();
            
            if (month >= 7 && month <= 12) {
                targetSemester = "2";
            }
        } catch (Exception e) {
            // 날짜 파싱 실패 시 기본값 1학기로 진행하거나 에러 로그
            System.err.println("[Warning] 날짜 파싱 오류, 기본 1학기로 조회: " + date);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(scheduleFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 10) {
                    String sSemester = parts[1].trim(); // 파일의 학기 정보
                    String sRoom = parts[3].trim();
                    String sDay = parts[4].trim();
                    String sStart = parts[5].trim();
                    String sType = parts[9].trim();

                    // [수정] 학기(sSemester)가 현재 날짜의 학기(targetSemester)와 일치하는지 확인
                    if (sSemester.equals(targetSemester) &&
                        sRoom.equals(room) && 
                        sDay.equals(shortDay) && 
                        sStart.equals(time) && 
                        "수업".equals(sType)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 2. 예약 현황 확인 (ReservationInfo.txt)
     * 포맷: 건물(0), 강의실(1), 날짜(2), 요일(3), 시작(4), 종료(5), ID(6), 역할(7), 목적(8), 인원(9), 상태(10), 사유(11)
     * 조건: 상태가 "REJECTED"가 아닌 경우(WAIT, APPROVED)만 true 반환
     */
    // 상태 문자열 반환 메서드
    @Override
    protected String getReservationStatus(String room, String date, String time) {
        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 12) { // 12칸 포맷
                    String rRoom = parts[1].trim();
                    String rDate = parts[2].trim();
                    String rStart = parts[4].trim();
                    String rStatus = parts[10].trim(); // APPROVED, WAIT, REJECTED

                    String normalizedDate = date.replace("-", "/").replace(" ", "");
                    String normalizedRDate = rDate.replace("-", "/").replace(" ", "");

                    if (rRoom.equals(room) && normalizedRDate.equals(normalizedDate) && rStart.equals(time)) {
                        // 거절된 건 무시, 나머지는 상태 반환
                        if (!"REJECTED".equals(rStatus)) {
                            return rStatus; // "APPROVED" or "WAIT" 리턴
                        }
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return null; // 예약 없음
    }
}
