/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.server.model.ReservationIterator;
import deu.cse.lectureroomreservation2.server.model.Reservation;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author rbcks
 */
public class StudentReservationList implements ReservationAggregate {
    private List<Reservation> reservations;

    // 파일에서 읽어온 String 리스트를 Reservation 객체 리스트로 변환
    public StudentReservationList(List<String> fileLines) {
        this.reservations = new ArrayList<>();
        if (fileLines != null) {
            for (String line : fileLines) {
                reservations.add(new Reservation(line));
            }
        }
    }
    
    @Override
    public ReservationIterator iterator() {
        return new StudentReservationIterator(this.reservations);
    }
}
