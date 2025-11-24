/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package deu.cse.lectureroomreservation2.client.view;

import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.common.ScheduleRequest;
import deu.cse.lectureroomreservation2.common.ScheduleResult;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 *
 * @author Jimin
 */
public class RoomScheduleManagementView extends javax.swing.JFrame {

    private final Client client;

    // 클라이언트 객체를 받아 UI 초기화 및 시간표 자동 로딩 설정
    public RoomScheduleManagementView(Client client) {
        this.client = client;           // 먼저 client 설정
        initComponents();               // UI 초기화
        setLocationRelativeTo(null);
        loadTimetableOnRoomSelect();
    }

    // 콤보 박스로 강의실 선택 시 시간표 자동 로드
    private void loadTimetableOnRoomSelect() {
        cmbRoomSelect.addActionListener(evt -> {
            String selectedRoom = cmbRoomSelect.getSelectedItem().toString();
            loadTimetable(selectedRoom);
        });
    }

    // 테이블을 빈 값으로 초기화하는 메서드
    private void initializeTimetable() {
        for (int i = 0; i < tblTimetable.getRowCount(); i++) {
            for (int j = 2; j < tblTimetable.getColumnCount(); j++) {
                tblTimetable.setValueAt("", i, j);
            }
        }
    }

    // 클라이언트와 통신하여 선택된 강의실의 시간표를 서버에서 조회 후 테이블에 출력
    private void loadTimetable(String selectedRoom) {
        initializeTimetable();
        String type = rbLecture.isSelected() ? "수업" : "제한";

        try {
            for (String day : new String[]{"월", "화", "수", "목", "금"}) {
                ScheduleRequest req = new ScheduleRequest("LOAD", selectedRoom, day, null, null, null, type);
                ScheduleResult result = client.sendScheduleRequest(req);

                if (result.isSuccess() && result.getData() != null) {
                    for (Map.Entry<String, String> entry : result.getData().entrySet()) {
                        int rowIndex = getRowForTime(entry.getKey());
                        int colIndex = getDayIndex(day);  // 각 요일별 열 인덱스

                        if (rowIndex != -1 && colIndex != -1) {
                            tblTimetable.setValueAt(entry.getValue(), rowIndex, colIndex);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "시간표 불러오기 실패");
        }
    }

    // 요일을 열 인덱스로 변환
    private int getDayIndex(String day) {
        switch (day) {
            case "월":
                return 2;
            case "화":
                return 3;
            case "수":
                return 4;
            case "목":
                return 5;
            case "금":
                return 6;
            default:
                return -1;
        }
    }

    // 시작 시간을 행 인덱스로 변환
    private int getRowForTime(String time) {
        switch (time) {
            case "09:00":
                return 0;
            case "10:00":
                return 1;
            case "11:00":
                return 2;
            case "12:00":
                return 3;
            case "13:00":
                return 4;
            case "14:00":
                return 5;
            case "15:00":
                return 6;
            case "16:00":
                return 7;
            case "17:00":
                return 8;
            default:
                return -1;
        }
    }

    private void validateTimeSelection() {
        String start = (String) cmbStartTime.getSelectedItem();
        String end = (String) cmbEndTime.getSelectedItem();

        if (start != null && end != null) {
            String[] startParts = start.split(":");
            String[] endParts = end.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int totalStart = startHour * 60 + startMinute;
            int totalEnd = endHour * 60 + endMinute;
            int duration = totalEnd - totalStart;

            // 종료 시간이 시작 시간보다 늦어야 함
            if (totalEnd <= totalStart) {
                JOptionPane.showMessageDialog(this, "종료 시간은 시작 시간보다 늦어야 합니다.");
                cmbEndTime.setSelectedIndex(-1);
                return;
            }

            // 정확히 50분 차이만 허용
            if (duration != 50) {
                JOptionPane.showMessageDialog(this, "시작 시간과 종료 시간은 50분 단위만 허용됩니다.");
                cmbEndTime.setSelectedIndex(-1);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        txtSubject = new javax.swing.JTextField();
        cmbDayOfWeek = new javax.swing.JComboBox<>();
        cmbEndTime = new javax.swing.JComboBox<>();
        lblTitle = new javax.swing.JLabel();
        cmbStartTime = new javax.swing.JComboBox<>();
        btnBack = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        lblSubject = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblTimetable = new javax.swing.JTable();
        lblDayOfWeek = new javax.swing.JLabel();
        lblRoomSelect = new javax.swing.JLabel();
        lblTableTitle = new javax.swing.JLabel();
        lblStartTime = new javax.swing.JLabel();
        btnDelete = new javax.swing.JButton();
        lblEndTime = new javax.swing.JLabel();
        btnEdit = new javax.swing.JButton();
        cmbRoomSelect = new javax.swing.JComboBox<>();
        rbLecture = new javax.swing.JRadioButton();
        rbBlock = new javax.swing.JRadioButton();
        txtContent = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        txtSubject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSubjectActionPerformed(evt);
            }
        });

        cmbDayOfWeek.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "월", "화", "수", "목", "금", "토", "일" }));

