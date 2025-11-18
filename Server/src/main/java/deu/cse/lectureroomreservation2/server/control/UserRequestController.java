package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.UserManage;
import deu.cse.lectureroomreservation2.server.model.UserFileManager;
import deu.cse.lectureroomreservation2.server.model.EncryptedUserFileManager;
import java.util.ArrayList;
import java.util.List;

public class UserRequestController {

    // 데코레이터 적용
    private UserFileManager userFileManager = new EncryptedUserFileManager();

    /**
     * 사용자 검색
     */
    public List<String[]> handleSearchRequest(String roleFilter, String nameFilter) {
        List<UserManage> users = userFileManager.searchUsers(roleFilter, nameFilter);
        List<String[]> result = new ArrayList<>();

        for (UserManage user : users) {
            result.add(new String[]{
                user.getRole(), user.getName(), user.getId(), user.getPassword()
            });
        }
        return result;
    }

    /**
     * 사용자 삭제
     */
    public boolean deleteUser(String role, String id) {
        return userFileManager.deleteUser(role, id); // ✔ 수정
    }

    /**
     * ID 중복 검사 & 저장
     */
    public List<String[]> saveUserAndGetSingleUser(String[] newUser) {
        String role = newUser[0];
        String name = newUser[1];
        String id = newUser[2];
        String password = newUser[3];

        // ✔ 중복 검사 수정
        if (userFileManager.isDuplicateId(id)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // ✔ 암호화 저장하도록 수정됨
        UserManage user = new UserManage(role, name, id, password);
        userFileManager.saveUser(user);

        List<String[]> result = new ArrayList<>();
        result.add(new String[]{role, name, id, password});
        return result;
    }
}
