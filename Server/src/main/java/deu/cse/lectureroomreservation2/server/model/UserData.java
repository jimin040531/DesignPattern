/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import deu.cse.lectureroomreservation2.server.control.receiveController;

/**
 *
 * @author Prof.Jong Min Lee
 */
public class UserData {

    private static final Path filePath = Paths.get(receiveController.getUserFileName());

    public Optional<User> getUser(String id, String password, String role) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split(",");
                /* if (tokens.length != 4) {
                    continue;
                }*/

                String fileRole = tokens[0].trim().toUpperCase();
                String fileId = tokens[2].trim();
                String filePw = tokens[3].trim();

                if (fileRole.equals(role.trim().toUpperCase())
                        && fileId.equals(id.trim())
                        && filePw.equals(password.trim())) {
                    return Optional.of(new User(fileId, filePw, fileRole));
                }
            }
        } catch (IOException e) {
            System.err.println("UserInfo.txt 읽기 오류: " + e.getMessage());
        }

        return Optional.empty();
    }

    // 비밀번호 변경
    public synchronized boolean updatePassword(String id, String currentPw, String newPw) {
        if (!Files.exists(filePath)) {
            System.err.println("파일이 존재하지 않습니다: " + filePath);
            return false;
        }
        try {
            List<String> lines = Files.readAllLines(filePath);
            List<String> updatedLines = new ArrayList<>();
            boolean updated = false;

            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String fileId = parts[2].trim();
                    String filePw = parts[3].trim();

                    if (fileId.equals(id.trim()) && filePw.equals(currentPw.trim())) {
                        parts[3] = newPw;
                        updated = true;
                    }

                    updatedLines.add(String.join(",", parts));
                } else {
                    updatedLines.add(line);
                }
            }

            if (updated) {
                Files.write(filePath, updatedLines);
            }

            return updated;

        } catch (IOException e) {
            System.err.println("비밀번호 변경 오류: " + e.getMessage());
            return false;
        }
    }
}
