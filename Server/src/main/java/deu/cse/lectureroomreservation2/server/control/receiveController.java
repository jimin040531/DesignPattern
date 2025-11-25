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
        if (projectPath.endsWith("Server")) {
            return projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        } else {
            return projectPath + File.separator + "Server"
                    + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        }
    }

    // 클래스 로딩 시 한 번만 실행
    static {
        System.out.println(">>> 현재 데이터 파일 경로: " + filePath);

        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        copyResourceIfNotExists("UserInfo.txt", UserFileName);
        copyResourceIfNotExists("noticeSave.txt", noticeFileName);
        copyResourceIfNotExists("ScheduleInfo.txt", ScheduleInfoFileName);
        copyResourceIfNotExists("ReservationInfo.txt", ReservationInfoFileName);
        copyResourceIfNotExists("BuildingInfo.txt", BuildingInfoFileName);
    }

    private static void copyResourceIfNotExists(String resourceName, String destPath) {
        File destFile = new File(destPath);
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
                .purpose(req.getPurpose()) 
                .userCount(req.getUserCount())
                // ★★★ [핵심 수정] 이 줄이 빠져 있어서 null이 저장되었습니다. ★★★
                .buildingName(req.getBuildingName()) 
                .build();

        // 2. ReserveManager에는 details 객체 하나만 전달
        return ReserveManager.reserve(details);
    }
}