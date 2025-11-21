/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;
import java.io.*;
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
    protected boolean hasRegularClass(String room, String day, String time) {
        // 파일의 요일("월")과 입력 요일("월요일") 형식을 맞춤
        String shortDay = day.length() > 1 ? day.substring(0, 1) : day;

        try (BufferedReader br = new BufferedReader(new FileReader(scheduleFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 10) {
                    String sRoom = parts[3].trim();   // 강의실
                    String sDay = parts[4].trim();    // 요일
                    String sStart = parts[5].trim();  // 시작시간
                    String sType = parts[9].trim();   // 유형(수업)

                    if (sRoom.equals(room) && sDay.equals(shortDay) && sStart.equals(time) && "수업".equals(sType)) {
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
    @Override
    protected boolean hasActiveReservation(String room, String date, String time) {
        System.out.println(String.format("[DEBUG] 검색중: 방=%s, 날짜=%s, 시간=%s", room, date, time));
        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    String rRoom = parts[1].trim();     // 강의실
                    String rDate = parts[2].trim();     // 날짜 (yyyy/MM/dd)
                    String rStart = parts[4].trim();    // 시작시간
                    String rStatus = parts[10].trim();  // 상태 (APPROVED, WAIT, REJECTED)

                    // 날짜 포맷 통일 ('-' -> '/')
                    String normalizedDate = date.replace("-", "/");
                    String normalizedRDate = rDate.replace("-", "/");

                    if (rRoom.equals(room) && normalizedRDate.equals(normalizedDate) && rStart.equals(time)) {
                        // 거절된 예약은 없는 셈 침 (즉, 승인되거나 대기중인 것만 "예약됨"으로 간주)
                        if (!"REJECTED".equals(rStatus)) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
