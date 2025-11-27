package deu.cse.lectureroomreservation2.client.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FirstSemesterStrategy (1학기 전략) 검증 테스트
 */
class FirstSemesterStrategyTest {

    private final SemesterStrategy strategy = new FirstSemesterStrategy();

    @Test
    void 시작또는종료가_null이면_에러메시지() {
        String msg1 = strategy.validateTimeRange(null, "09:50");
        String msg2 = strategy.validateTimeRange("09:00", null);

        assertEquals("시작 시간과 종료 시간을 모두 선택해 주세요.", msg1);
        assertEquals("시작 시간과 종료 시간을 모두 선택해 주세요.", msg2);
    }

    @Test
    void 종료시간이_시작시간보다_빠르거나같으면_에러() {
        String msg1 = strategy.validateTimeRange("10:00", "09:50"); // 빨라짐
        String msg2 = strategy.validateTimeRange("10:00", "10:00"); // 같음

        assertEquals("종료 시간은 시작 시간보다 늦어야 합니다.", msg1);
        assertEquals("종료 시간은 시작 시간보다 늦어야 합니다.", msg2);
    }

    @Test
    void 수업길이가_50분이_아니면_에러() {
        // 60분 차이
        String msg = strategy.validateTimeRange("09:00", "10:00");

        assertEquals("시작 시간과 종료 시간은 50분 단위만 허용됩니다.", msg);
    }

    @Test
    void 수업길이가_정확히_50분이면_null반환() {
        String msg1 = strategy.validateTimeRange("09:00", "09:50");
        String msg2 = strategy.validateTimeRange("10:00", "10:50");

        assertNull(msg1);
        assertNull(msg2);
    }
}
