/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import deu.cse.lectureroomreservation2.server.model.GetReservation;

/**
 *
 * @author namw2
 */
public class TableRearrange {

    public static void InsertTable(String room, String day, JTable ViewTimeTable, String[][] result, String[][] result2) { //TODO if써서 reservation이랑 schedule 분할
        // DefaultTableModel을 사용하여 JTable 생성

        String[] columnNames = {"시작시간", "끝시간", "강의실번호", "예약현황"};

        // 해당 시간에 해당하는 행 찾기
        // 데이터베이스에는 행의 순서가 없다 -> 단순 for로 다 돌리면 안됨
        // for 돌리기전 선택된 호실+ 요일에 대해 배열 또 만들기
        String fixResult[][] = new String[result.length][5];
        String fixResult2[][] = new String[result2.length][5];
        int fixRow = 0;
        int fixRow2 = 0;

        for (int i = 0; i < result.length; i++) {
            if (result[i][0].trim().equals(room.trim()) && result[i][1].trim().equals(day.trim())) {
                fixResult[fixRow][0] = result[i][2]; // start time
                fixResult[fixRow][1] = result[i][3]; // end time
                fixResult[fixRow][2] = result[i][0]; // room
                fixResult[fixRow][3] = result[i][4]; // state 0- 공실, 1-학생예약, 2-정규수업, 3-제한
                fixResult[fixRow][4] = result[i][6];
                //System.out.println(i+"입니다");
                fixRow++;
            }
        }
        for (int j = 0; j < result2.length; j++) {
            if (result2[j][0].trim().equals(room.trim()) && result2[j][1].trim().equals(day.trim())) {
                //System.out.println("이거는 제대로 작동하나?2"+j+"와"+fixRow2);
                fixResult2[fixRow2][0] = result2[j][2].trim(); // start time
                fixResult2[fixRow2][1] = result2[j][3].trim(); // end time
                fixResult2[fixRow2][2] = result2[j][0].trim(); // room
                if (result2[j][5].equals("수업")) {
                    fixResult2[fixRow2][3] = "2"; //state 0- 공실, 1-학생예약, 2-정규수업, 3-제한
                } else if (result2[j][5].equals("제한")) {
                    fixResult2[fixRow2][3] = "3";
                } else {
                    //System.out.println("뭔가 잘못됨1");
                    fixResult2[fixRow2][3] = "error"; //이거 쓸일이 없으면 좋을건데
                }
                fixRow2++;
            }// else System.out.println("뭔가 잘못됨2"+j+"와"+fixRow2);
        }

        /*
        for (String[] row : fixResult2) {
            System.out.println("이건 fxr2");
            System.out.println(Arrays.toString(row));
        }
         */
        String[][] finalResult2 = new String[fixRow2][4];
        for (int i = 0; i < fixRow2; i++) {
            finalResult2[i] = fixResult2[i];
        }

        /*for (String[] row : finalResult2) {
            System.out.println("이건 fr2");
            System.out.println(Arrays.toString(row));
        }*/
        String[][] finalResult = new String[fixRow][4];
        for (int i = 0; i < fixRow; i++) {
            finalResult[i] = fixResult[i];
        }

        /*for (String[] row : finalResult) {
            System.out.println("이건 fr");
            System.out.println(Arrays.toString(row));
        }*/
        int colLength = finalResult.length > 0 ? finalResult[0].length
                : (finalResult2.length > 0 ? finalResult2[0].length : 0);

        String[][] combinedResult = new String[finalResult.length + finalResult2.length][colLength];
        if (finalResult.length > 0) {
            System.arraycopy(finalResult, 0, combinedResult, 0, finalResult.length);
        }

        if (finalResult2.length > 0) {
            System.arraycopy(finalResult2, 0, combinedResult, finalResult.length, finalResult2.length);
        }
        System.arraycopy(finalResult, 0, combinedResult, 0, finalResult.length);
        System.arraycopy(finalResult2, 0, combinedResult, finalResult.length, finalResult2.length);

        /*
        for (String[] row : combinedResult) {
            System.out.println(Arrays.toString(row));
        }
         */
        // 모델 객체 가져오기
        DefaultTableModel model = (DefaultTableModel) ViewTimeTable.getModel();

        // 학생예약이 된 행을 추적하기 위한 Set
        java.util.Set<Integer> reservedRows1 = new java.util.HashSet<>();
        java.util.Set<Integer> reservedRows2 = new java.util.HashSet<>();
        java.util.Set<Integer> reservedRows3 = new java.util.HashSet<>();

        // finalResult 형식: {startTime, endTime, room, state,date}
        for (String[] row : combinedResult) {
            String startTime = row[0];
            String roomNum = row[2];
            String state = row[3];
            String Date = row[4];

            if (state.equals("1")) { // 상태가 예약이라면
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (model.getValueAt(i, 0).equals(startTime)) {
                        model.setValueAt(roomNum, i, 2);               // Room 설정
                        model.setValueAt("학생예약", i, 3);           // State 변경
                        model.setValueAt(Date, i, 4);
                        reservedRows1.add(i); // 색칠할 행 기억
                        break;
                    }
                }
            } else if (state.equals("2")) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (model.getValueAt(i, 0).equals(startTime)) {
                        model.setValueAt(roomNum, i, 2);               // Room 설정
                        model.setValueAt("교수예약", i, 3);           // State 변경
                        model.setValueAt(Date, i, 4);
                        reservedRows2.add(i); // 색칠할 행 기억
                        break;
                    }
                }
            } else if (state.equals("3")) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (model.getValueAt(i, 0).equals(startTime)) {
                        model.setValueAt(roomNum, i, 2);               // Room 설정
                        model.setValueAt("사용제한", i, 3);           // State 변경
                        model.setValueAt(Date, i, 4);
                        reservedRows3.add(i); // 색칠할 행 기억
                        break;
                    }
                }
            }
        }

        // 렌더러 정의
        TableCellRenderer colorRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected && reservedRows1.contains(row)) {
                    c.setBackground(Color.PINK); // 예약된 셀 색상
                    c.setForeground(Color.BLACK);
                } else if (!isSelected && reservedRows2.contains(row)) {
                    c.setBackground(Color.YELLOW); // 예약된 셀 색상
                    c.setForeground(Color.BLACK);
                } else if (!isSelected && reservedRows3.contains(row)) {
                    c.setBackground(Color.ORANGE); // 예약된 셀 색상
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };

        // 모든 열에 렌더러 적용
        for (int col = 0; col < ViewTimeTable.getColumnCount(); col++) {
            ViewTimeTable.getColumnModel().getColumn(col).setCellRenderer(colorRenderer);
        }

        ViewTimeTable.repaint(); // 테이블 다시 그리기    

    }

    public static void QuickInsertTable2(JTable QuickViewTimeTable, String[][] result, String[][] result2, String UserID) {
        String[][] allReservations = new String[result.length + result2.length][5];
        int idx = 0;

        for (String[] row : result) {
            allReservations[idx][0] = row[2]; // startTime
            allReservations[idx][1] = row[3]; // endTime
            allReservations[idx][2] = row[0]; // room
            allReservations[idx][3] = row[4]; // state
            allReservations[idx][4] = row[6]; // date
            idx++;
        }

        for (String[] row : allReservations) {
            System.out.println(Arrays.toString(row));
        }

        for (String[] row2 : result2) {
            allReservations[idx][0] = row2[2]; // startTime
            allReservations[idx][1] = row2[3]; // endTime
            allReservations[idx][2] = row2[0]; // room
            if (row2[5].trim().equals("수업")) {
                allReservations[idx][3] = "2";
            } else if (row2[5].trim().equals("제한")) {
                allReservations[idx][3] = "3";
            } else {
                allReservations[idx][3] = "error";
            }
            allReservations[idx][4] = row2[1]; // 날짜 대신 요일 넣기
            idx++;
        }

        for (String[] row : allReservations) {
            System.out.println(Arrays.toString(row));
        }

        // 현재 시간 가져오기
        String[] currentTime = GetReservation.getPCTime(); // currentTime[0]: HH:mm, currentTime[1]: yy/MM/dd,CurrentTime[2]:월
        String todayDate = currentTime[1];
        idx = 0;

        //TODO 시간 임시 테스트용 - 안쓸시엔 꼭 주석 처리 
        currentTime[0] = "11:00";
        currentTime[1] = "25/05/30";
        currentTime[2] = "금";

        System.out.println(currentTime[0]);
        System.out.println(currentTime[1]);
        System.out.println(currentTime[2]);

        //String[][] finalReservations = new String[allReservations.length][5];
        List<String[]> FinalReservations = new ArrayList<>();

        for (String[] row : allReservations) {
            if (row[4].equals(currentTime[1]) || row[4].trim().equals((String) currentTime[2].trim())) {
                FinalReservations.add(Arrays.copyOf(row, row.length)); // 동적으로 리스트에 추가
            }
        }

        System.out.println("test입니다1:FinalReservations출력"); //TODO 지우기

        for (String[] row : FinalReservations) {
            System.out.println(Arrays.toString(row)); // 각 행을 출력
        }

        FinalReservations.replaceAll(row //공백 삭제
                -> Arrays.stream(row)
                        .map(String::trim) // 각 요소에 trim 적용
                        .toArray(String[]::new) // 다시 배열로 변환
        );

        FinalReservations.sort(Comparator.comparingInt(row -> Integer.parseInt(row[0].substring(0, 2)))); //오름차순 정렬

        for (String[] row : FinalReservations) {
            if (Integer.parseInt(row[0].substring(0, 2)) < Integer.parseInt(currentTime[0].substring(0, 2))) {

            }
        }

        for (int i = 0; i < FinalReservations.size(); i++) {
            String[] row = FinalReservations.get(i); // 현재 행 가져오기

            if (Integer.parseInt(row[0].substring(0, 2)) < Integer.parseInt(currentTime[0].substring(0, 2)) + 1) {//현재시간제외를 위해 +1
                FinalReservations.remove(i); // 현재 행 삭제
                i--; // 리스트 크기가 줄어들었으므로 인덱스 조정
            } else {
                break; // 조건을 만족하지 않으면 반복문 종료
            }
        }

        //Integer.parseInt(data1[0].substring(0, 2)
        System.out.println("test입니다1-1:정렬및 공백 삭제후FinalReservations출력"); //TODO 지우기

        for (String[] row : FinalReservations) {
            System.out.println(Arrays.toString(row)); // 각 행을 출력
        }

        //TODO바로 밑 주석처리된 코드는 지울게 아니라 수정해야함 trim은 이미 완료했고 현 시간 비교해서 나온 뭐시기 해야함
        String[][] finalReservationsArray = FinalReservations.stream()
                .map(row -> Arrays.stream(row)
                .map(String::trim) // 각 요소에 trim 적용
                .toArray(String[]::new)) // 다시 배열로 변환
                .toArray(String[][]::new); // 2차원 배열로 변환

        System.out.println("test입니다2:finalReservationsArray출력"); //TODO 지우기

        for (String[] row : finalReservationsArray) {
            System.out.println(Arrays.toString(row)); // 각 행을 출력
        }

        Arrays.sort(finalReservationsArray, Comparator.comparing(o -> o[0]));

        System.out.println("test입니다3:정렬후finalReservationsArray출력"); //TODO 지우기
        for (String[] row : finalReservationsArray) {
            System.out.println(Arrays.toString(row));
        }

        //TODO 일단 서버 시간을 사용하여 날짜랑 요일 비교후 빼오고 오름차순 정렬까지는 완선
        /*
        *이제 가능한 시간부터 채우되 리스트에 들어가 있으면 제외하고 이를 반복해서 5개 뽑아오기
         */
        List<String[]> FiveReservations = new ArrayList<>(); //가능한 시간 5개가 들어갈 배열
        List<String[]> ColumnFilterReservations = new ArrayList<>(); //finalReservationsArray에서 비교를위해 0,1,2행만 남기고 자른걸 저장한 배열
        String[] roomslots = {"908", "911", "912", "913", "914", "915", "916", "918"};
        String[][] timeslots = {
            {"09:00", "09:50"},
            {"10:00", "10:50"},
            {"11:00", "11:50"},
            {"12:00", "12:50"},
            {"13:00", "13:50"},
            {"14:00", "14:50"},
            {"15:00", "15:50"},
            {"16:00", "16:50"},
            {"17:00", "17:50"}
        };

        for (String[] reservation : finalReservationsArray) {
            ColumnFilterReservations.add(Arrays.copyOf(reservation, 3));
        }

        System.out.println("남은 예약 정보:필요없는 값 제거");
        for (String[] reservation : ColumnFilterReservations) {
            System.out.println(Arrays.toString(reservation));
        }

        //루프를 72번 돌려야할듯 RoomSlots X timeSlots
        int stack = 0;
        outerloop:
        for (int i = 0; i < timeslots.length; i++) { // time
            if (Integer.parseInt(timeslots[i][0].substring(0, 2)) < Integer.parseInt(currentTime[0].substring(0, 2)) + 1) {
                continue;
            }
            for (int j = 0; j < roomslots.length; j++) { // room
                String[] temp = {timeslots[i][0], timeslots[i][1], roomslots[j]};

                boolean isReserved = false;
                for (String[] reserved : ColumnFilterReservations) {
                    if (Arrays.equals(temp, reserved)) {
                        isReserved = true;
                        break;
                    }
                }
                if (isReserved) {
                    continue;
                }

                FiveReservations.add(temp);
                stack++;

                if (stack == 5) {
                    break outerloop;
                }
            }
        }

        System.out.println("가능한 예약 정보:가장 빠른 시간대의 강의실 5개");
        for (String[] reservation : FiveReservations) {
            System.out.println(Arrays.toString(reservation));
        }

        DefaultTableModel model = (DefaultTableModel) QuickViewTimeTable.getModel();
        model.setRowCount(0);

        // FiveReservations에 있는 데이터를 JTable에 추가
        for (String[] reservation : FiveReservations) {
            String startTime = reservation[0];
            String endTime = reservation[1];
            String room = reservation[2];
            String status = "공실";
            String date = todayDate;

            model.addRow(new Object[]{startTime, endTime, room, status, date});
        }
    }

}
