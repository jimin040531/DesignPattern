package deu.cse.lectureroomreservation2.server.control;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AutoReserveCleaner extends Thread {
    private final String userFilePath = receiveController.getUserFileName();

    public AutoReserveCleaner() {
        setDaemon(true); // 서버 종료시 자동 종료
    }

    @Override
    public void run() {
        while (true) {
            try {
                cleanOldReserves();
                System.out.println("오래된 예약 정보 자동 정리 완료");
                Thread.sleep(30 * 1000); // 60초마다 실행 (원하면 더 짧게/길게 조정)
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanOldReserves() {
        List<String> lines = new ArrayList<>();
        boolean changed = false; // 변경 여부 플래그

        try (BufferedReader br = new BufferedReader(new FileReader(userFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // UserInfo.txt: 역할,이름,ID,비번,예약1,예약2,...
                if (parts.length < 5) {
                    lines.add(line);
                    continue;
                }
                List<String> validReserves = new ArrayList<>();
                for (int i = 4; i < parts.length; i++) {
                    String reserve = parts[i].trim();
                    String[] reserveParts = reserve.split("/");
                    if (reserveParts.length < 5)
                        continue;
                    String datePart = reserveParts[1].trim() + "-" + reserveParts[2].trim() + "-"
                            + reserveParts[3].trim();
                    String timePart = reserveParts[4].trim().split(" ")[1]; // 끝나는 시간
                    try {
                        LocalDateTime endDateTime = LocalDateTime.parse(
                                datePart + "T" + timePart,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                        if (endDateTime.isAfter(LocalDateTime.now())) {
                            validReserves.add(reserve);
                        } else {
                            changed = true;
                        }
                    } catch (Exception e) {
                        validReserves.add(reserve); // 파싱 실패시 일단 남김
                    }
                }
                // 라인 재구성: 역할,이름,ID,비번,예약1,예약2,...
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 4; i++)
                    sb.append(parts[i]).append(i < 3 ? "," : "");
                for (String r : validReserves)
                    sb.append(",").append(r);
                lines.add(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 변경이 있을 때만 파일에 다시 저장
        if (changed) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(userFilePath))) {
                for (String l : lines) {
                    bw.write(l);
                    bw.newLine();
                }
                System.out.println("오래된 예약 정보가 삭제되어 파일이 갱신되었습니다.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("오래된 예약 정보가 없어 파일을 갱신하지 않았습니다.");
        }
    }
}