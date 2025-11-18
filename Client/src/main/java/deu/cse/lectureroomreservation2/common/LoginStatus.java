package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;
import java.util.Objects;

public class LoginStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean loginSuccess;
    private String role;
    private String message;

    // 기본 생성자
    public LoginStatus() {
        this.loginSuccess = false;
        this.role = "NONE";
        this.message = null;
    }

    // 전체 필드 생성자
    public LoginStatus(boolean loginSuccess, String role, String message) {
        this.loginSuccess = loginSuccess;
        this.role = role;
        this.message = message;
    }

    // ======= Getter / Setter =======
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // ======= equals / hashCode =======
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Boolean.hashCode(this.loginSuccess);
        hash = 67 * hash + Objects.hashCode(this.role);
        hash = 67 * hash + Objects.hashCode(this.message);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        LoginStatus other = (LoginStatus) obj;
        return loginSuccess == other.loginSuccess
                && Objects.equals(role, other.role)
                && Objects.equals(message, other.message);
    }
}
