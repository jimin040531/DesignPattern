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
    
    private ReservationBuilder builder; 
    
    public void setBuilder(ReservationBuilder builder) {
        this.builder = builder;
    }
    
    // 복합 객체 조립 과정 캡슐화 (시간 정보 파라미터 제거)
    public ReservationDetails constructReservation(
            String buildingName, String roomNumber, String date, String day,
            String startTime, String endTime,
            String purpose, int userCount) { // ★ startTime, endTime 제거
        
        if (this.builder == null) {
            throw new IllegalStateException("Builder가 설정되지 않았습니다.");
        }
        
        // 1. 필수 정보 조립 
        builder.buildBaseInfo(buildingName, roomNumber, date, day, startTime, endTime);
        
        // 2. 선택적 정보 조립
        builder.buildPurpose(purpose);
        builder.buildUserCount(userCount);
        
        // 3. 완성된 객체 반환
        return builder.getReservationDetails();
    }
}