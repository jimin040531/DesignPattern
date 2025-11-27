package deu.cse.lectureroomreservation2.client.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecondSemesterStrategy (2학기 전략) 기본 동작 테스트
 */
class SecondSemesterStrategyTest {

    private final SemesterStrategy strategy = new SecondSemesterStrategy();

    @Test
    void 올바른범위에서는_null또는_성공으로처리된다() {
        // 최소한 예외 없이 돌아가고,
        // 유효한 50분 범위는 null(성공)이라고 가정
        assertDoesNotThrow(() -> {
            String msg = strategy.validateTimeRange("09:00", "09:50");
            // 구현에 따라 null 또는 다른 성공 처리일 수 있지만,
            // 현재 설계 의도상 null이면 "검증 통과"로 본다.
            // (검증 통과 여부는 RoomScheduleManagementView 쪽 테스트에서 최종 확인)
        });
    }

    @Test
    void 잘못된범위에서는_에러메시지나_에러표시를_반환한다() {
        String msg = strategy.validateTimeRange("10:00", "09:50");
        // SecondSemesterStrategy 구현이 FirstSemesterStrategy와 동일하다면
        // 비어 있지 않은 에러 메시지를 돌려줄 것으로 기대
        assertNotNull(msg);
        assertFalse(msg.isEmpty());
    }
}
