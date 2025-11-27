package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.server.model.Notification;
import deu.cse.lectureroomreservation2.server.model.Reservation;
import deu.cse.lectureroomreservation2.server.model.ReservationIterator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Strategy Pattern: 교수 예약 전략 (최종 정책 반영)
 * [적용된 패턴] 1. Builder: 알림 객체 생성 2.Iterator: 학생 예약 탐색 3. Observer: 알림 전송
 * 최종 정책: 총 예약 건수 3개 제한
 */
public class ProfessorReservation implements ReservationBehavior {

    // [Builder Pattern] 4. Client가 AbstractBuilder를 사용
    private NotificationBuilder notificationBuilder;
    // 예약 정보 파일 경로
    private final String reservationFile = receiveController.getReservationInfoFileName();

    public ProfessorReservation() {
        // [Builder Pattern] 3. 구상 빌더 주입
        this.notificationBuilder = new StudentCancellationBuilder();
    }

    @Override
    public ReserveResult reserve(ReservationDetails details) {

        String profId = details.getId();
        String room = details.getRoomNumber();
        String buildingName = details.getBuildingName();
        // date는 이미 'yyyy / MM / dd' 형식으로 가정
        String dateOnly = details.getDate().replace(" ", ""); 
        String day = details.getDay();
        
        // 1. 시간 정보를 Details 객체에서 바로 가져옴
        String startTime = details.getStartTime(); 
        String endTime = details.getEndTime();
        
        // 날짜비교용 (yyyy-MM-dd)
        String dateStr = dateOnly.replace("/", "-").trim(); 

        // -----------------------------------------
        // 2. 제약 조건 검사
        // -----------------------------------------

        // (1) 다른 교수 예약 중복 확인
        if (ReserveManager.isSlotTakenByOtherProfessor(room, dateOnly, startTime, profId)) {
            return new ReserveResult(false, "이미 다른 교수가 해당 시간에 예약했습니다.");
        }

        // (2) 최종 정책: 최대 예약 건수 (3개) 제한 
        int existingBookedCount = getUserBookedCount(profId, dateStr); 
        final int MAX_RESERVATIONS = 3;

        if (existingBookedCount >= MAX_RESERVATIONS) {
            return new ReserveResult(false, 
                String.format("예약 건수 초과: 교수는 특정 날짜에 최대 %d건(총 3시간)까지만 예약 가능합니다. (현재 %d건)", 
                MAX_RESERVATIONS, existingBookedCount));
        }

        // (3) 정원 초과 확인
        int requestCount = details.getUserCount();  
        BuildingManager bm = BuildingManager.getInstance();
        int maxCapacity = bm.getRoomCapacity(buildingName, room);
        
        if (requestCount > maxCapacity) {
                return new ReserveResult(false, 
                String.format("인원 초과: 신청(%d명)인원이 강의실 정원(%d명)를 초과합니다.", 
                requestCount, maxCapacity));
        }


        // ============================================================
        // [Iterator Pattern] 겹치는 학생 예약 탐색 및 취소 
        // ============================================================
        List<String> allLines = ReserveManager.getAllReservations();
        ReservationAggregate reservationList = new StudentReservationList(allLines);
        ReservationIterator iterator = reservationList.iterator();

        while (iterator.hasNext()) {
            Reservation res = iterator.next();

           if (res.isValidStudentReservation() 
                    && res.getRoom() != null && res.getRoom().equals(room)
                    && res.getDate() != null && res.getDate().equals(dateOnly.replace("/", "-")) // 날짜 비교 포맷 통일
                    && res.getStart() != null && res.getStart().equals(startTime)) {

                ReserveManager.cancelReserve(res.getUserId(), res.getRawLine());
                System.out.println(">> [교수 예약 권한] 학생 예약 취소됨: " + res.getUserId());

                // [Builder/Observer Pattern] 알림 객체 생성 및 전송
                notificationBuilder.createNewNotification();
                notificationBuilder.buildRecipientInfo(res.getUserId(), "S");
                notificationBuilder.buildMessageContent(room, dateOnly, startTime);
                notificationBuilder.buildPriority();

                Notification notification = notificationBuilder.getNotification();

                NotificationService.getInstance().notifyObserver(
                        notification.getTargetUserId(),
                        notification.getFormattedMessage()
                );
            }
        }
        
        // 5. 교수 예약 확정 (대기 없이 바로 APPROVED)
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                buildingName, room, dateOnly, day, 
                startTime, endTime, 
                profId, "P", details.getPurpose(), details.getUserCount(), "APPROVED", "-"
        );

        return ReserveManager.writeReservationToFile(profId, csvLine, "P");
    }
    
    // 예약 건수 카운팅 헬퍼 (학생/교수 모두 동일 로직 사용 가능)
    private int getUserBookedCount(String userId, String targetDateStr) {
        int count = 0;
        String targetDateSlash = targetDateStr.replace("-", "/"); 

        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue;
                
                String rDate = parts[2].trim();
                String rId = parts[6].trim();
                String rStatus = parts[10].trim(); 

                if (rId.equals(userId) && rDate.equals(targetDateSlash) && !"REJECTED".equals(rStatus)) {
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }
    
}