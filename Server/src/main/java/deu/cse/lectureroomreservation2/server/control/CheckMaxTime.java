/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import java.io.*;

/**
 *
 * @author H
 * 
 * UserInfo.txt 파일 구조
 * 역할,이름,ID,비번,예약1,예약2,...
 */
public class CheckMaxTime implements Serializable {
    private String id;

    //private final String USER_FILE = "UserInfotest.txt";
    private final String USER_FILE = receiveController.getUserFileName();
    
    public CheckMaxTime(String id) {
        this.id = id;
    }

    // 예약 정보가 4개인지 확인
    public boolean check() {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // UserInfo.txt: 역할,이름,ID,비번,예약1,예약2,...
                if (parts.length >= 4 && parts[2].trim().equals(id)) {
                    // 예약 정보는 5번째(4 index)부터
                    int reserveCount = parts.length - 4;
                    return reserveCount == 4;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // id를 찾지 못하면 false
    }
}