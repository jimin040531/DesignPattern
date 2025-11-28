package deu.cse.lectureroomreservation2.client.view;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoomScheduleManagementView와 전략 객체 연동 테스트
 */
class RoomScheduleManagementViewTest {
    @Test
    void 생성시_기본전략은_FirstSemesterStrategy이다() throws Exception {
        RoomScheduleManagementView view = new RoomScheduleManagementView(null);

        SemesterStrategy current = getSemesterStrategy(view);

        assertNotNull(current);
        assertTrue(current instanceof FirstSemesterStrategy);
    }

    @Test
    void 학기콤보박스를_바꾸면_전략인스턴스가_런타임에_교체된다() throws Exception {
        RoomScheduleManagementView view = new RoomScheduleManagementView(null);

        JComboBox<String> cmbSemester = getSemesterCombo(view);

        // "2" 선택 → SecondSemesterStrategy
        cmbSemester.setSelectedItem("2");
        SemesterStrategy afterSecond = getSemesterStrategy(view);
        assertTrue(afterSecond instanceof SecondSemesterStrategy);

        // "1" 다시 선택 → FirstSemesterStrategy
        cmbSemester.setSelectedItem("1");
        SemesterStrategy afterFirst = getSemesterStrategy(view);
        assertTrue(afterFirst instanceof FirstSemesterStrategy);
    }

    @Test
    void validateTimeSelection에서_현재전략에게_시간값을_전달한다() throws Exception {
        RoomScheduleManagementView view = new RoomScheduleManagementView(null);

        JComboBox<String> cmbStart = getStartCombo(view);
        JComboBox<String> cmbEnd = getEndCombo(view);

        cmbStart.addItem("09:00");
        cmbEnd.addItem("09:50");
        cmbStart.setSelectedItem("09:00");
        cmbEnd.setSelectedItem("09:50");

        // 호출 여부를 확인할 Fake 전략
        class FakeStrategy implements SemesterStrategy {
            String lastStart;
            String lastEnd;
            @Override
            public String validateTimeRange(String start, String end) {
                this.lastStart = start;
                this.lastEnd = end;
                return null; // 에러 없음
            }
        }

        FakeStrategy fake = new FakeStrategy();
        setSemesterStrategy(view, fake);

        invokeValidateTimeSelection(view);

        assertEquals("09:00", fake.lastStart);
        assertEquals("09:50", fake.lastEnd);
        // 에러가 없으므로 종료 시간 선택은 유지
        assertEquals("09:50", cmbEnd.getSelectedItem());
    }

    @Test
    void 검증에러가_발생하면_종료시간콤보박스를_초기화한다() throws Exception {
        RoomScheduleManagementView view = new RoomScheduleManagementView(null);

        JComboBox<String> cmbStart = getStartCombo(view);
        JComboBox<String> cmbEnd = getEndCombo(view);

        cmbStart.addItem("10:00");
        cmbEnd.addItem("09:50");
        cmbStart.setSelectedItem("10:00");
        cmbEnd.setSelectedItem("09:50");

        // 항상 에러를 반환하는 Fake 전략
        SemesterStrategy fake = (start, end) -> "테스트용 에러 메시지";
        setSemesterStrategy(view, fake);

        invokeValidateTimeSelection(view);

        // 에러 발생 시 종료 시간 선택 해제(-1) 되어야 함
        assertEquals(-1, cmbEnd.getSelectedIndex());
    }

    // ====== reflection helper ======

    private SemesterStrategy getSemesterStrategy(RoomScheduleManagementView view) throws Exception {
        Field f = RoomScheduleManagementView.class.getDeclaredField("semesterStrategy");
        f.setAccessible(true);
        return (SemesterStrategy) f.get(view);
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> getSemesterCombo(RoomScheduleManagementView view) throws Exception {
        Field f = RoomScheduleManagementView.class.getDeclaredField("cmbSemester");
        f.setAccessible(true);
        return (JComboBox<String>) f.get(view);
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> getStartCombo(RoomScheduleManagementView view) throws Exception {
        Field f = RoomScheduleManagementView.class.getDeclaredField("cmbStartTime");
        f.setAccessible(true);
        return (JComboBox<String>) f.get(view);
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> getEndCombo(RoomScheduleManagementView view) throws Exception {
        Field f = RoomScheduleManagementView.class.getDeclaredField("cmbEndTime");
        f.setAccessible(true);
        return (JComboBox<String>) f.get(view);
    }

    private void setSemesterStrategy(RoomScheduleManagementView view, SemesterStrategy s) throws Exception {
        Field f = RoomScheduleManagementView.class.getDeclaredField("semesterStrategy");
        f.setAccessible(true);
        f.set(view, s);
    }

    private void invokeValidateTimeSelection(RoomScheduleManagementView view) throws Exception {
        Method m = RoomScheduleManagementView.class.getDeclaredMethod("validateTimeSelection");
        m.setAccessible(true);
        m.invoke(view);
    }
}
