/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Jimin
 */

/**
 * 서버 -> 클라이언트로 사용자 관련 작업 결과를 전달할 때 사용
 * 직렬화 가능하고, 요청 성공 여부, 메시지, 사용자 리스트를 담고 있음
 */
public class UserResult implements Serializable {

    private final boolean success;
    private final String message;
    private final List<String[]> userList; // 검색 결과 -> 각 항목 : [role, name, id, password]

    public UserResult(boolean success, String message, List<String[]> userList) {
        this.success = success;
        this.message = message;
        this.userList = userList;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String[]> getUserList() {
        return userList;
    }
}
