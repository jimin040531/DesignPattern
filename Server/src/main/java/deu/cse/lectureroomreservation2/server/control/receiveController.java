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

    public static String getBuildingInfoFileName() {
        return BuildingInfoFileName;
    }

    public ReserveResult handleReserve(ReserveRequest req) {

        // 2. 구상 빌더 인스턴스 생성
        ReservationBuilder builder = new ConcreteReservationBuilder(req.getId(), req.getRole());

        // 3. 디렉터 인스턴스 생성 및 빌더 설정
        ReservationDirector director = new ReservationDirector();
        director.setBuilder(builder);

        // 4. 디렉터에게 조립 요청 (분리된 시간 정보 전달)
        ReservationDetails details = director.constructReservation(
                req.getBuildingName(),
                req.getRoomNumber(),
                req.getDate(), 
                req.getDay(),
                req.getStartTime(), 
                req.getEndTime(), 
                req.getPurpose(),
                req.getUserCount()
        ); // constructReservation 호출 끝

        // 5. ReserveManager에 details 객체 전달
        return ReserveManager.reserve(details);
    }
}
