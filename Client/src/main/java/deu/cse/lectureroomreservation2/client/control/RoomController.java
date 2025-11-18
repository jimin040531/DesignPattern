// 파일 경로: deu/cse/lectureroomreservation2/client/control/RoomController.java
package deu.cse.lectureroomreservation2.client.control;

import com.toedter.calendar.IDateEvaluator;
import com.toedter.calendar.JCalendar;
import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.client.view.LRCompleteCheck;
import deu.cse.lectureroomreservation2.client.view.MyReservationView;
import deu.cse.lectureroomreservation2.client.view.ProfessorMainMenu;
import deu.cse.lectureroomreservation2.client.view.StudentMainMenu;
import deu.cse.lectureroomreservation2.client.view.ViewRoom;
import java.awt.Color;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class RoomController {

    private final ViewRoom view;
    private final Client client;

    private String selectedRoom = null;
    private String startR, endR, roomR, stateR, dayR, choosedDate;
    private boolean isProgrammaticChange = false;

    // [수정] 'currentlyReservedDays' 멤버 변수 선언
    private Set<Integer> currentlyReservedDays = new HashSet<>();

    public RoomController(ViewRoom view, Client client) {
        this.view = view;
        this.client = client;
    }

    /**
     * View의 이벤트 리스너를 초기화합니다.
     */
    public void initController() {
        // [필터 영역]
        view.getBuildingComboBox().addActionListener(e -> loadFloors());
        view.getFloorComboBox().addActionListener(e -> loadRooms());

        // [강의실 목록]
        view.getRoomListTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && view.getRoomListTable().getSelectedRow() != -1) {
                // 1. 선택된 강의실 저장
                this.selectedRoom = view.getRoomListTable().getValueAt(view.getRoomListTable().getSelectedRow(), 0).toString();

                // 2. [일별] 탭의 시간표 로드
                loadRoomTimetable();

                // 3. [주별], [월별] 탭 활성화
                view.getMainTabbedPane().setEnabledAt(1, true);
                view.getMainTabbedPane().setEnabledAt(2, true);

                // 4. [신규] 강의실이 바뀌었을 때, 현재 탭에 맞춰 즉시 갱신
                int selectedIndex = view.getMainTabbedPane().getSelectedIndex();
                if (selectedIndex == 1) {
                    loadWeeklyView();
                } else if (selectedIndex == 2) {
                    loadMonthlyView();
                }
            } else if (view.getRoomListTable().getSelectedRow() == -1) {
                // 선택이 풀리면 탭 비활성화
                this.selectedRoom = null;
                view.getMainTabbedPane().setEnabledAt(1, false);
                view.getMainTabbedPane().setEnabledAt(2, false);
            }
        });

        // [탭 1: 일별 예약]
        view.getYearComboBox().addActionListener(e -> {
            if (!isProgrammaticChange) {
                handleDateChange();
            }
        });
        view.getMonthComboBox().addActionListener(e -> {
            if (!isProgrammaticChange) {
                handleDateChange();
            }
        });
        view.getDayComboBox().addActionListener(e -> {
            if (!isProgrammaticChange) {
                updateDayOfWeek();
            }
        });
        view.getDayOfWeekComboBox().addActionListener(e -> {
            if (!isProgrammaticChange) {
                updateDateByDayOfWeek();
            }
        });

        view.getReservationButton().addActionListener(e -> handleReservationButton());
        view.getGoBackButton().addActionListener(e -> handleGoBackButton());
        view.getRefreshButton().addActionListener(e -> loadRoomTimetable());
        view.getViewTimeTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = view.getViewTimeTable().rowAtPoint(evt.getPoint());
                if (row != -1) {
                    startR = String.valueOf(view.getViewTimeTable().getValueAt(row, 0));
                    endR = String.valueOf(view.getViewTimeTable().getValueAt(row, 1));
                    roomR = String.valueOf(view.getViewTimeTable().getValueAt(row, 2));
                    stateR = String.valueOf(view.getViewTimeTable().getValueAt(row, 3));
                    dayR = String.valueOf(view.getViewTimeTable().getValueAt(row, 4));
                    updateChoosedDate();
                }
            }
        });

        // [탭 2: 주별 현황]
        view.getPrevWeekButton().addActionListener(e -> navigateWeek(-7));
        view.getNextWeekButton().addActionListener(e -> navigateWeek(7));

        // [탭 2 & 3] 탭 변경 리스너
        view.getMainTabbedPane().addChangeListener(e -> {
            int index = view.getMainTabbedPane().getSelectedIndex();
            if (index == 1 && view.getMainTabbedPane().isEnabledAt(1)) {
                loadWeeklyView();
            } else if (index == 2 && view.getMainTabbedPane().isEnabledAt(2)) {
                loadMonthlyView();
            }
        });

        // [탭 3: 월별 현황]
        view.getMonthlyCalendar().addPropertyChangeListener("calendar", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // [수정] 날짜(day) 클릭 리스너 제거! (handleCalendarDateClick 호출 제거)

                // [유지] 월(month) 변경 리스너는 유지 (달력의 <, > 버튼 클릭 시)
                if ("month".equals(evt.getPropertyName()) && !isProgrammaticChange) {
                    loadMonthlyView();
                }
            }
        });

        // [신규] 월별 탭의 시간대 콤보박스 리스너 추가
        view.getMonthlyTimeSlotComboBox().addActionListener(e -> {
            // 콤보박스 변경 시, 월별 탭이 활성화 상태일 때만 갱신
            if (view.getMainTabbedPane().getSelectedIndex() == 2 && view.getMainTabbedPane().isEnabledAt(2)) {
                loadMonthlyView();
            }
        });

        // [신규] 최초 실행 시 [주별], [월별] 탭 비활성화
        view.getMainTabbedPane().setEnabledAt(1, false);
        view.getMainTabbedPane().setEnabledAt(2, false);

        // [신규] 월별 캘린더 색칠용 평가기(Evaluator) 초기 설정
        setupCalendarEvaluators();
    }

    /**
     * [신규] 주차 이동 (이전/다음 주)
     */
    private void navigateWeek(int daysToMove) {
        LocalDate currentDate = getSelectedDateFromComboBox();
        LocalDate newDate = currentDate.plusDays(daysToMove);

        // [일별] 탭의 콤보박스를 새 날짜로 강제 설정
        isProgrammaticChange = true;
        view.getYearComboBox().setSelectedItem(String.valueOf(newDate.getYear()));
        view.getMonthComboBox().setSelectedItem(String.format("%02d", newDate.getMonthValue()));
        updateDayComboBoxItems();
        view.getDayComboBox().setSelectedItem(String.format("%02d", newDate.getDayOfMonth()));

        String[] daysKor = {"월", "화", "수", "목", "금", "토", "일"};
        String dayKor = daysKor[newDate.getDayOfWeek().getValue() - 1];
        view.getDayOfWeekComboBox().setSelectedItem(dayKor);

        isProgrammaticChange = false;

        updateChoosedDate(); // [일별] 탭 날짜 라벨 갱신
        loadWeeklyView(); // [주별] 탭 새로고침
    }

    /**
     * 날짜 콤보박스들을 현재 날짜 기준으로 초기화합니다.
     */
    public void initDateComboBoxes() {
        isProgrammaticChange = true;

        JComboBox<String> yearBox = view.getYearComboBox();
        JComboBox<String> monthBox = view.getMonthComboBox();
        JComboBox<String> dayBox = view.getDayComboBox();
        JComboBox<String> dayOfWeekBox = view.getDayOfWeekComboBox();

        LocalDate now = LocalDate.now();

        if (now.getDayOfWeek() == DayOfWeek.SATURDAY) {
            now = now.plusDays(2);
        } else if (now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            now = now.plusDays(1);
        }

        yearBox.removeAllItems();
        for (int y = now.getYear(); y <= now.getYear() + 2; y++) {
            yearBox.addItem(String.valueOf(y));
        }

        monthBox.removeAllItems();
        for (int m = 1; m <= 12; m++) {
            monthBox.addItem(String.format("%02d", m));
        }

        dayBox.removeAllItems();
        int lastDay = now.lengthOfMonth();
        for (int d = 1; d <= lastDay; d++) {
            dayBox.addItem(String.format("%02d", d));
        }

        yearBox.setSelectedItem(String.valueOf(now.getYear()));
        monthBox.setSelectedItem(String.format("%02d", now.getMonthValue()));
        dayBox.setSelectedItem(String.format("%02d", now.getDayOfMonth()));

        String[] daysKor = {"월", "화", "수", "목", "금", "토", "일"};
        String dayKor = daysKor[now.getDayOfWeek().getValue() - 1];
        dayOfWeekBox.setSelectedItem(dayKor);

        isProgrammaticChange = false;
        updateChoosedDate();
    }

    /**
     * 건물 목록을 서버에서 불러옵니다. (SwingWorker 사용)
     */
    public void loadBuildings() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return client.getBuildingList();
            }

            @Override
            protected void done() {
                try {
                    List<String> buildings = get();
                    view.getBuildingComboBox().removeAllItems();
                    for (String building : buildings) {
                        view.getBuildingComboBox().addItem(building);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(view, "건물 목록 로딩 실패: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * 층 목록을 서버에서 불러옵니다. (SwingWorker 사용)
     */
    private void loadFloors() {
        String selectedBuilding = (String) view.getBuildingComboBox().getSelectedItem();
        if (selectedBuilding == null) {
            return;
        }

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return client.getFloorList(selectedBuilding);
            }

            @Override
            protected void done() {
                try {
                    List<String> floors = get();
                    view.getFloorComboBox().removeAllItems();
                    for (String floor : floors) {
                        view.getFloorComboBox().addItem(floor);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(view, "층 목록 로딩 실패: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * 강의실 목록을 서버에서 불러옵니다. (SwingWorker 사용)
     */
    private void loadRooms() {
        String selectedBuilding = (String) view.getBuildingComboBox().getSelectedItem();
        String selectedFloor = (String) view.getFloorComboBox().getSelectedItem();

        DefaultTableModel model = (DefaultTableModel) view.getRoomListTable().getModel();
        model.setRowCount(0);
        ((DefaultTableModel) view.getViewTimeTable().getModel()).setRowCount(0);
        this.selectedRoom = null;

        // [신규] 강의실 선택이 해제되었으므로 탭 비활성화
        view.getMainTabbedPane().setEnabledAt(1, false);
        view.getMainTabbedPane().setEnabledAt(2, false);

        if (selectedBuilding == null || selectedFloor == null) {
            return;
        }

        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                return client.getRoomList(selectedBuilding, selectedFloor);
            }

            @Override
            protected void done() {
                try {
                    List<String[]> rooms = get();
                    for (String[] room : rooms) {
                        model.addRow(new Object[]{room[0], room[1], Integer.parseInt(room[2])});
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(view, "강의실 목록 로딩 실패: " + e.getMessage());
                }
            }
        }.execute();
    }

    // --- 날짜 연동 로직 ---
    private void handleDateChange() {
        updateDayComboBoxItems();
        updateDayOfWeek();
    }

    private void updateDayOfWeek() {
        if (isProgrammaticChange) {
            return;
        }
        try {
            int y = Integer.parseInt((String) view.getYearComboBox().getSelectedItem());
            int m = Integer.parseInt((String) view.getMonthComboBox().getSelectedItem());
            int d = Integer.parseInt((String) view.getDayComboBox().getSelectedItem());
            LocalDate date = LocalDate.of(y, m, d);

            if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                date = date.plusDays(2);
            } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                date = date.plusDays(1);
            }

            isProgrammaticChange = true;
            view.getYearComboBox().setSelectedItem(String.valueOf(date.getYear()));
            view.getMonthComboBox().setSelectedItem(String.format("%02d", date.getMonthValue()));
            updateDayComboBoxItems();
            view.getDayComboBox().setSelectedItem(String.format("%02d", date.getDayOfMonth()));

            String[] daysKor = {"월", "화", "수", "목", "금", "토", "일"};
            String dayKor = daysKor[date.getDayOfWeek().getValue() - 1];
            view.getDayOfWeekComboBox().setSelectedItem(dayKor);
            isProgrammaticChange = false;

            updateChoosedDate();
            loadRoomTimetable();
        } catch (Exception ex) {
            /* 무시 */ }
    }

    private void updateDateByDayOfWeek() {
        if (isProgrammaticChange) {
            return;
        }
        try {
            int y = Integer.parseInt((String) view.getYearComboBox().getSelectedItem());
            int m = Integer.parseInt((String) view.getMonthComboBox().getSelectedItem());
            int d = Integer.parseInt((String) view.getDayComboBox().getSelectedItem());
            LocalDate baseDate = LocalDate.of(y, m, d);

            String selectedDayKor = (String) view.getDayOfWeekComboBox().getSelectedItem();
            String[] daysKor = {"월", "화", "수", "목", "금"};
            int targetDayIndex = -1;
            for (int i = 0; i < daysKor.length; i++) {
                if (daysKor[i].equals(selectedDayKor)) {
                    targetDayIndex = i; // 0=월
                    break;
                }
            }
            if (targetDayIndex == -1) {
                return;
            }

            DayOfWeek targetDow = DayOfWeek.of(targetDayIndex + 1); // 1=월
            LocalDate targetDate = baseDate.with(targetDow);

            isProgrammaticChange = true;
            view.getYearComboBox().setSelectedItem(String.valueOf(targetDate.getYear()));
            view.getMonthComboBox().setSelectedItem(String.format("%02d", targetDate.getMonthValue()));
            updateDayComboBoxItems();
            view.getDayComboBox().setSelectedItem(String.format("%02d", targetDate.getDayOfMonth()));
            isProgrammaticChange = false;

            updateChoosedDate();
            loadRoomTimetable();
        } catch (Exception ex) {
            /* 무시 */ }
    }

    private void updateDayComboBoxItems() {
        try {
            int y = Integer.parseInt((String) view.getYearComboBox().getSelectedItem());
            int m = Integer.parseInt((String) view.getMonthComboBox().getSelectedItem());
            int lastDay = LocalDate.of(y, m, 1).lengthOfMonth();
            String selectedDay = (String) view.getDayComboBox().getSelectedItem();

            isProgrammaticChange = true;
            view.getDayComboBox().removeAllItems();
            for (int d = 1; d <= lastDay; d++) {
                view.getDayComboBox().addItem(String.format("%02d", d));
            }

            if (selectedDay != null && Integer.parseInt(selectedDay) <= lastDay) {
                view.getDayComboBox().setSelectedItem(selectedDay);
            } else {
                view.getDayComboBox().setSelectedItem(String.format("%02d", lastDay));
            }
            isProgrammaticChange = false;
        } catch (Exception e) {
        }
    }

    // --- 버튼 핸들러 ---
    private void handleGoBackButton() {
        if (view.getRole().equals("S")) {
            new StudentMainMenu(view.getUserid(), client).setVisible(true);
        }
        if (view.getRole().equals("P")) {
            new ProfessorMainMenu(view.getUserid(), client).setVisible(true);
        }
        view.dispose();
    }

    private void handleReservationButton() {
        updateChoosedDate();
        if (stateR == null || startR == null || roomR == null || choosedDate == null) {
            JOptionPane.showMessageDialog(view, "예약할 시간대를 먼저 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String trimmedState = stateR.replaceAll("\\s+", "");
        if ("정규수업".equals(trimmedState) || "교수예약".equals(trimmedState)) {
            JOptionPane.showMessageDialog(view, "해당 시간은 예약할 수 없습니다.\n(정규수업/교수예약 시간)", "예약 불가", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("예약초과".equals(trimmedState) && !"P".equals(view.getRole())) {
            JOptionPane.showMessageDialog(view, "해당 시간은 예약할 수 없습니다.\n(예약 초과)", "예약 불가", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fullDate = choosedDate + startR + " " + endR;
        String fullDay = dayR + "요일";
        String oldReserveInfo = null;

        if ("change".equals(view.getCheck())) {
            oldReserveInfo = MyReservationView.cancelreservation;
        }

        new LRCompleteCheck(view.getUserid(), view.getRole(), roomR, fullDate, fullDay, client, oldReserveInfo).setVisible(true);
        view.dispose();
    }

    private void updateChoosedDate() {
        String y = (String) view.getYearComboBox().getSelectedItem();
        String m = (String) view.getMonthComboBox().getSelectedItem();
        String d = (String) view.getDayComboBox().getSelectedItem();

        if (y != null && m != null && d != null) {
            choosedDate = y + " / " + m + " / " + d + " / ";
            view.getChoosedDateLabel().setText(y + "년 " + m + "월 " + d + "일");
        }
    }

    /**
     * [사용 안 함] '월별 현황' 탭의 캘린더에서 날짜를 클릭했을 때 호출됩니다. [주의] 이 기능은 initController()에서
     * 리스너가 제거되어 현재 사용되지 않습니다.
     */
    private void handleCalendarDateClick() {
        if (isProgrammaticChange) {
            return;
        }

        java.util.Date date = view.getMonthlyCalendar().getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);

        isProgrammaticChange = true;
        view.getYearComboBox().setSelectedItem(String.valueOf(y));
        view.getMonthComboBox().setSelectedItem(String.format("%02d", m));
        updateDayComboBoxItems();
        view.getDayComboBox().setSelectedItem(String.format("%02d", d));
        isProgrammaticChange = false;

        updateDayOfWeek();
        view.getMainTabbedPane().setSelectedIndex(0);
    }

    // --- 탭 변경 핸들러 ---
    /**
     * '주별 현황' 탭의 데이터를 로드합니다. (이전 코드와 동일)
     */
    private void loadWeeklyView() {
        if (this.selectedRoom == null) {
            return;
        }

        LocalDate selectedDate = getSelectedDateFromComboBox();
        LocalDate monday = selectedDate.with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);
        String roomNum = this.selectedRoom;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        view.getWeeklyDateLabel().setText(monday.format(dtf) + " ~ " + friday.format(dtf));

        DefaultTableModel model = (DefaultTableModel) view.getWeeklyTable().getModel();
        // 기존 테이블 내용 초기화
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 1; c <= 5; c++) { // 월요일(1)부터 금요일(5)까지
                model.setValueAt("", r, c);
            }
        }

        // 🚨 [수정된 부분 1]: SwingWorker 제네릭 타입을 서버 반환 타입에 맞춥니다.
        new SwingWorker<Map<String, List<String[]>>, Void>() {
            @Override
            protected Map<String, List<String[]>> doInBackground() throws Exception {
                // Client.java의 getWeeklySchedule 메서드도 이 타입을 반환해야 합니다.
                return client.getWeeklySchedule(roomNum, monday);
            }

            @Override
            protected void done() {
                try {
                    // 🚨 [수정된 부분 2]: get() 메서드의 반환 타입을 정확히 캐스팅합니다.
                    Map<String, List<String[]>> weekData = get();

                    // 🚨 [수정된 부분 3]: 서버가 보낸 실제 키 형식 (시간대 + 50분 단위)에 맞춥니다.
                    // 서버에서 '09:00~09:50' 형태로 보낸다고 가정하고 키를 정의합니다.
                    String[] timeKeys = {
                        "09:00~09:50", "10:00~10:50", "11:00~11:50", "12:00~12:50", "13:00~13:50",
                        "14:00~14:50", "15:00~15:50", "16:00~16:50", "17:00~17:50"
                    };

                    // 🚨 [수정된 부분 4]: 테이블 데이터 매핑 로직 수정
                    for (int r = 0; r < timeKeys.length; r++) {
                        // Map에서 해당 시간대의 5일치 상태 목록 (List<String[]>)을 가져옵니다.
                        List<String[]> dailyStatusList = weekData.get(timeKeys[r]);

                        if (dailyStatusList != null) {
                            // dailyStatusList는 [월요일 상태], [화요일 상태], ... 순서로 5개의 항목을 가집니다.
                            for (int c = 0; c < dailyStatusList.size(); c++) {
                                // String[] dayInfo: [0] = 날짜 (MM/dd), [1] = 상태 (예약가능 등)
                                String[] dayInfo = dailyStatusList.get(c);

                                // 테이블에 표시할 것은 상태 값 (dayInfo[1])입니다.
                                String state = dayInfo[1];

                                // 테이블의 데이터 열 인덱스는 1 (월요일)부터 시작합니다. (0은 Time 열)
                                model.setValueAt(state, r, c + 1);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(view, "주별 현황 로딩 실패: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * [신규] '월별 현황' 탭의 캘린더 색상 평가기를 설정합니다.
     */
    private void setupCalendarEvaluators() {
        JCalendar calendar = view.getMonthlyCalendar();

        // 1. [예약됨] (빨간색) 평가기
        IDateEvaluator reservedEvaluator = new IDateEvaluator() {
            @Override
            public boolean isSpecial(Date date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                // JCalendar는 현재 월이 아닌 날짜도 표시하므로, 현재 월만 대상으로 함
                if (cal.get(Calendar.MONTH) == calendar.getMonthChooser().getMonth()) {
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    // 'currentlyReservedDays' 변수를 사용
                    return currentlyReservedDays.contains(day);
                }
                return false;
            }

            public Color getSpecialBackgroudColor() {
                return new Color(255, 182, 193);
            } // Light Red

            @Override
            public Color getSpecialForegroundColor() {
                return Color.BLACK;
            }

            @Override
            public String getSpecialTooltip() {
                return "예약됨";
            }

            @Override
            public boolean isInvalid(Date date) {
                return false;
            }

            public Color getInvalidBackgroudColor() {
                return null;
            }

            @Override
            public Color getInvalidForegroundColor() {
                return null;
            }

            @Override
            public Color getSpecialBackroundColor() {
                // UnsupportedOperationException 수정: getSpecialBackgroudColor와 동일한 색상 반환
                return new Color(255, 182, 193);
            }

            @Override
            public Color getInvalidBackroundColor() {
                // UnsupportedOperationException 수정: null 반환
                return null;
            }

            @Override
            public String getInvalidTooltip() {
                // UnsupportedOperationException 수정: null 반환
                return null;
            }
        };

        // 2. [예약 가능] (초록색) 평가기
        IDateEvaluator availableEvaluator = new IDateEvaluator() {
            @Override
            public boolean isSpecial(Date date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                if (cal.get(Calendar.MONTH) == calendar.getMonthChooser().getMonth()) {
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    // 주말(토/일)은 '예약 가능'으로 표시하지 않음
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                        return false;
                    }
                    // 'currentlyReservedDays' 변수를 사용
                    return !currentlyReservedDays.contains(day);
                }
                return false;
            }

            public Color getSpecialBackgroudColor() {
                return new Color(144, 238, 144);
            } // Light Green

            @Override
            public Color getSpecialForegroundColor() {
                return Color.BLACK;
            }

            @Override
            public String getSpecialTooltip() {
                return "예약 가능";
            }

            @Override
            public boolean isInvalid(Date date) {
                return false;
            }

            public Color getInvalidBackgroudColor() {
                return null;
            }

            @Override
            public Color getInvalidForegroundColor() {
                return null;
            }

            @Override
            public Color getSpecialBackroundColor() {
                // UnsupportedOperationException 수정: getSpecialBackgroudColor와 동일한 색상 반환
                return new Color(144, 238, 144);
            }

            @Override
            public Color getInvalidBackroundColor() {
                // UnsupportedOperationException 수정: null 반환
                return null;
            }

            @Override
            public String getInvalidTooltip() {
                // UnsupportedOperationException 수정: null 반환
                return null;
            }
        };

        // 3. 캘린더에 평가기들 추가
        calendar.getDayChooser().addDateEvaluator(reservedEvaluator);
        calendar.getDayChooser().addDateEvaluator(availableEvaluator);
    }

    /**
     * '월별 현황' 탭의 데이터를 로드하고 캘린더에 색칠합니다. [대폭 수정됨]
     */
    private void loadMonthlyView() {
        if (this.selectedRoom == null) {
            return;
        }

        JCalendar calendar = view.getMonthlyCalendar();
        final int year = calendar.getYearChooser().getYear();
        final int month = calendar.getMonthChooser().getMonth() + 1;
        String roomNum = this.selectedRoom;

        String selectedTimeSlot = (String) view.getMonthlyTimeSlotComboBox().getSelectedItem();
        if (selectedTimeSlot == null) {
            return;
        }
        String startTime = selectedTimeSlot.split(" - ")[0];

        // 1. SwingWorker 반환 타입을 서버가 보내는 List<String>으로 변경 (ArrayList가 List를 구현)
        new SwingWorker<List<String>, Void>() { // <--- Set<Integer> -> List<String>으로 변경
            @Override
            protected List<String> doInBackground() throws Exception {
                // [중요] Client.java 내부의 getMonthlyReservedDates는 이제 List<String>을 반환해야 함.
                return client.getMonthlyReservedDates(roomNum, year, month, startTime);
            }

            @Override
            protected void done() {
                try {
                    // 2. 서버로부터 List<String> 형태의 월별 상태 데이터 수신
                    List<String> monthlyStatusList = get();

                    // 3. 캘린더 색칠을 위한 Set<Integer> (예약된 날짜 목록)을 새로 생성
                    Set<Integer> reservedDays = new HashSet<>();

                    // 4. 수신된 List를 분석하여 예약된 날짜를 추출 (PROFESSOR/STUDENT 상태의 날짜만)
                    for (String status : monthlyStatusList) {
                        String[] parts = status.split(":");
                        if (parts.length == 2) {
                            int day = Integer.parseInt(parts[0]);
                            String type = parts[1].trim();

                            // "NONE"이 아닌 경우, 즉 교수 또는 학생 예약이 있는 경우 예약된 날짜로 간주
                            if (!"NONE".equals(type)) {
                                reservedDays.add(day);
                            }
                        }
                    }

                    // 5. 멤버 변수 갱신
                    currentlyReservedDays = reservedDays; // <--- Set<Integer> 타입이 됨

                    // 6. UI 갱신 (repaint)
                    isProgrammaticChange = true;
                    calendar.getMonthChooser().setMonth(month - 1);
                    isProgrammaticChange = false;

                    calendar.repaint();

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(view, "월별 현황 로딩 실패: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * 날짜 콤보박스에서 LocalDate 객체를 가져오는 헬퍼 메서드
     */
    private LocalDate getSelectedDateFromComboBox() {
        try {
            int y = Integer.parseInt((String) view.getYearComboBox().getSelectedItem());
            int m = Integer.parseInt((String) view.getMonthComboBox().getSelectedItem());
            int d = Integer.parseInt((String) view.getDayComboBox().getSelectedItem());
            return LocalDate.of(y, m, d);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    /**
     * [탭 1]의 메인 시간표를 로드합니다. (이전 코드와 동일)
     */
    public void loadRoomTimetable() {
        if (this.selectedRoom == null) {
            ((DefaultTableModel) view.getViewTimeTable().getModel()).setRowCount(0);
            return;
        }

        updateChoosedDate();
        String dayOfWeek = (String) view.getDayOfWeekComboBox().getSelectedItem();
        String roomNum = this.selectedRoom;

        String year = (String) view.getYearComboBox().getSelectedItem();
        String month = (String) view.getMonthComboBox().getSelectedItem();
        String dayOfMonth = (String) view.getDayComboBox().getSelectedItem();

        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rowDataList = new ArrayList<>();
                try {
                    List<String[]> slots = client.getRoomSlots(roomNum, dayOfWeek);

                    for (String[] slot : slots) {
                        String start = slot[0];
                        String end = slot[1];
                        String date = year + " / " + month + " / " + dayOfMonth + " / " + start + " " + end;
                        String state = client.getRoomState(roomNum, dayOfWeek, start, end, date);
                        rowDataList.add(new Object[]{start, end, roomNum, state, dayOfWeek});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    rowDataList.clear();
                    rowDataList.add(new Object[]{"서버 오류", "", "", "", ""});
                }
                return rowDataList;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rowDataList = get();
                    DefaultTableModel model = (DefaultTableModel) view.getViewTimeTable().getModel();
                    model.setRowCount(0);
                    for (Object[] row : rowDataList) {
                        model.addRow(row);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(view, "시간표 로딩 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
