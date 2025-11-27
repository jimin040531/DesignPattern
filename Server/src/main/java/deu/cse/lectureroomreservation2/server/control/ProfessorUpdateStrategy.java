package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.util.List;

/**
 * Strategy Pattern: 교수의 '예약 변경' 전략 (SFR-208)
 * - (기존 updateReserve 로직과 동일)
 */
public class ProfessorUpdateStrategy implements UpdateReservationStrategy {

    @Override
    public ReserveResult execute(ReservationDetails details) {
        
        // Builder로 받은 데이터 추출
        String id = details.getId();
        String role = details.getRole();
        String oldReserveInfo = details.getOldReserveInfo();
        String newRoom = details.getNewRoomNumber();
        String newDate = details.getNewDate();
        String newDay = details.getNewDay();

        // 포맷팅된 새 예약 문자열 생성
        String newReserve = ReserveManager.makeReserveInfo(newRoom, newDate, newDay);

        // 1. 새 예약 시간이 다른 교수와 겹치는지 확인
        if (ReserveManager.hasOtherProfessorReserve(newReserve, id)) {
            return new ReserveResult(false, "이미 다른 교수가 해당 시간에 예약했습니다. (기존 예약 유지됨)");
        }

        // 2. 기존 예약 삭제
        ReserveResult cancelResult = ReserveManager.cancelReserve(id, oldReserveInfo);
        if (!cancelResult.getResult()) {
            return new ReserveResult(false, "기존 예약 삭제 실패: " + cancelResult.getReason());
        }

        // 3. (SFR-208) 새 예약 시간의 학생 예약을 강제 취소 및 알림
        List<String> affectedStudents = ReserveManager.cancelStudentReservesForProfessor(newRoom, newDate, newDay);
        if (!affectedStudents.isEmpty()) {
            String message = newReserve + " 예약이 교수 예약으로 인해 자동 취소되었습니다.";
            noticeController.addNotice(affectedStudents, message);
        }

        // 4. 새 예약 추가 (헬퍼 메서드 사용, "P"로 4개 제한 무시)
        ReserveResult reserveResult = ReserveManager.writeReservationToFile(id, newReserve, "P");
        
        // 5. 롤백
        if (!reserveResult.getResult()) {
            ReserveManager.restoreReservation(id, role, oldReserveInfo); // 롤백
            return new ReserveResult(false, reserveResult.getReason() + " (기존 예약 복원됨)");
        }

        return new ReserveResult(true, "예약이 성공적으로 수정되었습니다.");
    }
}