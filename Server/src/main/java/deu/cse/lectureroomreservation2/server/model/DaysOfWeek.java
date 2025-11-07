/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

package deu.cse.lectureroomreservation2.server.model;

/**
 *
 * @author Jimin
 */

/**
 * DaysOfWeek 열거형은 요일 정보를 표현하며,
 * 한글 요일 문자열을 영어 enum으로 변환하는 유틸리티 메서드를 포함함
 */

public enum DaysOfWeek {
    MONDAY(0),
    TUESDAY(1),
    WEDNESDAY(2),
    THURSDAY(3),
    FRIDAY(4),
    SATURDAY(5),
    SUNDAY(6);

    private final int n;

    DaysOfWeek(int n) {
        this.n = n;
    }

    public int index() {
        return this.n;
    }

    // 한글 요일을 영어 요일로 변환
    public static DaysOfWeek fromKoreanDay(String koreanDay) {
        switch (koreanDay) {
            case "월" -> {
                return MONDAY;
            }
            case "화" -> {
                return TUESDAY;
            }
            case "수" -> {
                return WEDNESDAY;
            }
            case "목" -> {
                return THURSDAY;
            }
            case "금" -> {
                return FRIDAY;
            }
            case "토" -> {
                return SATURDAY;
            }
            case "일" -> {
                return SUNDAY;
            }
            default -> throw new IllegalArgumentException("Invalid day: " + koreanDay);
        }
    }
}
