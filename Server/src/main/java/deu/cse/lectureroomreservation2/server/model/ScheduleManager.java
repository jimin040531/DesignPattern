package deu.cse.lectureroomreservation2.server.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 메모리상에서 강의실 시간표 정보를 관리하는 클래스
 * 강의실 번호별 LectureRoom 객체를 저장하고, 시간표 조회 및 추가 기능을 제공
 */
public class ScheduleManager {
    private final Map<String, LectureRoom> lectureRooms;

    public ScheduleManager() {
        this.lectureRooms = new HashMap<>();
    }

    // 시간표 추가
    public void addSchedule(String roomNumber, DaysOfWeek day, String startTime, String endTime, String subject, String type) {
        LectureRoom room = lectureRooms.computeIfAbsent(roomNumber, LectureRoom::new);
        room.addFixedSchedule(day, startTime, endTime, subject, type);
    }

    public Map<String, String> getSchedule(String roomNumber, DaysOfWeek day, String type) {
        LectureRoom room = lectureRooms.get(roomNumber);
        if (room == null) return null;

        return room.getFixedScheduleByType(day, type); // ⬅️ LectureRoom에서 처리
    }
}
