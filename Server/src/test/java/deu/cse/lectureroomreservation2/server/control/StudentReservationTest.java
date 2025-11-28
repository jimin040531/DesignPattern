package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.ReserveResult;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StudentReservationTest {

    private StudentReservation studentReservation;

    // 경로 설정
    private final String BASE_PATH = System.getProperty("user.dir");
    // receiveController 로직을 참고하여 경로 보정
    private final String RESOURCE_PATH = BASE_PATH.endsWith("Server") 
            ? Paths.get(BASE_PATH, "src", "main", "resources").toString()
            : Paths.get(BASE_PATH, "Server", "src", "main", "resources").toString();
            
    private final String RESERVE_FILE_PATH = Paths.get(RESOURCE_PATH, "ReservationInfo.txt").toString();
    private final String BUILDING_FILE_PATH = Paths.get(RESOURCE_PATH, "BuildingInfo.txt").toString();

    private File originalReserveFile, backupReserveFile;
    private File originalBuildingFile, backupBuildingFile;

    // 테스트 상수
    private final String STUD_ID = "20212991";
    private final String BUILDING = "공학관";
    private final String ROOM = "908";
    private final String DATE_TOMORROW = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    private final String DATE_TODAY = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    @BeforeEach
    void setUp() throws Exception {
        // 1. [파일 백업] ReservationInfo.txt & BuildingInfo.txt
        backupFile(RESERVE_FILE_PATH);
        backupFile(BUILDING_FILE_PATH);

        // 2. [테스트 데이터 생성] 빈 예약 파일 생성
        createFile(RESERVE_FILE_PATH);
        
        // 3. BuildingInfo.txt에 테스트용 강의실 정보 쓰기 (공학관 908호, 정원 30명)
        writeLine(BUILDING_FILE_PATH, "공학관,9층,908,강의실,30");

        // 4. BuildingManager 싱글톤 리셋
        resetBuildingManager();

        studentReservation = new StudentReservation();
    }

    @AfterEach
    void tearDown() {
        // 5. [복구] 테스트 파일 삭제 및 원본 복구
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
        if (file.exists()) file.delete(); // 테스트용 파일 삭제
        if (backup.exists()) backup.renameTo(file); // 원본 복구
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

    private void resetBuildingManager() throws Exception {
        Field instance = BuildingManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null); // instance = null;
        // 다음 getInstance() 호출 시 생성자가 실행되면서 파일을 다시 읽음
    }

    private ReservationDetails createDetails(String date, String startTime, String endTime, int userCount) {
        ReservationDetails details = new ReservationDetails(STUD_ID, "S");
        details.setBuildingName(BUILDING);
        details.setRoomNumber(ROOM);
        details.setDate(date);
        details.setDay("월");
        details.setStartTime(startTime);
        details.setEndTime(endTime);
        details.setUserCount(userCount);
        details.setPurpose("조별 과제");
        return details;
    }

    // --- Tests ---

    @Test
    @DisplayName("[TC-01] 정상 예약 (내일, 정원 내)")
    void testReserve_Success() {
        System.out.println("\n========== [TC-01] 정상 예약 ==========");
        
        // Given: 내일 날짜, 5명 (정원 30명의 50%인 15명 이내)
        ReservationDetails details = createDetails(DATE_TOMORROW, "10:00", "11:00", 5);

        // When
        ReserveResult result = studentReservation.reserve(details);

        // Then
        assertTrue(result.getResult(), "정상적인 예약은 성공해야 합니다.");
        System.out.println(">> 메시지: " + result.getReason());
        System.out.println(">> 검증 결과: PASS (예약 성공)");
    }

    @Test
    @DisplayName("[TC-02] 당일 예약 불가")
    void testReserve_Fail_Today() {
        System.out.println("\n========== [TC-02] 당일 예약 불가 ==========");
        
        // Given: 오늘 날짜 예약 시도
        ReservationDetails details = createDetails(DATE_TODAY, "10:00", "11:00", 5);
        
        // When
        ReserveResult result = studentReservation.reserve(details);

        // Then
        assertFalse(result.getResult(), "당일 예약은 실패해야 합니다.");
        assertTrue(result.getReason().contains("당일 예약"), "에러 메시지에 '당일 예약' 내용이 포함되어야 합니다.");
        
        System.out.println(">> 메시지: " + result.getReason());
        System.out.println(">> 검증 결과: PASS (당일 예약 차단 확인)");
    }

    @Test
    @DisplayName("[TC-03] 총 예약 건수 초과 (2건 제한)")
    void testReserve_Fail_LimitExceeded() {
        System.out.println("\n========== [TC-03] 총 예약 건수 초과 ==========");
        
        // 1. 이미 2건이 예약되어 있다고 가정 (파일에 강제 기입)
        // ReserveManager의 포맷: 건물,강의실,날짜,요일,시작,끝,ID,Role,목적,인원,상태,비고
        String line1 = String.format("%s,%s,%s,월,09:00,10:00,%s,S,Study,5,WAIT,-", BUILDING, ROOM, DATE_TOMORROW.replace("-","/"), STUD_ID);
        String line2 = String.format("%s,%s,%s,월,10:00,11:00,%s,S,Study,5,WAIT,-", BUILDING, ROOM, DATE_TOMORROW.replace("-","/"), STUD_ID);
        
        writeLine(RESERVE_FILE_PATH, line1);
        writeLine(RESERVE_FILE_PATH, line2);
        System.out.println(">> 사전 설정: 기존 예약 2건 파일에 입력 완료");

        // 2. 3번째 예약 시도
        ReservationDetails details = createDetails(DATE_TOMORROW, "13:00", "14:00", 5);
        ReserveResult result = studentReservation.reserve(details);

        // 3. 검증
        assertFalse(result.getResult(), "예약 제한(2건)을 초과하면 실패해야 합니다.");
        assertTrue(result.getReason().contains("예약 건수 초과"), "에러 메시지에 '예약 건수 초과' 내용이 포함되어야 합니다.");
        
        System.out.println(">> 메시지: " + result.getReason());
        System.out.println(">> 검증 결과: PASS (예약 건수 제한 확인)");
    }
}