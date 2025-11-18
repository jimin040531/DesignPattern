/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.ReserveRequest;
import java.io.*;
import java.nio.file.*;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 *
 * @author H
 */
public class receiveController {

    // 파일 경로 및 이름 지정 // 2025.05.26 추가
    // 리소스 파일을 사용자 홈 디렉터리 아래에 저장
    private static final String filePath = System.getProperty("user.home") + File.separator + "resources";
    private static final String UserFileName = filePath + File.separator + "UserInfo.txt";
    private static final String noticeFileName = filePath + File.separator + "noticeSave.txt";
    private static final String ScheduleInfoFileName = filePath + File.separator + "ScheduleInfo.txt";
    private static final String ReservationInfoFileName = filePath + File.separator + "ReservationInfo.txt";
    private static final String BuildingInfoFileName = filePath + File.separator + "BuildingInfo.txt";

    static {
        // 디렉터리 없으면 생성
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 리소스 파일 복사
        copyResourceIfNotExists("UserInfo.txt", UserFileName);
        copyResourceIfNotExists("noticeSave.txt", noticeFileName);
        copyResourceIfNotExists("ScheduleInfo.txt", ScheduleInfoFileName);
        copyResourceIfNotExists("ReservationInfo.txt", ReservationInfoFileName);
        //copyResourceIfNotExists("BuildingInfo.txt", BuildingInfoFileName);
    }

    private static void copyResourceIfNotExists(String resourceName, String destPath) {
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            try (InputStream in = receiveController.class.getClassLoader().getResourceAsStream(resourceName)) {
                if (in != null) {
                    Files.copy(in, destFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getFilepath() {
        return filePath;
    }

    public static String getUserFileName() {
        return UserFileName;
    }

    public static String getNoticeFileName() {
        return noticeFileName;
    }

    public static String getScheduleInfoFileName() {
        return ScheduleInfoFileName;
    }

    public static String getReservationInfoFileName() {
        return ReservationInfoFileName;
    }

   /**
     * BuildingInfo.txt 파일의 경로 반환
     * 프로젝트 내부 리소스 폴더에서 직접 읽어옴
     * @return "BuildingInfo.txt"의 실제 절대 경로
     */
    public static String getBuildingInfoFileName() {
        return BuildingInfoFileName;
    }
    
    /**
     * 리소스 파일의 실제 경로 반환
     * @param resourceName 찾으려는 파일 이름 (예: "BuildingInfo.txt")
     * @return 파일의 실제 절대 경로
     */
    private static String getResourcePath(String resourceName) {
        try {
            // 이 로직은 JAR/WAR 내부 경로를 반환하므로, BuildingInfo 처리가 변경된 후에는 사용하지 않습니다.
            URL resourceUrl = receiveController.class.getClassLoader().getResource(resourceName);
            if (resourceUrl == null) {
                throw new FileNotFoundException("Resource not found in classpath: " + resourceName);
            }
            // 절대 경로가 아닌 복사된 파일을 사용해야 합니다.
            // 하지만 기존 코드를 유지하기 위해 그대로 둡니다.
            return java.nio.file.Paths.get(resourceUrl.toURI()).toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not get resource path", e);
        }
    }

    // 예약 요청 처리
    public ReserveResult handleReserve(ReserveRequest req) {

        // 1. Builder를 사용하여 ReservationDetails 객체 생성
        ReservationDetails details = new ReservationDetails.Builder(req.getId(), req.getRole())
                .roomNumber(req.getRoomNumber())
                .date(req.getDate())
                .day(req.getDay())
                // .purpose(req.getPurpose()) // (SFR-206 확장 시)
                // .numberOfStudents(req.getCount()) // (SFR-204 확장 시)
                .build();
        
        // 2. ReserveManager에는 details 객체 하나만 전달
        return ReserveManager.reserve(details);
    }
    
    /* [삭제됨] - 메서드 중복 오류
    public ReserveResult handleReserve(ReserveRequest req) {
        // 역할별 분기 없이 ReserveManager의 새 reserve 메서드를 호출
        // ReserveManager가 내부적으로 Strategy를 선택하여 처리함
        return ReserveManager.reserve(req.getId(), req.getRole(), req.getRoomNumber(), req.getDate(), req.getDay());
    }
    */
}