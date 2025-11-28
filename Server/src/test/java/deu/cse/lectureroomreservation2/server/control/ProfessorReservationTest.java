package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ProfessorReservationTest {

    private ProfessorReservation professorReservation;

    private final String BASE_PATH = System.getProperty("user.dir");
    private final String RESOURCE_PATH = BASE_PATH.endsWith("Server") 
            ? Paths.get(BASE_PATH, "src", "main", "resources").toString()
            : Paths.get(BASE_PATH, "Server", "src", "main", "resources").toString();
            
    private final String RESERVE_FILE_PATH = Paths.get(RESOURCE_PATH, "ReservationInfo.txt").toString();
    private final String BUILDING_FILE_PATH = Paths.get(RESOURCE_PATH, "BuildingInfo.txt").toString();

    private final String PROF_ID = "12345";
    private final String BUILDING = "공학관";
    private final String ROOM = "908";
    private final String DATE_TOMORROW = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    @BeforeEach
    void setUp() throws IOException {
        // 1. 파일 백업
        backupFile(RESERVE_FILE_PATH);
        backupFile(BUILDING_FILE_PATH); // 교수도 BuildingManager로 인원체크 하므로 필요

        // 2. 파일 초기화 및 테스트용 데이터 주입
        createFile(RESERVE_FILE_PATH);
        
        // 정원 100명 설정 
        writeLine(BUILDING_FILE_PATH, "공학관,9층,908,강의실,100");

        // BuildingManager 리셋 
        try {
            java.lang.reflect.Field instance = BuildingManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) { e.printStackTrace(); }

        professorReservation = new ProfessorReservation();
    }

    @AfterEach
    void tearDown() {
        restoreFile(RESERVE_FILE_PATH);
        restoreFile(BUILDING_FILE_PATH);
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

    private ReservationDetails createDetails(String startTime, String endTime) {
        ReservationDetails details = new ReservationDetails(PROF_ID, "P");
        details.setBuildingName(BUILDING);
        details.setRoomNumber(ROOM);
        details.setDate(DATE_TOMORROW); 
        details.setDay("월");
        details.setStartTime(startTime);
        details.setEndTime(endTime);
        details.setUserCount(20);
        details.setPurpose("보강");
        return details;
    }

    // --- Tests ---

    @Test
    @DisplayName("[TC-01] 교수 정상 예약")
    void testReserve_Success() {
        System.out.println("\n========== [TC-01] 교수 정상 예약 ==========");

        ReservationDetails details = createDetails("13:00", "15:00");
        ReserveResult result = professorReservation.reserve(details);

        assertTrue(result.getResult());
        System.out.println(">> 메시지: " + result.getReason());
        System.out.println(">> 검증 결과: PASS (예약 성공)");
    }

    @Test
    @DisplayName("[TC-02] 다른 교수와 시간 충돌 시 실패")
    void testReserve_Fail_Conflict() {
        System.out.println("\n========== [TC-02] 다른 교수와 시간 충돌 ==========");

        // 1. 상황 설정: 다른 교수(ID: 99999)가 이미 14:00~15:00에 예약함 (APPROVED 상태)
        // ReserveManager 형식: 건물,호수,날짜,요일,시작,끝,ID,Role,목적,인원,상태,비고
        String conflictLine = String.format("%s,%s,%s,월,14:00,15:00,99999,P,Meeting,10,APPROVED,-",
                BUILDING, ROOM, DATE_TOMORROW.replace("-", "/"));
        
        writeLine(RESERVE_FILE_PATH, conflictLine);
        System.out.println(">> 사전 설정: 다른 교수의 예약 데이터 주입 (14:00~15:00)");

        // 2. 내 예약 시도: 14:00 ~ 15:00
        ReservationDetails details = createDetails("14:00", "15:00");
        ReserveResult result = professorReservation.reserve(details);

        // 3. 검증
        assertFalse(result.getResult(), "시간이 겹치면 예약이 실패해야 합니다.");
        assertTrue(result.getReason().contains("다른 교수"), "에러 메시지가 충돌 관련이어야 합니다.");
        
        System.out.println(">> 메시지: " + result.getReason());
        System.out.println(">> 검증 결과: PASS (충돌 감지 성공)");
    }

    @Test
    @DisplayName("[TC-03] 학생 예약 무시 확인")
    void testReserve_BumpStudent() {
        System.out.println("\n========== [TC-03] 학생 예약 무시 ==========");

        // 1. 학생 예약(S)이 미리 존재함
        String studentLine = String.format("%s,%s,%s,월,10:00,11:00,20201111,S,Study,10,WAIT,-",
                BUILDING, ROOM, DATE_TOMORROW.replace("-", "/"));
        writeLine(RESERVE_FILE_PATH, studentLine);
        System.out.println(">> 사전 설정: 학생 예약 데이터 주입 (10:00~11:00)");

        // 2. 교수가 동일 시간에 예약 시도
        ReservationDetails details = createDetails("10:00", "11:00");
        ReserveResult result = professorReservation.reserve(details);

        // 3. 검증: 교수는 학생을 밀어내고 성공해야 함
        assertTrue(result.getResult());
        
        System.out.println(">> 메시지: " + result.getReason());
        System.out.println(">> 검증 결과: PASS (학생 예약 취소 및 교수 예약 성공)");
    }
}