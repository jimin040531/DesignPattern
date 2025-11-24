/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package deu.cse.lectureroomreservation2.client.view;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import deu.cse.lectureroomreservation2.client.Client;
import deu.cse.lectureroomreservation2.client.view.AdminMainView;
import deu.cse.lectureroomreservation2.client.view.FirstSemesterStrategy;
import deu.cse.lectureroomreservation2.client.view.SecondSemesterStrategy;
import deu.cse.lectureroomreservation2.client.view.SemesterStrategy;
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
    private SemesterStrategy semesterStrategy;

    // 클라이언트 객체를 받아 UI 초기화 및 시간표 자동 로딩 설정
    public RoomScheduleManagementView(Client client) {
        this.client = client;           // 먼저 client 설정
        initComponents();               // UI 초기화
        setLocationRelativeTo(null);
        // (1) BuildingInfo.txt에서 건물 목록 로드
        loadBuildingsFromFile();

        // (2) 학기 기본값 세팅 (원하면 현재 학기 설정)
        cmbSemester.setSelectedIndex(0); // "1" 학기 선택

        // (3) 건물 선택시 강의실 목록 다시 채우기
        cmbBuilding.addActionListener(evt -> loadRoomsForSelectedBuilding());

        // (4) 강의실 선택시 시간표 자동 로드
        loadTimetableOnRoomSelect();

        // 🔹 기본 전략: 1학기 전략
        this.semesterStrategy = new FirstSemesterStrategy();

        // 🔹 학기 콤보 박스 변경 시 전략 교체
        cmbSemester.addActionListener(evt -> {
            String sem = (String) cmbSemester.getSelectedItem();
            if ("1".equals(sem)) {
                semesterStrategy = new FirstSemesterStrategy();
            } else if ("2".equals(sem)) {
                semesterStrategy = new SecondSemesterStrategy();
            } else {
                // 혹시 모를 예외 상황 대비 (기본은 1학기 전략)
                semesterStrategy = new FirstSemesterStrategy();
            }
        });
    }

    // BuildingInfo.txt를 읽어서 "건물" 콤보박스 채우기
    private void loadBuildingsFromFile() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("BuildingInfo.txt");
            if (is == null) {
                JOptionPane.showMessageDialog(this, "BuildingInfo.txt를 찾을 수 없습니다.");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            java.util.Set<String> buildingSet = new java.util.LinkedHashSet<>();

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;          // 빈 줄
                }
                if (line.startsWith("#")) {
                    continue;    // 주석 줄
                }
                String[] parts = line.split(",");
                if (parts.length >= 1) {
                    String buildingName = parts[0].trim();
                    buildingSet.add(buildingName);
                }
            }

            cmbBuilding.removeAllItems();
            for (String b : buildingSet) {
                cmbBuilding.addItem(b);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "건물 정보를 불러오는 중 오류 발생: " + e.getMessage());
        }
    }

    // 선택된 건물에 속한 강의실만 cmbRoomSelect 에 채우기
    private void loadRoomsForSelectedBuilding() {
        String selectedBuilding = (String) cmbBuilding.getSelectedItem();
        if (selectedBuilding == null || selectedBuilding.trim().isEmpty()) {
            return;
        }

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("BuildingInfo.txt");
            if (is == null) {
                JOptionPane.showMessageDialog(this, "BuildingInfo.txt를 찾을 수 없습니다.");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            cmbRoomSelect.removeAllItems();

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                // 형식: 건물명,층,호수,용도,인원수
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String buildingName = parts[0].trim();
                    String floor = parts[1].trim();    // 필요하면 나중에 사용
                    String roomNumber = parts[2].trim();

                    if (buildingName.equals(selectedBuilding)) {
                        cmbRoomSelect.addItem(roomNumber);
                    }
                }
            }

            // 방 목록을 채운 뒤 첫 번째 방 선택 시 자동으로 시간표 로드
            if (cmbRoomSelect.getItemCount() > 0) {
                cmbRoomSelect.setSelectedIndex(0);
                String firstRoom = cmbRoomSelect.getSelectedItem().toString();
                loadTimetable(firstRoom);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "강의실 정보를 불러오는 중 오류 발생: " + e.getMessage());
        }
    }

    // 테이블을 빈 값으로 초기화하는 메서드
    private void initializeTimetable() {
        for (int i = 0; i < tblTimetable.getRowCount(); i++) {
            for (int j = 2; j < tblTimetable.getColumnCount(); j++) {
                tblTimetable.setValueAt("", i, j);
            }
        }
    }

    private void loadTimetableOnRoomSelect() {
        cmbRoomSelect.addActionListener(evt -> {
            Object sel = cmbRoomSelect.getSelectedItem();
            if (sel == null) {
                return;
            }
            String room = sel.toString().trim();
            if (room.isEmpty()) {
                return;
            }
            // 선택된 강의실의 시간표 로드
            loadTimetable(room);
        });
    }

    private void loadTimetable(String selectedRoom) {
        initializeTimetable();

        String type = rbLecture.isSelected() ? "수업" : "제한";

        // 🔹 년도 / 학기 / 건물
        String year = jTextField1.getText().trim();          // ✨ 반드시 jTextField1 사용
        String semester = (String) cmbSemester.getSelectedItem();
        String building = (String) cmbBuilding.getSelectedItem();

        // 🔹 기본 입력 값 검증
        if (year.isEmpty() || semester == null || building == null || selectedRoom == null) {
            // 년도나 건물 등이 비어 있으면 아예 서버 요청을 보내지 않음
            return;
        }

        try {
            // 월~금 요일별로 한 번씩 LOAD 요청
            for (String day : new String[]{"월", "화", "수", "목", "금"}) {

                ScheduleRequest req = new ScheduleRequest(
                        "LOAD",
                        year,
                        semester,
                        building,
                        selectedRoom,
                        day,
                        null, // start
                        null, // end
                        null, // subject
                        null, // professor
                        type
                );

                ScheduleResult result = client.sendScheduleRequest(req);

                // 응답이 null 이거나 실패면 건너뛴다 (예외는 catch에서 처리)
                if (result == null || !result.isSuccess() || result.getData() == null) {
                    continue;
                }

                for (Map.Entry<String, String> entry : result.getData().entrySet()) {
                    String timeKey = entry.getKey();   // "09:00" 또는 "09:00~09:50" 둘 중 하나일 수 있음
                    String text = entry.getValue();

                    // key 가 "09:00~09:50" 형식이면 앞부분만 사용
                    String startTime = timeKey;
                    int tildeIndex = timeKey.indexOf("~");
                    if (tildeIndex != -1) {
                        startTime = timeKey.substring(0, tildeIndex);
                    }

                    int rowIndex = getRowForTime(startTime);
                    int colIndex = getDayIndex(day);

                    if (rowIndex != -1 && colIndex != -1) {
                        tblTimetable.setValueAt(text, rowIndex, colIndex);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();  // 콘솔에 실제 원인 찍힘
            JOptionPane.showMessageDialog(this, "시간표 불러오기 실패: " + e.getMessage());
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

        // 전략이 아직 설정 안 됐을 경우를 대비한 방어 코드
        if (semesterStrategy == null) {
            semesterStrategy = new FirstSemesterStrategy();
        }

        // 🔹 전략에게 검증을 위임
        String errorMessage = semesterStrategy.validateTimeRange(start, end);

        // 🔹 에러가 있으면 메시지 띄우고 종료 시간 초기화
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(this, errorMessage);
            cmbEndTime.setSelectedIndex(-1);
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
        jSeparator1 = new javax.swing.JSeparator();
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
        txtContent = new javax.swing.JLabel();
        txtYear = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cmbSemester = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        cmbBuilding = new javax.swing.JComboBox<>();
        btnBackup = new javax.swing.JButton();
        btnRestore = new javax.swing.JButton();
        lblProfessor = new javax.swing.JLabel();
        txtProfessor = new javax.swing.JTextField();

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

        txtContent.setText("과목명/제한사유 :");

        txtYear.setText("년도");

        jTextField1.setColumns(4);
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel2.setText("학기");

        cmbSemester.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2" }));

        jLabel3.setText("건물");

        cmbBuilding.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnBackup.setText("백업");
        btnBackup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackupActionPerformed(evt);
            }
        });

        btnRestore.setText("복원");
        btnRestore.setToolTipText("");
        btnRestore.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRestoreActionPerformed(evt);
            }
        });

        lblProfessor.setText("교수 입력 :");

        txtProfessor.setColumns(8);
        txtProfessor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtProfessorActionPerformed(evt);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnBack)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(108, 108, 108)
                                .addComponent(txtYear)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(2, 2, 2)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTitle)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblSubject)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cmbBuilding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblRoomSelect)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbRoomSelect, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(btnBackup, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnRestore, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(rbLecture)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblProfessor)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtProfessor, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(36, 36, 36))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(lblDayOfWeek)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cmbDayOfWeek, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblStartTime)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lblEndTime)))
                        .addGap(66, 66, 66)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnDelete, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblTableTitle, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(cmbEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(txtContent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(48, 48, 48)
                        .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblTitle)
                                    .addComponent(btnBack))
                                .addGap(18, 18, 18)
                                .addComponent(lblSubject)
                                .addGap(70, 70, 70))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnAdd)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnEdit)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDelete)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblTableTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(btnBackup)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnRestore)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtYear)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)
                            .addComponent(cmbSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(cmbBuilding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbRoomSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRoomSelect))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(rbLecture)
                            .addComponent(lblDayOfWeek)
                            .addComponent(cmbDayOfWeek, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStartTime)
                            .addComponent(cmbStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblEndTime)
                            .addComponent(cmbEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSubject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtContent)
                            .addComponent(lblProfessor)
                            .addComponent(txtProfessor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(74, 74, 74)))
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

        String year = jTextField1.getText().trim();
        String semester = (String) cmbSemester.getSelectedItem();
        String building = (String) cmbBuilding.getSelectedItem();
        String selectedRoom = cmbRoomSelect.getSelectedItem().toString().trim();
        String subject = txtSubject.getText().trim();
        String dayOfWeek = cmbDayOfWeek.getSelectedItem().toString().trim();
        String startTime = cmbStartTime.getSelectedItem().toString().trim();
        String endTime = cmbEndTime.getSelectedItem().toString().trim();
        String type = rbLecture.isSelected() ? "수업" : "제한";
        String professor;
        if (rbLecture.isSelected()) {               // 수업일 때만 교수명 필수
            professor = txtProfessor.getText().trim();
            if (professor.isEmpty()) {
                JOptionPane.showMessageDialog(this, "교수명을 입력해 주세요.");
                return;
            }
        } else {                                    // 제한일 때는 굳이 필요 없으니 "-"
            professor = "-";
        }

        if (year.isEmpty() || semester == null || building == null
                || selectedRoom.isEmpty() || subject.isEmpty() || dayOfWeek.isEmpty()
                || startTime.isEmpty() || endTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "년도, 학기, 건물, 강의실, 과목, 요일, 시간을 모두 입력해야 합니다.");
            return;
        }

        try {
            // 새 생성자 사용
            ScheduleRequest req = new ScheduleRequest(
                    "ADD",
                    year,
                    semester,
                    building,
                    selectedRoom,
                    dayOfWeek,
                    startTime,
                    endTime,
                    subject,
                    professor,
                    type
            );
            ScheduleResult result = client.sendScheduleRequest(req);
            if (result.isSuccess()) {
                loadTimetable(selectedRoom);
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

        // 🔹 새로 추가된 부분: 년도 / 학기 / 건물 / 교수 읽기
        String year = jTextField1.getText().trim();
        String semester = (String) cmbSemester.getSelectedItem();
        String building = (String) cmbBuilding.getSelectedItem();
        String selectedRoom = cmbRoomSelect.getSelectedItem().toString().trim();
        String subject = txtSubject.getText().trim();
        String dayOfWeek = cmbDayOfWeek.getSelectedItem().toString().trim();
        String startTime = cmbStartTime.getSelectedItem().toString().trim();
        String endTime = cmbEndTime.getSelectedItem().toString().trim();
        String type = rbLecture.isSelected() ? "수업" : "제한";
        String professor;
        if (rbLecture.isSelected()) {
            professor = txtProfessor.getText().trim();
            if (professor.isEmpty()) {
                JOptionPane.showMessageDialog(this, "교수명을 입력해 주세요.");
                return;
            }
        } else {
            professor = "-";
        }

        // 🔹 유효성 검사에 year/semester/building도 추가
        if (year.isEmpty() || semester == null || building == null) {
            JOptionPane.showMessageDialog(this, "년도, 학기, 건물을 먼저 입력/선택해 주세요.");
            return;
        }

        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "수정할 과목명을 입력해주세요.");
            return;
        }

        // UPDATE 요청 생성
        try {
            // 🔥 여기 한 줄이 “옛날 7개짜리 → 새 11개짜리”로 바뀐 것
            ScheduleRequest req = new ScheduleRequest(
                    "UPDATE",
                    year,
                    semester,
                    building,
                    selectedRoom,
                    dayOfWeek,
                    startTime,
                    endTime,
                    subject,
                    professor,
                    type
            );

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

        // 🔹 년도 / 학기 / 건물 읽기
        String year = jTextField1.getText().trim();
        String semester = (String) cmbSemester.getSelectedItem();
        String building = (String) cmbBuilding.getSelectedItem();

        // 🔹 강의실 / 요일 / 시간
        String selectedRoom = cmbRoomSelect.getSelectedItem().toString().trim();
        String dayOfWeek = cmbDayOfWeek.getSelectedItem().toString().trim();
        String startTime = cmbStartTime.getSelectedItem().toString().trim();
        String endTime = cmbEndTime.getSelectedItem().toString().trim();

        // 🔹 교수명은 아직 입력 필드가 없으니 임시로 "-"
        String professor = "-";
        String subject = "";   // 삭제니까 굳이 안 써도 됨
        String type = rbLecture.isSelected() ? "수업" : "제한";

        // 필수값 체크 (년도/학기/건물까지 포함)
        if (year.isEmpty() || semester == null || building == null
                || selectedRoom.isEmpty() || dayOfWeek.isEmpty()
                || startTime.isEmpty() || endTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "년도, 학기, 건물, 강의실, 요일, 시간을 모두 입력해야 합니다.");
            return;
        }

        try {
            // 11개 인자 사용하는 새로운 생성자
            ScheduleRequest req = new ScheduleRequest(
                    "DELETE",
                    year,
                    semester,
                    building,
                    selectedRoom,
                    dayOfWeek,
                    startTime,
                    endTime,
                    subject,
                    professor,
                    type
            );

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
            e.printStackTrace(); // 어디서 터지는지 확인용
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

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void btnBackupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackupActionPerformed
        try {
            // 서버에서 ScheduleInfo.txt를 이 이름으로 복사하게 됨
            String backupName = "ScheduleInfo_backup.txt";

            // Client.java에 우리가 추가한 메서드
            ScheduleResult result = client.sendScheduleBackupRequest(backupName);

            JOptionPane.showMessageDialog(this, result.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "백업 중 오류 발생");
        }
    }//GEN-LAST:event_btnBackupActionPerformed

    private void btnRestoreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestoreActionPerformed
        try {
            String backupName = "ScheduleInfo_backup.txt";

            ScheduleResult result = client.sendScheduleRestoreRequest(backupName);
            JOptionPane.showMessageDialog(this, result.getMessage());

            // 복원 성공 시, 현재 선택된 강의실 시간표 다시 불러오기
            Object sel = cmbRoomSelect.getSelectedItem();
            if (sel != null) {
                String room = sel.toString().trim();
                if (!room.isEmpty()) {
                    loadTimetable(room);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "복원 중 오류 발생");
        }
    }//GEN-LAST:event_btnRestoreActionPerformed

    private void txtProfessorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtProfessorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtProfessorActionPerformed

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnBackup;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnRestore;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<String> cmbBuilding;
    private javax.swing.JComboBox<String> cmbDayOfWeek;
    private javax.swing.JComboBox<String> cmbEndTime;
    private javax.swing.JComboBox<String> cmbRoomSelect;
    private javax.swing.JComboBox<String> cmbSemester;
    private javax.swing.JComboBox<String> cmbStartTime;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel lblDayOfWeek;
    private javax.swing.JLabel lblEndTime;
    private javax.swing.JLabel lblProfessor;
    private javax.swing.JLabel lblRoomSelect;
    private javax.swing.JLabel lblStartTime;
    private javax.swing.JLabel lblSubject;
    private javax.swing.JLabel lblTableTitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JRadioButton rbLecture;
    private javax.swing.JTable tblTimetable;
    private javax.swing.JLabel txtContent;
    private javax.swing.JTextField txtProfessor;
    private javax.swing.JTextField txtSubject;
    private javax.swing.JLabel txtYear;
    // End of variables declaration//GEN-END:variables
}
