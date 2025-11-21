/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;
import deu.cse.lectureroomreservation2.server.model.ReservationIterator;
import deu.cse.lectureroomreservation2.server.model.Reservation;
import java.util.List;
/**
 *
 * @author rbcks
 */
public class StudentReservationIterator implements ReservationIterator {
    private List<Reservation> reservations;
    private int position = 0;

    public StudentReservationIterator(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @Override
    public boolean hasNext() {
        return position < reservations.size();
    }

    @Override
    public Reservation next() {
        Reservation reservation = reservations.get(position);
        position++;
        return reservation;
    }
}
