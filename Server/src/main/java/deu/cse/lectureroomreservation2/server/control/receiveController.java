/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.control.ReserveManager;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.ReserveRequest;
import java.io.*;
import java.nio.file.*;

import java.util.List;


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
        copyResourceIfNotExists("BuildingInfo.txt", BuildingInfoFileName);
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

    public static String getBuildingInfoFileName() {
        return BuildingInfoFileName;
    }
    
    // 예약 요청 처리
    public ReserveResult handleReserve(ReserveRequest req) {
        /*
         * // 교수 예약 처리 시작
         * if ("P".equals(req.getRole())) {
         * System.out.println("서버 받음 receiveController : "
         * + req.getId() + " " + req.getRole() + " " + req.getRoomNumber() + " " +
         * req.getDate() + " "
         * + req.getDay());
         * 
         * // 예약 처리부분
         * setReserveP reserve = new setReserveP(req.getId(), req.getRoomNumber(),
         * req.getDate(), req.getDay());
         * return new ReserveResult(reserve.getResult(), reserve.getReason());
         * }
         * // 학생 예약 처리 시작
         * else if ("S".equals(req.getRole())) {
         * System.out.println("서버 받음 receiveController : "
         * + req.getId() + " " + req.getRole() + " " + req.getRoomNumber() + " " +
         * req.getDate() + " "
         * + req.getDay());
         * 
         * // 예약 처리
         * setReserveS reserve = new setReserveS(req.getId(), req.getRoomNumber(),
         * req.getDate(), req.getDay());
         * return new ReserveResult(reserve.getResult(), reserve.getReason());
         * } else {
         * return new ReserveResult(false, "역할 오류");
         * }
         */

        if ("P".equals(req.getRole())) {
            // 교수 예약 시 학생 예약 중복 취소
            List<String> affectedStudents = ReserveManager.cancelStudentReservesForProfessor(
                    req.getRoomNumber(), req.getDate(), req.getDay());
            if (!affectedStudents.isEmpty() && req.getNotice() != null && !req.getNotice().isEmpty()) {
                noticeController.addNotice(affectedStudents, req.getNotice());
            }
        }

        // 역할별 분기 없이 ReserveManager에서 처리
        return ReserveManager.reserve(req.getId(), req.getRole(), req.getRoomNumber(), req.getDate(), req.getDay());

    }
}
