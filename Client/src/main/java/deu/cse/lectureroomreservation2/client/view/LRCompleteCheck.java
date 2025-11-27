package deu.cse.lectureroomreservation2.client.view;

import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.common.ReserveResult;
import deu.cse.lectureroomreservation2.common.CheckMaxTimeResult;
import java.io.*;
import javax.swing.JOptionPane;

public class LRCompleteCheck extends javax.swing.JFrame {

    private final String id;
    private final String role;
    private final String buildingName;
    String roomNumber;
    String date;
    String day;
    String showDate;
    String notice = "기본 공지사항 내용";
    private Client client;
    String IsChange;
    // UI 컴포넌트 추가 선언
    private javax.swing.JTextField txtUserId;
    private javax.swing.JTextField txtPurpose;
    private javax.swing.JTextField txtUserCount;
    private javax.swing.JLabel lblUserId;
    private javax.swing.JLabel lblPurpose;
    private javax.swing.JLabel lblUserCount;
    
    // 기본 생성자 (테스트용)
    public LRCompleteCheck() {
        this("20203139", "S", "공학관" ,"915", "2025 05 15 15:00 16:00", "목요일", null, null);
    }
    

    public LRCompleteCheck(String id, String role, String buildingName, String roomNumber, String date, String day, Client client, String IsChange) {
        setTitle("강의실 예약 확인");
        this.client = client;
        this.id = id;
        this.role = role;
        this.buildingName = buildingName;
        this.roomNumber = roomNumber;
        this.date = date; // 예: "2025 / 06 / 03 / 10:00 10:50"
        this.day = day;
        this.IsChange = IsChange;

        initComponents(); // UI 초기화
        
        // 데이터 세팅
        viewSelectRoom.setText(roomNumber);
        viewSelectTime.setText(date + " (" + day + ")");
        txtUserId.setText(id); // 학번 자동 입력

        // 읽기 전용 설정
        viewSelectRoom.setEditable(false);
        viewSelectTime.setEditable(false);
        txtUserId.setEditable(false);

        setLocationRelativeTo(null);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        viewSelectRoom = new javax.swing.JTextField();
        viewSelectTime = new javax.swing.JTextField();
        
        // 추가된 컴포넌트 초기화
        lblUserId = new javax.swing.JLabel("학번:");
        txtUserId = new javax.swing.JTextField();
        lblPurpose = new javax.swing.JLabel("사용 목적:");
        txtPurpose = new javax.swing.JTextField();
        lblUserCount = new javax.swing.JLabel("사용 인원:");
        txtUserCount = new javax.swing.JTextField();

        LastLRCancel = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        LastLRButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("맑은 고딕", 1, 18));
        jLabel1.setText("예약 정보 입력");

        LastLRCancel.setText("취소");
        LastLRCancel.addActionListener(evt -> this.dispose());

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("위 정보로 예약하시겠습니까?");

