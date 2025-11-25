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
        // ... (기존 필터 및 테이블 리스너 코드는 위와 동일하게 유지) ...
        view.getBuildingComboBox().addActionListener(e -> loadFloors());
        view.getFloorComboBox().addActionListener(e -> loadRooms());

        view.getRoomListTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && view.getRoomListTable().getSelectedRow() != -1) {
                this.selectedRoom = view.getRoomListTable().getValueAt(view.getRoomListTable().getSelectedRow(), 0).toString();
                loadRoomTimetable();
                view.getMainTabbedPane().setEnabledAt(1, true);
                view.getMainTabbedPane().setEnabledAt(2, true);

                int selectedIndex = view.getMainTabbedPane().getSelectedIndex();
                if (selectedIndex == 1) loadWeeklyView();
                else if (selectedIndex == 2) loadMonthlyView();
            } else if (view.getRoomListTable().getSelectedRow() == -1) {
                this.selectedRoom = null;
                view.getMainTabbedPane().setEnabledAt(1, false);
                view.getMainTabbedPane().setEnabledAt(2, false);
            }
        });
        
        // [탭 1: 일별 예약] 리스너
        view.getYearComboBox().addActionListener(e -> { if (!isProgrammaticChange) handleDateChange(); });
        view.getMonthComboBox().addActionListener(e -> { if (!isProgrammaticChange) handleDateChange(); });
        view.getDayComboBox().addActionListener(e -> { if (!isProgrammaticChange) updateDayOfWeek(); });
        view.getDayOfWeekComboBox().addActionListener(e -> { if (!isProgrammaticChange) updateDateByDayOfWeek(); });
        view.getReservationButton().addActionListener(e -> handleReservationButton());
        view.getGoBackButton().addActionListener(e -> handleGoBackButton());
        view.getRefreshButton().addActionListener(e -> loadRoomTimetable());
        
        view.getViewTimeTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = view.getViewTimeTable().rowAtPoint(evt.getPoint());
                if (row != -1) {
                    // 1. 기존 로직 (선택된 값 저장)
                    startR = String.valueOf(view.getViewTimeTable().getValueAt(row, 0));
                    endR = String.valueOf(view.getViewTimeTable().getValueAt(row, 1));
                    roomR = String.valueOf(view.getViewTimeTable().getValueAt(row, 2));
                    stateR = String.valueOf(view.getViewTimeTable().getValueAt(row, 3));
                    dayR = String.valueOf(view.getViewTimeTable().getValueAt(row, 4));
                    updateChoosedDate(); // 날짜 갱신

                    // -------------------------------------------------------
                    // [신규 기능] 클릭 시 서버에 실시간 인원 현황 요청 및 UI 업데이트
                    // -------------------------------------------------------
                    if (choosedDate != null) {
                        // 날짜 포맷 정리 (예: "2025 / 06 / 03 / " -> "2025/06/03")
                        String[] dateParts = choosedDate.split("/");
                        String dateSimple = dateParts[0].trim() + "/" + dateParts[1].trim() + "/" + dateParts[2].trim();
                        
                        String selectedBuilding = (String) view.getBuildingComboBox().getSelectedItem();
                        
                        // 서버 요청 (Client.java에 추가한 메서드 호출)
                        int[] stats = client.getReservationStats(selectedBuilding, roomR, dateSimple, startR);
                        int currentCount = stats[0]; // 현재 예약된 인원 (대기+확정)
                        int maxCapacity = stats[1];  // 강의실 최대 수용 인원
                        
                        // 50% 제한 인원 계산
                        int limit50 = (int)(maxCapacity * 0.5); 

                        // 라벨 텍스트 업데이트
                        // 표시 예: "예약 현황: 5 / 25 명 (정원 50명)"
                        String statusText = String.format("<html>예약 현황: <font color='red'>%d</font> / %d 명 (총 정원 %d명)</html>", 
                                                          currentCount, limit50, maxCapacity);
                        view.getUserCountLabel().setText(statusText);
                    }
                    // -------------------------------------------------------
                }
            }
        });

        // [탭 2: 주별 현황]
        view.getPrevWeekButton().addActionListener(e -> navigateWeek(-7));
        view.getNextWeekButton().addActionListener(e -> navigateWeek(7));

        // 탭 변경 감지
        view.getMainTabbedPane().addChangeListener(e -> {
            int index = view.getMainTabbedPane().getSelectedIndex();
            if (index == 1 && view.getMainTabbedPane().isEnabledAt(1)) loadWeeklyView();
            else if (index == 2 && view.getMainTabbedPane().isEnabledAt(2)) loadMonthlyView();
        });

        // ============================================================
        // ★ [핵심 수정] 월별 달력 완전 잠금 (클릭 반응 X, 색상 O) ★
        // ============================================================
        
        // 1. 월/년 변경 감지 -> 데이터 새로고침 & 버튼 다시 잠그기
        view.getMonthlyCalendar().addPropertyChangeListener("calendar", evt -> {
            if (isProgrammaticChange) return;

            Calendar oldCal = (Calendar) evt.getOldValue();
            Calendar newCal = (Calendar) evt.getNewValue();
            
            // 월이나 년이 바뀌었을 때만 로직 수행
            if (oldCal != null && newCal != null && 
               (oldCal.get(Calendar.MONTH) != newCal.get(Calendar.MONTH) || 
                oldCal.get(Calendar.YEAR) != newCal.get(Calendar.YEAR))) {
                
                loadMonthlyView();
                
                // ★ 중요: 달력이 다시 그려질 때 버튼 리스너가 재생성될 수 있으므로 다시 제거해줌
                javax.swing.SwingUtilities.invokeLater(this::lockCalendarButtons);
            }
        });
        
        // 2. 시간대 변경 시 리로드
        view.getMonthlyTimeSlotComboBox().addActionListener(e -> {
            if (view.getMainTabbedPane().getSelectedIndex() == 2 && view.getMainTabbedPane().isEnabledAt(2)) {
                loadMonthlyView();
            }
        });
        
        // 3. 기타 설정
        view.getMainTabbedPane().setEnabledAt(1, false);
        view.getMainTabbedPane().setEnabledAt(2, false);
        setupCalendarEvaluators();
        
        // 4. 장식 제거
        view.getMonthlyCalendar().getDayChooser().setDecorationBackgroundVisible(false);
        view.getMonthlyCalendar().getDayChooser().setDecorationBordersVisible(false);
        
        // ★ [최초 실행] 버튼 잠그기 실행
        lockCalendarButtons();
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
        
        // 현재 선택된 건물 이름 가져오기
        String currentBuilding = (String) view.getBuildingComboBox().getSelectedItem();
        
        // 디버깅 로그
        System.out.println(">> 예약 버튼 클릭됨. 선택된 건물: " + currentBuilding);

        // 2. 값이 없으면 방어 코드
        if (currentBuilding == null || currentBuilding.isEmpty()) {
            JOptionPane.showMessageDialog(view, "건물을 선택해주세요.");
            return;
        }

        // 3. 생성자에 전달
        new LRCompleteCheck(
            view.getUserid(), 
            view.getRole(), 
            currentBuilding, // <--- 이 값이 정확히 넘어가야 함
            roomR, 
            fullDate, 
            fullDay, 
            client, 
            oldReserveInfo
        ).setVisible(true);
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
    /**
     * [탭 1] 메인 시간표 로드 (09:00 ~ 17:00 고정 출력)
     */
    public void loadRoomTimetable() {
        if (this.selectedRoom == null) {
            ((DefaultTableModel) view.getViewTimeTable().getModel()).setRowCount(0);
            return;
        }

        updateChoosedDate(); // 선택된 날짜 갱신
        String dayOfWeek = (String) view.getDayOfWeekComboBox().getSelectedItem(); // "월요일"
        String roomNum = this.selectedRoom;
        
        // 날짜 문자열 조합 (yyyy / MM / dd)
        String y = (String) view.getYearComboBox().getSelectedItem();
        String m = (String) view.getMonthComboBox().getSelectedItem();
        String d = (String) view.getDayComboBox().getSelectedItem();
        String fullDate = y + " / " + m + " / " + d; // 서버로 보낼 날짜

        // 고정 시간대 배열 (9시 ~ 17시)
        String[] timeSlots = {
            "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"
        };

        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rowDataList = new ArrayList<>();
                try {
                    // 각 시간대별로 서버에 상태를 물어봅니다.
                    for (String start : timeSlots) {
                        // 종료 시간 계산 (50분 수업 가정)
                        String end = start.split(":")[0] + ":50"; 
                        
                        // 날짜 정보에 시간까지 포함해서 보냄
                        String dateTime = fullDate + " / " + start + " " + end;
                        
                        // 서버에 상태 요청 (예약가능, 정규수업, 예약중 등)
                        String state = client.getRoomState(roomNum, dayOfWeek, start, end, dateTime);
                        
                        rowDataList.add(new Object[]{start, end, roomNum, state, dayOfWeek});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return rowDataList;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rowDataList = get();
                    DefaultTableModel model = (DefaultTableModel) view.getViewTimeTable().getModel();
                    model.setRowCount(0); // 기존 데이터 초기화
                    
                    // 9개의 행을 테이블에 추가
                    for (Object[] row : rowDataList) {
                        model.addRow(row);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    // 예약 화면 띄우기
    private void goToReservationWindow(Calendar cal) {
        // 1. 필요한 데이터 추출
        String selectedRoom = this.selectedRoom; // 현재 선택된 강의실
        String timeSlot = (String) view.getMonthlyTimeSlotComboBox().getSelectedItem(); // 예: "09:00 - 09:50"

        if (selectedRoom == null || timeSlot == null) {
            JOptionPane.showMessageDialog(view, "강의실과 시간을 먼저 선택해주세요.");
            return;
        }

        // 2. 날짜 포맷팅 (YYYY / MM / DD)
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        String dateStr = String.format("%04d / %02d / %02d", year, month, day);

        // 3. 시간 포맷팅 (09:00 09:50 형태로 분리)
        String[] times = timeSlot.split(" - ");
        String startTime = times[0];
        String endTime = times[1];

        // 4. 서버로 보낼 최종 날짜 문자열 조합
        // 형식: "YYYY / MM / DD / HH:mm HH:mm"
        String fullDate = dateStr + " / " + startTime + " " + endTime;

        // 5. 요일 구하기
        String[] daysKor = {"일", "월", "화", "수", "목", "금", "토"};
        String dayOfWeek = daysKor[cal.get(Calendar.DAY_OF_WEEK) - 1] + "요일";

        // 6. 예약 확인 창 띄우기
        // 현재 선택된 건물 이름 가져오기
        String currentBuilding = (String) view.getBuildingComboBox().getSelectedItem();
        // LRCompleteCheck 생성자에 파라미터 전달
        new LRCompleteCheck(
                view.getUserid(),
                view.getRole(),
                currentBuilding,
                selectedRoom,
                fullDate,
                dayOfWeek,
                client,
                null // 신규 예약이므로 oldReserveInfo는 null
        ).setVisible(true);

        // 현재 창 닫기 (선택 사항)
        // view.dispose(); 
    }
    
    // [신규 메서드] 달력 버튼의 클릭 기능을 제거하여 '단순 조회용'으로 만듦
    private void lockCalendarButtons() {
        // JCalendar 내부의 날짜 버튼들이 담긴 패널을 가져옴
        javax.swing.JPanel dayPanel = view.getMonthlyCalendar().getDayChooser().getDayPanel();
        
        // 패널 안에 있는 모든 컴포넌트(날짜 버튼들)를 순회
        for (java.awt.Component comp : dayPanel.getComponents()) {
            if (comp instanceof javax.swing.JButton) {
                javax.swing.JButton btn = (javax.swing.JButton) comp;
                
                // 1. 마우스 리스너 제거 (클릭해도 반응 안 함 -> 회색 안 바뀜)
                for (java.awt.event.MouseListener ml : btn.getMouseListeners()) {
                    btn.removeMouseListener(ml);
                }
                
                // 2. 키보드 리스너 제거 (엔터 쳐도 반응 안 함)
                for (java.awt.event.KeyListener kl : btn.getKeyListeners()) {
                    btn.removeKeyListener(kl);
                }

                // 3. 포커스 및 호버 효과 제거
                btn.setFocusable(false);       // 선택 테두리 제거
                btn.setRolloverEnabled(false); // 마우스 올렸을 때 색 변화 제거
                // btn.setEnabled(false); // <-- 이걸 쓰면 색깔이 흐려지므로 쓰지 않습니다!
            }
        }
    }
}
