/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.LoginStatus;
import deu.cse.lectureroomreservation2.server.model.UserData;
import deu.cse.lectureroomreservation2.server.model.User;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author skylo
 */
public class LoginController {
    
    private Path userFile;
    private final UserData userData = new UserData();
    
    public LoginController() {
        this.userFile = Paths.get(receiveController.getUserFileName()); // 기본 파일
    }

    public LoginController(Path testPath) {
        this.userFile = testPath; // 테스트용 생성자
    }

    public LoginStatus authenticate(String id, String password, String selectedRole) {
        LoginStatus status = new LoginStatus();
        status.setLoginSuccess(false);
        status.setRole("NONE");

        if (id == null || password == null || selectedRole == null
                || id.trim().isEmpty() || password.trim().isEmpty() || selectedRole.trim().isEmpty()) {
            return status;
        }

        selectedRole = selectedRole.trim().toUpperCase();

        if (!selectedRole.equals("S") && !selectedRole.equals("P") && !selectedRole.equals("A")) {
            return status;
        }

        var optionalUser = userData.getUser(id, password, selectedRole);

        if (optionalUser.isEmpty()) {
            return status;
        }

        User user = optionalUser.get();

        boolean valid = switch (user.getRole()) {
            case "S" ->
                id.length() == 8 && id.matches("\\d{8}");
            case "P" ->
                id.length() == 5 && id.matches("\\d{5}");
            case "A" ->
                true;
            default ->
                false;
        };

        if (!valid) {
            return status;
        }

        status.setLoginSuccess(true);
        status.setRole(switch (user.getRole()) {
            case "S" ->
                "STUDENT";
            case "P" ->
                "PROFESSOR";
            case "A" ->
                "ADMIN";
            default ->
                "NONE";
        });

        return status;
    }
}
