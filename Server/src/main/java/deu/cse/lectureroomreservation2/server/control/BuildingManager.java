// 파일 경로: deu/cse/lectureroomreservation2/server/control/BuildingManager.java
package deu.cse.lectureroomreservation2.server.control;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton Pattern 적용 (Thread-Safe Version)
 * 강의실 정보(수용인원, 건물명 등)를 관리하는 유일한 객체
 * 특징: 서버 시작 시 파일을 메모리에 한 번만 로딩하여 성능 최적화 (Read-Only)
 */
public class BuildingManager {
    
    // 1. 유일한 인스턴스를 저장할 static 변수
    private static BuildingManager instance;
    private final String filePath;
    
    // 파일 내용을 메모리에 저장해둘 리스트 (캐시)
    // 구조: [건물명, 층, 호수, 타입, 수용인원]
    private List<String[]> allRoomData; 

    // 2. 생성자를 private으로 막아 외부에서 new 금지
    private BuildingManager() {
        this.filePath = receiveController.getBuildingInfoFileName();
        this.allRoomData = new ArrayList<>();
        
        // 생성자에서 딱 한 번 파일을 읽어 메모리에 저장 (속도 향상)
        loadAllDataFromFile(); 
    }
    
    // 내부 헬퍼 메서드: 파일 -> 메모리 적재
    private void loadAllDataFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // "공학관,9층,908,세미나실,30" -> 콤마로 분리
                String[] parts = line.split(",");
                if (parts.length >= 5) { 
                    allRoomData.add(parts); 
                }
            }
            // 로그 확인용 (필요 없으면 삭제 가능)
            System.out.println("BuildingManager: 건물 정보 로딩 완료 (" + allRoomData.size() + "개 강의실)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3. 외부 접근 메서드 (Thread-Safe 동기화 적용)
    public static synchronized BuildingManager getInstance() {
        if (instance == null) {
            instance = new BuildingManager();
        }
        return instance;
    }

    // ================= [조회 기능] =================
    // 파일 입출력 없이 메모리(allRoomData)에서 바로 꺼내주므로 매우 빠름

    // 1. 모든 건물 목록 조회 (중복 제거)
    public List<String> getBuildingList() {
        Set<String> buildings = new LinkedHashSet<>(); // 순서 보장 + 중복 제거
        for (String[] parts : allRoomData) {
            buildings.add(parts[0].trim());
        }
        return new ArrayList<>(buildings);
    }

    // 2. 특정 건물의 층 목록 조회
    public List<String> getFloorList(String buildingName) {
        Set<String> floors = new LinkedHashSet<>();
        for (String[] parts : allRoomData) {
            if (parts[0].trim().equals(buildingName)) {
                floors.add(parts[1].trim());
            }
        }
        return new ArrayList<>(floors);
    }

    // 3. 특정 층의 강의실 목록 조회 (호수, 타입, 인원 반환)
    public List<String[]> getRoomList(String buildingName, String floorName) {
        List<String[]> rooms = new ArrayList<>();
        for (String[] parts : allRoomData) {
            if (parts[0].trim().equals(buildingName) && parts[1].trim().equals(floorName)) {
                // 필요한 정보만 추려서 리스트에 추가
                rooms.add(new String[]{parts[2].trim(), parts[3].trim(), parts[4].trim()});
            }
        }
        return rooms;
    }
    
    // 4. 특정 강의실의 최대 수용 인원 반환 (건물 이름 포함 검색)
    public int getRoomCapacity(String buildingName, String roomNumber) {
        for (String[] parts : allRoomData) {
            // parts[0]: 건물명, parts[2]: 강의실번호, parts[4]: 인원
            // 건물 이름과 강의실 번호가 모두 일치해야 함
            if (parts[0].trim().equals(buildingName) && parts[2].trim().equals(roomNumber)) {
                try {
                    return Integer.parseInt(parts[4].trim());
                } catch (NumberFormatException e) {
                    return 0; // 숫자 변환 에러 시 0
                }
            }
        }
        
        // 건물 이름이 null이거나 못 찾았을 경우 호수만으로 검색 (기존 로직 호환성)
        if (buildingName == null) {
             for (String[] parts : allRoomData) {
                if (parts[2].trim().equals(roomNumber)) {
                    try { return Integer.parseInt(parts[4].trim()); } catch(Exception e) {}
                }
             }
        }
        
        return 0; // 못 찾으면 0
    }
    
    // 5. 특정 강의실의 건물 이름 반환
    public String getBuildingName(String roomNumber) {
        for (String[] parts : allRoomData) {
            if (parts[2].trim().equals(roomNumber)) {
                return parts[0].trim();
            }
        }
        return "Unknown";
    }
}