package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;

public class StudentReservationStrategy implements ReservationStrategy {

    @Override
    public ReserveResult execute(ReservationDetails details) {
        String id = details.getId();
        String room = details.getRoomNumber();
        String fullDateInfo = details.getDate(); // "2025 / 06 / 03 / 10:00 11:00"
        String day = details.getDay();   // "화요일"

        // 날짜와 시간 파싱
        String[] tokens = fullDateInfo.split("/");
        if(tokens.length < 4) return new ReserveResult(false, "날짜 형식 오류");
        
        String dateOnly = tokens[0].trim() + "/" + tokens[1].trim() + "/" + tokens[2].trim();
        String[] times = tokens[3].trim().split(" ");
        String startTime = times[0];
        String endTime = times[1];

        // [포맷] 건물, 강의실, 날짜, 요일, 시작, 종료, ID, 역할, 목적, 인원, 상태, 사유
        // 건물명은 임시로 "공학관" (추후 클라이언트에서 받거나 맵핑 필요)
        String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                "공학관", room, dateOnly, day, startTime, endTime, id, "S",
                "개인학습", "1", "WAIT", "-");

        // 4. 파일 쓰기 (중복 체크는 Checker가 하지만 여기선 생략)
        return ReserveManager.writeReservationToFile(id, csvLine, "S");
    }
}