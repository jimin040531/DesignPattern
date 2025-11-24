/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.DaysOfWeek;
import deu.cse.lectureroomreservation2.server.model.ScheduleFileManager;
import deu.cse.lectureroomreservation2.server.model.ScheduleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jimin
 */
/**
 * 시간표 정보를 파일에서 읽고 쓰는 로직 + 메모리상에서 시간표를 관리하는 ScheduleManager 
 * -> 시간표의 조회/추가/삭제/수정
 * 기능 제공
 *
 * 서버에서 ScheduleRequest를 처리할 때 사용됨
 *
 * 주요 구성:
 * - ScheduleFileManager: 텍스트 파일 입출력 담당 
 * - ScheduleManager: 메모리상 시간표 구조화 및 조회 담당
 *
 */
public class TimeTableController {

    private final ScheduleFileManager fileManager;
    private ScheduleManager scheduleManager;

    public TimeTableController() {
        this.fileManager = new ScheduleFileManager();
        this.scheduleManager = new ScheduleManager();
    }

    public TimeTableController(ScheduleFileManager fileManager) {
        this.fileManager = fileManager;
        this.scheduleManager = new ScheduleManager();
    }

    /**
     * ScheduleFileManager를 통해 파일에서 모든 시간표를 읽음
     * ScheduleManager에 시간표 정보 로드
     * 각 줄은 ["강의실", "요일", "시작시간", "종료시간", "과목", "타입"] 형식
     */
    public void loadSchedulesFromFile() {
        scheduleManager = new ScheduleManager();    // 초기화
        List<String[]> rawLines = fileManager.readAllLines();
            
        for (String[] parts : rawLines) {
            if (parts.length == 6) {
                String room = parts[0].trim();
                DaysOfWeek day = DaysOfWeek.fromKoreanDay(parts[1].trim());
                String start = parts[2].trim();
                String end = parts[3].trim();
                String subject = parts[4].trim();
                String type = parts[5].trim();
                scheduleManager.addSchedule(room, day, start, end, subject, type);
            }
        }
    }

    /**
     * 파일에 이미 같은 강의실/요일/시간대의 시간표가 존재하는지 확인
     *
     * @param room
     * @param day
     * @param start
     * @param end
     * @return true: 중복 있음 / false: 없음
     */
    public boolean isScheduleExists(String room, String day, String start, String end) {
        List<String[]> lines = fileManager.readAllLines();
        for (String[] parts : lines) {
            if (parts.length >= 4
                    && parts[0].trim().equals(room)
                    && parts[1].trim().equals(day)
                    && parts[2].trim().equals(start)
                    && parts[3].trim().equals(end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 파일에 새 시간표 항목 추가 
     * 중복 항목이 있을 경우 -> 예외 처리
     * @param room
     * @param day
     * @param start
     * @param end
     * @param subject
     * @param type
     */
    public void addScheduleToFile(String room, String day, String start, String end, String subject, String type) {
        if (isScheduleExists(room, day, start, end)) {
            throw new IllegalArgumentException("이미 등록된 시간표입니다.");
        }

        String line = String.join(",", room.trim(), day.trim(), start.trim(), end.trim(), subject.trim(), type.trim());
        fileManager.appendLine(line);
    }

    /**
     * 지정된 강의실/요일/시간대의 시간표 항목 삭제
     * 
     * @param room
     * @param day
     * @param start
     * @param end
     * @return 삭제 성공 여부
     */
    public boolean deleteScheduleFromFile(String room, String day, String start, String end) {
        List<String[]> lines = fileManager.readAllLines();
        List<String> updated = new ArrayList<>();
        boolean deleted = false;

        for (String[] parts : lines) {
            if (parts.length >= 4
                    && parts[0].trim().equals(room)
                    && parts[1].trim().equals(day)
                    && parts[2].trim().equals(start)
                    && parts[3].trim().equals(end)) {
                deleted = true; // 해당 항목 삭제
                continue;
            }
            updated.add(String.join(",", parts));   // 나머지는 유지
        }

        if (deleted) {
            fileManager.overwriteAll(updated);  // 전체 파일 덮어쓰기
        }

        return deleted;
    }

    /**
     * 기존 시간표를 삭제한 후 새 정보로 다시 추가해서 수정하는 방식 사용
     *
     * @param room
     * @param day
     * @param start
     * @param subject
     * @param end
     * @param type
     * @return 수정 성공 여부
     */
    public boolean updateSchedule(String room, String day, String start, String end, String subject, String type) {
        boolean deleted = deleteScheduleFromFile(room, day, start, end);
        if (deleted) {
            addScheduleToFile(room, day, start, end, subject, type);
            return true;
        }
        return false;
    }

    /**
     * 특정 강의실/요일/타입에 해당하는 시간표 정보를 Map으로 반환
     *
     * @param room
     * @param day
     * @param type
     * @return Map<시간대, 과목명 또는 제한사유>
     */
    public Map<String, String> getScheduleForRoom(String room, String day, String type) {
        loadSchedulesFromFile(); // 항상 최신화
        DaysOfWeek dayOfWeek = DaysOfWeek.fromKoreanDay(day);
        return scheduleManager.getSchedule(room, dayOfWeek, type);
    }
}
