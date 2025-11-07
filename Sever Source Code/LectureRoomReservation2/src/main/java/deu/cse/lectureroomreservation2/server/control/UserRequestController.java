/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.UserManage;
import deu.cse.lectureroomreservation2.server.model.UserFileManager;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jimin
 */
/**
 * 클라이언트의 사용자 관련 요청(UserRequest)을 처리 사용자 추가, 삭제, 검색 -> 모델(UserFileManager)로 처리
 */
public class UserRequestController {

    // 사용자 정보를 파일에 저장하고 읽어오는 모델 객체
    private final UserFileManager FileManager = new UserFileManager();

    /**
     * 사용자 검색 요청 처리
     *
     * @param roleFilter 역할 필터 ("P", "S")
     * @param nameFilter 이름 포함 필터 (ex : "길동")
     * @return 사용자 정보를 [role, name, id, password] 형식으로 반환
     */
    public List<String[]> handleSearchRequest(String roleFilter, String nameFilter) {
        List<UserManage> users = FileManager.searchUsers(roleFilter, nameFilter);
        List<String[]> result = new ArrayList<>();

        for (UserManage user : users) {
            result.add(new String[]{
                user.getRole(), user.getName(), user.getId(), user.getPassword()
            });
        }

        return result;
    }

    /**
     * 사용자 삭제 처리
     *
     * @param role 역할 (P/S)
     * @param id 사용자 ID
     * @return 삭제 성공 여부
     */
    public boolean deleteUser(String role, String id) {
        return FileManager.deleteUser(role, id);
    }

    /**
     * 사용자 저장 전에 ID 중복을 검사하고, 중복이 없으면 저장 후 newUser만 반환
     *
     * @param newUser [role, name, id, password]
     * @return 저장한 사용자 1명의 정보 리스트
     */
    public List<String[]> saveUserAndGetSingleUser(String[] newUser) {
        String role = newUser[0];
        String name = newUser[1];
        String id = newUser[2];
        String password = newUser[3];

        // 중복 검사 (교수 & 학생 ID도 중복 X)
        if (FileManager.isDuplicateId(id)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 저장
        UserManage user = new UserManage(role, name, id, password);
        FileManager.saveUser(user);

        // 1명만 리스트로 반환
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{role, name, id, password});
        return result;
    }
}
