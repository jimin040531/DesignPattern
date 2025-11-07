/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.lectureroomreservation2.client;

/**
 *
 * @author namw2
 */
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;



public class Student extends User{
    public Student(String ID, String name, String phoneNumber) {
        this.Id = ID;
        this.Name = name;
        this.PhoneNumber = phoneNumber;
        this.PassWord = phoneNumber.substring(phoneNumber.length() - 8); // 초기 비번
        this.ReservationTime = 0;
    }
    
    
}
