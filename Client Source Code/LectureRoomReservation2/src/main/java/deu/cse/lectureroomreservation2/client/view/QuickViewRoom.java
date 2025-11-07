/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package deu.cse.lectureroomreservation2.client.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import javax.swing.table.DefaultTableModel;
import javax.swing.Timer;
import deu.cse.lectureroomreservation2.client.Client;

/**
 *
 * @author namw2
 */
public class QuickViewRoom extends javax.swing.JFrame {

    Client client;
    String userid;
    String role;

    String setRoomNumber;
    String setStartTime;
    String setEndTime;
    String setDayOfWeek;
    String setState;

    private LocalDate quickViewTargetDate;

    /**
     * Creates new form QuickViewRoom
     */
    public QuickViewRoom() {
        initComponents();

        /*
         * Student a = new Student("20213066","홍길동","01012345678");
         * String[][] tempResult = GetReservation.GetTime("reservation");
         * String[][] tempResult2 = GetReservation.GetTime("schedule");
         * TableRearrange.QuickInsertTable2(QuickViewTimeTable, tempResult,
         * tempResult2,a.getId());
         */
    }

    public QuickViewRoom(Client client, String userid, String role) {
        setTitle("빠른 강의실 예약");
        this.client = client;
        this.userid = userid;
        this.role = role;

        initComponents();
        loadQuickTableData();

        // 10초(10,000ms)마다 테이블 새로고침
        refreshTimer = new Timer(10_000, e -> loadQuickTableData());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        // 테이블 행 선택 이벤트 추가
        QuickViewTimeTable.getSelectionModel().addListSelectionListener(e -> {
            // 마우스 클릭 또는 키보드 이동 등으로 선택이 바뀔 때마다 호출
            int selectedRow = QuickViewTimeTable.getSelectedRow();
            if (selectedRow >= 0) {
                setStartTime = (String) QuickViewTimeTable.getValueAt(selectedRow, 0);
                setEndTime = (String) QuickViewTimeTable.getValueAt(selectedRow, 1);
                setRoomNumber = (String) QuickViewTimeTable.getValueAt(selectedRow, 2);
                setState = (String) QuickViewTimeTable.getValueAt(selectedRow, 3);
                setDayOfWeek = (String) QuickViewTimeTable.getValueAt(selectedRow, 4);
            }
        });
    }

    private Timer refreshTimer;

