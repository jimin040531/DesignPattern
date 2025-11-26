/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package deu.cse.lectureroomreservation2.server.control;

import deu.cse.lectureroomreservation2.common.LoginStatus;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author win10
 */
public class QueuedLoginProxyTest {

    private QueuedLoginProxy instance;
    private Semaphore limiter;

    // ===== 테스트용 가짜 AuthService =====
    private static class StubAuthService implements AuthService {

        @Override
        public LoginStatus authenticate(String id, String password, String selectedRole) {
            // 아이디/비번/역할이 맞으면 성공, 아니면 실패라고 가정
            if ("dummyId".equals(id)
                    && "dummyPw".equals(password)
                    && "STUDENT".equals(selectedRole)) {
                return new LoginStatus(true, selectedRole, "SUCCESS");
            }
            return new LoginStatus(false, selectedRole, "FAIL");
        }
    }

    public QueuedLoginProxyTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        // 동시 허용 인원 1명짜리 세마포어
        limiter = new Semaphore(1);

        // 테스트용 가짜 AuthService 사용
        AuthService real = new StubAuthService();

        // 프록시 생성
        instance = new QueuedLoginProxy(real, limiter);
    }

    @AfterEach
    public void tearDown() {
    }

    // ================== 테스트 1: 성공 케이스 ==================
    @Test
    public void authenticate_success_returnsSuccessStatus() {
        String id = "dummyId";
        String password = "dummyPw";
        String selectedRole = "STUDENT";

        LoginStatus result = instance.authenticate(id, password, selectedRole);

        assertNotNull(result);
        assertTrue(result.isLoginSuccess());
        assertEquals("STUDENT", result.getRole());
    }

    // ================== 테스트 2: 실패 시 세마포어 다시 반납되는지 ==================
    @Test
    public void authenticate_fail_releasesPermit() {
        String id = "dummyId";
        String password = "wrongPw";   // 틀린 비번
        String selectedRole = "STUDENT";

        int before = limiter.availablePermits();

        LoginStatus result = instance.authenticate(id, password, selectedRole);

        int after = limiter.availablePermits();

        assertNotNull(result);
        assertFalse(result.isLoginSuccess());
        // 실패한 뒤에는 세마포어 개수가 다시 원래 값으로 돌아와야 함
        assertEquals(before, after);
    }
}
