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

    private static final String RESERVE_FILE = receiveController.getReservationInfoFileName();
    private static final String SCHEDULE_FILE = receiveController.getScheduleInfoFileName();
    private static final Object FILE_LOCK = new Object();

    // ---------------------------------------------------------
    // [Read] 1. 월별 / 주별 조회 (Template Method 사용)
    // ---------------------------------------------------------
    // 월별 조회 시 "AVAILABLE" -> "NONE"으로 변경하여 클라이언트로 전송
    public static List<String> getReservationStatusForMonth(String roomNumber, int year, int month, String startTime) {
        synchronized (FILE_LOCK) {
            List<String> monthlyStatus = new ArrayList<>();
            YearMonth yearMonth = YearMonth.of(year, month);
            int daysInMonth = yearMonth.lengthOfMonth();

            System.out.println(">>> [ReserveManager] 월별 조회: " + roomNumber + ", " + year + "-" + month + ", 시간:" + startTime);

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = yearMonth.atDay(day);
                String status = checkReservationStatus(roomNumber, currentDate, startTime);

                // 클라이언트(ViewRoom)는 "NONE"일 때만 초록색으로 칠합니다.
                if ("AVAILABLE".equals(status)) {
                    status = "NONE";
                }

                monthlyStatus.add(day + ":" + status);
            }
            return monthlyStatus;
        }
    }

    public static Map<String, List<String[]>> getWeeklySchedule(String roomNumber, LocalDate startMonday) {
        synchronized (FILE_LOCK) {
            String[] timeSlots = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
            Map<String, List<String[]>> weeklyData = new LinkedHashMap<>();

            for (String start : timeSlots) {
                List<String[]> dailyStatus = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    LocalDate currentDate = startMonday.plusDays(i);
                    String status = checkReservationStatus(roomNumber, currentDate, start);
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
    public static String getRoomState(String room, String day, String start, String end, String date) {
        try {
            String[] parts = date.split("/");
            String year = parts[0].trim();
            String month = parts[1].trim();
            String d = parts[2].trim();
            LocalDate targetDate = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(d));
            String status = checkReservationStatus(room, targetDate, start);
            return convertStatusToKorean(status);
        } catch (Exception e) {
            e.printStackTrace();
            return "오류";
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
    public static int[] getReservationStats(String room, String dateOnly, String startTime) {
        int currentTotalCount = 0; // 확정(APPROVED) + 대기(WAIT) 인원 합계

        synchronized (FILE_LOCK) {
            try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    // 포맷: 건물,강의실,날짜,요일,시작,종료,ID,역할,목적,인원,상태,사유
                    if (parts.length < 11) {
                        continue;
                    }

                    String rRoom = parts[1].trim();
                    String rDate = parts[2].trim();  // yyyy/MM/dd
                    String rStart = parts[4].trim(); // HH:mm
                    String rStatus = parts[10].trim(); // APPROVED, WAIT, REJECTED

                    int count = 1;
                    try {
                        count = Integer.parseInt(parts[9].trim());
                    } catch (Exception e) {
                        count = 1; // 파싱 실패시 기본 1명
                    }

                    // 날짜 포맷 통일 (입력이 2025-06-03 등으로 올 수 있으므로 /로 변환)
                    String targetDate = dateOnly.replace("-", "/");

                    // 조건 일치 확인 (강의실, 날짜, 시작시간)
                    if (rRoom.equals(room) && rDate.equals(targetDate) && rStart.equals(startTime)) {
                        // 거절된(REJECTED) 예약은 제외하고, 승인(APPROVED)과 대기(WAIT)를 모두 합산
                        if ("APPROVED".equals(rStatus) || "WAIT".equals(rStatus)) {
                            currentTotalCount += count;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //[Singleton Pattern 적용]
        // BuildingManager 클래스의 유일한 인스턴스를 가져와서 해당 강의실의 최대 수용 인원을 조회
        // new BuildingManager()를 하지 않고 getInstance()를 사용.
        int maxCapacity = BuildingManager.getInstance().getRoomCapacity(room);

        // 결과 반환: [현재 예약된 총 인원, 최대 수용 인원]
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
                    if (equalsReserveInfo(line, reserveInfo)) {
                        deleted = true;
                        continue; // 쓰지 않고 건너뜀 (삭제)
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
        String tRoom = target[1].trim();
        String tDate = target[2].trim();
        String tStart = target[4].trim();

        String status = checkReservationStatus(tRoom,
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
                    if (parts[1].trim().equals(tRoom)
                            && parts[2].trim().equals(tDate)
                            && parts[4].trim().equals(tStart)
                            && parts[7].trim().equals("P")
                            && !"REJECTED".equals(parts[10].trim())) {
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
        if (target.length < 6) {
            return 0;
        }

        String tRoom = target[1].trim();
        String tDate = target[2].trim();
        String tStart = target[4].trim();
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 11) {
                    if (parts[1].trim().equals(tRoom)
                            && parts[2].trim().equals(tDate)
                            && parts[4].trim().equals(tStart)
                            && !"REJECTED".equals(parts[10].trim())) {
                        // 인원수(9번 인덱스)를 더해야 함 (SFR-202 40명 제한)
                        try {
                            count += Integer.parseInt(parts[9].trim());
                        } catch (NumberFormatException e) {
                            count++; // 인원 파싱 실패시 1명으로 간주
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
    private static String checkReservationStatus(String room, LocalDate date, String startTime) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        AbstractReservationChecker checker = new TextFileReservationChecker();
        return checker.checkStatus(room, dateString, dayName, startTime);
    }

    // 한글 상태 메시지 변환 (일별 테이블용)
    private static String convertStatusToKorean(String code) {
        switch (code) {
            case "CLASS":
                return "정규 수업";
            case "RESERVED":
                return "예약 대기";
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

    // --- Strategy 실행 ---
    public static ReserveResult reserve(ReservationDetails details) {
        ReservationStrategy strategy;
        if ("P".equals(details.getRole())) {
            strategy = new ProfessorReservationStrategy();
        } else if ("S".equals(details.getRole())) {
            strategy = new StudentReservationStrategy();
        } else {
            return new ReserveResult(false, "알 수 없는 역할");
        }
        return strategy.execute(details);
    }

    // --- Other Methods ---
    public static String makeReserveInfo(String room, String date, String day) {
        return String.format("%s,%s,%s", room, date, day); // 임시 포맷
    }

    public static boolean equalsReserveInfo(String info1, String info2) {
        return info1.trim().equals(info2.trim());
    }

    public static List<String> cancelStudentReservesForProfessor(String roomNumber, String date, String day) {
        return new ArrayList<>();
    }

    public static boolean hasOtherProfessorReserve(String newReserve, String selfId) {
        return false;
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

    // 미사용 또는 클라이언트 호환용 (빈 구현)
    public static List<String> getReserveInfoById(String id) {
        return new ArrayList<>();
    }

    public static List<String> getReserveInfoAdvanced(String i, String r, String d) {
        return new ArrayList<>();
    }

    public static ReserveResult updateReserve(ReservationDetails d) {
        return new ReserveResult(false, "");
    }

    // 예약 내역 조회
    public static ReserveManageResult searchUserAndReservations(String userId, String room, String date) {
    List<String[]> resultList = new ArrayList<>();

    synchronized (FILE_LOCK) {
        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length < 12) continue;

                String building = parts[0].trim();
                String roomNum  = parts[1].trim();
                String fullDate = parts[2].trim(); // yyyy/MM/dd
                String weekDay  = parts[3].trim();
                String start    = parts[4].trim();
                String end      = parts[5].trim();
                String id       = parts[6].trim();
                String content  = parts[8].trim();
                String status   = parts[10].trim();
                String reason   = parts[11].trim();

                // 날짜 쪼개기
                String[] d = fullDate.split("/");
                if (d.length < 3) continue;
                String year  = d[0];
                String month = d[1];
                String day   = d[2];

                // === 검색 조건 필터 ===
                if (userId != null && !userId.isEmpty() && !id.equals(userId)) continue;
                if (room   != null && !room.isEmpty()   && !roomNum.equals(room)) continue;
                if (date   != null && !date.isEmpty()   && !fullDate.equals(date)) continue;

                // JTable 컬럼 순서에 맞게 한 줄 구성
                String[] row = {
                    building,   // 건물
                    id,         // 사용자 ID
                    roomNum,    // 강의실
                    year,       // 년
                    month,      // 월
                    day,        // 일
                    start,      // 시작 시간
                    end,        // 종료 시간
                    weekDay,    // 요일
                    content,    // 내용
                    status,     // 상태
                    reason      // 사유
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
    public static ReserveManageResult approveOrReject(String command, String userId, String reserveInfo, String reason) {
        synchronized (FILE_LOCK) {
            File file = new File(RESERVE_FILE);
            if (!file.exists()) {
                return new ReserveManageResult(false, "예약 파일이 존재하지 않습니다.", null);
            }

            List<String> lines = new ArrayList<>();
            boolean updated = false;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // 예약 정보 비교
                    if (line.contains(reserveInfo)) {
                        String[] arr = line.split(",");

                        // 현재 상태 (WAIT, APPROVED, REJECTED)
                        String status = arr[arr.length - 2];

                        // 승인일 경우
                        if (command.equals("APPROVE")) {
                            arr[arr.length - 2] = "APPROVED";  // 상태 변경
                            arr[arr.length - 1] = "-";         // 사유 초기화
                        //거절일 경우
                        } else if (command.equals("REJECT")) {
                            arr[arr.length - 2] = "REJECTED";  // 상태 변경
                            arr[arr.length - 1] = reason;      // 사유 저장
                        }

                        line = String.join(",", arr);
                        updated = true;
                    }
                    lines.add(line);
                }
            } catch (Exception e) {
                return new ReserveManageResult(false, "파일 읽기 오류: " + e.getMessage(), null);
            }

            if (!updated) return new ReserveManageResult(false, "해당 예약을 찾을 수 없습니다.", null);

            // 파일 덮어쓰기
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (String l : lines) {
                    bw.write(l);
                    bw.newLine();
                }
            } catch (Exception e) {
                return new ReserveManageResult(false, "파일 저장 오류: " + e.getMessage(), null);
            }

            return new ReserveManageResult(true, 
            (command.equals("APPROVE") ? "승인 완료!" : "거절 완료!"), null);
        }
    }

    public static List<String> getUserIdsByReserveInfo(String r) {
        return new ArrayList<>();
    }
}
