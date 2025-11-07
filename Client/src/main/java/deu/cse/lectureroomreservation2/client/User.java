/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.client;

/**
 * 처음부터 arraylist쓸걸...
 *
 */
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.*;
import java.awt.Color;
import java.awt.Component;

import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public abstract class User {

    protected String Name;          //이름
    protected String PhoneNumber;   //휴대폰 번호 -> 비번에 사용
    protected int ReservationTime;  //예약 시간
    protected String Id;            //아이디 = 학번/교수번호/관리자는 마음대로
    protected String PassWord;      //초기 비번 = 휴대폰 번호 뒷8자리

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String PhoneNumber) {
        this.PhoneNumber = PhoneNumber;
    }

    public int getReservationTime() {
        return ReservationTime;
    }

    public void setReservationTime(int ReservationTime) {
        this.ReservationTime = ReservationTime;
    }

    public String getId() {
        return Id;
    }

    public void setId(String Id) {
        this.Id = Id;
    }

    public String getPassWord() {
        return PassWord;
    }

    public void setPassWord(String PassWord) {
        this.PassWord = PassWord;
    }
    
    
    public void MyReservation(){
    
    
    }
    
}
