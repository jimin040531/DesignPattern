/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author Jimin
 */
public class ReservationDirector {
    
    // AbstractBuilder에 대한 연관
    private ReservationBuilder builder; 
    
    public void setBuilder(ReservationBuilder builder) {
        this.builder = builder;
    }
    
    // 복합 객체 조립 과정 캡슐화
    public ReservationDetails constructReservation(
            String buildingName, String roomNumber, String date, String day,
            String startTime, String endTime,
            String purpose, int userCount) {        
        if (this.builder == null) {
            throw new IllegalStateException("Builder가 설정되지 않았습니다.");
        }
        builder.buildBaseInfo(buildingName, roomNumber, date, day, startTime, endTime);
        builder.buildPurpose(purpose);
        builder.buildUserCount(userCount);
        return builder.getReservationDetails();
    }
}
