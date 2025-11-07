package deu.cse.lectureroomreservation2.client.view;

import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.CheckMaxTimeResult;
import java.io.*;
import javax.swing.JOptionPane;

public class LRCompleteCheck extends javax.swing.JFrame {

    private final String id;
    private final String role;
    String roomNumber;
    String date;
    String day;
    String showDate;
    String notice = "기본 공지사항 내용";
    private Client client;
    String IsChange;

    public LRCompleteCheck() {
        initComponents();

        setLocationRelativeTo(null);

        String tID = "20203139";
        String tRole = "S";

        String troomNumber = "915";
        String tdate = "2025 05 15 15:00 16:00";
        String tday = "목요일";
        showDate = tdate + " / " + tday;

        this.id = tID;
        this.role = tRole;
        this.roomNumber = troomNumber;
        this.date = tdate;
        this.day = tday;

        viewSelectRoom.setText(troomNumber);
        viewSelectTime.setText(showDate);

        viewSelectRoom.setEditable(false);
        viewSelectTime.setEditable(false);
    }

    public LRCompleteCheck(String id, String role, String roomNumber, String date, String day, Client client, String IsChange) {
        setTitle("강의실 예약");
        // id : 사용자 아이디, role : 사용자 역할("P" 또는 "S"), roomNumber : 강의실 번호, date : 년 월 일
        // 시작(시간:분) 끝(시간:분), day : 요일, client 클라이언트 넘겨주기
        this.client = client;

        initComponents();
        this.id = id;
        this.role = role;
        this.roomNumber = roomNumber;
        this.date = date;
        this.day = day;
        this.IsChange = IsChange;

        if(IsChange != null) {
            System.out.println("예약 변경 모드로 실행됨: " + IsChange);
            System.out.println("역할 정보 : " + role);
        } else {
            System.out.println("IsChange 가 null 이라서 신규 예약 모드로 실행됨");
            System.out.println("역할 정보 : " + role);
        }
        System.out.println();

        showDate = date + " / " + day;

        setLocationRelativeTo(null);

        viewSelectRoom.setText(roomNumber);
        viewSelectTime.setText(showDate);

        viewSelectRoom.setEditable(false);
        viewSelectTime.setEditable(false);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        viewSelectRoom = new javax.swing.JTextField();
        viewSelectTime = new javax.swing.JTextField();
        LastLRCancel = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        LastLRButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("예약 확인");

        viewSelectRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewSelectRoomActionPerformed(evt);
            }
        });

        viewSelectTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewSelectTimeActionPerformed(evt);
            }
        });

        LastLRCancel.setText("취소");
        LastLRCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LastLRCancelActionPerformed(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("예약하시겠습니까?");

        LastLRButton1.setText("확인");
        LastLRButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    LastLRButton1ActionPerformed(evt);
                } catch (ClassNotFoundException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                74, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(viewSelectRoom)
                                                        .addComponent(viewSelectTime,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, 441,
                                                                Short.MAX_VALUE))
                                                .addContainerGap())
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout
                                                .createSequentialGroup()
                                                .addGap(61, 61, 61)
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGap(58, 58, 58)
                                                                .addComponent(LastLRButton1)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(LastLRCancel))
                                                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                264, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(94, 94, 94)))));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 23,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(viewSelectRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 50,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(viewSelectTime, javax.swing.GroupLayout.PREFERRED_SIZE, 50,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(LastLRCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 38,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(LastLRButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 38,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap()));

        pack();
    }

    private void viewSelectRoomActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_viewSelectRoomActionPerformed
        // TODO add your handling code here: 강의실 표시 Jtextfield
    }// GEN-LAST:event_viewSelectRoomActionPerformed

    private void LastLRCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_LastLRCancelActionPerformed
        // TODO add your handling code here: 취소 버튼
        if (role.equals("S")) {
            new StudentMainMenu(id, client).setVisible(true);
        }
        if (role.equals("P")) {
            new ProfessorMainMenu(id, client).setVisible(true);
        }
        this.dispose();
    }// GEN-LAST:event_LastLRCancelActionPerformed

    private void viewSelectTimeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_viewSelectTimeActionPerformed
        // TODO add your handling code here: 날짜 시간 표시 Jtextfield
    }// GEN-LAST:event_viewSelectTimeActionPerformed

    private void LastLRButton1ActionPerformed(java.awt.event.ActionEvent evt)
            throws ClassNotFoundException, IOException {// GEN-FIRST:event_LastLRButton1ActionPerformed

        // 학생인 경우 최대 예약 초과 체크
        if (role.equals("S")) {
            CheckMaxTimeResult checkResult = client.sendCheckMaxTimeRequest(id);
            if (checkResult.isExceeded()) {
                // 최대 예약 초과 시 안내 후 종료
                JOptionPane.showMessageDialog(null, "최대 예약시간이 초과되었습니다", "예약 실패", JOptionPane.ERROR_MESSAGE);
                this.dispose();
                return;
            }
        }

        // 교수인 경우 공지사항 입력, 학생은 null
        String noticeToSend = null;
        if (role.equals("P")) {
            noticeWriterView noticeDialog = new noticeWriterView();
            noticeDialog.setLocationRelativeTo(this);
            noticeDialog.setNoticeListener(new noticeWriterView.NoticeListener() {
                @Override
                public void onNoticeEntered(String noticeText) {
                    if (noticeText == null || noticeText.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(LRCompleteCheck.this, "공지사항을 입력해야 예약이 완료됩니다.", "입력 필요",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    try {
                        ReserveResult result;
                        if (IsChange != null) {
                            // 예약 변경 처리
                            result = client.sendModifyReserveRequest(
                                    id, IsChange, roomNumber, date, day, role
                            );
                        } else {
                            // 신규 예약 처리
                            result = client.sendReserveRequest(
                                    id, role, roomNumber, date, day, noticeText
                            );
                        }
                        new viewResultLR().viewResult(result.getResult(), result.getReason());
                        if (result.getResult()) {
                            new ProfessorMainMenu(id, client).setVisible(true);
                            LRCompleteCheck.this.dispose();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(LRCompleteCheck.this, "오류가 발생했습니다: " + ex.getMessage(), "에러",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            noticeDialog.setVisible(true);
            return; // 교수는 여기서 예약 진행을 콜백에서 처리하므로 아래 코드 실행 안 함

        }

        // 학생 신규/변경 처리 (학생은 예약 변경이 없다면 기존 로직대로)
        ReserveResult result;
        if(IsChange != null) {
            // 예약 변경 처리
            result = client.sendModifyReserveRequest(id, IsChange, roomNumber, date, day, role);
            new viewResultLR().viewResult(result.getResult(), result.getReason());
        } else {
            // 신규 예약 처리
            result = client.sendReserveRequest(id, role, roomNumber, date, day, noticeToSend);
            new viewResultLR().viewResult(result.getResult(), result.getReason());
        }

        if (result.getResult()) {
            if (role.equals("P")) {
                new ProfessorMainMenu(id, client).setVisible(true);
            } else if (role.equals("S")) {
                new StudentMainMenu(id, client).setVisible(true);
            }
            this.dispose();
        }
    }// GEN-LAST:event_LastLRButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LRCompleteCheck.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LRCompleteCheck.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LRCompleteCheck.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LRCompleteCheck.class.getName()).log(java.util.logging.Level.SEVERE,
                    null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // new LRCompleteCheck(id, role, roomNumber, date, day, null).setVisible(true);
                new LRCompleteCheck(null, null, null, null, null, null, null).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton LastLRButton1;
    private javax.swing.JButton LastLRCancel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JTextField viewSelectRoom;
    private javax.swing.JTextField viewSelectTime;
    // End of variables declaration//GEN-END:variables
}
