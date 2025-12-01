/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import java.io.*;
import java.util.*;
/**
 *
 * @author Jimin
 */
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import deu.cse.lectureroomreservation2.server.control.receiveController;

/**
 * 시간표 정보를 파일에서 읽고 쓰는 역할 수행 - readAllLines(): 파일 전체 읽기 - appendLine(): 한 줄 추가 -
 * overwriteAll(): 전체 덮어쓰기
 */
public class ScheduleFileManager {

    private final String filePath;

    // 기본 생성자 (기본 경로를 지정)
    public ScheduleFileManager() {
        this.filePath = receiveController.getScheduleInfoFileName(); // 기본 시간표 파일 경로
    }

    // 경로를 직접 받는 생성자
    public ScheduleFileManager(String filePath) {
        this.filePath = filePath;
    }

    public List<String[]> readAllLines() {
        List<String[]> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line.trim().split(","));
            }
        } catch (IOException e) {
            throw new RuntimeException("시간표 파일 읽기 실패", e);
        }
        return list;
    }

    public synchronized void appendLine(String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("시간표 추가 중 오류 발생", e);
        }
    }

    public synchronized void overwriteAll(List<String> newLines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : newLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("시간표 파일 덮어쓰기 실패", e);
        }
    }

    public boolean backupFile(String backupFileName) {
        try {
            // 원본: 현재 사용 중인 시간표 파일 (ScheduleInfo.txt)
            Path source = Paths.get(filePath);   // 예: .../Server/src/main/resources/ScheduleInfo.txt

            // 백업 파일: 서버 데이터 폴더 + 백업 이름
            Path target = Paths.get(receiveController.getFilepath(), backupFileName);

            System.out.println("백업 source = " + source.toAbsolutePath());
            System.out.println("백업 target = " + target.toAbsolutePath());

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

// 백업 파일을 ScheduleInfo.txt 로 복원하는 기능
    // 백업 파일을 ScheduleInfo.txt 로 복원하는 기능
    public boolean restoreFile(String backupFileName) {
        try {
            // 원본 시간표 파일 경로 (ScheduleInfo.txt)
            Path target = Paths.get(filePath);

            // 원본 파일이 있는 폴더 (src/main/resources)
            Path resourcesDir = target.getParent(); // .../src/main/resources

            // 그 폴더 안의 백업 파일 (예: ScheduleInfo_backup.txt)
            Path source = resourcesDir.resolve(backupFileName);

            System.out.println("복원 source 절대경로 = " + source.toAbsolutePath());
            System.out.println("복원 target 절대경로 = " + target.toAbsolutePath());

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
