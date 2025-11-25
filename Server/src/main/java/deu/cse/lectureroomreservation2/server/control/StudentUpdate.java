package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;

/**
 * Strategy Pattern: 학생의 '예약 변경' 전략
 * - (기존 updateReserve 로직과 동일 + SFR-202)
 */
public class StudentUpdate implements ReservationUpdateBehavior {

    @Override
    public ReserveResult update(ReservationDetails details) {
        
        // Builder로 받은 데이터 추출
        String id = details.getId();
        String role = details.getRole();
        String oldReserveInfo = details.getOldReserveInfo();
        String newRoom = details.getNewRoomNumber();
        String newDate = details.getNewDate();
        String newDay = details.getNewDay();
        
        // 포맷팅된 새 예약 문자열 생성
        String newReserve = ReserveManager.makeReserveInfo(newRoom, newDate, newDay);
        
        // 1. 새 예약 시간이 교수 예약과 겹치는지 확인
        if (ReserveManager.hasProfessorReserve(newReserve)) {
            return new ReserveResult(false, "해당 시간은 교수 예약으로 인해 예약할 수 없습니다. (기존 예약 유지됨)");
        }

        // 2. (SFR-202) 40명 인원 제한 확인
        int userCount = ReserveManager.countUsersByReserveInfo(newReserve);
        if (userCount >= 40) {
            return new ReserveResult(false, "동일 시간대 최대 예약 인원(40명) 초과 (기존 예약 유지됨)");
        }

        // 3. 기존 예약 삭제
        ReserveResult cancelResult = ReserveManager.cancelReserve(id, oldReserveInfo);
        if (!cancelResult.getResult()) {
            return new ReserveResult(false, "기존 예약 삭제 실패: " + cancelResult.getReason());
        }

        // 4. 새 예약 추가 (헬퍼 메서드 사용, "S"로 4개 제한 적용)
        ReserveResult reserveResult = ReserveManager.writeReservationToFile(id, newReserve, "S");

        // 5. 롤백
        if (!reserveResult.getResult()) {
            ReserveManager.restoreReservation(id, role, oldReserveInfo); // 롤백
            return new ReserveResult(false, reserveResult.getReason() + " (기존 예약 복원됨)");
        }

        return new ReserveResult(true, "예약이 성공적으로 수정되었습니다.");
    }
}