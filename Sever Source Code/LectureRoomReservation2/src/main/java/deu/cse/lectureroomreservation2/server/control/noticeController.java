package deu.cse.lectureroomreservation2.server.control;

import java.util.*;
import java.io.*;

public class noticeController {
    private static final String NOTICE_FILE = receiveController.getNoticeFileName();

    // 파일 존재 여부 확인 및 없으면 생성
    private static void ensureNoticeFileExists() {
        File file = new File(NOTICE_FILE);
        if (!file.exists()) {
            try {
                // file.getParentFile().mkdirs(); // 상위 폴더도 생성
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 공지 추가 (id별로 한 줄에 여러 공지)
    public static void addNotice(List<String> studentIds, String notice) {
        ensureNoticeFileExists();
        Map<String, List<String>> noticeMap = loadAllNotices();
        for (String id : studentIds) {
            noticeMap.computeIfAbsent(id, k -> new ArrayList<>()).add(notice);
        }
        saveAllNotices(noticeMap);
    }

    // 특정 학생의 공지사항 모두 조회
    public static List<String> getNotices(String studentId) {
        ensureNoticeFileExists();
        Map<String, List<String>> noticeMap = loadAllNotices();

        return noticeMap.getOrDefault(studentId, new ArrayList<>());
    }

    // 특정 학생의 특정 공지사항 삭제
    public static void removeNotice(String studentId, String notice) {
        ensureNoticeFileExists();
        Map<String, List<String>> noticeMap = loadAllNotices();
        List<String> notices = noticeMap.get(studentId);
        if (notices != null) {
            notices.remove(notice);
            if (notices.isEmpty()) {
                noticeMap.remove(studentId);
            }
            saveAllNotices(noticeMap);
        }
    }

    // 모든 공지사항 파일에서 읽기
    private static Map<String, List<String>> loadAllNotices() {
        Map<String, List<String>> noticeMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(NOTICE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String id = parts[0];
                    List<String> notices = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        notices.add(parts[i]);
                    }
                    noticeMap.put(id, notices);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return noticeMap;
    }

    // 모든 공지사항 파일에 저장
    private static void saveAllNotices(Map<String, List<String>> noticeMap) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(NOTICE_FILE))) {
            for (Map.Entry<String, List<String>> entry : noticeMap.entrySet()) {
                StringBuilder sb = new StringBuilder(entry.getKey());
                for (String notice : entry.getValue()) {
                    sb.append(",").append(notice);
                }
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}