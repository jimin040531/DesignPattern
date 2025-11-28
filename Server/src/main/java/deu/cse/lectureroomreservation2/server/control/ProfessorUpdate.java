// 파일 경로: deu/cse/lectureroomreservation2/server/control/ProfessorUpdate.java
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.util.List;

public class ProfessorUpdate implements ReservationUpdateBehavior {

    @Override
    public ReserveResult update(ReservationDetails details) {
        
        String id = details.getId();
        String role = details.getRole();
        String oldReserveInfo = details.getOldReserveInfo();
        
        String newRoom = details.getNewRoomNumber();
        String newBuilding = details.getBuildingName();
        String fullDateInfo = details.getNewDate();
        String newDay = details.getNewDay();
        String purpose = details.getPurpose();
        int userCount = details.getUserCount();

        // 1. 날짜/시간 파싱
        String[] tokens = fullDateInfo.split("/");
        String dateOnly = tokens[0].trim() + "/" + tokens[1].trim() + "/" + tokens[2].trim();
        
        String[] times = tokens[3].trim().split(" ");
        String startTimeStr = times[0]; 
        String endTimeStr = times[1];

        String tempReserveCheck = ReserveManager.makeReserveInfo(newBuilding, newRoom, dateOnly, newDay, startTimeStr, endTimeStr);

        // 2. 다른 교수 예약 확인
        if (ReserveManager.hasOtherProfessorReserve(tempReserveCheck, id)) {
            return new ReserveResult(false, "이미 다른 교수가 해당 시간에 예약했습니다.");
        }

        // 3. 기존 예약 삭제
        ReserveResult cancelResult = ReserveManager.cancelReserve(id, oldReserveInfo);
        if (!cancelResult.getResult()) {
            return new ReserveResult(false, "기존 예약 삭제 실패");
        }

        // 4. 학생 예약 취소 및 알림 (SFR-208)
        List<String> affectedStudents = ReserveManager.cancelStudentReservesForProfessor(newRoom, dateOnly, startTimeStr); 
        // (참고: ReserveManager.cancelStudentReservesForProfessor 메서드 내부 구현이 필요할 수 있음)
        
        // 5. 새 예약 추가 (12칸 포맷)
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                newBuilding, 
                newRoom, 
                dateOnly, 
                newDay, 
                startTimeStr, 
                endTimeStr, 
                id, 
                "P", 
                purpose, 
                userCount, 
                "APPROVED", // 교수는 바로 승인
                "-"
        );

        ReserveResult reserveResult = ReserveManager.writeReservationToFile(id, csvLine, "P");
        
        // 6. 롤백
        if (!reserveResult.getResult()) {
            ReserveManager.restoreReservation(id, role, oldReserveInfo);
            return new ReserveResult(false, "변경 실패 (복원됨)");
        }

        return new ReserveResult(true, "예약이 성공적으로 수정되었습니다.");
    }
}