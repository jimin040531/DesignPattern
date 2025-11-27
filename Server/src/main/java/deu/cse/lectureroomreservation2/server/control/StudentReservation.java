package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 학생 예약 전략 구현체
 * - 당일 예약 불가
 * - 수용 인원 50% 제한 (Singleton 사용)
 * - 총 예약 건수 2개 제한 (날짜 불문 전체 합계)
 */
public class StudentReservation implements ReservationBehavior {

    // 예약 정보 파일 경로
    private final String reservationFile = receiveController.getReservationInfoFileName();

    @Override
    public ReserveResult reserve(ReservationDetails details) {
        String id = details.getId();
        String room = details.getRoomNumber();
        String buildingName = details.getBuildingName();
        String dateOnly = details.getDate();
        String day = details.getDay();
        String startTimeStr = details.getStartTime();
        String endTimeStr = details.getEndTime();
        int requestCount = details.getUserCount();
        String purpose = details.getPurpose();

        // -----------------------------------------
        // 1. 날짜 및 시간 파싱
        // -----------------------------------------
        String dateStr = dateOnly.replace("/", "-").trim(); // 날짜비교용
        
        // LocalDate 변환
        LocalDate reserveDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // -----------------------------------------
        // 2. 제약 조건 검사
        // -----------------------------------------

        // (1) 당일 예약 불가 (최소 하루 전)
        if (!reserveDate.isAfter(LocalDate.now())) {
            return new ReserveResult(false, "당일 예약은 불가능합니다. 최소 하루 전에 예약해주세요.");
        }

        // (2) [수정됨] 최종 정책: 총 예약 건수 (2개) 제한 (날짜 불문)
        // 기존 getUserBookedCount(id, dateStr) 대신 getTotalReservedCount(id) 사용
        int totalBookedCount = getTotalReservedCount(id); 
        final int MAX_TOTAL_RESERVATIONS = 2;

        if (totalBookedCount >= MAX_TOTAL_RESERVATIONS) {
            return new ReserveResult(false, 
                String.format("예약 건수 초과: 학생은 날짜와 상관없이 총 %d건까지만 예약할 수 있습니다. (현재 %d건 보유 중)", 
                MAX_TOTAL_RESERVATIONS, totalBookedCount));
        }

        // (3) [Singleton 패턴 사용] 수용 인원 50% 제한
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
        
        // -----------------------------------------
        // 3. 파일 저장
        // -----------------------------------------
        
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
     * [수정됨] 헬퍼 메서드: 날짜와 상관없이 해당 학생이 가진 총 유효 예약 건수 반환
     */
    private int getTotalReservedCount(String userId) {
        int count = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // 인덱스: ID(6), 상태(10)
                if (parts.length < 11) continue;
                
                String rId = parts[6].trim();
                String rStatus = parts[10].trim(); 

                // 날짜 비교 로직 제거 -> 전체 날짜 대상
                // ID가 일치하고, 거절된(REJECTED) 예약이 아닌 경우 카운트 (WAIT, APPROVED 모두 포함)
                if (rId.equals(userId) && !"REJECTED".equals(rStatus)) {
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }
}