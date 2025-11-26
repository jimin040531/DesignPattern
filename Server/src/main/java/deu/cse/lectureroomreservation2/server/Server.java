 /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package deu.cse.lectureroomreservation2.server;

import deu.cse.lectureroomreservation2.server.control.AutoReserveCleaner;
import deu.cse.lectureroomreservation2.common.LoginStatus;
import deu.cse.lectureroomreservation2.server.control.LoginController;
import deu.cse.lectureroomreservation2.server.control.AuthService;
import deu.cse.lectureroomreservation2.server.control.QueuedLoginProxy;
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
    private static final int MAX_CLIENTS = 3;   // 최대 동시접속 가능 인원 수 3명.
    private final Semaphore connectionLimiter = new Semaphore(MAX_CLIENTS); // 최대 3명까지

    private final Set<String> loggedInUsers = Collections.synchronizedSet(new HashSet<>());

    public Server() {
        // LoginController를 프록시로 감싸서 생성
        controller = new QueuedLoginProxy(new LoginController(), connectionLimiter);
        
       // 예약 정보 자동 삭제 스레드 시작
        new AutoReserveCleaner().start();
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
        int port = 5000;
        printServerHealthCheck();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server port : " + port + " Waiting now...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // 클라이언트 접속 대기
                System.out.println("new client connect : " + clientSocket.getInetAddress());    // 새 클라이언트 연결 + 주소 sout

                // 클라이언트 하나를 처리할 스레드 생성
                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void printServerHealthCheck() {
        System.out.println("\n=== [System Init] 서버 초기 상태 점검 (Strategy Pattern) ===");
        deu.cse.lectureroomreservation2.server.control.SystemMonitor monitor 
            = new deu.cse.lectureroomreservation2.server.control.SystemMonitor();
            
        // 전략 1: 파일 확인
        System.out.println(monitor.checkSystem());
        
        // 전략 2: 메모리 확인 (전략 교체)
        monitor.setStrategy(new deu.cse.lectureroomreservation2.server.control.ResourceCheckStrategy());
        System.out.println(monitor.checkSystem());
        System.out.println("======================================================\n");
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Server server = new Server();
        server.start(); // 서버 실행
    }

}
