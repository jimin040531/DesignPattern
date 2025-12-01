/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package deu.cse.lectureroomreservation2.server;

import deu.cse.lectureroomreservation2.server.control.AutoReserveCleaner;
import deu.cse.lectureroomreservation2.server.control.ReserveManager; 
import deu.cse.lectureroomreservation2.server.control.SystemMonitor;
import deu.cse.lectureroomreservation2.server.control.ResourceCheckStrategy;
import deu.cse.lectureroomreservation2.server.control.LoginController;
import deu.cse.lectureroomreservation2.server.control.AuthService;
import deu.cse.lectureroomreservation2.server.control.QueuedLoginProxy;
import deu.cse.lectureroomreservation2.common.LoginStatus;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.*;

/**
 *
 * @author Prof.Jong Min Lee
 */
public class Server {

    private final AuthService controller;
    private static final int MAX_CLIENTS = 3;   
    private final Semaphore connectionLimiter = new Semaphore(MAX_CLIENTS); 
    private final Set<String> loggedInUsers = Collections.synchronizedSet(new HashSet<>());

    public Server() {
        controller = new QueuedLoginProxy(new LoginController(), connectionLimiter);
    }

    public Set<String> getLoggedInUsers() {
        return loggedInUsers;
    }

    public boolean isUserLoggedIn(String id) {
        return loggedInUsers.contains(id);
    }

    public void addLoggedInUser(String id) {
        loggedInUsers.add(id);
    }

    public void removeLoggedInUser(String id) {
        loggedInUsers.remove(id);
    }

    public LoginStatus requestAuth(String id, String password, String selectedRole) {
        LoginStatus status = controller.authenticate(id, password, selectedRole);

        if (status.isLoginSuccess()) {
            System.out.printf(">>> id = %s, password = %s, selected = %s%n%n", id, password, selectedRole);
        } else {
            System.out.println(">>> ID , PW, Role 재확인.");
        }

        return status;
    }

    public Semaphore getConnectionLimiter() {
        return connectionLimiter;
    }

    // 서버 시작
    public void start() {
        
        try {
            java.net.InetAddress local = java.net.InetAddress.getLocalHost();
            System.out.println("\n------------------------------------------------");
            System.out.println(">>> 현재 서버 컴퓨터의 IP 주소: " + local.getHostAddress());
            System.out.println("------------------------------------------------\n");
        } catch (Exception e) {
            System.out.println("IP 주소를 가져올 수 없습니다.");
        }
        
        int port = 5000;
        printServerHealthCheck();

        // 서버 켜지자마자 1회 정리
        ReserveManager.purgePastReservations();

        // 주기적 정리 스레드 시작
        new AutoReserveCleaner().start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server port : " + port + " Waiting now...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("new client connect : " + clientSocket.getInetAddress());    

                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printServerHealthCheck() {
        System.out.println("\n=== [System Init] 서버 초기 상태 점검 (Strategy Pattern) ===");

        SystemMonitor monitor = new SystemMonitor();

        // 전략 1: 파일 확인
        System.out.println(monitor.checkSystem());

        // 전략 2: 메모리 확인 (전략 교체)
        monitor.setStrategy(new ResourceCheckStrategy());
        System.out.println(monitor.checkSystem());
        System.out.println("======================================================\n");
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start(); 
    }
}