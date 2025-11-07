package deu.cse.lectureroomreservation2.client.view;

import javax.swing.JOptionPane;

public class viewResultLR {
    public void viewResult() {
        boolean check = true;
        String reason = "예약 성공 또는 실패 view 테스트";
        
        String showReason = "사유 : " + reason;
        if(check == false) {
            int goExit = JOptionPane.showConfirmDialog(null, showReason, "예약 실패", JOptionPane.DEFAULT_OPTION);
            if (goExit == JOptionPane.OK_OPTION) {
                return;
            }
        } else {
            JOptionPane.showMessageDialog(null, "예약 성공!", "예약 성공", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }
    
    
    public void viewResult(boolean check, String reason) {
        String showReason = "예약에 실패하였습니다! \n 사유 : " + reason;
        if(check == false) {
            int goExit = JOptionPane.showConfirmDialog(null, showReason, "예약 실패", JOptionPane.DEFAULT_OPTION);
            if (goExit == JOptionPane.OK_OPTION) {
                return;
            }
            
        } else {
            JOptionPane.showMessageDialog(null, "예약 성공!", "예약 성공", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }
}
