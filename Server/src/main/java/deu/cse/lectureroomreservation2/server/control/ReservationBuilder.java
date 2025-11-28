/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author Jimin
 */
// 빌더 패턴의 AbstractBuilder 역할
public interface ReservationBuilder {
    void buildBaseInfo(String buildingName, String roomNumber,
                       String date, String day,
                       String startTime, String endTime);
    void buildPurpose(String purpose);
    void buildUserCount(int userCount);
    ReservationDetails getReservationDetails();
}



