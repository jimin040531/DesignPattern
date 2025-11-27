package deu.cse.lectureroomreservation2.server.control;

import java.io.*;

public class AutoReserveCleaner extends Thread {
    
    public AutoReserveCleaner() {
        setDaemon(true); // 서버 메인 스레드 종료 시 함께 종료
    }

   @Override
    public void run() {
        System.out.println(">>> [AutoReserveCleaner] 자동 예약 정리 시스템 가동 (주기: 1분)");
        while (true) {
            try {
                // 예약 정리 로직 실행 (지난 예약 삭제)
                ReserveManager.purgePastReservations();
                 
                // 1분(60초)마다 검사하도록 변경
                Thread.sleep(60 * 1000); // 1분 대기
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}