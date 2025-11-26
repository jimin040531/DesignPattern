/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

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
    public void buildBaseInfo(String buildingName, String roomNumber, String date, String day) {
        // 조립 단계 1: 핵심 예약 정보 설정
        reservationDetails.setBuildingName(buildingName);
        reservationDetails.setRoomNumber(roomNumber);
        reservationDetails.setDate(date);
        reservationDetails.setDay(day);
        // setStartTime, setEndTime 호출 제거
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
        return reservationDetails;
    }
}
