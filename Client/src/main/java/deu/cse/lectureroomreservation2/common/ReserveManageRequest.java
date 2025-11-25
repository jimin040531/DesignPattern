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
 * 클라이언트에서 서버로 예약 내역 관리 명령을 보낼 때 사용되는 객체
 * 예약 조회, 수정, 삭제 요청 시 필요한 데이터를 포함 함
 */
public class ReserveManageRequest implements Serializable {

    private final String command;       // 명령 종류 : SEARCH, UPDATE, DELETE
    private final String userId;        // 요청을 보낸 사용자 ID
    private final String building;      // 강의실 건물 (검색 시)
    private final String room;          // 강의실 번호 (검색 시)
    private final String date;          // 날짜 (검색 시)
    private final String oldReserveInfo; // 기존 예약 정보 (수정 시)
    private final String newBuilding;   // 강의실 건물 (수정 시)
    private final String newRoom;       // 새 강의실 번호 (수정 시)
    private final String newDate;       // 새 날짜 (수정 시)
    private final String newDay;        // 새 요일 (수정 시)
    private final String role;          // 사용자 역할 (필요 시)
    private final String reserveInfo;   // 예약 정보 (삭제 시)

    public ReserveManageRequest(String command, String userId, String building, String room, String date,
            String oldReserveInfo, String newBuilding, String newRoom, String newDate,
            String newDay, String role, String reserveInfo) {
        this.command = command;
        this.userId = userId;
        this.building = building;
        this.room = room;
        this.date = date;
        this.oldReserveInfo = oldReserveInfo;
        this.newBuilding = newBuilding;
        this.newRoom = newRoom;
        this.newDate = newDate;
        this.newDay = newDay;
        this.role = role;
        this.reserveInfo = reserveInfo;
    }

    public String getCommand() {
        return command;
    }

    public String getUserId() {
        return userId;
    }
    
    public String getBuilding() {
        return building;
    }
    

    public String getRoom() {
        return room;
    }

    public String getDate() {
        return date;
    }

    public String getOldReserveInfo() {
        return oldReserveInfo;
    }

    public String getNewBuilding() {
        return newBuilding;
    }
    

    public String getNewRoom() {
        return newRoom;
    }

    public String getNewDate() {
        return newDate;
    }

    public String getNewDay() {
        return newDay;
    }

    public String getRole() {
        return role;
    }

    public String getReserveInfo() {
        return reserveInfo;
    }
}
