/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

/**
 *
 * @author Jimin
 */
public class UserManage {
    protected String role;
    protected String name;
    protected String id;
    protected String password;

    public UserManage(String role, String name, String id, String password) {
        this.role = role;
        this.name = name;
        this.id = id;
        this.password = password;
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
}
