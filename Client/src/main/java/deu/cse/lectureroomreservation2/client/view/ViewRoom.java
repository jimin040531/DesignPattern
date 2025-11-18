// 파일 경로: deu/cse/lectureroomreservation2/client/view/ViewRoom.java
package deu.cse.lectureroomreservation2.client.view;

import com.toedter.calendar.JCalendar;
import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.client.control.RoomController;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


public class ViewRoom extends javax.swing.JFrame {

    // 1. MVC 컨트롤러
    private RoomController controller;

    // 2. UI 컴포넌트
    // [신규] 필터 영역 (탭 밖으로 이동)
    private JComboBox<String> buildingComboBox;
    private JComboBox<String> floorComboBox;
    private JTable roomListTable;
    
    // [신규] 결과 탭 영역
    private JTabbedPane mainTabbedPane;
    
    // [탭 1: 일별] 컴포넌트
    private JComboBox<String> Year;
    private JComboBox<String> Month;
    private JComboBox<String> day;
    private JComboBox<String> DayComboBox;
    private JTable ViewTimeTable;
    private JLabel ChoosedDate;
    
    // [탭 2: 주별] 컴포넌트
    private JTable weeklyTable;
    private JButton prevWeekButton;
    private JButton nextWeekButton;
    private JLabel weeklyDateLabel;
    
    // [탭 3: 월별] 컴포넌트
    private JCalendar monthlyCalendar;
    private JComboBox<String> monthlyTimeSlotComboBox; // [신규] 월별 탭의 시간 선택 콤보박스
    
    // 하단 버튼
    private JButton reservationButton;
    private JButton goBackButton;
    private JButton RefreshButton;
    
    // 3. Context
    Client client;
    String userid;
    String role;
    String check;

    /**
     * 생성자
     */
    public ViewRoom(Client client, String userid, String role, String check) {
        setTitle("강의실 조회 및 예약 (V3 - 필터 분리)");
        this.client = client;
        this.userid = userid;
        this.role = role;
        this.check = check;

        this.controller = new RoomController(this, client);
        initComponentsDynamic();
        controller.initController();
        controller.loadBuildings();
        controller.initDateComboBoxes();
        
        setSize(900, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    }

    /**
     * 동적 UI 생성
     */
    private void initComponentsDynamic() {
        // --- 1. 프레임의 기본 레이아웃 설정 ---
        this.setLayout(new BorderLayout(10, 10));

        // --- 2. 컴포넌트 인스턴스 생성 ---
        mainTabbedPane = new JTabbedPane();
        buildingComboBox = new JComboBox<>();
        floorComboBox = new JComboBox<>();
        roomListTable = new JTable(new DefaultTableModel(new Object[]{"강의실", "유형", "수용인원"}, 0){
             @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        
        Year = new JComboBox<>();
        Month = new JComboBox<>();
        day = new JComboBox<>();
        DayComboBox = new JComboBox<>(new String[]{"월", "화", "수", "목", "금"});
        ChoosedDate = new JLabel("0000년00월00일");
        
        ViewTimeTable = new JTable();
        ViewTimeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {"Title Start", "Time End", "Room", "State", "Day"}
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });

        reservationButton = new JButton("예약");
        goBackButton = new JButton("뒤로가기");
        RefreshButton = new JButton("새로고침");
        
        if ("change".equals(check)) {
            reservationButton.setText("예약변경");
        }

        // --- 3. [상단] 필터 패널 구성 (JTabbedPane 밖으로 이동) ---
        JPanel filterPanel = new JPanel(new BorderLayout(0, 5));
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboPanel.add(new JLabel("  건물:"));
        comboPanel.add(buildingComboBox);
        comboPanel.add(new JLabel("  층:"));
        comboPanel.add(floorComboBox);
        
        filterPanel.add(comboPanel, BorderLayout.NORTH);
        filterPanel.add(new JScrollPane(roomListTable), BorderLayout.CENTER);
        roomListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filterPanel.setPreferredSize(new java.awt.Dimension(880, 250));
        
        // [수정] 프레임의 상단(NORTH)에 필터 패널 추가
        this.add(filterPanel, BorderLayout.NORTH);

        // --- 4. [중앙] 탭 패널 구성 ---
        
        // 4-1. [탭 1: 일별 예약] 패널 구성
        JPanel dailyPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));
        JPanel dateSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateSelectPanel.add(new JLabel("  선택날짜:"));
        dateSelectPanel.add(ChoosedDate);
        dateSelectPanel.add(new JLabel("  날짜 변경:"));
        dateSelectPanel.add(Year);
        dateSelectPanel.add(Month);
        dateSelectPanel.add(day);
        dateSelectPanel.add(new JLabel("  요일:"));
        dateSelectPanel.add(DayComboBox);
        
        centerPanel.add(dateSelectPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(ViewTimeTable), BorderLayout.CENTER);