        LastLRButton1.setText("확인");
        LastLRButton1.addActionListener(evt -> {
            try {
                LastLRButton1ActionPerformed(evt);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 레이아웃 설정 (간단하게 수정)
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(viewSelectTime)
                    .addComponent(viewSelectRoom)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblPurpose)
                                    .addComponent(lblUserId)
                                    .addComponent(lblUserCount))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtUserCount)
                                    .addComponent(txtUserId)
                                    .addComponent(txtPurpose, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(LastLRButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(LastLRCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(viewSelectRoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(viewSelectTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUserId)
                    .addComponent(txtUserId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPurpose)
                    .addComponent(txtPurpose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUserCount)
                    .addComponent(txtUserCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LastLRCancel)
                    .addComponent(LastLRButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private void viewSelectRoomActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_viewSelectRoomActionPerformed
        // TODO add your handling code here: 강의실 표시 Jtextfield
    }// GEN-LAST:event_viewSelectRoomActionPerformed

    private void LastLRCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_LastLRCancelActionPerformed
        // TODO add your handling code here: 취소 버튼
        /*if (role.equals("S")) {
            new StudentMainMenu(id, client).setVisible(true);
        }
        if (role.equals("P")) {
            new ProfessorMainMenu(id, client).setVisible(true);
        }*/
        this.dispose();
    }// GEN-LAST:event_LastLRCancelActionPerformed

    private void viewSelectTimeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_viewSelectTimeActionPerformed
        // TODO add your handling code here: 날짜 시간 표시 Jtextfield
    }// GEN-LAST:event_viewSelectTimeActionPerformed

    private void LastLRButton1ActionPerformed(java.awt.event.ActionEvent evt) throws Exception {
        System.out.println(">> 예약 요청 전송: 건물명=" + this.buildingName + ", 강의실=" + roomNumber);
        if (this.buildingName == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "오류: 건물 이름이 없습니다. 다시 시도해주세요.");
            return;
        }
        // 1. 입력값 검증
        String purpose = txtPurpose.getText().trim();
        String countStr = txtUserCount.getText().trim();

        if (purpose.isEmpty() || countStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "사용 목적과 인원을 모두 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userCount;
        try {
            userCount = Integer.parseInt(countStr);
            if (userCount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "인원은 1 이상의 숫자여야 합니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. 학생인 경우 최대 예약 가능 시간(개수) 체크
        if ("S".equals(role)) {
            CheckMaxTimeResult checkResult = client.sendCheckMaxTimeRequest(id);
            if (checkResult.isExceeded()) {
                JOptionPane.showMessageDialog(null, "최대 예약 가능 시간(4시간)을 초과했습니다.", "예약 실패", JOptionPane.ERROR_MESSAGE);
                this.dispose();
                return;
            }
        }

        // 3. 예약 요청 전송 (수정된 sendReserveRequest 사용)
        String fullDateInfo = date; // date 멤버 변수에 "yyyy / MM / dd / HH:mm HH:mm" 저장 가정
        String dateOnly;
        String startTime;
        String endTime;
        
        String[] tokens = fullDateInfo.split("/");
        if (tokens.length == 4) {
            // 날짜 부분 추출: "yyyy / MM / dd"
            dateOnly = tokens[0].trim() + "/" + tokens[1].trim() + "/" + tokens[2].trim();
            
            // 시간 부분 추출: "HH:mm HH:mm" -> "HH:mm" (시작), "HH:mm" (종료)
            String[] times = tokens[3].trim().split(" ");
            if (times.length == 2) {
                startTime = times[0];
                endTime = times[1];
            } else {
                JOptionPane.showMessageDialog(this, "시간 정보가 올바르지 않습니다.", "파싱 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            JOptionPane.showMessageDialog(this, "날짜 형식 오류: '년/월/일/시간' 형식이 아닙니다.", "파싱 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. 예약 요청 전송 (수정된 sendReserveRequest 사용)
        ReserveResult result;
        if (IsChange != null) {
            
            result = client.sendModifyReserveRequest(
                    id, 
                    IsChange, 
                    this.buildingName, 
                    roomNumber, 
                    fullDateInfo, // ★★★ newDate 파라미터에는 복합 문자열 그대로 전달 ★★★
                    day, 
                    purpose, 
                    userCount, 
                    role
            );
        } else {
            // 신규 예약
            result = client.sendReserveRequest(
                    id, 
                    role, 
                    this.buildingName, 
                    roomNumber, 
                    dateOnly,   // 날짜만 분리하여 전달 
                    day, 
                    startTime,  // 시작 시간 분리하여 전달
                    endTime,    // 종료 시간 분리하여 전달
                    purpose, 
                    userCount
            );
        }

        // 5. 결과 처리
        new viewResultLR().viewResult(result.getResult(), result.getReason());
        
        if (result.getResult()) {
            this.dispose();
        }
    }

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
                new LRCompleteCheck(null, null, null,null, null, null, null, null).setVisible(true);
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
