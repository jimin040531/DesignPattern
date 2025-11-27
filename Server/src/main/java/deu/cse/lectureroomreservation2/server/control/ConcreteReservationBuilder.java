/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import java.time.Duration;
import java.time.LocalTime;

/**
 *
 * @author Jimin
 */
public class ConcreteReservationBuilder implements ReservationBuilder {
    private ReservationDetails reservationDetails;
    
    // 생성 시점에 Product 객체를 생성 (필수 정보: id, role)
    public ConcreteReservationBuilder(String id, String role) {
        this.reservationDetails = new ReservationDetails(id, role);
    }

    @Override
    public void buildBaseInfo(String buildingName, String roomNumber, String date, String day, String startTime, String endTime) {
        // 조립 단계 1: 핵심 예약 정보 설정
        reservationDetails.setBuildingName(buildingName);
        reservationDetails.setRoomNumber(roomNumber);
        reservationDetails.setDate(date);
        reservationDetails.setDay(day);
        reservationDetails.setStartTime(startTime);
        reservationDetails.setEndTime(endTime);
    }

    @Override
    public void buildPurpose(String purpose) {
        // 조립 단계 2: 목적 설정
        reservationDetails.setPurpose(purpose);
    }

    @Override
    public void buildUserCount(int userCount) {
        // 조립 단계 3: 인원 설정
        reservationDetails.setUserCount(userCount);
    }

    @Override
    public ReservationDetails getReservationDetails() {
        validateReservationDuration();
        return reservationDetails;
    }
    
    // 2시간 제한을 검증하는 헬퍼 메서드
    private void validateReservationDuration() {
        String startTime = reservationDetails.getStartTime();
        String endTime = reservationDetails.getEndTime();
        
        try {
            // 시간 파싱 (HH:mm 형식)
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            
            // Duration 계산
            long durationMinutes = Duration.between(start, end).toMinutes();
            
            // 요구사항: 최대 2시간(120분) 검사
            if (durationMinutes <= 0 || durationMinutes > 120) {
                throw new IllegalArgumentException("예약 시간은 최소 1분, 최대 2시간(120분)을 초과할 수 없습니다.");
            }
        } catch (Exception e) {
            // 시간 파싱 오류 또는 유효성 검사 실패 시 생성 실패
            throw new IllegalStateException("예약 시간 정보가 유효하지 않거나 형식이 잘못되었습니다: " + e.getMessage());
        }
    }
}
