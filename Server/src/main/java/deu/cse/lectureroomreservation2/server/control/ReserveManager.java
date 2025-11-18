package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveManageResult;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class ReserveManager {

    // 사용자 정보 파일 경로 (예약 정보도 이 파일에 저장)
    private static final String USER_FILE = receiveController.getUserFileName();
    private static final String RESERVE_FILE = receiveController.getReservationInfoFileName();
    private static final String SCHEDULE_FILE = receiveController.getScheduleInfoFileName();
    private static final int MAX_RESERVE = 4;
    private static final Object FILE_LOCK = new Object();

    // 요일 변환: "월" → "월요일" 등으로 변환
    private static String toFullDayName(String shortDay) {
        if (shortDay == null) {
            return "";
        }
        switch (shortDay.trim()) {
            case "월":
                return "월요일";
            case "화":
                return "화요일";
            case "수":
                return "수요일";
            case "목":
                return "목요일";
            case "금":
                return "금요일";
            default:
                return shortDay.trim();
        }
    }

    /**
     * 예약 요청을 처리하는 메서드입니다. (Strategy + Builder Pattern 적용)
     */
    public static ReserveResult reserve(ReservationDetails details) {
        synchronized (FILE_LOCK) {
            // --- 1. 공통 전처리 (유효성 검사) ---
            String date = details.getDate();
            try {
                String[] dateParts = date.split("/");
                if (dateParts.length != 4) {
                    return new ReserveResult(false, "예약 날짜/시간 형식이 올바르지 않습니다.");
                }
                String year = dateParts[0].trim();
                String month = dateParts[1].trim();
                String dayOfMonth = dateParts[2].trim();
                String[] times = dateParts[3].trim().split(" ");
                if (times.length != 2) {
                    return new ReserveResult(false, "예약 시간 정보가 올바르지 않습니다.");
                }
                String startTime = times[0];
                String endTime = times[1];

                LocalDateTime startDateTime = LocalDateTime.parse(
                        year + "-" + month + "-" + dayOfMonth + "T" + startTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                LocalDateTime endDateTime = LocalDateTime.parse(
                        year + "-" + month + "-" + dayOfMonth + "T" + endTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

                if (startDateTime.isBefore(LocalDateTime.now())) {
                    return new ReserveResult(false, "과거 시간대에는 예약할 수 없습니다.");
                }

                long minutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes();
                if (!(minutes == 50 || minutes == 60)) {
                    return new ReserveResult(false, "예약은 50분 또는 1시간 단위로만 가능합니다.");
                }
            } catch (Exception e) {
                return new ReserveResult(false, "날짜/시간 형식 오류");
            }

            // --- 2. 전략(Strategy) 선택 ---
            ReservationStrategy strategy;
            if ("P".equals(details.getRole())) {
                strategy = new ProfessorReservationStrategy();
            } else if ("S".equals(details.getRole())) {
                strategy = new StudentReservationStrategy();
            } else {
                return new ReserveResult(false, "알 수 없는 사용자 역할입니다.");
            }

            // --- 3. 선택된 전략 실행 ---
            return strategy.execute(details);
        }
    }

    // 예약 정보 생성(포맷 일관성 보장)
    public static String makeReserveInfo(String roomNumber, String date, String day) {
        return String.format("%s / %s / %s", roomNumber.trim(), date.trim(), toFullDayName(day));
    }

    // 예약 정보 비교(공백, 대소문자 무시)
    public static boolean equalsReserveInfo(String info1, String info2) {
        return info1.replaceAll("\\s+", " ").trim().equalsIgnoreCase(info2.replaceAll("\\s+", " ").trim());
    }

    /**
     * id로 예약 정보 조회 - 클라이언트 요청 시 사용
     */
    public static List<String> getReserveInfoById(String id) {
        synchronized (FILE_LOCK) {
            List<String> reserves = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 && parts[2].trim().equals(id)) {
                        for (int i = 4; i < parts.length; i++) {
                            reserves.add(parts[i].trim());
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return reserves;
        }
    }

    /**
     * 예약 정보 조회 (id, room, date 중 하나 이상 조건으로 조회)
     */
    public static List<String> getReserveInfoAdvanced(String id, String room, String date) {
        List<String> result = new ArrayList<>();
        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length < 5) {
                        continue;
                    }
                    String userId = parts[2].trim();
                    for (int i = 4; i < parts.length; i++) {
                        String reserve = parts[i].trim();
                        String[] reserveParts = reserve.split("/");
                        if (reserveParts.length < 6) {
                            continue;
                        }
                        String reserveRoom = reserveParts[0].trim();
                        String reserveDate = reserveParts[1].trim() + " / " + reserveParts[2].trim() + " / "
                                + reserveParts[3].trim();
                        boolean match = true;
                        if (id != null && !id.trim().equals(userId.trim())) {
                            match = false;
                        }
                        if (room != null && !room.equals(reserveRoom)) {
                            match = false;
                        }
                        if (date != null && !date.equals(reserveDate)) {
                            match = false;
                        }
                        if (match) {
                            result.add(userId + " / " + reserve);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 6번: 예약 정보로 예약한 사용자 id 목록 조회
     */
    public static List<String> getUserIdsByReserveInfo(String reserveInfo) {
        synchronized (FILE_LOCK) {
            List<String> userIds = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        String userId = parts[2].trim();
                        for (int i = 4; i < parts.length; i++) {
                            if (equalsReserveInfo(parts[i], reserveInfo)) {
                                userIds.add(userId);
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return userIds;
        }
    }

    /**
     * 예약 정보로 총 예약자 수 조회 - 클라이언트 요청 시 사용
     */
    public static int countUsersByReserveInfo(String reserveInfo) {
        synchronized (FILE_LOCK) {
            int count = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    for (int i = 4; i < parts.length; i++) {
                        if (equalsReserveInfo(parts[i], reserveInfo)) {
                            count++;
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return count;
        }
    }

    public static ReserveResult cancelReserve(String id, String reserveInfo) {
        synchronized (FILE_LOCK) {
            List<String> lines = new ArrayList<>();
            boolean updated = false;

            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 && parts[2].trim().equals(id)) {
                        List<String> reserves = new ArrayList<>();
                        for (int i = 4; i < parts.length; i++) {
                            reserves.add(parts[i].trim());
                        }
                        if (!reserves.removeIf(r -> equalsReserveInfo(r, reserveInfo))) {
                            return new ReserveResult(false, "해당 예약 정보를 찾을 수 없습니다.");
                        }
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            sb.append(parts[i]).append(i < 3 ? "," : "");
                        }
                        for (String r : reserves) {
                            sb.append(",").append(r);
                        }
                        lines.add(sb.toString());
                        updated = true;
                    } else {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ReserveResult(false, "파일 읽기 오류");
            }

            if (updated) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
                    for (String l : lines) {
                        bw.write(l);
                        bw.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ReserveResult(false, "파일 저장 오류");
                }
                return new ReserveResult(true, "예약이 취소되었습니다.");
            }
            return new ReserveResult(false, "사용자 정보를 찾을 수 없습니다.");
        }
    }

    // 교수 예약 여부 조회 - 클라이언트 요청 시 사용
    public static boolean hasProfessorReserve(String reserveInfo) {
        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 && parts[0].trim().equalsIgnoreCase("P")) {
                        for (int i = 4; i < parts.length; i++) {
                            if (equalsReserveInfo(parts[i], reserveInfo)) {
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

    // 교수 예약 시 학생 예약 중복 취소 및 해당 학생 ID 리스트 반환
    public static List<String> cancelStudentReservesForProfessor(String roomNumber, String date, String day) {
        synchronized (FILE_LOCK) {
            List<String> affectedStudentIds = new ArrayList<>();
            String targetReserve = makeReserveInfo(roomNumber, date, day);
            List<String> lines = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 5 && "S".equals(parts[0].trim())) {
                        List<String> reserves = new ArrayList<>();
                        boolean removed = false;
                        for (int i = 4; i < parts.length; i++) {
                            if (equalsReserveInfo(parts[i], targetReserve)) {
                                removed = true;
                            } else {
                                reserves.add(parts[i].trim());
                            }
                        }
                        if (removed) {
                            affectedStudentIds.add(parts[2].trim());
                        }
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            sb.append(parts[i]).append(i < 3 ? "," : "");
                        }
                        for (String r : reserves) {
                            sb.append(",").append(r);
                        }
                        lines.add(sb.toString());
                    } else {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
                for (String l : lines) {
                    bw.write(l);
                    bw.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return affectedStudentIds;
        }
    }

    // 강의실 조회 - state (정규수업, 교수예약, 예약가능, 예약초과)
    public static String getRoomState(String room, String day, String start, String end, String date) {
        synchronized (FILE_LOCK) {
            // 1. 정규수업 체크
            try (BufferedReader br = new BufferedReader(new FileReader(SCHEDULE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        String fullDay = toFullDayName(parts[1].trim());
                        if (parts[0].trim().equals(room) && fullDay.equals(toFullDayName(day))
                                && parts[2].trim().equals(start) && parts[3].trim().equals(end)
                                && parts[5].trim().equals("수업")) {
                            return "정규수업";
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String reserveInfo = room + " / " + date + " / " + toFullDayName(day);

            // 2. 교수예약 체크
            if (hasProfessorReserve(reserveInfo)) {
                return "교수예약";
            }

            // 3. 예약 가능/초과 체크
            int count = countUsersByReserveInfo(reserveInfo);
            if (count <= 39) {
                return "예약가능";
            } else {
                return "예약초과";
            }
        }
    }

    // 강의실 조회 - 강의실 예약 시간대 조회
    public static List<String[]> getRoomSlots(String room, String day) {
        List<String[]> slots = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String fullDay = toFullDayName(parts[1].trim());
                    if (parts[0].trim().equals(room) && fullDay.equals(toFullDayName(day))) {
                        String start = parts[2].trim();
                        String end = parts[3].trim();
                        slots.add(new String[]{start, end});
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return slots;
    }

    /**
     * 예약 수정 요청을 처리하는 메서드 (Strategy + Builder Pattern 적용)
     */
    public static ReserveResult updateReserve(ReservationDetails details) {
        synchronized (FILE_LOCK) {

            // --- 1. 전략(Strategy) 선택 ---
            UpdateReservationStrategy strategy;
            if ("P".equals(details.getRole())) {
                strategy = new ProfessorUpdateStrategy();
            } else if ("S".equals(details.getRole())) {
                strategy = new StudentUpdateStrategy();
            } else {
                return new ReserveResult(false, "알 수 없는 사용자 역할입니다.");
            }

            // --- 2. 선택된 전략 실행 ---
            return strategy.execute(details); // details 객체 통째로 전달
        }
    }

    public static ReserveManageResult searchUserAndReservations(String userId, String room, String date) {
        synchronized (FILE_LOCK) {
            boolean userFound = false;
            List<String[]> result = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length < 4) {
                        continue;
                    }

                    String id = parts[2].trim();
                    if (!userId.isEmpty() && !id.equals(userId)) {
                        continue;
                    }

                    userFound = true;

                    for (int i = 4; i < parts.length; i++) {
                        String reserve = parts[i].trim();
                        String[] tokens = reserve.split("/");
                        if (tokens.length < 6) {
                            continue;
                        }

                        String roomNum = tokens[0].trim();
                        String y = tokens[1].trim();
                        String m = tokens[2].trim();
                        String d = tokens[3].trim();
                        String fullDate = y + " / " + m + " / " + d;

                        if (!room.isEmpty() && !roomNum.equals(room)) {
                            continue;
                        }
                        if (!date.isEmpty() && !date.equals(fullDate)) {
                            continue;
                        }

                        String[] times = tokens[4].trim().split(" ");
                        String day = tokens[5].trim();

                        result.add(new String[]{
                            id, roomNum, y, m, d, times[0], times[1], day
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ReserveManageResult(false, "파일 읽기 오류", null);
            }

            if (!userFound) {
                return new ReserveManageResult(false, "사용자 정보 없음", null);
            } else if (result.isEmpty()) {
                return new ReserveManageResult(true, "예약 없음", new ArrayList<>());
            } else {
                return new ReserveManageResult(true, "조회 성공", result);
            }
        }
    }

    /**
     * [HELPER 1] 공통 예약 파일 쓰기 로직 (Strategy들이 호출)
     */
    public static ReserveResult writeReservationToFile(String id, String newReserve, String role) {
        synchronized (FILE_LOCK) {
            List<String> lines = new ArrayList<>();
            boolean updated = false;

            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 && parts[2].trim().equals(id)) {
                        List<String> reserves = new ArrayList<>();
                        for (int i = 4; i < parts.length; i++) {
                            reserves.add(parts[i].trim());
                        }
                        for (String r : reserves) {
                            if (equalsReserveInfo(r, newReserve)) {
                                return new ReserveResult(false, "이미 동일한 예약이 존재합니다.");
                            }
                        }
                        if (reserves.size() >= MAX_RESERVE && !"P".equals(role)) {
                            return new ReserveResult(false, "최대 예약 개수 초과");
                        }
                        reserves.add(newReserve);

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            sb.append(parts[i]).append(i < 3 ? "," : "");
                        }
                        for (String r : reserves) {
                            sb.append(",").append(r);
                        }
                        lines.add(sb.toString());
                        updated = true;
                    } else {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ReserveResult(false, "파일 읽기 오류(write)");
            }

            if (updated) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
                    for (String l : lines) {
                        bw.write(l);
                        bw.newLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ReserveResult(false, "파일 저장 오류(write)");
                }
                return new ReserveResult(true, "예약 성공");
            }
            return new ReserveResult(false, "사용자 정보를 찾을 수 없습니다.");
        }
    }

    /**
     * [HELPER 2] 자신을 제외한 다른 교수의 예약이 있는지 확인 (ProfessorStrategy가 호출)
     */
    public static boolean hasOtherProfessorReserve(String newReserve, String selfId) {
        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 && "P".equals(parts[0].trim()) && !parts[2].trim().equals(selfId)) {
                        for (int i = 4; i < parts.length; i++) {
                            if (equalsReserveInfo(parts[i], newReserve)) {
                                return true; // 다른 교수가 예약함
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false; // 예약 없음
        }
    }

    /**
     * [HELPER 3] 예약 변경 실패 시 기존 예약을 복원 (롤백)
     */
    public static void restoreReservation(String id, String role, String oldReserveInfo) {
        try {
            String[] tokens = oldReserveInfo.split("/");
            if (tokens.length >= 6) {
                String oldRoom = tokens[0].trim();
                String year = tokens[1].trim();
                String month = tokens[2].trim();
                String day = tokens[3].trim();
                String[] times = tokens[4].trim().split(" ");
                String oldDay = tokens[5].trim();
                String restoreDate = year + " / " + month + " / " + day + " / " + times[0] + " " + times[1];

                writeReservationToFile(id, makeReserveInfo(oldRoom, restoreDate, oldDay), role);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * [Template Method Pattern (SFR-201: 월별 현황 조회)]
     * @param roomNumber
     */
    public static List<String> getReservationStatusForMonth(String roomNumber, int year, int month, String startTime) {
        synchronized (FILE_LOCK) {
            List<String> monthlyStatus = new ArrayList<>();
            YearMonth yearMonth = YearMonth.of(year, month);
            int daysInMonth = yearMonth.lengthOfMonth();

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = yearMonth.atDay(day);
                // findDailyReservationType 호출 시 startTime 전달
                String dayStatus = findDailyReservationType(roomNumber, currentDate, startTime); 
                monthlyStatus.add(String.format("%d:%s", day, dayStatus));
            }
            return monthlyStatus;
        }
    }

    /**
     * [신규 헬퍼] Template Method의 세부 단계(Sub-step)
     */
    private static String findDailyReservationType(String roomNumber, LocalDate date, String startTime) { 
        // 1. 정규 수업 체크 (최우선)
        if (isClassScheduled(roomNumber, date, startTime)) {
            return "CLASS"; // 정규 수업이 있다면 CLASS 반환
        }

        // 2. 예약 정보 체크 (기존 로직 유지)
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"));

        boolean professorReserved = false;
        boolean studentReserved = false;

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) {
                    continue;
                }

                String role = parts[0].trim();

                for (int i = 4; i < parts.length; i++) {
                    String reserve = parts[i].trim();
                    // 해당 날짜 및 시간대(startTime)가 포함된 예약이 있는지 확인합니다.
                    if (reserve.startsWith(roomNumber + " / " + dateString) && reserve.contains(startTime)) {
                        if ("P".equals(role)) {
                            professorReserved = true;
                        } else if ("S".equals(role)) {
                            studentReserved = true;
                        }
                    }
                }

                if (professorReserved) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3. 우선순위에 따라 반환 (CLASS는 이미 위에서 처리됨)
        if (professorReserved) {
            return "PROFESSOR";
        } else if (studentReserved) {
            return "STUDENT";
        } else {
            return "NONE"; // CLASS, PROFESSOR, STUDENT 모두 없으면 예약 가능
        }
    }

    /**
     * 특정 강의실, 날짜, 시작 시간에 정규 수업이 있는지 확인합니다.
     *
     * @return 정규 수업이 있으면 true
     */
    private static boolean isClassScheduled(String roomNumber, LocalDate date, String startTime) {
        String endTime = calculateEndTime(startTime);
        String fullDayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(SCHEDULE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        String scheduleDay = toFullDayName(parts[1].trim());

                        if (parts[0].trim().equals(roomNumber)
                                && scheduleDay.equals(fullDayName)
                                && parts[2].trim().equals(startTime)
                                && parts[3].trim().equals(endTime)
                                && parts[5].trim().equals("수업")) {
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * [Template Method Pattern (주간 현황 조회)] ClassCastException 방지를 위해 반환형을 Map으로
     * 유지하고 로직만 정의합니다.
     */
    public static Map<String, List<String[]>> getWeeklySchedule(String roomNumber, LocalDate startMonday) {
        synchronized (FILE_LOCK) {
            // 시간대 정의 (클라이언트의 ViewRoom.java에 맞춰서 9시부터 17시까지 50분 단위로 가정)
            String[] timeSlots = {
                "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"
            };

            Map<String, List<String[]>> weeklyData = new LinkedHashMap<>(); // 순서 보장

            for (String start : timeSlots) {
                String end = calculateEndTime(start); // 50분 또는 1시간 후 끝 시간 계산 (구현 필요)

                List<String[]> dailyStatus = new ArrayList<>();

                for (int i = 0; i < 5; i++) { // 월요일(i=0)부터 금요일(i=4)까지
                    LocalDate currentDate = startMonday.plusDays(i);
                    String dateString = currentDate.format(DateTimeFormatter.ofPattern("yyyy / MM / dd"));
                    String dayName = currentDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

                    // 주간 조회의 세부 단계: 특정 시간대의 상태를 확인
                    String state = getRoomStateForWeekly(roomNumber, dayName, start, end, dateString);

                    // { 날짜, 상태 } 형식으로 저장
                    dailyStatus.add(new String[]{currentDate.format(DateTimeFormatter.ofPattern("MM/dd")), state});
                }

                weeklyData.put(start + "~" + end, dailyStatus); // Key를 "09:00~09:50" 형태로 변경하여 클라이언트 매핑 용이하게 함
            }

            return weeklyData;
        }
    }

    /**
     * [신규 헬퍼] 주간 조회를 위한 상태 확인 (정규수업, 교수, 학생, 가능) 이 메서드는 getRoomState를 단순화하고 주간
     * 뷰에 맞게 반환형을 정의합니다.
     */
    private static String getRoomStateForWeekly(String room, String day, String start, String end, String date) {
        synchronized (FILE_LOCK) {
            // 1. 정규수업 체크
            try (BufferedReader br = new BufferedReader(new FileReader(SCHEDULE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        if (parts[0].trim().equals(room) && toFullDayName(parts[1].trim()).equals(toFullDayName(day))
                                && parts[2].trim().equals(start) && parts[3].trim().equals(end)
                                && parts[5].trim().equals("수업")) {
                            return "정규수업";
                        }
                    }
                }
            } catch (IOException e) {
                /* 무시하고 다음 단계로 진행 */ }

            // 예약 정보 문자열 생성 (makeReserveInfo 포맷에 맞춤)
            String dateWithTime = date + " / " + start + " " + end;
            String reserveInfo = makeReserveInfo(room, dateWithTime, day);

            // 2. 교수 예약 체크
            if (hasProfessorReserve(reserveInfo)) {
                return "교수예약";
            }

            // 3. 학생 예약 체크
            int count = countUsersByReserveInfo(reserveInfo);
            if (count > 0) {
                return "학생예약";
            }

            return "예약가능";
        }
    }

    /**
     * [신규 헬퍼] 시작 시간에 따른 종료 시간 계산 (임시 구현)
     */
    private static String calculateEndTime(String startTime) {
        // 실제 구현에서는 강의실 예약 단위(50분/60분)에 따라 달라질 수 있으나, 임시로 50분 뒤로 설정
        // 클라이언트 코드(ViewRoom.java)의 타임 슬롯이 50분 단위일 가능성이 높으므로 50분으로 가정
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime start = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.parse(startTime, formatter));
            return start.plusMinutes(50).format(formatter);
        } catch (Exception e) {
            return "End"; // 오류 발생 시
        }
    }
}
