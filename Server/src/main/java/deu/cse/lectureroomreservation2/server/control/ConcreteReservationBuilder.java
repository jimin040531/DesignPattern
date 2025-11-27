/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Jimin
 */
public class ConcreteReservationBuilder implements ReservationBuilder {

    private ReservationDetails reservationDetails;

    public ConcreteReservationBuilder(String id, String role) {
        this.reservationDetails = new ReservationDetails(id, role);
    }

    @Override
    public void buildBaseInfo(String buildingName, String roomNumber, String date, String day, String startTime, String endTime) {
        reservationDetails.setBuildingName(buildingName);
        reservationDetails.setRoomNumber(roomNumber);
        reservationDetails.setDate(date);
        reservationDetails.setDay(day);
        reservationDetails.setStartTime(startTime);
        reservationDetails.setEndTime(endTime);
    }

    @Override
    public void buildPurpose(String purpose) {
        reservationDetails.setPurpose(purpose);
    }

    @Override
    public void buildUserCount(int userCount) {
        reservationDetails.setUserCount(userCount);
    }

    @Override
    public ReservationDetails getReservationDetails() {
        // [수정] 객체 반환 전, 제약 조건 2가지를 검증합니다.
        validateReservationDuration(); // 1. 2시간 이내
        validateReservationDate();     // 2. 최소 하루 전 (학생만)

        return reservationDetails;
    }

    // 1. 시간 제한 검증 (수정됨)
    private void validateReservationDuration() {
        String startTime = reservationDetails.getStartTime();
        String endTime = reservationDetails.getEndTime();
        String role = reservationDetails.getRole(); // 역할 가져오기

        try {
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);

            // 분 단위 시간 차이 계산
            long durationMinutes = java.time.Duration.between(start, end).toMinutes();

            // 시간 역전(종료가 시작보다 빠름) 체크
            if (durationMinutes <= 0) {
                throw new IllegalArgumentException("종료 시간은 시작 시간보다 뒤여야 합니다.");
            }

            // [핵심 수정] 역할별 시간 제한 분기 처리
            if ("S".equals(role)) {
                // 학생: 최대 2시간 (120분)
                if (durationMinutes > 120) {
                    throw new IllegalArgumentException("학생은 1회 최대 2시간(120분)까지만 예약 가능합니다.");
                }
            } else if ("P".equals(role)) {
                // 교수: 최대 3시간 (180분) - SFR 205, 206 반영
                if (durationMinutes > 180) {
                    throw new IllegalArgumentException("교수는 1회 최대 3시간(180분)까지만 예약 가능합니다.");
                }
            }

        } catch (Exception e) {
            // IllegalArgumentException은 그대로 던지고, 파싱 에러 등은 메시지 포장
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalStateException("시간 형식 오류: " + e.getMessage());
        }
    }

    // 2. [수정] 날짜 검증 (학생은 당일 예약 불가)
    private void validateReservationDate() {
        if ("S".equals(reservationDetails.getRole())) {
            String dateStr = reservationDetails.getDate();
            try {
                // 날짜 포맷 통일 (yyyy/MM/dd)
                String cleanDate = dateStr.replace("-", "/").trim();
                LocalDate reserveDate = LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyy/MM/dd"));

                // 오늘 날짜와 비교 (예약 날짜가 오늘보다 뒤여야 함 -> 내일부터 가능)
                if (!reserveDate.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("당일 예약은 불가능합니다. 최소 하루 전에 예약해주세요.");
                }
            } catch (Exception e) {
                // IllegalArgumentException(위에서 우리가 던진 것)은 그대로 상위로 전달
                if (e instanceof IllegalArgumentException) {
                    throw e;
                }
                // DateTimeParseException 등 다른 에러가 나면 날짜 형식이 잘못된 것이므로 예외 발생
                throw new IllegalStateException("날짜 형식이 올바르지 않습니다: " + e.getMessage());
            }
        }
    }
}
