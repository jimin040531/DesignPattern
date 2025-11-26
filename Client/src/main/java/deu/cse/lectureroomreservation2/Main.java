/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package deu.cse.lectureroomreservation2;
 
import deu.cse.lectureroomreservation2.client.view.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JOptionPane;
import java.util.regex.Pattern;
        
/**
 *
 * @author SAMSUNG
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            }
            new LoginFrame().setVisible(true);
        });
    }

    public static void main(String[] args) {
        //클라이언트 실행시 뒤 에 서버 주소를 넣으면 해당 주소로 연결됨
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            }
            if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
                // IP 또는 도메인 정규식
                String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
                String domainRegex = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
                if (!Pattern.matches(ipRegex, args[0]) && !Pattern.matches(domainRegex, args[0])) {
                    JOptionPane.showMessageDialog(null, "명령줄 오류 : \n아이피 또는 도메인 형식이 올바르지 않습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                new LoginFrame(args[0]).setVisible(true);
            } else {
                new LoginFrame().setVisible(true);
            }
        });
    }

}