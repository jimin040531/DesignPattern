/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.common;

import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.io.*;

/**
 *
 * @author Prof.Jong Min Lee
 */
@Getter
@Setter
@Builder
public class LoginStatus implements Serializable {

    private static final long serialVersionUID = 1L;
    @Builder.Default
    private Boolean loginSuccess;

    @Builder.Default
    private String role;
    private String message;

    public LoginStatus() {
        this.loginSuccess = false;
        this.role = "NONE";
        this.message = null;
    }

    public LoginStatus(Boolean loginSuccess, String role, String message) {
        this.loginSuccess = loginSuccess;
        this.role = role;
        this.message = message;
    }

    public boolean isLoginSuccess() {
        return loginSuccess;
    }

    public void setLoginSuccess(boolean loginSuccess) {
        this.loginSuccess = loginSuccess;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // equals & hashCode 자동 생성됨
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Boolean.hashCode(this.loginSuccess);
        hash = 67 * hash + Objects.hashCode(this.role);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LoginStatus other = (LoginStatus) obj;
        //return this.loginSuccess == other.loginSuccess
        //       && Objects.equals(this.role, other.role);
        return Objects.equals(loginSuccess, other.loginSuccess)
                && Objects.equals(role, other.role);
    }

}
