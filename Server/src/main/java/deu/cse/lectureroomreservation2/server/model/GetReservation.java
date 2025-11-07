/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import deu.cse.lectureroomreservation2.server.control.receiveController;

/**
 *
 * @author namw2
 *
 */
public class GetReservation {

    //TODO 나중에 server로 옮기기
    //TODO 날짜 어떻게 할건지 생각해보기
    public static String[][] GetTime(String what) { //텍스트 파일을 읽어와서 배열에 저장 
        String ReservationInfoPath = receiveController.getReservationInfoFileName();
        String ScheduleInfoPath = receiveController.getScheduleInfoFileName();

        List<String[]> ReservationInfoList = new ArrayList<>();
        List<String[]> ScheduleInfoList = new ArrayList<>();

        try (BufferedReader Rbr = new BufferedReader(new FileReader(ReservationInfoPath)); BufferedReader Lbr = new BufferedReader(new FileReader(ScheduleInfoPath));) {
            String Rline, Lline;
            while ((Rline = Rbr.readLine()) != null) {
                // 쉼표 기준으로 나누고 배열에 저장 - 강의실 예약 정보
                String[] parts = Rline.split(",");
                ReservationInfoList.add(parts);
            }
            while ((Lline = Lbr.readLine()) != null) {
                // 쉼표 기준으로 나누고 배열에 저장 - 정규강의 정보
                String[] parts = Lline.split(",");
                ScheduleInfoList.add(parts);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[][] Rresult = new String[ReservationInfoList.size()][];
        String[][] Sresult = new String[ScheduleInfoList.size()][];
        Rresult = ReservationInfoList.toArray(Rresult);
        Sresult = ScheduleInfoList.toArray(Sresult);

        //System.out.println(Rresult[0][3]);
        for (String[] row : Rresult) {
            System.out.println(Arrays.toString(row));
        }
        for (String[] row : Sresult) {
            System.out.println(Arrays.toString(row));
        }

        if (what.equals("reservation")) {
            return Rresult;
        } else if (what.equals("schedule")) {
            return Sresult;
        }

        return Rresult;
    }

    public static String[] getPCTime() {
        LocalDateTime now = LocalDateTime.now();
        String[] ServerDateTime = {"00:00", "00/00/00", "X"}; // 저장 공간

        // 형식 지정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm+yy/MM/dd");
        String weekday = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);

        // 출력
        String formattedNow = now.format(formatter);
        System.out.println("현재 시간: " + formattedNow + ", 요일: " + weekday);

        // '+' 기준으로 문자열 나누기
        String[] parts = formattedNow.split("\\+");
        if (parts.length == 2) {
            ServerDateTime[0] = parts[0]; // "HH:mm"
            ServerDateTime[1] = parts[1]; // "yy/MM/dd"
            ServerDateTime[2] = weekday;
        }

        return ServerDateTime;
    }
}
