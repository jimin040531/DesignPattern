/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.model;

import deu.cse.lectureroomreservation2.server.model.Reservation;

/**
 *
 * @author rbcks
 */
public interface ReservationIterator {
    boolean hasNext();
    Reservation next();
}
