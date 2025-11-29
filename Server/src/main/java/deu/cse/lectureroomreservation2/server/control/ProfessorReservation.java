package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.server.model.Notification;
import deu.cse.lectureroomreservation2.server.model.Reservation;
import deu.cse.lectureroomreservation2.server.model.ReservationIterator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Strategy Pattern: 교수 예약 전략 (최종 정책 반영)
 * [적용된 패턴] 1. Builder: 알림 객체 생성 2.Iterator: 학생 예약 탐색 3. Observer: 알림 전송
 * 최종 정책: 총 예약 건수 3개 제한 (날짜 불문)
 */
public class ProfessorReservation implements ReservationBehavior {

    private NotificationBuilder notificationBuilder;
    
    // 예약 정보 파일 경로
    private final String reservationFile = receiveController.getReservationInfoFileName();

    public ProfessorReservation() {
        // [Builder Pattern] 알림 빌더 초기화
        this.notificationBuilder = new StudentCancellationBuilder();
    }

    @Override
    public ReserveResult reserve(ReservationDetails details) {

        String profId = details.getId();
        String room = details.getRoomNumber();
        String buildingName = details.getBuildingName();
        
        // 1. 날짜 데이터 정규화 (핵심 수정 사항: 공백 제거 추가)
        // details.getDate()가 "2025 - 12 - 01" 같이 들어올 수 있으므로 공백 제거 필수
        // 예: "2025 - 12 - 01" -> "2025/12/01"
        String dateOnly = details.getDate().replace("-", "/").replace(" ", "").trim(); 
        
        String day = details.getDay();
        String startTime = details.getStartTime().trim(); 
        String endTime = details.getEndTime().trim();
        String purpose = details.getPurpose(); // 교수님의 예약 목적 (알림 사유로 사용)
        
        // 디버깅용 로그 (서버 콘솔 확인용)
        System.out.println(">> [DEBUG] 교수 예약 시도: " + room + "호 / " + dateOnly + " / " + startTime);

        // -----------------------------------------
        // 2. 제약 조건 검사
        // -----------------------------------------
        
        // (1) 다른 교수 예약 중복 확인
        if (ReserveManager.isSlotTakenByOtherProfessor(room, dateOnly, startTime, profId)) {
            return new ReserveResult(false, "이미 다른 교수가 해당 시간에 예약했습니다.");
        }

        // (2) 교수 총 예약 건수 제한 (3건)
        int totalBookedCount = getTotalReservedCount(profId);
        final int MAX_RESERVATIONS = 3;
        if (totalBookedCount >= MAX_RESERVATIONS) {
            return new ReserveResult(false, 
                String.format("예약 건수 초과: 교수는 총 %d건까지만 예약 가능합니다.", MAX_RESERVATIONS));
        }

        // (3) 인원 초과 확인
        int requestCount = details.getUserCount();  
        BuildingManager bm = BuildingManager.getInstance();
        int maxCapacity = bm.getRoomCapacity(buildingName, room);
        if (requestCount > maxCapacity) {
            return new ReserveResult(false, "인원 초과");
        }

        // ============================================================
        // [Iterator Pattern] 겹치는 학생 예약 탐색 및 취소
        // ============================================================
        List<String> allLines = ReserveManager.getAllReservations();
        ReservationAggregate reservationList = new StudentReservationList(allLines);
        ReservationIterator iterator = reservationList.iterator();

        while (iterator.hasNext()) {
            Reservation res = iterator.next();
            
            // 파일에 빈 줄이나 잘못된 데이터가 있어 필드가 null인 경우 건너뜀
            if (res.getRoom() == null || res.getDate() == null || res.getStart() == null) {
                continue;
            }
            
            // 파일에서 읽어온 데이터도 공백 제거 및 포맷 통일 후 비교
            String fileDate = res.getDate().replace("-", "/").replace(" ", "").trim();
            String fileRoom = res.getRoom().trim();
            String fileStart = res.getStart().trim();

            // 방 번호, 날짜, 시작 시간이 같고 + '학생' 예약인 경우
            if (res.isValidStudentReservation() 
                    && fileRoom.equals(room)
                    && fileDate.equals(dateOnly) // 공백 제거된 날짜끼리 비교
                    && fileStart.equals(startTime)) {

                // 1. 예약 취소 실행
                ReserveManager.cancelReserve(res.getUserId(), res.getRawLine());
                System.out.println(">> [교수 예약 권한] 학생 예약 취소됨: " + res.getUserId());

                // 2. [Builder Pattern] 알림 객체 생성 (공지사항 포함)
                notificationBuilder.createNewNotification();
                
                // 공지사항(사유) 주입
                if (notificationBuilder instanceof StudentCancellationBuilder) {
                    ((StudentCancellationBuilder) notificationBuilder).setCancellationReason(purpose);
                }
                
                notificationBuilder.buildRecipientInfo(res.getUserId(), "S");
                notificationBuilder.buildMessageContent(room, dateOnly, startTime);
                notificationBuilder.buildPriority();

                Notification notification = notificationBuilder.getNotification();

                // 3. [Observer Pattern] 알림 전송
                NotificationService.getInstance().notifyObserver(
                        notification.getTargetUserId(),
                        notification.getFormattedMessage()
                );
            }
        }
        
        // 5. 교수 예약 확정 (파일 저장)
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                buildingName, room, dateOnly, day, 
                startTime, endTime, 
                profId, "P", purpose, details.getUserCount(), "APPROVED", "-"
        );

        return ReserveManager.writeReservationToFile(profId, csvLine, "P");
    }
    
    // 교수 총 예약 건수 조회
    private int getTotalReservedCount(String userId) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue;
                String rId = parts[6].trim();
                String rStatus = parts[10].trim(); 
                if (rId.equals(userId) && !"REJECTED".equals(rStatus)) {
                    count++;
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return count;
    }
}