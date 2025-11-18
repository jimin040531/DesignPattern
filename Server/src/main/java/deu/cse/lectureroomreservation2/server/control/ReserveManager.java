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
    // ★ [수정 1] 월별 조회 시 "AVAILABLE" -> "NONE"으로 변경하여 클라이언트로 전송
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
        } catch (IOException e) { e.printStackTrace(); }

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
                         if(!"REJECTED".equals(parts[10].trim())) {
                            // [수정] 인덱스 4(시작), 5(종료) 저장
                            slots.add(new String[]{parts[4].trim(), parts[5].trim()});
                         }
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        
        // 중복 제거 및 정렬
        Set<String> uniqueSlots = new HashSet<>();
        List<String[]> uniqueList = new ArrayList<>();
        for(String[] s : slots) {
            if(uniqueSlots.add(s[0] + s[1])) uniqueList.add(s);
        }
        uniqueList.sort(Comparator.comparing(o -> o[0]));

        return uniqueList;
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

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                
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

    // ★ [수정] 예약 복구 (롤백용)
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
        if(target.length < 6) return false; // 포맷 안맞으면 패스
        
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
                if(parts.length >= 11) {
                    if(parts[1].trim().equals(tRoom) && 
                       parts[2].trim().equals(tDate) && 
                       parts[4].trim().equals(tStart) &&
                       parts[7].trim().equals("P") &&
                       !"REJECTED".equals(parts[10].trim())) {
                        return true;
                    }
                }
            }
        } catch(IOException e) { e.printStackTrace(); }
        return false;
    }
    
    public static int countUsersByReserveInfo(String reserveInfo) {
        String[] target = reserveInfo.split(",");
        if(target.length < 6) return 0;
        
        String tRoom = target[1].trim();
        String tDate = target[2].trim();
        String tStart = target[4].trim();
        int count = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(RESERVE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if(parts.length >= 11) {
                    if(parts[1].trim().equals(tRoom) && 
                       parts[2].trim().equals(tDate) && 
                       parts[4].trim().equals(tStart) &&
                       !"REJECTED".equals(parts[10].trim())) {
                        // 인원수(9번 인덱스)를 더해야 함 (SFR-202 40명 제한)
                        try {
                            count += Integer.parseInt(parts[9].trim());
                        } catch(NumberFormatException e) {
                            count++; // 인원 파싱 실패시 1명으로 간주
                        }
                    }
                }
            }
        } catch(IOException e) { e.printStackTrace(); }
        return count;
    }
    
    // --- Helpers ---
    
    private static String checkReservationStatus(String room, LocalDate date, String startTime) {
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        AbstractReservationChecker checker = new TextFileReservationChecker();
        return checker.checkStatus(room, dateString, dayName, startTime);
    }

    // ★ [수정 2] 한글 상태 메시지 변환 (일별 테이블용)
    private static String convertStatusToKorean(String code) {
        switch (code) {
            case "CLASS": return "정규 수업";
            case "RESERVED": return "예약 대기";
            case "AVAILABLE": return "예약 가능";
            default: return "";
        }
    }
    
    private static String calculateEndTime(String startTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime start = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.parse(startTime, formatter));
            return start.plusMinutes(50).format(formatter);
        } catch (Exception e) { return "End"; }
    }
    
    // --- Strategy 실행 ---
    public static ReserveResult reserve(ReservationDetails details) {
        ReservationStrategy strategy;
        if ("P".equals(details.getRole())) strategy = new ProfessorReservationStrategy();
        else if ("S".equals(details.getRole())) strategy = new StudentReservationStrategy();
        else return new ReserveResult(false, "알 수 없는 역할");
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
    
    // 미사용 또는 클라이언트 호환용 (빈 구현)
    public static List<String> getReserveInfoById(String id) { return new ArrayList<>(); }
    public static List<String> getReserveInfoAdvanced(String i, String r, String d) { return new ArrayList<>(); }
    public static ReserveResult updateReserve(ReservationDetails d) { return new ReserveResult(false, ""); }
    public static ReserveManageResult searchUserAndReservations(String u, String r, String d) { return null; }
    public static List<String> getUserIdsByReserveInfo(String r) { return new ArrayList<>(); }
}