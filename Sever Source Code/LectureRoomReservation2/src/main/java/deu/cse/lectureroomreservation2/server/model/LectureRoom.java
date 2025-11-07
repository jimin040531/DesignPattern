/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jimin
 */

/**
 * 특정 강의실의 요일별 고정 시간표를 저장하고 관리하는 클래스
 * - fixedSchedule: 요일 -> (시작시간 -> 시간표 항목) 구조의 맵
 * - 수업 추가 및 타입별 필터링 조회 기능 제공
 */
public class LectureRoom {
    private final String roomNumber;
    private final Map<DaysOfWeek, Map<String, ScheduleEntry>> fixedSchedule;

    public LectureRoom(String roomNumber) {
        this.roomNumber = roomNumber;
        this.fixedSchedule = new HashMap<>();
        for (DaysOfWeek day : DaysOfWeek.values()) {
            fixedSchedule.put(day, new HashMap<>());
        }
    }

    // 수업/제한 항목 추가
    public void addFixedSchedule(DaysOfWeek day, String startTime, String endTime, String subject, String type) {
        ScheduleEntry entry = new ScheduleEntry(subject, type, startTime, endTime);
        fixedSchedule.get(day).put(startTime, entry);  // ✅ key는 시작 시간만 사용
    }

    // 특정 요일의 전체 시간표 반환
    public Map<String, ScheduleEntry> getFixedSchedule(DaysOfWeek day) {
        return fixedSchedule.get(day);
    }

    // 특정 타입(수업/제한)에 해당하는 항목만 필터링하여 반환
    public Map<String, String> getFixedScheduleByType(DaysOfWeek day, String targetType) {
        Map<String, String> filtered = new HashMap<>();
        Map<String, ScheduleEntry> entries = fixedSchedule.get(day);
        for (Map.Entry<String, ScheduleEntry> entry : entries.entrySet()) {
            if (entry.getValue().getType().equals(targetType)) {
                filtered.put(entry.getKey(), entry.getValue().getSubject());
            }
        }
        return filtered;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
}

