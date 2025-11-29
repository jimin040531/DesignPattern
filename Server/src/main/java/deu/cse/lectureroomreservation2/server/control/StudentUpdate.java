// 파일 경로: deu/cse/lectureroomreservation2/server/control/StudentUpdate.java
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import java.time.LocalTime;

public class StudentUpdate implements ReservationUpdateBehavior {

    @Override
    public ReserveResult update(ReservationDetails details) {
        
        String id = details.getId();
        String role = details.getRole();
        String oldReserveInfo = details.getOldReserveInfo();
        
        // 새 예약 정보
        String newRoom = details.getNewRoomNumber();
        String newBuilding = details.getBuildingName(); // 건물명
        String fullDateInfo = details.getNewDate();     // "2025 / 12 / 01 / 17:00 17:50"
        String newDay = details.getNewDay();
        String purpose = details.getPurpose();
        int userCount = details.getUserCount();

        // 1. 날짜/시간 파싱
        String[] tokens = fullDateInfo.split("/");
        // "2025/12/01"
        String dateOnly = tokens[0].trim() + "/" + tokens[1].trim() + "/" + tokens[2].trim();
        
        String[] times = tokens[3].trim().split(" ");
        String startTimeStr = times[0]; 
        String endTimeStr = times[1]; // 이미 50분 뒤로 계산되어 옴 (Client ViewRoom 로직상)

        // 비교 및 확인용 임시 포맷 (HasProfessorReserve 등에서 사용)
        String tempReserveCheck = ReserveManager.makeReserveInfo(newBuilding, newRoom, dateOnly, newDay, startTimeStr, endTimeStr);
        
        // 2. 제약 조건 확인
        // (1) 교수 예약 중복 확인
        if (ReserveManager.hasProfessorReserve(tempReserveCheck)) {
            return new ReserveResult(false, "해당 시간은 교수 예약으로 인해 예약할 수 없습니다.");
        }

        // (2) 40명 인원 제한 확인
        int currentCount = ReserveManager.countUsersByReserveInfo(tempReserveCheck);
        if (currentCount + userCount > 40) {
             // return new ReserveResult(false, "인원 초과");
        }

        // 3. 기존 예약 삭제
        ReserveResult cancelResult = ReserveManager.cancelReserve(id, oldReserveInfo);
        if (!cancelResult.getResult()) {
            return new ReserveResult(false, "기존 예약 삭제 실패: " + cancelResult.getReason());
        }

        // 4. 새 예약 추가 (완전한 12칸 포맷 사용)
        // 포맷: 건물,강의실,날짜,요일,시작,끝,ID,역할,목적,인원,상태,사유
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                newBuilding,    // 건물
                newRoom,        // 강의실
                dateOnly,       // 날짜
                newDay,         // 요일
                startTimeStr,   // 시작
                endTimeStr,     // 끝
                id,             // ID
                "S",            // 역할
                purpose,        // 목적
                userCount,      // 인원
                "WAIT",         // 상태 (변경 시 다시 대기)
                "-"             // 사유
        );

        ReserveResult reserveResult = ReserveManager.writeReservationToFile(id, csvLine, "S");

        // 5. 실패 시 롤백
        if (!reserveResult.getResult()) {
            ReserveManager.restoreReservation(id, role, oldReserveInfo); 
            return new ReserveResult(false, "예약 변경 실패 (기존 예약 복원됨)");
        }

        return new ReserveResult(true, "예약이 성공적으로 수정되었습니다.");
    }
}