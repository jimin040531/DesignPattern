/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import java.io.File;

/**
 *
 * @author rbcks
 */
public class FileIntegrityStrategy implements ValidationStrategy {
    @Override
    public String validate() {
        // 확인해야 할 파일 목록
        String[] requiredFiles = {
            receiveController.getUserFileName(),
            receiveController.getReservationInfoFileName(),
            receiveController.getScheduleInfoFileName(),
            receiveController.getBuildingInfoFileName()
        };

        for (String filePath : requiredFiles) {
            File file = new File(filePath);
            if (!file.exists()) {
                return "ERROR: 필수 파일 누락됨 - " + file.getName();
            }
        }
        return "SUCCESS: 모든 데이터 파일 정상";
    }
}
