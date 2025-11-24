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
 * 시간표 정보를 파일에서 읽고 쓰는 로직 + 메모리상에서 시간표를 관리하는 ScheduleManager -> 시간표의 조회/추가/삭제/수정
 * 기능 제공
 *
 * 서버에서 ScheduleRequest를 처리할 때 사용됨
 *
 * 주요 구성: - ScheduleFileManager: 텍스트 파일 입출력 담당 - ScheduleManager: 메모리상 시간표 구조화 및
 * 조회 담당
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
     * ScheduleFileManager를 통해 파일에서 모든 시간표를 읽음 ScheduleManager에 시간표 정보 로드 각 줄은
     * ["강의실", "요일", "시작시간", "종료시간", "과목", "타입"] 형식
     */
    public void loadSchedulesFromFile(String year, String semester, String building) {

        scheduleManager = new ScheduleManager();
        List<String[]> rawLines = fileManager.readAllLines();

        for (String[] parts : rawLines) {

            // 10개 형식이 아닐 경우 무시
            if (parts.length < 10) {
                continue;
            }

            String fileYear = parts[0].trim();
            String fileSemester = parts[1].trim();
            String fileBuilding = parts[2].trim();
            String room = parts[3].trim();
            String day = parts[4].trim();
            String start = parts[5].trim();
            String end = parts[6].trim();
            String subject = parts[7].trim();
            String professor = parts[8].trim();
            String type = parts[9].trim();

            // 요청한 (year, semester, building) 과 맞는 경우만 로딩
            if (fileYear.equals(year)
                    && fileSemester.equals(semester)
                    && fileBuilding.equals(building)) {

                scheduleManager.addSchedule(
                        room,
                        DaysOfWeek.fromKoreanDay(day),
                        start,
                        end,
                        subject,
                        type
                );
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
    public boolean isScheduleExists(String year, String semester, String building,
            String room, String day, String start, String end) {
        List<String[]> lines = fileManager.readAllLines();
        for (String[] parts : lines) {
            // 파일 한 줄은 year, semester, building, room, day, start, end, subject, professor, type
            if (parts.length >= 7
                    && parts[0].trim().equals(year)
                    && parts[1].trim().equals(semester)
                    && parts[2].trim().equals(building)
                    && parts[3].trim().equals(room)
                    && parts[4].trim().equals(day)
                    && parts[5].trim().equals(start)
                    && parts[6].trim().equals(end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 파일에 새 시간표 항목 추가 중복 항목이 있을 경우 -> 예외 처리
     *
     * @param room
     * @param day
     * @param start
     * @param end
     * @param subject
     * @param type
     */
    /**
     * 파일에 새 시간표 항목 추가 (10개 컬럼 버전) year, semester, building, room, day, start,
     * end, subject, professor, type
     */
    public void addScheduleToFile(String year, String semester, String building,
            String room, String day,
            String start, String end,
            String subject, String professor,
            String type) {

        // 1) 먼저 중복 체크 (같은 년도/학기/건물/강의실/요일/시간대 있는지)
        if (isScheduleExists(year, semester, building, room, day, start, end)) {
            throw new IllegalArgumentException("이미 등록된 시간표입니다.");
        }

        // 2) 파일에 쓸 한 줄 만들기
        String line = String.join(",",
                year.trim(),
                semester.trim(),
                building.trim(),
                room.trim(),
                day.trim(),
                start.trim(),
                end.trim(),
                subject.trim(),
                professor.trim(),
                type.trim()
        );

        // 3) 맨 끝에 추가
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
    /**
     * 지정된 년도/학기/건물/강의실/요일/시간대의 시간표 항목 삭제
     *
     * @return 삭제 성공 여부
     */
    public boolean deleteScheduleFromFile(String year, String semester, String building,
            String room, String day,
            String start, String end) {
        List<String[]> lines = fileManager.readAllLines();
        List<String> updated = new ArrayList<>();
        boolean deleted = false;

        for (String[] parts : lines) {
            // parts : [0]=year, [1]=semester, [2]=building, [3]=room, [4]=day, [5]=start, [6]=end, [7]=subject, [8]=professor, [9]=type
            if (parts.length >= 7
                    && parts[0].trim().equals(year)
                    && parts[1].trim().equals(semester)
                    && parts[2].trim().equals(building)
                    && parts[3].trim().equals(room)
                    && parts[4].trim().equals(day)
                    && parts[5].trim().equals(start)
                    && parts[6].trim().equals(end)) {
                // 이 줄은 삭제 대상 → updated 에 안 넣음
                deleted = true;
                continue;
            }

            // 삭제 대상이 아니면 원래 줄 그대로 합쳐서 다시 저장 목록에 추가
            updated.add(String.join(",", parts));
        }

        if (deleted) {
            // 수정된 전체 내용으로 파일 덮어쓰기
            fileManager.overwriteAll(updated);
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
    /**
     * 기존 시간표를 삭제한 후 새 정보로 다시 추가해서 수정하는 방식
     */
    public boolean updateSchedule(String year, String semester, String building,
            String room, String day,
            String start, String end,
            String subject, String professor,
            String type) {

        // 1) 먼저 기존 줄 삭제 시도
        boolean deleted = deleteScheduleFromFile(year, semester, building, room, day, start, end);

        // 2) 기존 줄이 있었으면 → 새 정보로 다시 추가
        if (deleted) {
            addScheduleToFile(year, semester, building, room, day, start, end, subject, professor, type);
            return true;
        }

        // 3) 기존 줄이 없었다 → 수정할 것이 없으므로 false
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
        DaysOfWeek dayOfWeek = DaysOfWeek.fromKoreanDay(day);
        return scheduleManager.getSchedule(room, dayOfWeek, type);
    }
    // ===========================
    //  📁 시간표 전체 백업 / 복원
    // ===========================

    /**
     * 현재 사용 중인 시간표 파일(ScheduleInfo.txt)을 지정한 이름의 백업 파일로 복사한다.
     *
     * @param backupName 생성할 백업 파일 이름 (예: "ScheduleInfo_backup.txt")
     * @return true : 백업 성공 false : 백업 실패
     */
    public boolean backupSchedule(String backupName) {
        return fileManager.backupFile(backupName);
    }

    /**
     * 지정한 백업 파일을 읽어서 현재 시간표 파일(ScheduleInfo.txt)을 덮어쓴다.
     *
     * @param backupName 사용할 백업 파일 이름
     * @return true : 복원 성공 false : 복원 실패
     */
    public boolean restoreSchedule(String backupName) {
        return fileManager.restoreFile(backupName);
    }
}
