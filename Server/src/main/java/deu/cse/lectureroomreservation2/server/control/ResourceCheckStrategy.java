/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author rbcks
 */
public class ResourceCheckStrategy implements ValidationStrategy {
    @Override
    public String validate() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        // 메모리가 10MB 이하로 남았으면 경고
        if (freeMemory < 10 * 1024 * 1024) { 
            return "WARNING: 서버 메모리 부족 (" + (freeMemory / 1024 / 1024) + "MB 남음)";
        }
        return "SUCCESS: 시스템 리소스 충분함";
    }
}
