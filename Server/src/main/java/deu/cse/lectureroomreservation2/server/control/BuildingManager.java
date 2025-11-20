// 파일 경로: deu/cse/lectureroomreservation2/server/control/BuildingManager.java
package deu.cse.lectureroomreservation2.server.control;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet; // 순서 보장
import java.util.List;
import java.util.Set;

/**
 * Singleton Pattern 적용
 * 강의실 정보(수용인원, 건물명)를 관리하는 유일한 객체
 */
/**
 * BuildingInfo.txt 파일을 읽어 건물/층/강의실 구조를 관리합니다.
 */
public class BuildingManager {
    
    // 1. 유일한 인스턴스를 저장할 static 변수
    private static BuildingManager instance;
    private final String filePath;
    
    // 2. 생성자를 private으로 막아 외부에서 new 금지
    private BuildingManager() {
        // receiveController에서 파일 경로를 가져옴
        this.filePath = receiveController.getBuildingInfoFileName();
    }
    
    // 3. 외부에서 접근 가능한 public static 메서드 (지연 로딩 + 동기화)
    public static synchronized BuildingManager getInstance() {
        if (instance == null) {
            instance = new BuildingManager();
        }
        return instance;
    }
 
    // 모든 건물 목록 조회 (중복 제거)
    public List<String> getBuildingList() {
        Set<String> buildings = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    buildings.add(parts[0].trim());
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return new ArrayList<>(buildings);
    }

    // 특정 건물의 층 목록 조회 (중복 제거)
    public List<String> getFloorList(String buildingName) {
        Set<String> floors = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 1 && parts[0].trim().equals(buildingName)) {
                    floors.add(parts[1].trim());
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return new ArrayList<>(floors);
    }

    // 특정 층의 강의실 목록 조회 (이름, 유형, 인원)
    public List<String[]> getRoomList(String buildingName, String floorName) {
        List<String[]> rooms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 4 && parts[0].trim().equals(buildingName) && parts[1].trim().equals(floorName)) {
                    // RoomName, RoomType, Capacity
                    rooms.add(new String[]{parts[2].trim(), parts[3].trim(), parts[4].trim()});
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return new ArrayList<>(rooms);
    }
    
    /**
     * 특정 강의실의 최대 수용 인원을 반환
     * BuildingInfo.txt 형식: 건물, 층, 호수, 타입, 수용인원
     */
    public int getRoomCapacity(String roomNumber) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // parts[2]가 강의실 번호, parts[4]가 수용인원
                if (parts.length >= 5 && parts[2].trim().equals(roomNumber)) {
                    return Integer.parseInt(parts[4].trim());
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return 0; // 못 찾거나 에러 시 0 반환
    }
    
    /**
     * 특정 강의실의 건물 이름을 반환
     */
    public String getBuildingName(String roomNumber) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // parts[0]가 건물명, parts[2]가 강의실 번호
                if (parts.length >= 5 && parts[2].trim().equals(roomNumber)) {
                    return parts[0].trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown"; // 못 찾으면 Unknown
    }
}