package deu.cse.lectureroomreservation2.client.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SemesterStrategy 인터페이스 기반 다형성 테스트
 */
class SemesterStrategyTest {

    @Test
    void 인터페이스_타입으로_전략을_동일하게_다룰수있다() {
        SemesterStrategy first = new FirstSemesterStrategy();
        SemesterStrategy second = new SecondSemesterStrategy();

        // 올바른 50분 범위는 두 전략 모두 null(성공)이어야 한다고 가정
        assertNull(first.validateTimeRange("09:00", "09:50"));
        assertNull(second.validateTimeRange("09:00", "09:50"));

        // 잘못된 범위(종료<=시작)는 두 전략 모두 에러 메시지(문자열)를 반환해야 함
        assertNotNull(first.validateTimeRange("10:00", "09:50"));
        assertNotNull(second.validateTimeRange("10:00", "09:50"));
    }
}
