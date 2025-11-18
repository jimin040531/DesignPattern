package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;

/**
 * Strategy Pattern: 학생 예약 전략
 */
public class StudentReservationStrategy implements ReservationStrategy {

    @Override
    public ReserveResult execute(ReservationDetails details) {
        
        // Builder로 받은 데이터 추출
        String id = details.getId();
        String roomNumber = details.getRoomNumber();
        String date = details.getDate();
        String day = details.getDay();
        
        // 포맷팅된 예약 문자열 생성
        String newReserve = ReserveManager.makeReserveInfo(roomNumber, date, day);

        // 1. 교수 예약 겹치는지 확인
        if (ReserveManager.hasProfessorReserve(newReserve)) {
            return new ReserveResult(false, "해당 시간은 교수 예약이 존재하여 예약할 수 없습니다.");
        }

        // 2. 40명 인원 제한 확인 (SFR-202)
        int userCount = ReserveManager.countUsersByReserveInfo(newReserve);
        if (userCount >= 40) {
            return new ReserveResult(false, "동일 시간대 최대 예약 인원(40명) 초과");
        }
        
        // 3. 파일 쓰기 (학생은 4개 제한 적용 "S" 전달)
        return ReserveManager.writeReservationToFile(id, newReserve, "S");
    }
}