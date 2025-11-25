package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;

/**
 * Strategy Pattern: 역할별 예약 전략을 위한 인터페이스
 */
public interface ReservationBehavior {
    /**
     * 역할별 예약 로직을 수행합니다.
     * @param details 예약에 필요한 모든 정보 (Builder Pattern)
     * @return 예약 처리 결과
     */
    ReserveResult reserve(ReservationDetails details);
}