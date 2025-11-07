/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import java.io.*;
import java.util.*;
import deu.cse.lectureroomreservation2.server.control.receiveController;

/**
 *
 * @author Jimin
 */
/**
 * 사용자 정보를 .txt 파일 기반으로 관리 저장 형식: role,name,id,password (CSV)
 */
public class UserFileManager {

    // UserInfo.txt 파일 경로 설정
    private static final String filePath = receiveController.getUserFileName();

    /**
     * 역할과 이름에 따라 사용자 검색
     *
     * @param roleFilter 역할 필터
     * @param nameFilter 이름 포함 문자열
     * @return 조건에 맞는 사용자 목록
     */
    public List<UserManage> searchUsers(String roleFilter, String nameFilter) {
        List<UserManage> result = new ArrayList<>();

        // 이름 필터가 비어있으면 전체 조회 방지
        if (nameFilter == null || nameFilter.trim().isEmpty()) {
            return result; // 아무 것도 반환하지 않음
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 5);  // 최대 5개까지만 자름 (예약정보까지 포함 가능)

                if (parts.length < 4) {
                    continue;
                }

                String role = parts[0].trim();
                String name = parts[1].trim();
                String id = parts[2].trim();
                String password = parts[3].trim();

                // 필터 조건에 맞으면 추가
                if (role.equals(roleFilter) && name.contains(nameFilter)) {
                    result.add(new UserManage(role, name, id, password));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 사용자 1명을 파일에 추가 저장
     *
     * @param user 저장할 사용자 객체
     */
    public void saveUser(UserManage user) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(String.join(",", user.getRole(), user.getName(), user.getId(), user.getPassword()));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 사용자 삭제 삭제 후 나머지 사용자 목록을 파일에 다시 저장
     *
     * @param role 대상 역할
     * @param id 대상 ID
     * @return 삭제 성공 여부
     */
    public boolean deleteUser(String role, String id) {
        if (role.equals("A")) {
            return false;
        }

        File inputFile = new File(filePath);
        List<String> updatedLines = new ArrayList<>();

        // 1. 메모리에 남길 라인들 저장
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", 4);
                if (parts.length < 4) {
                    continue;
                }

                String currentRole = parts[0].trim();
                String currentId = parts[2].trim();

                // 삭제 대상이면 저장하지 않음
                if (currentRole.equals(role) && currentId.equals(id)) {
                    continue;
                }

                updatedLines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // 2. 파일을 덮어쓰기 모드로 열고 updatedLines만 다시 저장
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile))) {
            for (String updatedLine : updatedLines) {
                writer.write(updatedLine);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * 파일에 저장된 모든 사용자 정보를 불러오기.
     *
     * @return 전체 사용자 목록
     */
    public List<UserManage> loadAllUsers() {
        List<UserManage> users = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 4) {
                    String role = tokens[0].trim();
                    String name = tokens[1].trim();
                    String id = tokens[2].trim();
                    String password = tokens[3].trim();

                    users.add(new UserManage(role, name, id, password));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return users;
    }

    public boolean isDuplicateId(String id) {
        List<UserManage> allUsers = loadAllUsers();
        for (UserManage user : allUsers) {
            if (user.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

}
