/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package deu.cse.lectureroomreservation2.client.view;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import deu.cse.lectureroomreservation2.common.*;
import javax.swing.table.*;
import deu.cse.lectureroomreservation2.client.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.ActionListener;

/**
 *
 * @author namw2
 */
// TODO 그거 요일을 어떻게 수정 필요할거같음 -- 좀 큰 수정필요
public class ViewRoom extends javax.swing.JFrame {

    Client client;
    String userid;
    String role;
    String check;
    private String startR, endR, roomR, stateR, dayR, choosedDate;
    private static String LastUsedButton = " "; // 새로고침 기능을 위해 마지막으로 선택한 버튼 정보를 저장
    private boolean initializing = true;
    private boolean isProgrammaticChange = false;

    /**
     * Creates new form viewRoom
     */
    public ViewRoom(Client client, String userid, String role, String check) {
        setTitle("강의실 조회 및 예약");
        this.client = client;
        this.userid = userid;
        this.role = role;
        this.check = check;

        initComponents();
        
        // 강의실 버튼 비활성화 910 917
        room910.setEnabled(false);
        room917.setEnabled(false);

        if ("change".equals(check)) {
            reservationButton.setText("예약변경");
        }

        ImageIcon stairIcon = new ImageIcon(getClass().getResource("/images/stairs.jpg"));
        Image stairImg = stairIcon.getImage().getScaledInstance(60, 110, Image.SCALE_SMOOTH);
        stair1.setIcon(new ImageIcon(stairImg));
        stair2.setIcon(new ImageIcon(stairImg));
        stair1.setText("");
        stair2.setText("");

        addDateComboBoxListeners();

        // === 날짜 콤보박스 초기화 ===
        LocalDate now = LocalDate.now();
        LocalDate initDate = now;
        if (now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            // 오늘이 주말이면 다음주 월요일로 이동
            int plusDays = (8 - now.getDayOfWeek().getValue()) % 7;
            if (plusDays == 0) {
                plusDays = 1; // 일요일이면 +1, 토요일이면 +2
            }
            initDate = now.plusDays(plusDays);
        }

        for (int y = initDate.getYear(); y <= initDate.getYear() + 1; y++) {
            Year.addItem(String.valueOf(y));
        }
        for (int m = 1; m <= 12; m++) {
            Month.addItem(String.format("%02d", m));
        }
        for (int d = 1; d <= 31; d++) {
            day.addItem(String.format("%02d", d));
        }
        Year.setSelectedItem(String.valueOf(initDate.getYear()));
        Month.setSelectedItem(String.format("%02d", initDate.getMonthValue()));
        day.setSelectedItem(String.format("%02d", initDate.getDayOfMonth()));

        // 요일 설정 (예시 : 월)
        DayOfWeek dayOfWeek = initDate.getDayOfWeek();
        String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
        String dayKor = daysKor[dayOfWeek.getValue() - 1];
        DayComboBox.setSelectedItem(dayKor);

        // === [추가] day 콤보박스 아이템을 해당 월에 맞게 초기화 ===
        updateDayComboBoxItems();

        initializing = false; // 클라이언트의 날짜가 주말일때 문제 생기는거 해결 - 초기화 끝!

    }

    private void addDateComboBoxListeners() {
        Year.addActionListener(e -> {
            if (initializing || isProgrammaticChange) {
                return;
            }
            updateDayComboBoxItems();
            correctInvalidDateSelection();
            refreshPage(); // 타이머 리셋
            updateChoosedDate();
        });
        Month.addActionListener(e -> {
            if (initializing || isProgrammaticChange) {
                return;
            }
            updateDayComboBoxItems();
            correctInvalidDateSelection();
            refreshPage();
            updateChoosedDate();
        });
        day.addActionListener(e -> {
            if (initializing || isProgrammaticChange) {
                return;
            }
            correctInvalidDateSelection();
            refreshPage();
            updateChoosedDate();
        });
        DayComboBox.addActionListener(e -> {
            if (initializing || isProgrammaticChange) {
                return;
            }
            updateDateBySelectedDayOfWeek();
            refreshPage();
            updateChoosedDate();
        });
    }

