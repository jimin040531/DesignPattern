package deu.cse.lectureroomreservation2.server.control;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Paths;

public class TextFileReservationCheckerTest {
    
    // 테스트 대상 객체
    private AbstractReservationChecker checker;

    // 경로 설정 (동적 절대 경로)
    private final String BASE_PATH = System.getProperty("user.dir");
    private final String RESOURCE_PATH = BASE_PATH.endsWith("Server") 
            ? Paths.get(BASE_PATH, "src", "main", "resources").toString()
            : Paths.get(BASE_PATH, "Server", "src", "main", "resources").toString();
            
    private final String RESERVE_FILE_PATH = Paths.get(RESOURCE_PATH, "ReservationInfo.txt").toString();
    private final String SCHEDULE_FILE_PATH = Paths.get(RESOURCE_PATH, "ScheduleInfo.txt").toString();

    // 백업 및 원본 파일 객체
    private File originalReserveFile, backupReserveFile;
    private File originalScheduleFile, backupScheduleFile;

    @BeforeEach
    void setUp() throws IOException {
        // 1. [파일 백업] 예약정보 & 시간표정보
        backupFile(RESERVE_FILE_PATH);
        backupFile(SCHEDULE_FILE_PATH);

        // 2. [파일 초기화] 빈 파일 생성
        createFile(RESERVE_FILE_PATH);
        createFile(SCHEDULE_FILE_PATH);

        // 3. [테스트 데이터 주입] 
        
        // [TC-01용] 정규 수업 데이터 (ScheduleInfo.txt)
        // TextFileReservationChecker 구현에 따르면 인덱스 9(유형)가 "수업"이어야 함
        // 포맷: 년도,학기,건물,호수,요일,시작,종료,과목,교수,유형
        // 2025년 1학기(6월), 공학관 908호, 월요일 09:00
        writeLine(SCHEDULE_FILE_PATH, "2025,1,공학관,908,월,09:00,09:50,캡스톤,홍길동,수업");

        // [TC-02용] 승인된 예약 (ReservationInfo.txt)
        // 포맷: 건물(0),호수(1),날짜(2),요일(3),시작(4),종료(5),ID(6),역할(7),목적(8),인원(9),상태(10),사유(11)
        // 공학관 101호, 2025/06/12, 15:00, APPROVED
        writeLine(RESERVE_FILE_PATH, "공학관,101,2025/06/12,월,15:00,16:00,testUser,S,Study,5,APPROVED,-");

        // [TC-03용] 거절된 예약 (ReservationInfo.txt)
        // 공학관 912호, 2025/06/10, 09:00, REJECTED
        writeLine(RESERVE_FILE_PATH, "공학관,912,2025/06/10,화,09:00,10:00,testUser,S,Study,5,REJECTED,NoReason");

        // [TC-05용] 대기 중인 예약 (ReservationInfo.txt)
        // 공학관 915호, 2025/11/27, 14:00, WAIT
        writeLine(RESERVE_FILE_PATH, "공학관,915,2025/11/27,목,14:00,15:00,testUser,S,Study,5,WAIT,-");

        // 객체 생성
        checker = new TextFileReservationChecker();
    }

    @AfterEach
    void tearDown() {
        // 4. [복구] 테스트 파일 삭제 및 원본 복구
        restoreFile(RESERVE_FILE_PATH);
        restoreFile(SCHEDULE_FILE_PATH);
    }

    // --- Helper Methods ---
    private void backupFile(String path) {
        File file = new File(path);
        File backup = new File(path + ".bak");
        if (file.exists()) file.renameTo(backup);
    }

    private void restoreFile(String path) {
        File file = new File(path);
        File backup = new File(path + ".bak");
        if (file.exists()) file.delete(); 
        if (backup.exists()) backup.renameTo(file); 
    }

    private void createFile(String path) throws IOException {
        File file = new File(path);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        file.createNewFile();
    }

    private void writeLine(String path, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Tests ---

    /**
     * [TC-01] 정규 수업(RegularClass) 우선순위 테스트
     */
    @Test
    public void testCheckStatusRegularClass() {
        System.out.println("\n========== [TC-01] 정규 수업 우선순위 검증 ==========");
        System.out.println("[시나리오] 정규 수업('수업' 타입)이 있는 시간에는 예약이 있더라도 'CLASS' 반환");
        
        // 2025/06/02 -> 6월이므로 1학기 로직 적용
        // 요일: "월요일" -> 구현체에서 첫 글자 "월"로 변환하여 비교
        String result = checker.checkStatus("공학관", "908", "2025/06/02", "월요일", "09:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("CLASS", result, "정규 수업이 있으면 CLASS를 반환해야 합니다.");
        System.out.println(">> 검증 결과: PASS");
    }

    /**
     * [TC-02] 예약 승인(APPROVED) 상태 테스트
     */
    @Test
    public void testCheckStatusActiveReservationApproved() {
        System.out.println("\n========== [TC-02] 예약 승인(APPROVED) 검증 ==========");
        System.out.println("[시나리오] 수업이 없고 승인된 예약이 있는 경우 'APPROVED' 반환");
        
        String result = checker.checkStatus("공학관", "101", "2025/06/12", "월요일", "15:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("APPROVED", result, "승인된 예약은 APPROVED 상태여야 합니다.");
        System.out.println(">> 검증 결과: PASS");
    }

    /**
     * [TC-03] 예약 거절(REJECTED) 상태 테스트
     */
    @Test
    public void testCheckStatusActiveReservationRejected() {
        System.out.println("\n========== [TC-03] 예약 거절(REJECTED) 검증 ==========");
        System.out.println("[시나리오] 거절된 예약은 '없는 예약'으로 간주하여 'AVAILABLE' 반환");
        
        String result = checker.checkStatus("공학관", "912", "2025/06/10", "화요일", "09:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("AVAILABLE", result, "거절된 예약은 AVAILABLE로 처리되어야 합니다.");
        System.out.println(">> 검증 결과: PASS");
    }
    
    /**
     * [TC-04] 빈 강의실 테스트
     */
    @Test
    public void testCheckStatusAvailable() {
        System.out.println("\n========== [TC-04] 빈 강의실 검증 ==========");
        System.out.println("[시나리오] 수업도 없고 예약도 없는 경우 'AVAILABLE' 반환");
        
        String result = checker.checkStatus("공학관", "908", "2025/12/25", "목요일", "12:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("AVAILABLE", result, "빈 강의실은 AVAILABLE이어야 합니다.");
        System.out.println(">> 검증 결과: PASS");
    }

    /**
     * [TC-05] 예약 대기(WAIT) 상태 테스트
     */
    @Test
    public void testCheckStatusActiveReservationWait() {
        System.out.println("\n========== [TC-05] 예약 대기(WAIT) 검증 ==========");
        System.out.println("[시나리오] 대기 중인 예약도 유효한 예약 상태 'WAIT'를 반환");
        
        String result = checker.checkStatus("공학관", "915", "2025/11/27", "목요일", "14:00");
        
        System.out.println(">> 실행 결과: " + result);
        assertEquals("WAIT", result, "대기 중인 예약은 WAIT 상태여야 합니다.");
        System.out.println(">> 검증 결과: PASS");
    }
}