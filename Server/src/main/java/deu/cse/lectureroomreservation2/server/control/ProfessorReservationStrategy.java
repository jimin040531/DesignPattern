package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.util.List;

/**
 * Strategy Pattern: 교수 예약 전략 (SFR-208)
 * - (SFR-208) 학생 예약을 강제로 취소시킵니다.
 * - 다른 교수의 예약과 겹치는지 확인합니다.
 * - 개인 예약 개수 제한(4개)이 없습니다.
 */
public class ProfessorReservationStrategy implements ReservationStrategy {

    @Override
    public ReserveResult execute(ReservationDetails details) {
        
        // Builder로 받은 데이터 추출
        String id = details.getId();
        String roomNumber = details.getRoomNumber();
        String date = details.getDate();
        String day = details.getDay();
        
        // 포맷팅된 예약 문자열 생성
        String newReserve = ReserveManager.makeReserveInfo(roomNumber, date, day);

        // 1. (SFR-208) 학생 예약 강제 취소 및 알림
        List<String> affectedStudents = ReserveManager.cancelStudentReservesForProfessor(roomNumber, date, day);
        if (!affectedStudents.isEmpty()) {
            String message = newReserve + " 예약이 교수 예약으로 인해 자동 취소되었습니다.";
            noticeController.addNotice(affectedStudents, message);
        }

        // 2. 다른 교수 예약 중복 확인
        if (ReserveManager.hasOtherProfessorReserve(newReserve, id)) {
            return new ReserveResult(false, "이미 다른 교수가 해당 시간에 예약했습니다.");
        }

        // 3. 파일 쓰기 (교수는 4개 제한 없음 "P" 전달)
        return ReserveManager.writeReservationToFile(id, newReserve, "P");
    }
}