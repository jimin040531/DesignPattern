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
    
    // 조립의 핵심 단계: 예약에 필수적인 정보 설정
    void buildBaseInfo(String buildingName, String roomNumber,
                       String date, String day,
                       String startTime, String endTime);
    
    // 조립의 선택적 단계
    void buildPurpose(String purpose);
    void buildUserCount(int userCount);
    
    // 완성된 Product 반환
    ReservationDetails getReservationDetails();
}