        dailyPanel.add(centerPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(RefreshButton);
        buttonPanel.add(reservationButton);
        buttonPanel.add(goBackButton);
        
        dailyPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 4-2. [탭 2: 주별 현황] 패널 구성
        JPanel weeklyPanel = new JPanel(new BorderLayout());
        
        JPanel weeklyNavPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        prevWeekButton = new JButton("< 이전 주");
        nextWeekButton = new JButton("다음 주 >");
        weeklyDateLabel = new JLabel("YYYY-MM-DD ~ YYYY-MM-DD");
        weeklyNavPanel.add(prevWeekButton);
        weeklyNavPanel.add(weeklyDateLabel);
        weeklyNavPanel.add(nextWeekButton);
        weeklyPanel.add(weeklyNavPanel, BorderLayout.NORTH);

        DefaultTableModel weeklyModel = new DefaultTableModel(
            new String [] {"Time", "월", "화", "수", "목", "금"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        weeklyTable = new JTable(weeklyModel);
        
        String[] timeSlots = {
            "09:00 - 09:50", "10:00 - 10:50", "11:00 - 11:50", "12:00 - 12:50",
            "13:00 - 13:50", "14:00 - 14:50", "15:00 - 15:50", "16:00 - 16:50", "17:00 - 17:50"
        };
        for (String slot : timeSlots) {
            weeklyModel.addRow(new Object[]{slot, "", "", "", "", ""});
        }
        weeklyPanel.add(new JScrollPane(weeklyTable), BorderLayout.CENTER);

        // 4-3. [탭 3: 월별 현황] 패널 구성
        JPanel monthlyPanel = new JPanel(new BorderLayout(0, 10)); // [수정] 레이아웃 간격 조정
        
        // [신규] 월별 탭용 시간 슬롯 콤보박스 생성
        // (weeklyPanel에서 사용된 timeSlots 배열 재사용)
        monthlyTimeSlotComboBox = new JComboBox<>(timeSlots);
        
        // [신규] 월별 탭 상단: 시간 선택 패널
        JPanel monthlyControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monthlyControlPanel.add(new JLabel("  시간대 선택:"));
        monthlyControlPanel.add(monthlyTimeSlotComboBox);
        
        monthlyPanel.add(monthlyControlPanel, BorderLayout.NORTH); // 상단에 컨트롤 패널 추가
        
        monthlyCalendar = new JCalendar();
        monthlyPanel.add(monthlyCalendar, BorderLayout.CENTER);
        
        // [수정] 안내 문구 변경
        monthlyPanel.add(new JLabel("시간대를 선택하면 달력에 예약 가능 여부가 표시됩니다.", SwingConstants.CENTER), BorderLayout.SOUTH);

        // --- 5. 메인 탭에 패널들 추가 ---
        mainTabbedPane.addTab("  강의실 예약 (일별)  ", dailyPanel);
        mainTabbedPane.addTab("  주별 현황  ", weeklyPanel);
        mainTabbedPane.addTab("  월별 현황  ", monthlyPanel);

        // [수정] 프레임의 중앙(CENTER)에 탭 패널 추가
        this.add(mainTabbedPane, BorderLayout.CENTER);
    }
    
    
    // --- 컨트롤러 Getters 제공 ---
    public JTabbedPane getMainTabbedPane() { return mainTabbedPane; }
    public JComboBox<String> getBuildingComboBox() { return buildingComboBox; }
    public JComboBox<String> getFloorComboBox() { return floorComboBox; }
    public JTable getRoomListTable() { return roomListTable; }
    public JComboBox<String> getYearComboBox() { return Year; }
    public JComboBox<String> getMonthComboBox() { return Month; }
    public JComboBox<String> getDayComboBox() { return day; }
    public JComboBox<String> getDayOfWeekComboBox() { return DayComboBox; }
    public JTable getViewTimeTable() { return ViewTimeTable; }
    public JLabel getChoosedDateLabel() { return ChoosedDate; }
    public JButton getReservationButton() { return reservationButton; }
    public JButton getGoBackButton() { return goBackButton; }
    public JButton getRefreshButton() { return RefreshButton; }
    
    public JTable getWeeklyTable() { return weeklyTable; }
    public JCalendar getMonthlyCalendar() { return monthlyCalendar; }
    public JButton getPrevWeekButton() { return prevWeekButton; }
    public JButton getNextWeekButton() { return nextWeekButton; }
    public JLabel getWeeklyDateLabel() { return weeklyDateLabel; }
    
    // [신규] 월별 시간대 콤보박스 Getter
    public JComboBox<String> getMonthlyTimeSlotComboBox() { return monthlyTimeSlotComboBox; }
    
    // Getters for context
    public String getUserid() { return userid; }
    public String getRole() { return role; }
    public String getCheck() { return check; }
    public Client getClient() { return client; }
}