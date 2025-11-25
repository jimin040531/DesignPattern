package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * 학생 예약 전략 구현체
 * - 당일 예약 불가
 * - 수용 인원 50% 제한 (Singleton 사용)
 * - 3시간 연속 예약 불가
 */
public class StudentReservationStrategy implements ReservationStrategy {

    // 예약 정보 파일 경로 (연속 예약 확인용)
    private final String reservationFile = receiveController.getReservationInfoFileName();

    @Override
    public ReserveResult execute(ReservationDetails details) {
        String id = details.getId();
        String room = details.getRoomNumber();
        String buildingName = details.getBuildingName();
        String fullDateInfo = details.getDate(); // 예: "2025 / 06 / 03 / 10:00 10:50"
        String day = details.getDay();           // 예: "화요일"
        int requestCount = details.getUserCount();
        String purpose = details.getPurpose();

        // -----------------------------------------
        // 1. 날짜 및 시간 파싱
        // -----------------------------------------
        String[] tokens = fullDateInfo.split("/");
        if (tokens.length < 4) {
            return new ReserveResult(false, "날짜 형식 오류");
        }
        
        String dateOnly = tokens[0].trim() + "/" + tokens[1].trim() + "/" + tokens[2].trim();
        // 날짜비교용
        String dateStr = tokens[0].trim() + "-" + tokens[1].trim() + "-" + tokens[2].trim();        
        // 시간: "10:00" (시작 시간만 중요)
        String[] times = tokens[3].trim().split(" ");
        String startTimeStr = times[0]; 

        // LocalTime, LocalDate 변환
        LocalDate reserveDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalTime requestStart = LocalTime.parse(startTimeStr);

        // -----------------------------------------
        // 2. 제약 조건 검사
        // -----------------------------------------

        // (1) 당일 예약 불가 (내일부터 가능)
        if (!reserveDate.isAfter(LocalDate.now())) {
            return new ReserveResult(false, "당일 예약은 불가능합니다. 최소 하루 전에 예약해주세요.");
        }

        // (2) [Singleton 패턴 사용] 수용 인원 50% 제한
        // BuildingManager의 유일한 인스턴스를 가져옴
        BuildingManager bm = BuildingManager.getInstance();
        int maxCapacity = bm.getRoomCapacity(buildingName, room);

        if (maxCapacity == 0) {
            return new ReserveResult(false, "강의실 정보(" + room + ")를 찾을 수 없습니다.");
        }

        // 해당 시간대의 기존 예약 인원 총합 구하기
        int currentReservedCount = getExistingReservedCount(room, dateOnly, startTimeStr);
        int totalExpectedCount = currentReservedCount + requestCount;
        int limitCount = (int)(maxCapacity * 0.5);

        if (totalExpectedCount > limitCount) {
            return new ReserveResult(false, 
                String.format("인원 초과: 현재 %d명 예약됨. 신청(%d명) 시 50%%(%d명)를 초과합니다.", 
                currentReservedCount, requestCount, limitCount));
        }

        // (3) 3시간 연속 예약 제한 검사
        // 파일에서 해당 학생이 해당 날짜에 이미 예약한 시간(Hour)들을 가져옴
        Set<Integer> bookedHours = getUserBookedHours(id, dateStr);
        int currentHour = requestStart.getHour(); // 예: 10시 신청 시 10

        // Case A: 10시 신청 -> (9시, 11시)가 이미 있음 -> 9,10,11 연속됨 -> 불가
        if (bookedHours.contains(currentHour - 1) && bookedHours.contains(currentHour + 1)) {
            return new ReserveResult(false, "3시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }
        // Case B: 10시 신청 -> (8시, 9시)가 이미 있음 -> 8,9,10 연속됨 -> 불가
        if (bookedHours.contains(currentHour - 1) && bookedHours.contains(currentHour - 2)) {
            return new ReserveResult(false, "3시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }
        // Case C: 10시 신청 -> (11시, 12시)가 이미 있음 -> 10,11,12 연속됨 -> 불가
        if (bookedHours.contains(currentHour + 1) && bookedHours.contains(currentHour + 2)) {
            return new ReserveResult(false, "3시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }

        // -----------------------------------------
        // 3. 파일 저장
        // -----------------------------------------
        
        
        // 종료 시간 계산 (50분 수업 고정)
        // 10:00 -> 10:50
        String endTimeStr = requestStart.plusMinutes(50).toString();

        // 포맷: 건물이름,강의실,예약날짜,요일,시작,끝,학번,권한(S),사용목적,사용인원,WAIT,-
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                buildingName,   // 1. 건물이름
                room,           
                dateOnly,       
                day,            
                startTimeStr,   
                endTimeStr,     
                id,             
                "S",            
                purpose,        
                requestCount,   
                "WAIT",         
                "-"             
        );

        // ReserveManager를 통해 파일에 기록
        return ReserveManager.writeReservationToFile(id, csvLine, "S");
    }

    /**
     * 해당 강의실, 날짜, 시작 시간에 이미 예약된 총 인원수를 반환
     */
    private int getExistingReservedCount(String room, String dateOnly, String startTime) {
        int totalCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue;

                String rRoom = parts[1].trim();
                String rDate = parts[2].trim();
                String rStart = parts[4].trim();
                String rStatus = parts[10].trim();

                if (rRoom.equals(room) && rDate.equals(dateOnly) && rStart.equals(startTime)) {
                    if (!"REJECTED".equals(rStatus)) {
                        try {
                            totalCount += Integer.parseInt(parts[9].trim());
                        } catch (NumberFormatException e) {
                            totalCount += 1; 
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalCount;
    }
    
    /**
     * 헬퍼 메서드: 특정 날짜에 해당 학생이 예약한 시간(Hour) 집합 반환
     * @param userId 학생 ID
     * @param targetDate 대상 날짜 (yyyy-MM-dd)
     * @return 예약된 시간들의 집합 (예: {9, 14, 15})
     */
    private Set<Integer> getUserBookedHours(String userId, String targetDate) {
        Set<Integer> hours = new HashSet<>();
        // 파일엔 "2025/06/03" 형식으로 저장되어 있으므로 변환
        String targetDateSlash = targetDate.replace("-", "/"); 

        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // 인덱스: 날짜(2), 시작시간(4), ID(6), 상태(10)
                if (parts.length < 11) continue;
                
                String rDate = parts[2].trim();
                String rStart = parts[4].trim(); // "09:00"
                String rId = parts[6].trim();
                String rStatus = parts[10].trim(); 

                // ID 일치, 날짜 일치, 거절된 예약이 아닌 경우
                if (rId.equals(userId) && rDate.equals(targetDateSlash) && !"REJECTED".equals(rStatus)) {
                    try {
                        // "09:00" -> 9 (int)
                        int h = Integer.parseInt(rStart.split(":")[0]);
                        hours.add(h);
                    } catch (Exception e) { /* 시간 파싱 오류 무시 */ }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hours;
    }
}