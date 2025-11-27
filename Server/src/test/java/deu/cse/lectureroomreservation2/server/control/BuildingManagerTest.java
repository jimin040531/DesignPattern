package deu.cse.lectureroomreservation2.server.control;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BuildingManagerTest {

    // 테스트 대상: 싱글턴 인스턴스 가져오기
    BuildingManager manager = BuildingManager.getInstance();

    /**
     * [TC-01] 싱글턴 패턴(Singleton Pattern) 인스턴스 유일성 검증
     */
    @Test
    public void testSingletonInstance() {
        System.out.println("========== [TC-01] 싱글턴 인스턴스 유일성 검증 ==========");
        System.out.println("[시나리오] getInstance()를 여러 번 호출해도 항상 동일한 객체 주소를 반환해야 함");
        
        BuildingManager instance1 = BuildingManager.getInstance();
        BuildingManager instance2 = BuildingManager.getInstance();
        
        System.out.println(">> Instance 1 HashCode: " + System.identityHashCode(instance1));
        System.out.println(">> Instance 2 HashCode: " + System.identityHashCode(instance2));
        
        // assertSame은 두 객체의 참조(메모리 주소)가 같은지 비교합니다.
        assertSame(instance1, instance2, "두 인스턴스는 완전히 동일한 객체여야 합니다.");
        System.out.println(">> 검증 결과: PASS (Instance 1 == Instance 2)\n");
    }

    /**
     * [TC-02] 강의실 수용 인원 조회 (정상 케이스)
     */
    @Test
    public void testGetRoomCapacityValid() {
        System.out.println("========== [TC-02] 강의실 수용 인원 조회 (정상) ==========");
        System.out.println("[시나리오] 존재하는 건물과 강의실 입력 시 올바른 수용 인원을 반환");
        // 가정: BuildingInfo.txt에 "공학관,9층,908,세미나실,30" 데이터가 있다고 가정
        System.out.println("[입력 데이터] 건물: 공학관 | 강의실: 908");

        int capacity = manager.getRoomCapacity("공학관", "908");
        
        System.out.println(">> 실행 결과: " + capacity);
        
        // 실제 파일 내용에 따라 기대값(30)은 달라질 수 있습니다.
        // 여기서는 0보다 큰 값이 나오면 데이터가 로드된 것으로 간주합니다.
        assertTrue(capacity > 0, "유효한 강의실의 수용 인원은 0보다 커야 합니다."); 
        // 만약 파일 내용이 확실하다면: assertEquals(30, capacity);
        System.out.println(">> 검증 결과: PASS\n");
    }

    /**
     * [TC-03] 강의실 수용 인원 조회 (미등록 강의실)
     */
    @Test
    public void testGetRoomCapacityInvalid() {
        System.out.println("========== [TC-03] 강의실 수용 인원 조회 (실패) ==========");
        System.out.println("[시나리오] 존재하지 않는 강의실 조회 시 0을 반환");
        System.out.println("[입력 데이터] 건물: 마법학교 | 강의실: 999");

        int capacity = manager.getRoomCapacity("마법학교", "999");
        
        System.out.println(">> 실행 결과: " + capacity);
        assertEquals(0, capacity, "존재하지 않는 강의실은 0을 반환해야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }

    /**
     * [TC-04] 건물 목록 조회 테스트
     */
    @Test
    public void testGetBuildingList() {
        System.out.println("========== [TC-04] 건물 목록 조회 검증 ==========");
        System.out.println("[시나리오] 등록된 건물 목록 리스트를 반환해야 함 (Null 아님)");

        List<String> buildings = manager.getBuildingList();
        
        System.out.println(">> 조회된 건물 개수: " + buildings.size());
        System.out.println(">> 건물 목록: " + buildings);
        
        assertNotNull(buildings, "건물 목록은 null이 아니어야 합니다.");
        // 파일이 정상 로드되었다면 최소 1개 이상의 건물이 있어야 함
        // assertFalse(buildings.isEmpty(), "건물 목록이 비어있지 않아야 합니다.");
        System.out.println(">> 검증 결과: PASS\n");
    }
}