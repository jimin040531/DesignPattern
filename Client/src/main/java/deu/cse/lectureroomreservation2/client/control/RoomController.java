// 파일 경로: deu/cse/lectureroomreservation2/client/control/RoomController.java
package deu.cse.lectureroomreservation2.client.control;

import com.toedter.calendar.JCalendar;
import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.client.view.LRCompleteCheck;
import deu.cse.lectureroomreservation2.client.view.MyReservationView;
import deu.cse.lectureroomreservation2.client.view.ProfessorMainMenu;
import deu.cse.lectureroomreservation2.client.view.StudentMainMenu;
import deu.cse.lectureroomreservation2.client.view.ViewRoom;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class RoomController {

    private final ViewRoom view; 
    private final Client client;

    private String selectedRoom = null; 
    private String startR, endR, roomR, stateR, dayR, choosedDate;
    private boolean isProgrammaticChange = false; 

    public RoomController(ViewRoom view, Client client) {
        this.view = view;
        this.client = client;
    }

    /**
     * View의 이벤트 리스너를 초기화합니다.
     */
    public void initController() {
        // [수정] 필터는 탭 밖에 있으므로 항상 활성화
        view.getBuildingComboBox().addActionListener(e -> loadFloors());
        view.getFloorComboBox().addActionListener(e -> loadRooms());
        
        // [수정] 강의실 목록을 선택했을 때의 로직
        view.getRoomListTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && view.getRoomListTable().getSelectedRow() != -1) {
                // 1. 선택된 강의실 저장
                this.selectedRoom = view.getRoomListTable().getValueAt(view.getRoomListTable().getSelectedRow(), 0).toString();
                
                // 2. [일별] 탭의 시간표 로드
                loadRoomTimetable();
                
                // 3. [신규] 강의실이 선택되었으므로 [주별], [월별] 탭을 활성화!
                view.getMainTabbedPane().setEnabledAt(1, true); // 주별 탭
                view.getMainTabbedPane().setEnabledAt(2, true); // 월별 탭
            } else if (view.getRoomListTable().getSelectedRow() == -1) {
                // [신규] 선택이 풀리면 탭 비활성화
                this.selectedRoom = null;
                view.getMainTabbedPane().setEnabledAt(1, false);
                view.getMainTabbedPane().setEnabledAt(2, false);
            }
        });

        // [탭 1: 일별 예약]
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
            // [신규] 탭이 활성화 된 상태에서만 로드
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
                if ("calendar".equals(evt.getPropertyName()) && evt.getNewValue() != null && !isProgrammaticChange) {
                    handleCalendarDateClick();
                }
                if ("month".equals(evt.getPropertyName())) {
                    loadMonthlyView(); 
                }
            }
        });
        
        // [신규] 최초 실행 시 [주별], [월별] 탭 비활성화
        view.getMainTabbedPane().setEnabledAt(1, false);
        view.getMainTabbedPane().setEnabledAt(2, false);
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
        
        String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
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
        
        if (now.getDayOfWeek() == DayOfWeek.SATURDAY) { now = now.plusDays(2); } 
        else if (now.getDayOfWeek() == DayOfWeek.SUNDAY) { now = now.plusDays(1); }

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

        String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
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
        if (selectedBuilding == null) return;

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
                loadRooms();
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
        
        if (selectedBuilding == null || selectedFloor == null) return;

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
        if (isProgrammaticChange) return;
        try {
            int y = Integer.parseInt((String) view.getYearComboBox().getSelectedItem());
            int m = Integer.parseInt((String) view.getMonthComboBox().getSelectedItem());
            int d = Integer.parseInt((String) view.getDayComboBox().getSelectedItem());
            LocalDate date = LocalDate.of(y, m, d);
            
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY) { date = date.plusDays(2); } 
            else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) { date = date.plusDays(1); }
            
            isProgrammaticChange = true;
            view.getYearComboBox().setSelectedItem(String.valueOf(date.getYear()));
            view.getMonthComboBox().setSelectedItem(String.format("%02d", date.getMonthValue()));
            updateDayComboBoxItems(); 
            view.getDayComboBox().setSelectedItem(String.format("%02d", date.getDayOfMonth()));
            
            String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
            String dayKor = daysKor[date.getDayOfWeek().getValue() - 1];
            view.getDayOfWeekComboBox().setSelectedItem(dayKor);
            isProgrammaticChange = false;
            
            updateChoosedDate();
            loadRoomTimetable();
        } catch (Exception ex) { /* 무시 */ }
    }

    private void updateDateByDayOfWeek() {
        if (isProgrammaticChange) return;
        try {
            int y = Integer.parseInt((String) view.getYearComboBox().getSelectedItem());
            int m = Integer.parseInt((String) view.getMonthComboBox().getSelectedItem());
            int d = Integer.parseInt((String) view.getDayComboBox().getSelectedItem());
            LocalDate baseDate = LocalDate.of(y, m, d);
            
            String selectedDayKor = (String) view.getDayOfWeekComboBox().getSelectedItem();
            String[] daysKor = { "월", "화", "수", "목", "금" };
            int targetDayIndex = -1;
            for(int i=0; i<daysKor.length; i++) {
                if(daysKor[i].equals(selectedDayKor)) {
                    targetDayIndex = i; // 0=월
                    break;
                }
            }
            if (targetDayIndex == -1) return; 

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
        } catch (Exception ex) { /* 무시 */ }
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
        } catch (Exception e) {}
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
     * '월별 현황' 탭의 캘린더에서 날짜를 클릭했을 때 호출됩니다.
     */
    private void handleCalendarDateClick() {
        if (isProgrammaticChange) return; 

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
     * '주별 현황' 탭의 데이터를 로드합니다.
     */
    private void loadWeeklyView() {
        if (this.selectedRoom == null) {
            // 이 탭은 selectedRoom이 있어야만 활성화되므로, 이 메시지는 사실상 보일 일이 없음
            JOptionPane.showMessageDialog(view, "[일별 예약] 탭에서 먼저 강의실을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LocalDate selectedDate = getSelectedDateFromComboBox();
        LocalDate monday = selectedDate.with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4); 
        String roomNum = this.selectedRoom;
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        view.getWeeklyDateLabel().setText(monday.format(dtf) + " ~ " + friday.format(dtf));
        
        DefaultTableModel model = (DefaultTableModel) view.getWeeklyTable().getModel();
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 1; c <= 5; c++) {
                model.setValueAt("", r, c);
            }
        }

        new SwingWorker<Map<String, String[]>, Void>() {
            @Override
            protected Map<String, String[]> doInBackground() throws Exception {
                return client.getWeeklySchedule(roomNum, monday);
            }

            @Override
            protected void done() {
                try {
                    Map<String, String[]> weekData = get();
                    String[] timeKeys = {
                        "09:00", "10:00", "11:00", "12:00", "13:00", 
                        "14:00", "15:00", "16:00", "17:00"
                    };
                    
                    for (int r = 0; r < timeKeys.length; r++) { 
                        String[] dayStates = weekData.get(timeKeys[r]);
                        if (dayStates != null) {
                            for (int c = 0; c < dayStates.length; c++) { 
                                model.setValueAt(dayStates[c], r, c + 1); 
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
     * '월별 현황' 탭의 데이터를 로드하고 캘린더에 색칠합니다.
     */
    private void loadMonthlyView() {
        if (this.selectedRoom == null) {
            return; // 탭이 비활성화되어 있으므로 이 코드는 실행되지 않음
        }

        JCalendar calendar = view.getMonthlyCalendar();
        isProgrammaticChange = true;
        calendar.setDate(java.sql.Date.valueOf(getSelectedDateFromComboBox()));
        isProgrammaticChange = false;
        
        int year = calendar.getYearChooser().getYear();
        int month = calendar.getMonthChooser().getMonth() + 1; 
        String roomNum = this.selectedRoom;

        new SwingWorker<Map<Integer, String>, Void>() {
            @Override
            protected Map<Integer, String> doInBackground() throws Exception {
                return client.getMonthlySchedule(roomNum, year, month);
            }

            @Override
            protected void done() {
                try {
                    
                    
                    calendar.repaint();
                    
                } catch (Exception e) {
                    // 컴파일 오류 시, IDayRenderer 관련 4줄(import)과 이 try-catch 블록 내부를 삭제
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
     * [탭 1]의 메인 시간표를 로드합니다.
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