    public void refreshPage() { // TODO 버튼 누를때마다 새로운 새로고침 함수를 불러옴 갑자기 새로고침을 한번에 함
        if (refreshTimer != null && refreshTimer.isRunning()) {
            // System.out.println("이미 새로고침이 실행 중입니다.");
            return; // 이미 실행 중이면 중복 실행 방지
        }

        this.loadQuickTableData();
        refreshTimer = new Timer(30_000, e -> {
            System.out.println("30초 경과 - 새로고침 중..."); // TODO 나중에 지우기
            this.loadQuickTableData(); // 새로고침 동작
        });
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    private void loadQuickTableData() {
        DefaultTableModel model = (DefaultTableModel) QuickViewTimeTable.getModel();
        model.setRowCount(0);

        LocalDate nowDate = LocalDate.now();
        DayOfWeek dayOfWeekEnum = nowDate.getDayOfWeek();

        // 주말이면 다음주 월요일로 이동
        if (dayOfWeekEnum == DayOfWeek.SATURDAY) {
            nowDate = nowDate.plusDays(2); // 토요일 → 월요일
            dayOfWeekEnum = DayOfWeek.MONDAY;
        } else if (dayOfWeekEnum == DayOfWeek.SUNDAY) {
            nowDate = nowDate.plusDays(1); // 일요일 → 월요일
            dayOfWeekEnum = DayOfWeek.MONDAY;
        }
        // 조회 기준 날짜 저장
        quickViewTargetDate = nowDate;

        // 날짜 라벨에 표시 (예: 2025년 05월 26일)
        String dateText = String.format("%d년 %02d월 %02d일", nowDate.getYear(), nowDate.getMonthValue(),
                nowDate.getDayOfMonth());
        ShowCurrentDate.setText(dateText);

        String year = String.valueOf(nowDate.getYear());
        String month = String.format("%02d", nowDate.getMonthValue());
        String day = String.format("%02d", nowDate.getDayOfMonth());

        String[] daysKor = { "월", "화", "수", "목", "금", "토", "일" };
        String dayOfWeek = daysKor[dayOfWeekEnum.getValue() - 1];

        String[] allRooms = { "908", "910", "911", "912", "913", "914", "915", "916", "917", "918" };

        for (String room : allRooms) {
            try {
                java.util.List<String[]> slots = client.getRoomSlots(room, dayOfWeek);
                System.out.println(room + " " + dayOfWeek + " 슬롯 개수: " + slots.size());
                for (String[] slot : slots) {
                    String start = slot[0];
                    String end = slot[1];
                    // 빠른 예약은 항상 09:00 이후만 표시 (예시)
                    String date = year + " / " + month + " / " + day + " / " + start + " " + end;
                    String state = client.getRoomState(room, dayOfWeek, start, end, date);
                    if ("예약가능".equals(state.replaceAll("\\s+", ""))) {
                        model.addRow(new Object[] { start, end, room, state, dayOfWeek });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        QuickViewTimeTable = new javax.swing.JTable();
        reservationButton = new javax.swing.JButton();
        goBackButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        ShowCurrentDate = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(896, 320));

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("빠른 예약");

        QuickViewTimeTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, "공실", null },
                        { null, null, null, "공실", null },
                        { null, null, null, "공실", null },
                        { null, null, null, "공실", null },
                        { null, null, null, "공실", null }
                },
                new String[] {
                        "Time Start", "Time End", "Room", "State", "Day"
                }) {
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane1.setViewportView(QuickViewTimeTable);

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

        jLabel2.setText("*빠른 예약은 당일 예약만 가능합니다(토, 일인 경우 다음 주 월요일 예약이 표시됩니다)");

        ShowCurrentDate.setText("보여줄 날짜");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 884,
                                                Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(45, 45, 45)
                                                .addComponent(ShowCurrentDate, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        132, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout
                                                .createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(reservationButton, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(goBackButton, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 49,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2)
                                        .addComponent(ShowCurrentDate))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 131,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(reservationButton, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(goBackButton, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(23, Short.MAX_VALUE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void reservationButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_reservationButtonActionPerformed
        // TODO add your handling code here:
        // 예약 버튼 클릭 시 선택된 값이 있는지 확인
        if (setRoomNumber == null || setStartTime == null || setEndTime == null || setDayOfWeek == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "예약할 시간대를 먼저 선택하세요.", "알림",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 선택한 날짜 정보 가져오기
        LocalDate nowDate = quickViewTargetDate;
        String year = String.valueOf(nowDate.getYear());
        String month = String.format("%02d", nowDate.getMonthValue());
        String day = String.format("%02d", nowDate.getDayOfMonth());

        // date 문자열 생성: 년 / 월 / 일 시작시간 끝시간
        String date = year + " / " + month + " / " + day + " / " + setStartTime + " " + setEndTime;

        // 요일 변환: "수" → "수요일"
        String dayOfWeekFull = setDayOfWeek;
        switch (setDayOfWeek) {
            case "월":
                dayOfWeekFull = "월요일";
                break;
            case "화":
                dayOfWeekFull = "화요일";
                break;
            case "수":
                dayOfWeekFull = "수요일";
                break;
            case "목":
                dayOfWeekFull = "목요일";
                break;
            case "금":
                dayOfWeekFull = "금요일";
                break;
            case "토":
                dayOfWeekFull = "토요일";
                break;
            case "일":
                dayOfWeekFull = "일요일";
                break;
        }

        // LRCompleteCheck로 값 전달 및 화면 전환
        LRCompleteCheck Lcheck = new LRCompleteCheck(userid, role, setRoomNumber, date, dayOfWeekFull, client, null);
        // LRCompleteCheck Lcheck = new LRCompleteCheck("20211234", "S", "915", "2025 /
        // 05 / 21 12:00 13:00", "수요일", client);
        Lcheck.setVisible(true);

        // 타이머 종료
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        this.dispose();
    }// GEN-LAST:event_reservationButtonActionPerformed

    private void goBackButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_goBackButtonActionPerformed
        // TODO add your handling code here:
        if (role.equals("P")) {
            ProfessorMainMenu pmenu = new ProfessorMainMenu(userid, client);
            pmenu.setVisible(true);
        } else {
            StudentMainMenu smenu = new StudentMainMenu(userid, client);
            smenu.setVisible(true);
        }

        // 타이머 종료
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        this.dispose();
    }// GEN-LAST:event_goBackButtonActionPerformed

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
            java.util.logging.Logger.getLogger(QuickViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(QuickViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(QuickViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(QuickViewRoom.class.getName()).log(java.util.logging.Level.SEVERE, null,
                    ex);
        }
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new QuickViewRoom().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable QuickViewTimeTable;
    private javax.swing.JLabel ShowCurrentDate;
    private javax.swing.JButton goBackButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton reservationButton;
    // End of variables declaration//GEN-END:variables
}
