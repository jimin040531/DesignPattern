package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.UserData;

public class ChangePassController {

    private UserData userData;

    public ChangePassController() {
        userData = new UserData();
    }

    /**
     * 비밀번호 변경을 시도하는 메서드
     */
    public String changePassword(String userId, String currentPw, String newPw) {
        // 유효성 검사
        if (userId == null || currentPw == null || newPw == null
                || userId.trim().isEmpty() || currentPw.trim().isEmpty() || newPw.trim().isEmpty()) {
            return "모든 항목을 입력해주세요.";
        }

        if (currentPw.equals(newPw)) {
            return "기존 비밀번호와 동일합니다. 다른 비밀번호를 입력하세요.";
        }

        try {
            boolean updated = userData.updatePassword(userId, currentPw, newPw);

            if (updated) {
                return "SUCCESS";
            } else {
                return "ID 또는 현재 비밀번호가 일치하지 않습니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "비밀번호 변경 중 오류가 발생했습니다.";
        }
    }
}
