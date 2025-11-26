/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

/**
 *
 * @author win10
 */
public class EncryptedUserFileManager extends UserFileManager {

    @Override
    public void saveUser(UserManage user) {
        // 1) 원래 비밀번호 가져오기
        String rawPw = user.getPassword();

        // 2) 암호화
        String encryptedPw = PasswordUtil.encrypt(rawPw);

        // 3) 암호화된 비밀번호를 가진 새 UserManage 객체 생성
        UserManage encryptedUser =
                new UserManage(user.getRole(), user.getName(), user.getId(), encryptedPw);

        // 4) 나머지 저장 로직은 원래 UserFileManager(super)에 맡김
        super.saveUser(encryptedUser);
    }

}