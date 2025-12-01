package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveManageResult;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ReserveManager {

    private static final String RESERVE_FILE = receiveController.getReservationInfoFileName();
    private static final String SCHEDULE_FILE = receiveController.getScheduleInfoFileName();
    private static final Object FILE_LOCK = new Object();

    // ---------------------------------------------------------
    // [전략 패턴 적용] 1. 신규 예약 (reserve)
    // ---------------------------------------------------------
    public static ReserveResult reserve(ReservationDetails details) {
        ReservationBehavior behavior;
        if ("P".equals(details.getRole())) {
            behavior = new ProfessorReservation();
        } else if ("S".equals(details.getRole())) {
            behavior = new StudentReservation();
        } else {
            return new ReserveResult(false, "알 수 없는 역할");
        }
        return behavior.reserve(details);
    }

    // ---------------------------------------------------------
    // [전략 패턴 적용] 2. 예약 변경 (updateReserve)
    // ---------------------------------------------------------
    public static ReserveResult updateReserve(ReservationDetails details) {
        // 1. 인터페이스 선언
        ReservationUpdateBehavior updateBehavior;

        // 2. 런타임에 구상 클래스 인스턴스 결정
        if ("P".equals(details.getRole())) {
            updateBehavior = new ProfessorUpdate();
        } else {
            updateBehavior = new StudentUpdate();
        }

        // 3. 결정된 행동 실행
        return updateBehavior.update(details);
    }

    // ---------------------------------------------------------
    // [Read] 1. 월별 / 주별 조회 (Template Method 사용)
    // ---------------------------------------------------------
    // 월별 조회 시 "AVAILABLE" -> "NONE"으로 변경하여 클라이언트로 전송
    public static List<String> getReservationStatusForMonth(String buildingName, String roomNumber, int year, int month, String startTime) { // buildingName 추가
        synchronized (FILE_LOCK) {
            List<String> monthlyStatus = new ArrayList<>();
            YearMonth yearMonth = YearMonth.of(year, month);
            int daysInMonth = yearMonth.lengthOfMonth();

            System.out.println(">>> [ReserveManager] 월별 조회: " + buildingName + " " + roomNumber + ", " + year + "-" + month + ", 시간:" + startTime);

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = yearMonth.atDay(day);
                // [수정] buildingName 전달
                String status = checkReservationStatus(buildingName, roomNumber, currentDate, startTime);

                if ("AVAILABLE".equals(status)) {
                    status = "NONE";
                }
                monthlyStatus.add(day + ":" + status);
            }
            return monthlyStatus;
        }
    }

    // 2. 주별 조회 
    public static Map<String, List<String[]>> getWeeklySchedule(String buildingName, String roomNumber, LocalDate startMonday) { // buildingName 추가
        synchronized (FILE_LOCK) {
            String[] timeSlots = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
            Map<String, List<String[]>> weeklyData = new LinkedHashMap<>();

            for (String start : timeSlots) {
                List<String[]> dailyStatus = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    LocalDate currentDate = startMonday.plusDays(i);
                    // buildingName 전달
                    String status = checkReservationStatus(buildingName, roomNumber, currentDate, start);
                    String displayStatus = convertStatusToKorean(status);
                    dailyStatus.add(new String[]{currentDate.format(DateTimeFormatter.ofPattern("MM/dd")), displayStatus});
                }
                weeklyData.put(start + "~" + calculateEndTime(start), dailyStatus);
            }
            return weeklyData;
        }
    }

    // ---------------------------------------------------------
    // [Read] 2. 일별 조회
    // ---------------------------------------------------------
    // 3. 일별 상태 조회 (getRoomState) 수정
    public static String getRoomState(String buildingName, String room, String day, String start, String end, String date) { // buildingName 추가
        synchronized (FILE_LOCK) {
            try {
                String[] parts = date.split("/");
                String year = parts[0].trim();
                String month = parts[1].trim();
                String d = parts[2].trim();
                LocalDate targetDate = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(d));

                // buildingName 전달
                String status = checkReservationStatus(buildingName, room, targetDate, start);
                return convertStatusToKorean(status);
            } catch (Exception e) {
                e.printStackTrace();
                return "오류";
            }
        }
    }

    // 강의실 시간표 슬롯 조회 (일별 조회 테이블의 시간대 리스트용)
    public static List<String[]> getRoomSlots(String room, String day) {
        List<String[]> slots = new ArrayList<>();
        String shortDay = day.length() > 1 ? day.substring(0, 1) : day;

        // 1. 정규 수업 시간 가져오기 (ScheduleInfo.txt) - 10칸 포맷
        // 년도(0), 학기(1), 건물(2), 강의실(3), 요일(4), 시작(5), 종료(6)...
        try (BufferedReader br = new BufferedReader(new FileReader(SCHEDULE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 10) {
                    // [수정] 인덱스 3(강의실), 4(요일) 확인
                    if (parts[3].trim().equals(room) && parts[4].trim().equals(shortDay)) {
                        // [수정] 인덱스 5(시작), 6(종료) 저장
                        slots.add(new String[]{parts[5].trim(), parts[6].trim()});
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. 예약된 시간 가져오기 (ReservationInfo.txt) - 12칸 포맷
        // 건물(0), 강의실(1), 날짜(2), 요일(3), 시작(4), 종료(5)...
        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    // [수정] 인덱스 1(강의실), 3(요일) 확인
                    if (parts[1].trim().equals(room) && parts[3].trim().contains(shortDay)) {
                        // [수정] 상태(10)가 REJECTED가 아니면 추가
                        if (!"REJECTED".equals(parts[10].trim())) {
                            // [수정] 인덱스 4(시작), 5(종료) 저장
                            slots.add(new String[]{parts[4].trim(), parts[5].trim()});
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 중복 제거 및 정렬
        Set<String> uniqueSlots = new HashSet<>();
        List<String[]> uniqueList = new ArrayList<>();
        for (String[] s : slots) {
            if (uniqueSlots.add(s[0] + s[1])) {
                uniqueList.add(s);
            }
        }
        uniqueList.sort(Comparator.comparing(o -> o[0]));

        return uniqueList;
    }

    // ---------------------------------------------------------
    //예약 현황 통계 조회 (확정 인원, 대기 인원)
    // ---------------------------------------------------------
    public static int[] getReservationStats(String buildingName, String room, String dateOnly, String startTime) {
        int currentTotalCount = 0;

        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length < 12) {
                        continue; // [수정]
                    }
                    String rBuilding = parts[0].trim();
                    String rRoom = parts[1].trim();
                    String rDate = parts[2].trim();
                    String rStart = parts[4].trim();
                    String rStatus = parts[10].trim();

                    int count = 1;
                    try {
                        count = Integer.parseInt(parts[9].trim());
                    } catch (Exception e) {
                        count = 1;
                    }

                    String targetDate = dateOnly.replace("-", "/");

                    if (rBuilding.equals(buildingName) && rRoom.equals(room) && rDate.equals(targetDate) && rStart.equals(startTime)) {
                        if ("APPROVED".equals(rStatus) || "WAIT".equals(rStatus)) {
                            currentTotalCount += count;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int maxCapacity = BuildingManager.getInstance().getRoomCapacity(buildingName, room);
        return new int[]{currentTotalCount, maxCapacity};
    }

    // ---------------------------------------------------------
    // [Write] 예약 저장 (12칸 포맷)
    // ---------------------------------------------------------
    public static ReserveResult writeReservationToFile(String id, String csvLine, String role) {
        synchronized (FILE_LOCK) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(RESERVE_FILE, true))) {
                bw.write(csvLine);
                bw.newLine();
                return new ReserveResult(true, "예약 요청이 완료되었습니다.");
            } catch (IOException e) {
                e.printStackTrace();
                return new ReserveResult(false, "저장 중 오류 발생");
            }
        }
    }

    // ---------------------------------------------------------
    // [Modify] 예약 취소 및 복구 (핵심 수정)
    // ---------------------------------------------------------
    // 예약 취소 (파일에서 해당 줄 삭제)
    public static ReserveResult cancelReserve(String id, String reserveInfo) {
        synchronized (FILE_LOCK) {
            File inputFile = new File(RESERVE_FILE);
            File tempFile = new File(inputFile.getParent(), "temp_reserve.txt");
            boolean deleted = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile)); BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    // CSV 파일의 한 줄(line)과 삭제하려는 예약정보(reserveInfo)가 일치하면 삭제
                    // (공백 무시하고 비교)
                    if (deleted) {
                        writer.write(line);
                        writer.newLine();
                        continue;
                    }

                    // [핵심 수정 2] id(사용자 학번)를 인자로 추가 전달
                    if (equalsReserveInfo(line, reserveInfo, id)) {
                        deleted = true;
                        continue; // 파일에 쓰지 않고 건너뜀 (삭제 효과)
                    }

                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ReserveResult(false, "파일 처리 오류");
            }

            // 원본 파일 교체
            if (deleted) {
                if (inputFile.delete()) {
                    tempFile.renameTo(inputFile);
                    return new ReserveResult(true, "예약이 삭제되었습니다.");
                } else {
                    return new ReserveResult(false, "파일 교체 실패");
                }
            } else {
                tempFile.delete();
                return new ReserveResult(false, "해당 예약 정보를 찾을 수 없습니다.");
            }
        }
    }

    // 예약 복구 (롤백용)
    public static void restoreReservation(String id, String role, String oldReserveInfo) {
        // 단순히 삭제했던 문자열을 다시 파일 끝에 추가함
        writeReservationToFile(id, oldReserveInfo, role);
    }

    // ---------------------------------------------------------
    // [Check] 조건 확인 Helper
    // ---------------------------------------------------------
    public static boolean hasProfessorReserve(String reserveInfo) {
        // reserveInfo 포맷이 CSV 라인 형태여야 함 (전략에서 만들어진 newReserve)
        // CSV 파싱하여 날짜/시간/강의실이 겹치고 역할이 'P'인 것이 있는지 확인
        // (여기서는 CSV 문자열 비교가 어려우므로, checkReservationStatus 로직 등을 활용하거나
        //  newReserve 문자열을 파싱해서 비교해야 함. 일단 간단히 파일 스캔)

        String[] target = reserveInfo.split(",");
        if (target.length < 6) {
            return false; // 포맷 안맞으면 패스
        }

        String tBuilding = target[0].trim();
        String tRoom = target[1].trim();
        String tDate = target[2].trim();
        String tStart = target[4].trim();

        String status = checkReservationStatus(tBuilding, tRoom,
                LocalDate.parse(tDate, DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                tStart);

        // 'PROFESSOR' 상태가 리턴되면 교수가 예약한 것임 (TextFileReservationChecker 로직상)
        // 다만 checkReservationStatus는 내 예약인지 남의 예약인지 구분 안하므로 주의.
        // TextFileReservationChecker를 직접 쓰지 않고 파일 스캔
        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    if (parts[0].trim().equals(tBuilding)
                            && parts[1].trim().equals(tRoom)
                            && parts[2].trim().equals(tDate)
                            && parts[4].trim().equals(tStart)
                            && parts[7].trim().equals("P") // 역할이 'P' (교수)
                            && !"REJECTED".equals(parts[10].trim())) { // 거절된 예약 제외
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int countUsersByReserveInfo(String reserveInfo) {
        String[] target = reserveInfo.split(",");

        // 최소 6개 이상의 필드가 필요
        if (target.length < 6) {
            return 0;
        }

        String tBuilding = target[0].trim();
        String tRoom = target[1].trim();
        String tDate = target[2].trim();
        String tStart = target[4].trim();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {

                    String rBuilding = parts[0].trim(); // 파일에서 건물 이름 읽기

                    // [수정 3]: 건물 이름(rBuilding) 비교 조건 추가
                    if (rBuilding.equals(tBuilding) //f건물 이름 일치 조건 추가
                            && parts[1].trim().equals(tRoom)
                            && parts[2].trim().equals(tDate)
                            && parts[4].trim().equals(tStart)
                            && !"REJECTED".equals(parts[10].trim())) {

                        // 인원수(9번 인덱스)를 더해야 함 (SFR-202 40명 제한)
                        try {
                            count += Integer.parseInt(parts[9].trim());
                        } catch (NumberFormatException e) {
                            count++; // 인원 파싱 실패 시 1명으로 간주
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    // --- Helpers ---
    private static String checkReservationStatus(String buildingName, String room, LocalDate date, String startTime) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        AbstractReservationChecker checker = new TextFileReservationChecker();
        return checker.checkStatus(buildingName, room, dateString, dayName, startTime);
    }

    // 한글 상태 메시지 변환 
    private static String convertStatusToKorean(String code) {
        switch (code) {
            case "CLASS":
                return "정규 수업";
            case "WAIT":
                return "예약 대기";
            case "APPROVED":
                return "예약 확정";
            case "AVAILABLE":
                return "예약 가능";
            default:
                return "";
        }
    }

    private static String calculateEndTime(String startTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime start = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.parse(startTime, formatter));
            return start.plusMinutes(50).format(formatter);
        } catch (Exception e) {
            return "End";
        }
    }

    // --- Other Methods ---
    public static String makeReserveInfo(String building, String room, String date, String day, String start, String end) {
        // 최소 6개 필드 필요 (hasProfessorReserve 파싱 로직 기준)
        return String.format("%s,%s,%s,%s,%s,%s", building, room, date, day, start, end);
    }

    // 예약 비교 (CSV 형식과 슬래시 형식 모두 지원하도록 개선)
    // ReserveManager.java
    public static boolean equalsReserveInfo(String fileLine, String requestInfo, String targetUserId) {
        try {
            // 1. 파일 라인 파싱 (CSV: 쉼표 기준)
            String[] fParts = fileLine.split(",", -1);
            if (fParts.length < 11) { // 최소 ID(6)까지는 있어야 함
                return false;
            }

            // 파일의 핵심 키 추출
            String fBuilding = fParts[0].trim(); // 건물
            String fRoom = fParts[1].trim();     // 강의실
            String fDate = fParts[2].trim().replace("-", "/"); // 날짜 포맷 통일
            String fStart = fParts[4].trim();    // 시작 시간
            String fId = fParts[6].trim();       // 사용자 ID

            // 비교할 요청 데이터 변수
            String cBuilding = null;
            String cRoom = null;
            String cDate = null;
            String cStart = null;

            // 2. 요청 정보 파싱
            if (requestInfo.contains(",")) {
                // Case A: CSV 형식 (수정된 클라이언트가 보내는 형식)
                // 포맷: "건물,강의실,날짜,요일,시작,끝"
                String[] cParts = requestInfo.split(",", -1);
                if (cParts.length < 6) {
                    return false;
                }

                cBuilding = cParts[0].trim();
                cRoom = cParts[1].trim();
                cDate = cParts[2].trim().replace("-", "/");
                cStart = cParts[4].trim();

            } else {
                // Case B: 슬래시 형식 (구형 클라이언트 대응 - 필요 없다면 제거 가능)
                String[] cParts = requestInfo.split("/");
                // 주의: 슬래시 형식일 때 건물이 포함되어 있는지 확인 필요
                // 만약 건물이 없다면 비교가 불가능하므로 false 리턴하거나 로직 보강 필요
                if (cParts.length < 6) {
                    return false;
                }

                // 예시: 건물/강의실/년/월/일/... 순서라고 가정 시 파싱 로직 필요
                // 현재 Client는 쉼표(,) 포맷을 사용하므로 이 else 블록은 사실상 실행될 일이 없어야 함
                return false;
            }

            // [중요] 3. 비교 로직 (if-else 블록 밖에서 수행)
            // null 체크 (파싱 실패 대비)
            if (cBuilding == null || cRoom == null || cDate == null || cStart == null) {
                return false;
            }

            // 건물, 강의실, 날짜, 시작시간, 그리고 ★사용자 ID★ 일치 여부 확인
            return fBuilding.equals(cBuilding)
                    && fRoom.equals(cRoom)
                    && fDate.equals(cDate)
                    && fStart.equals(cStart)
                    && fId.equals(targetUserId); // [핵심] 본인 예약만 삭제

        } catch (Exception e) {
            System.err.println("예약 비교 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    public static List<String> cancelStudentReservesForProfessor(String roomNumber, String date, String day) {
        return new ArrayList<>();
    }

    public static boolean hasOtherProfessorReserve(String newReserve, String selfId) {
        // newReserve 포맷: "건물,강의실,날짜,요일,시작,끝" (makeReserveInfo로 생성됨)
        String[] target = newReserve.split(",");
        if (target.length < 6) {
            return false;
        }

        String tBuilding = target[0].trim(); // [추가] 건물
        String tRoom = target[1].trim();
        String tDate = target[2].trim();
        String tStart = target[4].trim();

        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    // [수정] 건물 이름 비교 추가 (parts[0])
                    if (parts[0].trim().equals(tBuilding)
                            && parts[1].trim().equals(tRoom)
                            && parts[2].trim().equals(tDate)
                            && parts[4].trim().equals(tStart)) {

                        String rId = parts[6].trim();
                        String rRole = parts[7].trim();
                        String rStatus = parts[10].trim();

                        // 교수의 예약이고(P), 거절되지 않았으며(APPROVED/WAIT), 내 ID가 아닌 경우
                        if ("P".equals(rRole) && !"REJECTED".equals(rStatus) && !rId.equals(selfId)) {
                            return true; // 충돌 발생
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============================================
    // 📁 ReservationInfo.txt 백업 / 복원
    // ============================================
    public static boolean backupReservationFile(String backupFileName) {
        synchronized (FILE_LOCK) {
            try {
                Path source = Paths.get(RESERVE_FILE);              // ReservationInfo.txt
                Path target = source.getParent().resolve(backupFileName); // 같은 폴더의 backup 파일

                System.out.println("예약 백업 source = " + source.toAbsolutePath());
                System.out.println("예약 백업 target = " + target.toAbsolutePath());

                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static boolean restoreReservationFile(String backupFileName) {
        synchronized (FILE_LOCK) {
            try {
                Path target = Paths.get(RESERVE_FILE);              // ReservationInfo.txt
                Path source = target.getParent().resolve(backupFileName); // backup 파일

                System.out.println("예약 복원 source = " + source.toAbsolutePath());
                System.out.println("예약 복원 target = " + target.toAbsolutePath());

                if (!Files.exists(source)) {
                    System.out.println("예약 복원 실패: 백업 파일이 존재하지 않습니다.");
                    return false;
                }

                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    // [Iterator 패턴 지원] 파일의 모든 예약 정보를 문자열 리스트로 반환
    public static List<String> getAllReservations() {
        List<String> allLines = new ArrayList<>();
        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    allLines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return allLines;
    }

    public static boolean isSlotTakenByOtherProfessor(String room, String dateOnly, String startTime, String currentProfId) {
        List<String> allLines = getAllReservations();

        String targetDate = dateOnly.replace("-", "/"); // yyyy/MM/dd 형식

        for (String line : allLines) {
            String[] parts = line.split(",");
            if (parts.length >= 11) {
                String rRoom = parts[1].trim();     // 강의실
                String rDate = parts[2].trim();     // 날짜
                String rStart = parts[4].trim();    // 시작시간
                String rId = parts[6].trim();       // ID
                String rRole = parts[7].trim();     // 역할
                String rStatus = parts[10].trim();  // 상태 (APPROVED)

                if (rRoom.equals(room) && rDate.equals(targetDate) && rStart.equals(startTime)) {
                    // 활성화된 교수 예약인지 확인 (APPROVED 상태만 가정)
                    if (rRole.equals("P") && rStatus.equals("APPROVED")) {
                        // 현재 예약하려는 교수 ID와 다른지 확인
                        if (!rId.equals(currentProfId)) {
                            return true; // 다른 교수가 선점
                        }
                    }
                }
            }
        }
        return false;
    }

    // 예약 내역 조회
    public static ReserveManageResult searchUserAndReservations(String userId, String building, String room, String date) {
        List<String[]> resultList = new ArrayList<>();

        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length < 12) {
                        continue; // [수정]
                    }
                    String buildingName = parts[0].trim();
                    String roomNum = parts[1].trim();
                    String fullDate = parts[2].trim();
                    String weekDay = parts[3].trim();
                    String startTime = parts[4].trim();
                    String endTime = parts[5].trim();
                    String id = parts[6].trim();
                    String content = parts[8].trim();
                    String status = parts[10].trim();
                    String reason = parts[11].trim();

                    // 날짜 분리
                    String[] d = fullDate.split("/");
                    if (d.length < 3) {
                        continue;
                    }
                    String year = d[0];
                    String month = d[1];
                    String day = d[2];

                    // 필터링
                    if (userId != null && !userId.isEmpty() && !id.equals(userId)) {
                        continue;
                    }
                    if (building != null && !building.isEmpty() && !buildingName.equals(building)) {
                        continue;
                    }
                    if (room != null && !room.isEmpty() && !roomNum.equals(room)) {
                        continue;
                    }
                    if (date != null && !date.isEmpty() && !fullDate.equals(date)) {
                        continue;
                    }

                    String[] row = {
                        buildingName, roomNum, id, year, month, day,
                        startTime, endTime, weekDay, content, status, reason, line
                    };
                    resultList.add(row);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return new ReserveManageResult(false, "서버 오류", null);
            }
        }

        if (resultList.isEmpty()) {
            return new ReserveManageResult(false, "예약 없음", null);
        }
        return new ReserveManageResult(true, "조회 완료", resultList);
    }

    //승인 or 거절
    // [수정] 승인/거절 (인덱스 고정으로 안전하게 변경)
    public static ReserveManageResult approveOrReject(String command, String userId, String reserveInfo, String reason) {
        synchronized (FILE_LOCK) {
            File file = new File(RESERVE_FILE);
            if (!file.exists()) {
                return new ReserveManageResult(false, "파일 없음", null);
            }

            List<String> lines = new ArrayList<>();
            boolean updated = false;

            // 클라이언트에서 온 정보 공백 제거
            String targetInfo = reserveInfo.trim();

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // 데이터 찾기
                    if (line.contains(targetInfo)) {
                        String[] arr = line.split(",", -1);

                        if (arr.length >= 12) {
                            if (command.equals("APPROVE")) {
                                arr[10] = "APPROVED";
                                arr[11] = "-";
                            } else if (command.equals("REJECT")) {
                                arr[10] = "REJECTED";
                                arr[11] = reason;
                            }
                            line = String.join(",", arr);
                            updated = true;
                        }
                    }
                    lines.add(line);
                }
            } catch (Exception e) {
                return new ReserveManageResult(false, "오류: " + e.getMessage(), null);
            }

            if (!updated) {
                return new ReserveManageResult(false, "예약을 찾을 수 없습니다.", null);
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (String l : lines) {
                    bw.write(l);
                    bw.newLine();
                }
            } catch (Exception e) {
                return new ReserveManageResult(false, "저장 오류", null);
            }

            // ============================================================
            // [Observer 패턴 활용] 알림 메시지 포맷팅 (요구사항 반영)
            // reserveInfo 형식: "건물,강의실,날짜,요일,시작,종료" (쉼표로 구분됨)
            // ============================================================
            String msg = "";
            try {
                String[] tokens = reserveInfo.split(",");
                if (tokens.length >= 5) {
                    String building = tokens[0]; // 건물
                    String room = tokens[1];     // 강의실
                    String date = tokens[2];     // 날짜
                    // tokens[3]은 요일
                    String start = tokens[4];    // 시작시간

                    if (command.equals("APPROVE")) {
                        // 포맷: 예약 날짜/건물/강의실/시작시간/ 예약이 승인되었습니다.
                        msg = String.format("%s / %s / %s호 / %s / 예약이 승인되었습니다.",
                                date, building, room, start);
                    } else {
                        // 포맷: 예약 날짜/건물/강의실/시작시간/거절사유 
                        msg = String.format("%s / %s / %s호 / %s / 거절사유 : %s / 예약이 거절 되었습니다.",
                                date, building, room, start, reason);
                    }
                } else {
                    // 포맷 파싱 실패 시 기본 메시지
                    msg = (command.equals("APPROVE") ? "예약 승인" : "예약 거절") + ": " + reserveInfo;
                }
            } catch (Exception e) {
                msg = "알림 생성 중 오류 발생";
            }

            // 알림 전송 (Observer에게 통지)
            NotificationService.getInstance().notifyObserver(userId, msg);

            return new ReserveManageResult(true, command + " 완료", null);
        }
    }

    // 미사용 또는 클라이언트 호환용 (빈 구현)
    public static List<String> getReserveInfoById(String id) {
        return getReserveInfoAdvanced(id, null, null);
    }

    //ReservationInfo.txt 읽어 데이터 필터링
    public static List<String> getReserveInfoAdvanced(String userId, String room, String date) {
        List<String> result = new ArrayList<>();

        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length < 12) {
                        continue;
                    }

                    String rBuilding = parts[0].trim(); // 건물 이름
                    String rRoom = parts[1].trim();
                    String rDate = parts[2].trim(); // 파일: 2025/11/27
                    String rDay = parts[3].trim();
                    String rStart = parts[4].trim();
                    String rEnd = parts[5].trim();
                    String rId = parts[6].trim();
                    String rStatus = parts[10].trim();

                    if (userId != null && !rId.equals(userId)) {
                        continue;
                    }
                    if (room != null && !rRoom.equals(room)) {
                        continue;
                    }

                    // 날짜 비교 (포맷 통일: 모두 /로 변환해서 비교)
                    if (date != null) {
                        String normDate = date.replace("-", "/").trim();
                        String normRDate = rDate.replace("-", "/").trim();
                        if (!normRDate.equals(normDate)) {
                            continue;
                        }
                    }

                    //if ("REJECTED".equals(rStatus)) continue;
                    // 날짜 분리 (슬래시 또는 하이픈 기준)
                    String[] dateTokens = rDate.split("[/-]");
                    if (dateTokens.length < 3) {
                        continue;
                    }

                    // 클라이언트 요청 포맷에 맞춰 데이터 추가
                    // 순서: 건물 / 강의실 / 년 / 월 / 일 / 요일 / 시작 / 끝 / 상태
                    String formattedInfo = String.format("%s / %s / %s / %s / %s / %s / %s / %s / %s",
                            rBuilding, // 0. 건물
                            rRoom, // 1. 강의실
                            dateTokens[0], // 2. 년
                            dateTokens[1], // 3. 월
                            dateTokens[2], // 4. 일
                            rDay, // 5. 요일
                            rStart, // 6. 시작
                            rEnd, // 7. 끝
                            rStatus // 8. 상태
                    );

                    result.add(formattedInfo);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static List<String> getUserIdsByReserveInfo(String r) {
        return new ArrayList<>();
    }

    public static void purgePastReservations() {
        synchronized (FILE_LOCK) {
            File inputFile = new File(RESERVE_FILE);
            File tempFile = new File(inputFile.getParent(), "temp_purge.txt");

            if (!inputFile.exists()) {
                return;
            }

            // 현재 시간 (예: 2025-11-27 10:00:00)
            LocalDateTime now = LocalDateTime.now();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            boolean needReplace = false; // 파일 변경이 필요한지 체크

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile)); BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);

                    // 데이터 형식이 안맞으면 그대로 유지
                    if (parts.length < 12) {
                        writer.write(line);
                        writer.newLine();
                        continue;
                    }

                    String rDate = parts[2].trim();    // 날짜 (2025/11/27)
                    String rEndTime = parts[5].trim(); // 종료 시간 (09:50) -> 시작시간(parts[4])이 아님에 주의!
                    String rStatus = parts[10].trim();

                    // 1. 이미 거절된 건은 삭제 대상 (파일에 안씀) -> needReplace = true
                    if ("REJECTED".equals(rStatus)) {
                        needReplace = true;
                        continue;
                    }

                    try {
                        // 날짜 포맷 통일 (혹시 모를 - 문자 처리)
                        LocalDate datePart = LocalDate.parse(rDate.replace("-", "/"), dateFormatter);
                        LocalTime timePart = LocalTime.parse(rEndTime, timeFormatter);

                        // 예약 종료 시점 (2025-11-27 09:50)
                        LocalDateTime endDateTime = LocalDateTime.of(datePart, timePart);

                        // [조건] 예약 종료 시간이 현재보다 '미래'여야 유지함.
                        // 09:50(종료) vs 10:00(현재) -> isAfter는 false -> else로 이동(삭제)
                        if (endDateTime.isAfter(now)) {
                            writer.write(line);
                            writer.newLine();
                        } else {
                            // 과거 예약이므로 삭제됨 (파일에 안씀)
                            needReplace = true;
                            // System.out.println("삭제된 예약: " + line);
                        }

                    } catch (Exception e) {
                        // 파싱 에러나면 안전하게 데이터 보존
                        writer.write(line);
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                tempFile.delete(); // 에러나면 임시파일 삭제하고 중단
                return;
            }

            // 변경사항이 있을 때만 원본 교체
            if (needReplace) {
                if (inputFile.delete()) {
                    tempFile.renameTo(inputFile);
                    System.out.println(">> [System] 지난 예약 데이터 정리 완료.");
                } else {
                    tempFile.delete();
                }
            } else {
                tempFile.delete(); // 변경사항 없으면 그냥 닫기
            }
        }
    }
}
