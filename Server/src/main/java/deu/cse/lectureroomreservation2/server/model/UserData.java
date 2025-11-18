/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import deu.cse.lectureroomreservation2.server.control.receiveController;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 사용자 데이터 처리 클래스
 * 저장 형식: role,name,id,password
 */
public class UserData {

    // UserInfo.txt 파일 경로
    private static final Path filePath = Paths.get(receiveController.getUserFileName());

    /**
     * 로그인 시 사용자 조회
     * 암호화된 비밀번호를 매칭하여 확인
     */
    public Optional<User> getUser(String id, String password, String role) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split(",");

                if (tokens.length < 4) continue;

                String fileRole = tokens[0].trim().toUpperCase();
                String fileName = tokens[1].trim();
                String fileId = tokens[2].trim();
                String filePw = tokens[3].trim(); // 암호화된 비밀번호

                // 역할, 아이디 매칭 후 암호화 검증
                if (fileRole.equals(role.trim().toUpperCase())
                        && fileId.equals(id.trim())
                        && PasswordUtil.matches(password.trim(), filePw)) {

                    return Optional.of(new User(fileId, filePw, fileRole));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * 새로운 사용자 추가 (이미 암호화된 비번 저장)
     */
    public void saveUser(User user) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) {
            writer.write(String.join(",", user.getRole(), user.getName(), user.getId(), user.getPassword()));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 모든 사용자 목록 불러오기
     */
    public List<UserManage> loadAllUsers() {
        List<UserManage> users = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");

                if (tokens.length == 4) {
                    users.add(new UserManage(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            tokens[2].trim(),
                            tokens[3].trim()
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return users;
    }

    /**
     * ID 중복 검사
     */
    public boolean isDuplicateId(String id) {
        return loadAllUsers().stream()
                .anyMatch(u -> u.getId().equals(id));
    }

    /**
     * 비밀번호 변경 — 암호화된 비밀번호 사용
     */
    public boolean updatePassword(String userId, String currentPw, String newPw) {
        File file = filePath.toFile();
        List<String> lines = new ArrayList<>();

        boolean updated = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.split(",", 4);
                if (tokens.length < 4) {
                    lines.add(line);
                    continue;
                }

                String role = tokens[0].trim();
                String name = tokens[1].trim();
                String id = tokens[2].trim();
                String pw = tokens[3].trim(); // 암호화된 실제 비밀번호

                // ★ ID와 현재 비밀번호가 정확히 맞는지 확인 (암호화 비교)
                if (id.equals(userId.trim())
                        && PasswordUtil.matches(currentPw.trim(), pw)) {

                    // ★ 새 비밀번호 암호화
                    String encryptedNewPw = PasswordUtil.encrypt(newPw.trim());
                    tokens[3] = encryptedNewPw;

                    updated = true;

                    String newLine = String.join(",", tokens[0], tokens[1], tokens[2], tokens[3]);
                    lines.add(newLine);

                } else {
                    lines.add(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!updated) return false;

        // 파일 전체 덮어쓰기
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String l : lines) {
                writer.write(l);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
