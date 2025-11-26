package deu.cse.lectureroomreservation2.server.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// 진짜 파일을 건드리지 않기 위해 UserFileManager의 saveUser를 가짜로 만듦
class MockUserFileManager extends UserFileManager {

    public UserManage receivedUser; // super.saveUser() 로 전달된 유저를 저장해둠

    @Override
    public void saveUser(UserManage user) {
        // 실제 파일 저장 로직은 실행하지 않고, 전달된 객체만 기억
        this.receivedUser = user;
    }
}

public class EncryptedUserFileManagerTest {

    @Test
    public void testSaveUser_encryptsPassword() {

        // 1) Mock 기반 데코레이터 구현
        MockUserFileManager mock = new MockUserFileManager();

        EncryptedUserFileManager encryptedManager = new EncryptedUserFileManager() {
            @Override
            public void saveUser(UserManage user) {
                // super.saveUser() 를 mock.saveUser() 로 바꿔치기
                String rawPw = user.getPassword();
                String encryptedPw = PasswordUtil.encrypt(rawPw);

                UserManage encryptedUser =
                        new UserManage(user.getRole(), user.getName(), user.getId(), encryptedPw);

                mock.saveUser(encryptedUser);
            }
        };

        // 2) 테스트용 유저 생성 (평문 비번)
        UserManage rawUser = new UserManage("STUDENT", "홍길동", "hong", "1234");

        // 3) 실행
        encryptedManager.saveUser(rawUser);

        // 4) 검증
        assertNotNull(mock.receivedUser, "Mock이 유저를 받아야 한다.");

        // 받아진 비밀번호는 암호화된 값이어야 함
        assertNotEquals("1234", mock.receivedUser.getPassword(),
                "비밀번호는 평문이 아닌 암호화된 값이어야 한다.");

        // 암호화 함수로 검증
        String expectedEncrypted = PasswordUtil.encrypt("1234");
        assertEquals(expectedEncrypted, mock.receivedUser.getPassword(),
                "Encrypt 결과가 PasswordUtil.encrypt()와 동일해야 한다.");
    }
}
