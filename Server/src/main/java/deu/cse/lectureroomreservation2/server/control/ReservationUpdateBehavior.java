package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;

/**
 * Strategy Pattern: '예약 변경' 전략을 위한 인터페이스
 */
public interface ReservationUpdateBehavior {
    /**
     * 역할별 예약 변경 로직을 수행합니다.
     * @param details 예약 변경에 필요한 모든 정보 (Builder Pattern)
     * @return 예약 변경 처리 결과
     */
    ReserveResult update(ReservationDetails details);
}