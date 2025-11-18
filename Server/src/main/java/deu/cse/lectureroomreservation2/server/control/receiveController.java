/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.ReserveRequest;
import java.io.*;
import java.nio.file.*;

/**
 *
 * @author H
 */
public class receiveController {

    // 1. 파일 경로 계산 (가장 먼저 실행됨)
    private static final String filePath = calculateResourcePath();
    
    // 2. 세부 파일 경로 설정
    private static final String UserFileName = filePath + File.separator + "UserInfo.txt";
    private static final String noticeFileName = filePath + File.separator + "noticeSave.txt";
    private static final String ScheduleInfoFileName = filePath + File.separator + "ScheduleInfo.txt";
    private static final String ReservationInfoFileName = filePath + File.separator + "ReservationInfo.txt";
    private static final String BuildingInfoFileName = filePath + File.separator + "BuildingInfo.txt";

    // 3. 경로 계산 로직 (팀원마다 다른 환경 대응)
    private static String calculateResourcePath() {
        String projectPath = System.getProperty("user.dir");
        // IDE나 실행 환경에 따라 'Server' 폴더 안일 수도, 밖일 수도 있음
        if (projectPath.endsWith("Server")) {
            return projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        } else {
            return projectPath + File.separator + "Server" + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        }
    }

    static {
        System.out.println(">>> 현재 데이터 파일 경로: " + filePath);
        
        // 디렉터리 없으면 생성
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 리소스 파일 복사 (파일이 없을 때만 복사하여 데이터 유지)
        copyResourceIfNotExists("UserInfo.txt", UserFileName);
        copyResourceIfNotExists("noticeSave.txt", noticeFileName);
        copyResourceIfNotExists("ScheduleInfo.txt", ScheduleInfoFileName);
        copyResourceIfNotExists("ReservationInfo.txt", ReservationInfoFileName);
        copyResourceIfNotExists("BuildingInfo.txt", BuildingInfoFileName);
    }

    private static void copyResourceIfNotExists(String resourceName, String destPath) {
        File destFile = new File(destPath);
        
        // 이미 파일이 존재하면 덮어쓰지 않고 유지
        if (destFile.exists()) {
            return;
        }

        try (InputStream in = receiveController.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                Files.copy(in, destFile.toPath());
                System.out.println("기본 파일 생성 완료: " + destPath);
            } else {
                System.out.println("기본 리소스를 찾을 수 없음 (빈 파일 생성): " + resourceName);
                destFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Getters ---
    public static String getFilepath() { return filePath; }
    public static String getUserFileName() { return UserFileName; }
    public static String getNoticeFileName() { return noticeFileName; }
    public static String getScheduleInfoFileName() { return ScheduleInfoFileName; }
    public static String getReservationInfoFileName() { return ReservationInfoFileName; }
    public static String getBuildingInfoFileName() { return BuildingInfoFileName; }

    // 예약 요청 처리
    public ReserveResult handleReserve(ReserveRequest req) {
        // 1. Builder를 사용하여 ReservationDetails 객체 생성
        ReservationDetails details = new ReservationDetails.Builder(req.getId(), req.getRole())
                .roomNumber(req.getRoomNumber())
                .date(req.getDate())
                .day(req.getDay())
                .build();
        
        // 2. ReserveManager에는 details 객체 하나만 전달
        return ReserveManager.reserve(details);
    }
}