        cmbEndTime.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "09:50", "10:50", "11:50", "12:50", "13:50", "14:50", "15:50", "16:50", "17:50" }));
        cmbEndTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbEndTimeActionPerformed(evt);
            }
        });

        lblTitle.setFont(new java.awt.Font("맑은 고딕", 1, 18)); // NOI18N
        lblTitle.setText("강의실 일정 관리");

        cmbStartTime.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00" }));
        cmbStartTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStartTimeActionPerformed(evt);
            }
        });

        btnBack.setText("<");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        btnAdd.setText("➕ 등록");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        tblTimetable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"09:00", "09:50", "", null, null, null, null},
                {"10:00", "10:50", null, null, null, null, null},
                {"11:00", "11:50", null, null, null, null, null},
                {"12:00", "12:50", null, null, null, null, null},
                {"13:00", "13:50", null, null, null, null, null},
                {"14:00", "14:50", null, null, null, null, null},
                {"15:00", "15:50", null, null, null, null, null},
                {"16:00", "16:50", null, null, null, null, null},
                {"17:00", "17:50", null, null, null, null, null}
            },
            new String [] {
                "시작", "종료", "월", "화", "수", "목", "금"
            }
        ));
        jScrollPane1.setViewportView(tblTimetable);

        lblDayOfWeek.setText("요일 :");

        lblRoomSelect.setText("강의실 :");

        lblTableTitle.setFont(new java.awt.Font("맑은 고딕", 1, 14)); // NOI18N
        lblTableTitle.setText("[ 강의실 일정표 ]");

        lblStartTime.setText("시작 시간 :");

        btnDelete.setText("🗑 삭제");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        lblEndTime.setText("종료 시간 :");

        btnEdit.setText("✏ 수정");
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        cmbRoomSelect.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "908", "911", "912", "913", "914", "915", "916", "918" }));
        cmbRoomSelect.setToolTipText("");
        cmbRoomSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbRoomSelectActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbLecture);
        rbLecture.setText("강의실 수업");
        rbLecture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbLectureActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbBlock);
        rbBlock.setText("강의실 제한");

        txtContent.setText("과목명/제한사유 :");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbBlock)
                            .addComponent(rbLecture))
                        .addGap(27, 27, 27)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(lblDayOfWeek)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbDayOfWeek, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRoomSelect)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbRoomSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTableTitle)
                                .addGap(2, 2, 2))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(txtContent)
                                    .addComponent(lblStartTime))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(cmbStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblEndTime)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cmbEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(txtSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnBack)
                        .addGap(181, 181, 181)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTitle)
                            .addComponent(lblSubject))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblTitle)
                            .addComponent(btnBack))
                        .addGap(18, 18, 18)
                        .addComponent(lblSubject)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmbEndTime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(cmbStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblStartTime)
                                .addComponent(lblEndTime)))
                        .addGap(29, 29, 29))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbRoomSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRoomSelect)
                            .addComponent(rbLecture))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbDayOfWeek, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblDayOfWeek)
                            .addComponent(txtSubject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(rbBlock)
                            .addComponent(txtContent)
                            .addComponent(btnAdd))))
                .addComponent(btnEdit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addComponent(lblTableTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        // TODO add your handling code here:
        new AdminMainView("A", client).setVisible(true);
        dispose();
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        if (cmbStartTime.getSelectedItem() == null || cmbEndTime.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "시작 시간과 종료 시간을 모두 선택해 주세요.");
            return;
        }
        
        String selectedRoom = cmbRoomSelect.getSelectedItem().toString().trim();
        String subject = txtSubject.getText().trim();
        String dayOfWeek = cmbDayOfWeek.getSelectedItem().toString().trim();
        String startTime = cmbStartTime.getSelectedItem().toString().trim();
        String endTime = cmbEndTime.getSelectedItem().toString().trim();
        String type = rbLecture.isSelected() ? "수업" : "제한";

        if (selectedRoom.isEmpty() || subject.isEmpty() || dayOfWeek.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 항목을 입력해야 합니다.");
            return;
        }

        try {
            // ADD 요청 생성 및 서버 전송 
            ScheduleRequest req = new ScheduleRequest("ADD", selectedRoom, dayOfWeek, startTime, endTime, subject, type);
            ScheduleResult result = client.sendScheduleRequest(req);
            if (result.isSuccess()) {
                loadTimetable(selectedRoom);    // 성공 시 시간표 갱신
                JOptionPane.showMessageDialog(this, "시간표가 추가되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "시간표 등록 중 오류 발생");
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        if (cmbStartTime.getSelectedItem() == null || cmbEndTime.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "시작 시간과 종료 시간을 모두 선택해 주세요.");
            return;
        }
        
        String selectedRoom = cmbRoomSelect.getSelectedItem().toString().trim();
        String subject = txtSubject.getText().trim();
        String dayOfWeek = cmbDayOfWeek.getSelectedItem().toString().trim();
        String startTime = cmbStartTime.getSelectedItem().toString().trim();
        String endTime = cmbEndTime.getSelectedItem().toString().trim();
        String type = rbLecture.isSelected() ? "수업" : "제한";

        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "수정할 과목명을 입력해주세요.");
            return;
        }

        // UPDATE 요청 생성
        try {
            ScheduleRequest req = new ScheduleRequest("UPDATE", selectedRoom, dayOfWeek, startTime, endTime, subject, type);
            ScheduleResult result = client.sendScheduleRequest(req);
            if (result.isSuccess()) {
                loadTimetable(selectedRoom);
                JOptionPane.showMessageDialog(this, "시간표가 수정되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "시간표 수정 중 오류 발생");
        }
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        if (cmbStartTime.getSelectedItem() == null || cmbEndTime.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "시작 시간과 종료 시간을 모두 선택해 주세요.");
            return;
        }
        
        String selectedRoom = cmbRoomSelect.getSelectedItem().toString().trim();
        String dayOfWeek = cmbDayOfWeek.getSelectedItem().toString().trim();
        String startTime = cmbStartTime.getSelectedItem().toString().trim();
        String endTime = cmbEndTime.getSelectedItem().toString().trim();

        try {
            // DELETE 요청 생성 (subject, type은 비워도 무방)
            ScheduleRequest req = new ScheduleRequest("DELETE", selectedRoom, dayOfWeek, startTime, endTime, "", "");
            ScheduleResult result = client.sendScheduleRequest(req);

            if (result.isSuccess()) {
                int rowIndex = getRowForTime(startTime);
                int colIndex = getDayIndex(dayOfWeek);
                if (rowIndex != -1 && colIndex != -1) {
                    tblTimetable.setValueAt("", rowIndex, colIndex);
                }
                JOptionPane.showMessageDialog(this, "시간표가 삭제되었습니다.");
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "시간표 삭제 중 오류 발생");
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void rbLectureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbLectureActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbLectureActionPerformed

    private void txtSubjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSubjectActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSubjectActionPerformed

    private void cmbRoomSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbRoomSelectActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbRoomSelectActionPerformed

    private void cmbStartTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStartTimeActionPerformed
        validateTimeSelection();
    }//GEN-LAST:event_cmbStartTimeActionPerformed

    private void cmbEndTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbEndTimeActionPerformed
        validateTimeSelection();
    }//GEN-LAST:event_cmbEndTimeActionPerformed

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<String> cmbDayOfWeek;
    private javax.swing.JComboBox<String> cmbEndTime;
    private javax.swing.JComboBox<String> cmbRoomSelect;
    private javax.swing.JComboBox<String> cmbStartTime;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblDayOfWeek;
    private javax.swing.JLabel lblEndTime;
    private javax.swing.JLabel lblRoomSelect;
    private javax.swing.JLabel lblStartTime;
    private javax.swing.JLabel lblSubject;
    private javax.swing.JLabel lblTableTitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JRadioButton rbBlock;
    private javax.swing.JRadioButton rbLecture;
    private javax.swing.JTable tblTimetable;
    private javax.swing.JLabel txtContent;
    private javax.swing.JTextField txtSubject;
    // End of variables declaration//GEN-END:variables
}
