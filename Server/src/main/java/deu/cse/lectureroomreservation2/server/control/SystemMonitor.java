/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

/**
 *
 * @author rbcks
 */
public class SystemMonitor {
    private ValidationStrategy strategy;

    // 기본 전략 설정 (파일 검사)
    public SystemMonitor() {
        this.strategy = new FileIntegrityStrategy();
    }

    // 런타임에 전략 교체 가능 (Strategy Pattern의 핵심)
    public void setStrategy(ValidationStrategy strategy) {
        this.strategy = strategy;
    }

    public String checkSystem() {
        if (strategy == null) return "전략이 설정되지 않음";
        return strategy.validate();
    }
}
