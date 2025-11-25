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
 * Strategy Pattern: 교수 예약 전략 (SFR-208) 
 * [적용된 패턴] 1. Builder: 알림 객체 생성 2.Iterator: 학생 예약 탐색 3. Observer: 알림 전송
 */
public class ProfessorReservation implements ReservationBehavior {

    // [Builder Pattern] 4. Client가 AbstractBuilder를 사용
    private NotificationBuilder notificationBuilder;

    public ProfessorReservation() {
        // [Builder Pattern] 3. 구상 빌더 주입
        this.notificationBuilder = new StudentCancellationBuilder();
    }

    @Override
    public ReserveResult reserve(ReservationDetails details) {

        String profId = details.getId();
        String room = details.getRoomNumber();
        String buildingName = details.getBuildingName();
        String date = details.getDate(); // "yyyy / MM / dd / HH:mm HH:mm"
        String day = details.getDay();
        
        // 1. 날짜 및 시간 파싱
        String[] tokens = date.split("/");
        // "2025 / 06 / 03" 형태로 변환 (공백 제거)
        String dateOnly = tokens[0].trim() + "/" + tokens[1].trim() + "/" + tokens[2].trim();

        String[] times = tokens[3].trim().split(" ");
        String startTime = times[0];
        String endTime = times[1];

        // 2. 포맷팅된 예약 문자열 생성
        //String newReserve = ReserveManager.makeReserveInfo(room, dateOnly, day);

        // 3. 다른 교수 예약 중복 확인 (기존 로직 유지)
        if (ReserveManager.isSlotTakenByOtherProfessor(room, dateOnly, startTime, profId)) {
            return new ReserveResult(false, "이미 다른 교수가 해당 시간에 예약했습니다.");
        }

        // ============================================================
        // [Iterator Pattern] 겹치는 학생 예약 탐색 및 취소
        // ============================================================
        // 1. 전체 예약 데이터 가져오기
        List<String> allLines = ReserveManager.getAllReservations();

        // [Iterator Question 1] Aggregate 생성 (컬렉션 은닉)
        ReservationAggregate reservationList = new StudentReservationList(allLines);

        // [Iterator Question 2] Iterator 획득
        ReservationIterator iterator = reservationList.iterator();

        // [Iterator Question 3] Iterator를 사용하여 순회
        while (iterator.hasNext()) {
            Reservation res = iterator.next();

            // 조건: 같은 방, 같은 날짜, 같은 시작 시간이고 '학생' 예약인 경우
            // (교수는 3시간 예약 가능하지만, 여기선 단순화를 위해 시작 시간이 같은 경우만 처리하거나
            //  필요하면 시간대 포함 여부 로직을 추가할 수 있음. 일단 '시작 시간 일치'로 구현)
           if (res.isValidStudentReservation() 
                    && res.getRoom() != null && res.getRoom().equals(room)
                    && res.getDate() != null && res.getDate().equals(dateOnly)
                    && res.getStart() != null && res.getStart().equals(startTime)) {

                // -> 학생 예약 취소 실행
                ReserveManager.cancelReserve(res.getUserId(), res.getRawLine());
                System.out.println(">> [교수 예약 권한] 학생 예약 취소됨: " + res.getUserId());

                // ============================================================
                // [Builder Pattern] 알림 객체 생성 (복잡한 객체 조립)
                // ============================================================
                notificationBuilder.createNewNotification();
                notificationBuilder.buildRecipientInfo(res.getUserId(), "S");
                notificationBuilder.buildMessageContent(room, dateOnly, startTime);
                notificationBuilder.buildPriority();

                // 완성된 알림 객체 가져오기
                Notification notification = notificationBuilder.getNotification();

                //디버그: 알림이 정상 생성되었는지 서버 콘솔에서 확인
                System.out.println(">> [DEBUG] 취소 알림 생성됨: " + notification.getFormattedMessage());
                System.out.println(">> [DEBUG] 대상 사용자 ID: " + notification.getTargetUserId());

                // ============================================================
                // [Observer Pattern] 알림 전송 (로그인 여부에 따라 자동 처리)
                // ============================================================
                NotificationService.getInstance().notifyObserver(
                        notification.getTargetUserId(),
                        notification.getFormattedMessage()
                );
            }
        }
        
        int requestCount = details.getUserCount();  //요청 인원
        
        //건물 이름과 강의실 번호로 정확한 정원 조회
        BuildingManager bm = BuildingManager.getInstance();
        int maxCapacity = bm.getRoomCapacity(buildingName, room);
        
        if (requestCount > maxCapacity) {
                return new ReserveResult(false, 
                String.format("인원 초과: 신청(%d명)인원이 강의실 정원(%d명)를 초과합니다.", 
                requestCount, maxCapacity));
        }
        
        // 4시간 연속 예약 제한 검사
        String startTimeStr = times[0]; 
        // 날짜비교용
        String dateStr = tokens[0].trim() + "-" + tokens[1].trim() + "-" + tokens[2].trim(); 
        
        // 파일에서 해당 교수가 해당 날짜에 이미 예약한 시간(Hour)들을 가져옴
        Set<Integer> bookedHours = getUserBookedHours(profId, dateStr);
        LocalTime requestStart = LocalTime.parse(startTimeStr);
        int currentHour = requestStart.getHour(); // 예: 10시 신청 시 10
        
        // Case A: 12시 신청 -> (9시, 10시, 11시)가 이미 있음 -> 9,10,11 연속됨 -> 불가
        if (bookedHours.contains(currentHour - 3) && bookedHours.contains(currentHour -2) && bookedHours.contains(currentHour -1) ) {
            return new ReserveResult(false, "4시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }
        // Case B: 11시 신청 -> (9시, 10시, 12시)가 이미 있음 -> 8,9,10 연속됨 -> 불가
        if (bookedHours.contains(currentHour - 2) && bookedHours.contains(currentHour - 1) && bookedHours.contains(currentHour +1) ) {
            return new ReserveResult(false, "4시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }
        // Case C: 10시 신청 -> (9시, 11시, 12시)가 이미 있음 -> 10,11,12 연속됨 -> 불가
        if (bookedHours.contains(currentHour -1) && bookedHours.contains(currentHour + 1) && bookedHours.contains(currentHour + 2)) {
            return new ReserveResult(false, "4시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }
        // Case C: 9시 신청 -> (10시, 11시, 12시)가 이미 있음 -> 10,11,12 연속됨 -> 불가
        if (bookedHours.contains(currentHour +1) && bookedHours.contains(currentHour + 2) && bookedHours.contains(currentHour + 3)) {
            return new ReserveResult(false, "4시간 연속으로 수업을 들을 수 없어 예약이 불가능합니다.");
        }
        
        // 4. 교수 예약 확정 (대기 없이 바로 APPROVED)
        // 포맷: 건물,강의실,날짜,요일,시작,끝,ID,P,목적,인원(0),APPROVED,-
        // 예약 저장 시 건물 이름 포함 (이미 details에 있는 buildingName 사용)
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                buildingName, room, dateOnly, day, startTime, endTime,
                profId, "P", details.getPurpose(), details.getUserCount(), "APPROVED", "-"
        );

        return ReserveManager.writeReservationToFile(profId, csvLine, "P");
    }
    
    
    // 예약 정보 파일 경로 (연속 예약 확인용)
    private final String reservationFile = receiveController.getReservationInfoFileName();
    private Set<Integer> getUserBookedHours(String userId, String targetDate) {
        Set<Integer> hours = new HashSet<>();
        String targetDateSlash = targetDate.replace("-", "/"); 

        try (BufferedReader br = new BufferedReader(new FileReader(reservationFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 11) continue;
                
                String rDate = parts[2].trim();
                String rStart = parts[4].trim();
                String rId = parts[6].trim();
                String rStatus = parts[10].trim(); 

                if (rId.equals(userId) && rDate.equals(targetDateSlash) && !"REJECTED".equals(rStatus)) {
                    try {
                        int h = Integer.parseInt(rStart.split(":")[0]);
                        hours.add(h);
                    } catch (Exception e) { }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hours;
    }
}
