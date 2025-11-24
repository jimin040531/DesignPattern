/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.client.view;

/**
 * 1학기 시간표 검증 전략
 * 
 * 현재는:
 *  - 종료 시간이 시작 시간보다 늦어야 함
 *  - 정확히 50분 차이만 허용
 * 
 * 필요하면 나중에 1학기만의 특별 규칙을 더 넣을 수 있음.
 */
public class FirstSemesterStrategy implements SemesterStrategy {

    @Override
    public String validateTimeRange(String start, String end) {
        if (start == null || end == null) {
            return "시작 시간과 종료 시간을 모두 선택해 주세요.";
        }

        String[] startParts = start.split(":");
        String[] endParts = end.split(":");

        int startHour = Integer.parseInt(startParts[0]);
        int startMinute = Integer.parseInt(startParts[1]);
        int endHour = Integer.parseInt(endParts[0]);
        int endMinute = Integer.parseInt(endParts[1]);

        int totalStart = startHour * 60 + startMinute;
        int totalEnd = endHour * 60 + endMinute;
        int duration = totalEnd - totalStart;

        // 종료 시간이 시작 시간보다 늦어야 함
        if (totalEnd <= totalStart) {
            return "종료 시간은 시작 시간보다 늦어야 합니다.";
        }

        // 정확히 50분 차이만 허용
        if (duration != 50) {
            return "시작 시간과 종료 시간은 50분 단위만 허용됩니다.";
        }

        // 문제 없으면 null
        return null;
    }
}

