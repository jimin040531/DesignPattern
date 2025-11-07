/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;

/**
 *
 * @author Jimin
 */

/**
 * 클라이언트 -> 서버로 사용자 관련 요청을 전달할 때 사용
 * 직렬화 가능하게 만들어 소켓 통신을 통해 전송
 */
public class UserRequest implements Serializable {

    private final String command; // 요청 명령 : ADD 또는 DELETE 또는 SEARCH
    private final String role;
    private final String name;
    private final String id;
    private final String password;
    private final String nameFilter;

    public UserRequest(String command, String role, String name, String id, String password, String nameFilter) {
        this.command = command;
        this.role = role;
        this.name = name;
        this.id = id;
        this.password = password;
        this.nameFilter = nameFilter;
    }

    public String getCommand() {
        return command;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getNameFilter() {
        return nameFilter;
    }
}