    private void updateChoosedDate() {
        String y = (String) Year.getSelectedItem();
        String m = (String) Month.getSelectedItem();
        String d = (String) day.getSelectedItem();

        if (y != null && m != null && d != null) {
            // 날짜 형식 맞추기 (월, 일이 한자리면 0 붙이기)
            if (m.length() == 1) {
                m = "0" + m;
            }
            if (d.length() == 1) {
                d = "0" + d;
            }

            ChoosedDate.setText(y + "년 " + m + "월 " + d + "일");
            choosedDate = y + " / " + m + " / " + d + " / ";// "2025 / 05 / 21 12:00 13:00"
            System.out.println(ChoosedDate);
            System.out.println(choosedDate);
        }
    }

    public void updateColumnValues(JTable table) { // 비어있는 칸은 공실로 처리
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowCount = model.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            model.setValueAt(" 공실 ", i, 3);
        }
    }

    public void loadData() {
        // SwingWorker로 서버 통신을 비동기로 처리
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rowDataList = new ArrayList<>();
                String year = (String) Year.getSelectedItem();
                String month = (String) Month.getSelectedItem();
                String dayOfMonth = (String) day.getSelectedItem();
                String dayOfWeek = (String) DayComboBox.getSelectedItem();
                String roomNum = LastUsedButton.trim().isEmpty() ? "908" : LastUsedButton;

                try {
                    // 서버에서 슬롯(시작,끝시간) 리스트 받아오기
                    java.util.List<String[]> slots = client.getRoomSlots(roomNum, dayOfWeek);

                    for (String[] slot : slots) {
                        String start = slot[0];
                        String end = slot[1];

                        // 날짜 포맷: yyyy / MM / dd / HH:mm HH:mm
                        String date = year + " / " + month + " / " + dayOfMonth + " / " + start + " " + end;

                        // 서버에서 상태 받아오기
                        String state = client.getRoomState(roomNum, dayOfWeek, start, end, date);

                        rowDataList.add(new Object[] { start, end, roomNum, state, dayOfWeek });
                    }
                } catch (Exception e) {
                    // 예외는 done()에서 처리
                    rowDataList.clear();
                    rowDataList.add(new Object[] { "서버 오류", "", "", "", "" });
                }
                return rowDataList;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rowDataList = get();
                    DefaultTableModel model = (DefaultTableModel) ViewTimeTable.getModel();
                    model.setRowCount(0);
                    for (Object[] row : rowDataList) {
                        model.addRow(row);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ViewRoom.this, "서버와 통신 중 오류가 발생했습니다.\n" + e.getMessage(), "오류",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private Timer refreshTimer;

    public void refreshPage() { // TODO 버튼 누를때마다 새로운 새로고침 함수를 불러옴 갑자기 새로고침을 한번에 함
        // 기존 타이머가 있으면 중지
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }

        this.loadData();
        refreshTimer = new Timer(30_000, e -> {
            System.out.println("30초 경과 - 새로고침 중..."); // TODO 나중에 지우기
            this.loadData(); // 새로고침 동작
        });
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    private void updateDayComboBoxByDate() {
        String y = (String) Year.getSelectedItem();
        String m = (String) Month.getSelectedItem();
        String d = (String) day.getSelectedItem();
        if (y == null || m == null || d == null) {
            return;
        }

        try {
            int year = Integer.parseInt(y);
            int month = Integer.parseInt(m);
            int dayOfMonth = Integer.parseInt(d);
            LocalDate date = LocalDate.of(year, month, dayOfMonth);
            DayOfWeek dow = date.getDayOfWeek();
            String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
            String dayKor = daysKor[dow.getValue() - 1];

            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                int plusDays = (8 - dow.getValue()) % 7;
                if (plusDays == 0) {
                    plusDays = 1;
                }
                LocalDate nextMonday = date.plusDays(plusDays);

                isProgrammaticChange = true;
                Year.setSelectedItem(String.valueOf(nextMonday.getYear()));
                Month.setSelectedItem(String.format("%02d", nextMonday.getMonthValue()));
                day.setSelectedItem(String.format("%02d", nextMonday.getDayOfMonth()));
                DayComboBox.setSelectedItem("월");
                isProgrammaticChange = false;

                if (!initializing) {
                    JOptionPane.showMessageDialog(this, "주말(토/일)은 선택할 수 없습니다.\n자동으로 다음 평일로 이동합니다.", "안내",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                isProgrammaticChange = true;
                DayComboBox.setSelectedItem(dayKor);
                isProgrammaticChange = false;
            }
        } catch (Exception ex) {
            // 날짜 파싱 오류 무시
        }
    }

    private void updateDateBySelectedDayOfWeek() {
        String y = (String) Year.getSelectedItem();
        String m = (String) Month.getSelectedItem();
        String d = (String) day.getSelectedItem();
        String selectedDay = (String) DayComboBox.getSelectedItem(); // "월", "화" 등

        if (y == null || m == null || d == null || selectedDay == null) {
            return;
        }

        try {
            int year = Integer.parseInt(y);
            int month = Integer.parseInt(m);
            int dayOfMonth = Integer.parseInt(d);
            java.time.LocalDate baseDate = java.time.LocalDate.of(year, month, dayOfMonth);

            // 현재 주의 월요일 구하기
            java.time.DayOfWeek baseDow = baseDate.getDayOfWeek();
            int daysFromMonday = baseDow.getValue() - java.time.DayOfWeek.MONDAY.getValue();
            java.time.LocalDate monday = baseDate.minusDays(daysFromMonday);

            // 선택한 요일의 인덱스 구하기 (월=1, ..., 금=5)
            String[] daysKor = { "월", "화", "수", "목", "금" };
            int targetDayIndex = -1;
            for (int i = 0; i < daysKor.length; i++) {
                if (daysKor[i].equals(selectedDay)) {
                    targetDayIndex = i;
                    break;
                }
            }
            if (targetDayIndex == -1) {
                return;
            }

            // 해당 주의 해당 요일 날짜 구하기
            java.time.LocalDate targetDate = monday.plusDays(targetDayIndex);

            // Year, Month, day 콤보박스 값 변경
            Year.setSelectedItem(String.valueOf(targetDate.getYear()));
            Month.setSelectedItem(String.format("%02d", targetDate.getMonthValue()));
            day.setSelectedItem(String.format("%02d", targetDate.getDayOfMonth()));

            // 날짜 라벨 및 요일 콤보박스 동기화
            updateDayComboBoxByDate();
        } catch (Exception ex) {
            // 날짜 파싱 오류 무시
        }
    }

    private void updateDayComboBoxItems() {
        String y = (String) Year.getSelectedItem();
        String m = (String) Month.getSelectedItem();
        if (y == null || m == null) {
            return;
        }

        int year = Integer.parseInt(y);
        int month = Integer.parseInt(m);

        // 해당 월의 마지막 일 구하기
        int lastDay = java.time.YearMonth.of(year, month).lengthOfMonth();

        // 현재 선택된 일 저장
        String selectedDay = (String) day.getSelectedItem();

        // day 콤보박스 아이템 재설정
        isProgrammaticChange = true;
        day.removeAllItems();
        for (int d = 1; d <= lastDay; d++) {
            day.addItem(String.format("%02d", d));
        }
        // 기존에 선택된 일이 유효하면 복원, 아니면 마지막 일로 선택
        if (selectedDay != null) {
            int sel = Integer.parseInt(selectedDay);
            if (sel <= lastDay) {
                day.setSelectedItem(String.format("%02d", sel));
            } else {
                day.setSelectedItem(String.format("%02d", lastDay));
            }
        }
        isProgrammaticChange = false;
    }

    private void correctInvalidDateSelection() {
        String y = (String) Year.getSelectedItem();
        String m = (String) Month.getSelectedItem();
        String d = (String) day.getSelectedItem();
        if (y == null || m == null || d == null) {
            return;
        }

        try {
            int year = Integer.parseInt(y);
            int month = Integer.parseInt(m);
            int dayOfMonth = Integer.parseInt(d);
            LocalDate selected = LocalDate.of(year, month, dayOfMonth);
            LocalDate today = LocalDate.now();

            boolean wasPast = selected.isBefore(today);

            // 과거면 오늘로 이동
            if (wasPast) {
                selected = today;
            }

            // 주말이면 다음 평일로 이동
            DayOfWeek dow = selected.getDayOfWeek();
            boolean wasWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            if (wasWeekend) {
                int plusDays = (8 - dow.getValue()) % 7;
                if (plusDays == 0) {
                    plusDays = 1;
                }
                selected = selected.plusDays(plusDays);
            }

            // 실제로 날짜가 바뀌었다면 콤보박스 값 변경
            if (wasPast || wasWeekend) {
                isProgrammaticChange = true;
                Year.setSelectedItem(String.valueOf(selected.getYear()));
                Month.setSelectedItem(String.format("%02d", selected.getMonthValue()));
                // === [추가] 월이 바뀌었을 수 있으니 day 콤보박스 아이템을 새로 갱신 ===
                updateDayComboBoxItems();
                day.setSelectedItem(String.format("%02d", selected.getDayOfMonth()));
                String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
                String dayKor = daysKor[selected.getDayOfWeek().getValue() - 1];
                DayComboBox.setSelectedItem(dayKor);
                isProgrammaticChange = false;

                if (!initializing) {
                    if (wasPast && wasWeekend) {
                        JOptionPane.showMessageDialog(this, "과거 날짜 또는 주말(토/일)은 선택할 수 없습니다.\n자동으로 다음 평일로 이동합니다.", "안내",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (wasPast) {
                        JOptionPane.showMessageDialog(this, "과거 날짜는 선택할 수 없습니다.", "안내",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (wasWeekend) {
                        JOptionPane.showMessageDialog(this, "주말(토/일)은 선택할 수 없습니다.\n자동으로 다음 평일로 이동합니다.", "안내",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } else {
                // 평일이면 요일 콤보박스만 맞추기
                isProgrammaticChange = true;
                String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
                String dayKor = daysKor[selected.getDayOfWeek().getValue() - 1];
                DayComboBox.setSelectedItem(dayKor);
                isProgrammaticChange = false;
            }
        } catch (Exception ex) {
            // 날짜 파싱 오류 무시
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        reservationPanel = new javax.swing.JPanel();
        room908 = new javax.swing.JButton();
        room911 = new javax.swing.JButton();
        room912 = new javax.swing.JButton();
        room918 = new javax.swing.JButton();
        room916 = new javax.swing.JButton();
        room915 = new javax.swing.JButton();
        room914 = new javax.swing.JButton();
        room913 = new javax.swing.JButton();
        stair2 = new javax.swing.JLabel();
        elebator = new javax.swing.JLabel();
        stair1 = new javax.swing.JLabel();
        room917 = new javax.swing.JButton();
        room910 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        reservationButton = new javax.swing.JButton();
        goBackButton = new javax.swing.JButton();
        ViewTimePane = new javax.swing.JScrollPane();
        ViewTimeTable = new javax.swing.JTable();
        DayComboBox = new javax.swing.JComboBox<>();
        DayLabel = new javax.swing.JLabel();
        RefreshButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        ChoosedDate = new javax.swing.JLabel();
        Year = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        Month = new javax.swing.JComboBox<>();
        day = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(896, 803));

        reservationPanel.setBackground(new java.awt.Color(255, 255, 255));
        reservationPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));

        room908.setBackground(new java.awt.Color(204, 153, 255));
        room908.setText("<html><center><b>908</b><br>세미나실</center></html>");
        room908.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room908ActionPerformed(evt);
            }
        });

        room911.setBackground(new java.awt.Color(255, 204, 204));
        room911.setText("<html><center><b>911</b><br>실습실</center></html>");
        room911.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room911ActionPerformed(evt);
            }
        });

        room912.setBackground(new java.awt.Color(204, 153, 255));
        room912.setText("<html><center><b>912</b><br>강의실</center></html>");
        room912.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room912ActionPerformed(evt);
            }
        });

        room918.setBackground(new java.awt.Color(255, 204, 204));
        room918.setText("<html><center><b>918</b><br>실습실</center></html>");
        room918.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room918ActionPerformed(evt);
            }
        });

        room916.setBackground(new java.awt.Color(255, 204, 204));
        room916.setText("<html><center><b>916</b><br>실습실</center></html>");
        room916.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room916ActionPerformed(evt);
            }
        });

        room915.setBackground(new java.awt.Color(255, 204, 204));
        room915.setText("<html><center><b>915</b><br>실습실</center></html>");
        room915.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room915ActionPerformed(evt);
            }
        });

        room914.setBackground(new java.awt.Color(204, 153, 255));
        room914.setText("<html><center><b>914</b><br>강의실</center></html>");
        room914.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room914ActionPerformed(evt);
            }
        });

        room913.setBackground(new java.awt.Color(204, 153, 255));
        room913.setText("<html><center><b>913</b><br>강의실</center></html>");
        room913.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                room913ActionPerformed(evt);
            }
        });

        stair2.setText("jLabel2");

        stair1.setText("jLabel6");

        room917.setBackground(new java.awt.Color(255, 255, 204));
        room917.setText("917");

        room910.setBackground(new java.awt.Color(153, 204, 255));
        room910.setText("910");

        javax.swing.GroupLayout reservationPanelLayout = new javax.swing.GroupLayout(reservationPanel);
        reservationPanel.setLayout(reservationPanelLayout);
        reservationPanelLayout.setHorizontalGroup(
            reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(reservationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(reservationPanelLayout.createSequentialGroup()
                        .addComponent(room918, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(room917, javax.swing.GroupLayout.PREFERRED_SIZE, 64, Short.MAX_VALUE))
                    .addGroup(reservationPanelLayout.createSequentialGroup()
                        .addComponent(room908, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(elebator, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(reservationPanelLayout.createSequentialGroup()
                        .addComponent(room916, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(room915, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(room914, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(room913, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(reservationPanelLayout.createSequentialGroup()
                        .addGap(74, 74, 74)
                        .addComponent(stair1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(room910, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(room911, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(room912, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(stair2, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        reservationPanelLayout.setVerticalGroup(
            reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(reservationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(room908, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(room911, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(room912, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(stair2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(elebator, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stair1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(room910, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 78, Short.MAX_VALUE)
                .addGroup(reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(reservationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(room918, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(room916, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(room915, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(room914, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(room913, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(room917, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("조회&예약");

        reservationButton.setText("예약");
        reservationButton.setActionCommand("reservation");
        reservationButton.setPreferredSize(new java.awt.Dimension(100, 50));
        reservationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reservationButtonActionPerformed(evt);
            }
        });

        goBackButton.setText("뒤로가기");
        goBackButton.setActionCommand("goBack");
        goBackButton.setPreferredSize(new java.awt.Dimension(100, 50));
        goBackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goBackButtonActionPerformed(evt);
            }
        });

        ViewTimeTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        ViewTimeTable.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        ViewTimeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"09:00", "09:50", null, " 공실 "},
                {"10:00", "10:50", null, " 공실 "},
                {"11:00", "11:50", null, " 공실 "},
                {"12:00", "12:50", null, " 공실 "},
                {"13:00", "13:50", null, " 공실 "},
                {"14:00", "14:50", null, " 공실 "},
                {"15:00", "15:50", null, " 공실 "},
                {"16:00", "16:50", null, " 공실 "},
                {"17:00", "17:50", null, " 공실 "}
            },
            new String [] {
                "Title Start", "Time End", "Room", "State","Day"
            }
        )
        {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀 편집 비활성화
            }
        }
    );
    ViewTimeTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    ViewTimeTable.setDoubleBuffered(true);
    ViewTimeTable.setGridColor(new java.awt.Color(0, 0, 0));
    ViewTimeTable.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            int row = ViewTimeTable.rowAtPoint(evt.getPoint());

            if (row != -1) {
                startR = String.valueOf(ViewTimeTable.getValueAt(row, 0));
                endR = String.valueOf(ViewTimeTable.getValueAt(row, 1));
                roomR = String.valueOf(ViewTimeTable.getValueAt(row, 2));
                stateR = String.valueOf(ViewTimeTable.getValueAt(row, 3));
                dayR = String.valueOf(ViewTimeTable.getValueAt(row, 4));
                updateChoosedDate();

                System.out.println(
                    "클릭한 행 정보:\n" +
                    "시작 시간: " + startR + "\n" +
                    "종료 시간: " + endR + "\n" +
                    "강의실: " + roomR + "\n" +
                    "상태: " + stateR + "\n" +
                    "요일: " + dayR
                );
            }
        }
    });
    ViewTimePane.setViewportView(ViewTimeTable);

    DayComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "월", "화", "수", "목", "금" }));
    DayComboBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            updateDateBySelectedDayOfWeek();
            loadData(); // 즉시 테이블 갱신
        }
    });

    DayLabel.setText("요일:");

    RefreshButton.setText("새로고침");
    RefreshButton.setActionCommand("reservation");
    RefreshButton.setPreferredSize(new java.awt.Dimension(100, 50));
    RefreshButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            RefreshButtonActionPerformed(evt);
        }
    });

    jLabel2.setText("선택날짜:");

    ChoosedDate.setText("0000년00월00일");

    Year.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2025", "추가예정.." }));

    jLabel4.setText("날짜 변경");

    Month.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12" }));

    day.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" }));

    /*Year.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (initializing) return;
            updateDayComboBoxByDate();
        }
    });*/
    /*Month.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (initializing) return;
            updateDayComboBoxByDate();
        }
    });*/
    /*day.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (initializing) return;
            updateDayComboBoxByDate();
        }
    });*/

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(reservationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addContainerGap())
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(6, 6, 6)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(ChoosedDate, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(Year, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(Month, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(DayLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(DayComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(ViewTimePane)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(RefreshButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(reservationButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(goBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGap(16, 16, 16))))
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(reservationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(DayComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(DayLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(ChoosedDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(Year, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(Month, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(ViewTimePane, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(reservationButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(goBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(RefreshButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(15, 15, 15))
    );

    pack();
    }// </editor-fold>//GEN-END:initComponents

    private void room916ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room916ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "916";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room916ActionPerformed

    private void room918ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room918ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "918";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room918ActionPerformed

    private void room908ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room908ActionPerformed
        // TODO add your handling code here:

        LastUsedButton = "908";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room908ActionPerformed

    private void room911ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room911ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "911";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room911ActionPerformed

    private void room912ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room912ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "912";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room912ActionPerformed

    private void room913ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room913ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "913";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room913ActionPerformed

    private void room914ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room914ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "914";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room914ActionPerformed

    private void room915ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_room915ActionPerformed
        // TODO add your handling code here:
        LastUsedButton = "915";
        this.loadData();
        this.refreshPage();
        updateChoosedDate();
    }// GEN-LAST:event_room915ActionPerformed

    private void reservationButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_reservationButtonActionPerformed
        // TODO add your handling code here: 예약/예약 변경 버튼
        LastUsedButton = " ";// "2025 / 05 / 21 12:00 13:00"

        // 예약 가능 여부 체크
        if (stateR == null || startR == null || endR == null || roomR == null || dayR == null || choosedDate == null) {
            JOptionPane.showMessageDialog(this, "예약할 시간대를 먼저 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String trimmedState = stateR.replaceAll("\\s+", ""); // 공백 제거

        // 정규수업, 교수예약은 무조건 예약 불가
        if ("정규수업".equals(trimmedState) || "교수예약".equals(trimmedState)) {
            JOptionPane.showMessageDialog(this, "해당 시간은 예약할 수 없습니다.\n(정규수업/교수예약 시간)", "예약 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 예약초과는 학생만 예약 불가, 교수는 예약 가능
        if ("예약초과".equals(trimmedState) && !"P".equals(role)) {
            JOptionPane.showMessageDialog(this, "해당 시간은 예약할 수 없습니다.\n(예약 초과)", "예약 불가", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // String newRoomNumber, newDate, String newDay, String role
        // 예약 변경 시 기존 예약 내역을 IsChange에 담아 넘김
        if ("change".equals(check)) {
            MyReservationView.changereservation[0] = roomR;
            MyReservationView.changereservation[1] = choosedDate + startR + " " + endR;
            MyReservationView.changereservation[2] = dayR;
            MyReservationView.changereservation[3] = role;

            // 기존 예약 내역을 문자열로 넘김 (예: "기존강의실/기존날짜/기존요일/역할" 등)
            String oldReserveInfo = MyReservationView.cancelreservation; // 기존 예약 내역 문자열

            LRCompleteCheck Lcheck = new LRCompleteCheck(
                    userid, role, roomR, choosedDate + startR + " " + endR, dayR + "요일", client, oldReserveInfo);
            Lcheck.setVisible(true);
            this.dispose();
            return;
        } else {
            // 신규 예약
            LRCompleteCheck Lcheck = new LRCompleteCheck(userid, role, roomR, choosedDate + startR + " " + endR,
                    dayR + "요일", client, null);
            Lcheck.setVisible(true);
        }

        this.dispose();

    }// GEN-LAST:event_reservationButtonActionPerformed

    private void goBackButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_goBackButtonActionPerformed
        // TODO add your handling code here:
        // new tempMainpage().setVisible(true);
        LastUsedButton = " ";
        if (role.equals("S")) {
            new StudentMainMenu(userid, client).setVisible(true);
        }
        if (role.equals("P")) {
            new ProfessorMainMenu(userid, client).setVisible(true);
        }
        this.dispose();
    }// GEN-LAST:event_goBackButtonActionPerformed

    private void RefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_RefreshButtonActionPerformed
        // TODO add your handling code here:
        this.refreshPage();
    }// GEN-LAST:event_RefreshButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ViewRoom(null, null, null, null).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ChoosedDate;
    private javax.swing.JComboBox<String> DayComboBox;
    private javax.swing.JLabel DayLabel;
    private javax.swing.JComboBox<String> Month;
    private javax.swing.JButton RefreshButton;
    private javax.swing.JScrollPane ViewTimePane;
    private javax.swing.JTable ViewTimeTable;
    private javax.swing.JComboBox<String> Year;
    private javax.swing.JComboBox<String> day;
    private javax.swing.JLabel elebator;
    private javax.swing.JButton goBackButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JButton reservationButton;
    private javax.swing.JPanel reservationPanel;
    private javax.swing.JButton room908;
    private javax.swing.JButton room910;
    private javax.swing.JButton room911;
    private javax.swing.JButton room912;
    private javax.swing.JButton room913;
    private javax.swing.JButton room914;
    private javax.swing.JButton room915;
    private javax.swing.JButton room916;
    private javax.swing.JButton room917;
    private javax.swing.JButton room918;
    private javax.swing.JLabel stair1;
    private javax.swing.JLabel stair2;
    // End of variables declaration//GEN-END:variables
}
