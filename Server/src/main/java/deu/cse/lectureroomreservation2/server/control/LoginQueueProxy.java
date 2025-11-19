package deu.cse.lectureroomreservation2.server.control;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 로그인 이후 동시 접속자 수를 3명으로 제한하고,
 * 초과 인원은 대기열(Queue)로 관리하는 Proxy 역할 클래스.
 *
 * - DB/파일 기반 인증(LoginController, UserData)은 건드리지 않음.
 * - 여기서는 "이미 인증된 사용자"에 대해서만 접속 상태를 관리한다.
 */
public class LoginQueueProxy {

    private static final int MAX_ACTIVE = 3;

    // 현재 ACTIVE 상태인 사용자 id
    private static final Set<String> activeUsers = new LinkedHashSet<>();
    // 대기열 (앞에서부터 순서대로)
    private static final LinkedList<String> waitingQueue = new LinkedList<>();

    /**
     * 로그인에 성공한 사용자를 큐에 등록하고,
     * 현재 상태를 문자열로 반환한다.
     *
     * @param userId 로그인 성공한 사용자 id
     * @return "ACTIVE" 또는 "WAITING:n"
     */
    public static synchronized String register(String userId) {
        // 이미 ACTIVE면 그대로 ACTIVE
        if (activeUsers.contains(userId)) {
            return "ACTIVE";
        }

        // 이미 대기열에 있다면, 현재 순번만 다시 계산해서 반환
        int idx = waitingQueue.indexOf(userId);
        if (idx >= 0) {
            return "WAITING:" + (idx + 1);
        }

        // 아직 아무 데도 없고, 자리가 남았으면 ACTIVE로 바로 등록
        if (activeUsers.size() < MAX_ACTIVE) {
            activeUsers.add(userId);
            return "ACTIVE";
        }

        // 자리가 없으면 대기열 맨 뒤에 추가
        waitingQueue.addLast(userId);
        return "WAITING:" + waitingQueue.size();
    }

    /**
     * 사용자가 로그아웃하거나 연결이 끊길 때 호출.
     * ACTIVE에서 제거하고, 자리가 나면 대기열 맨 앞 사용자를 ACTIVE로 올린다.
     */
    public static synchronized void leave(String userId) {
        boolean wasActive = activeUsers.remove(userId);
        if (wasActive) {
            // ACTIVE에서 나간 경우, 대기열 맨 앞을 올림
            if (!waitingQueue.isEmpty()) {
                String next = waitingQueue.removeFirst();
                activeUsers.add(next);
            }
        } else {
            // 대기中이던 사용자가 그냥 나간 경우
            waitingQueue.remove(userId);
        }
    }

    /**
     * 현재 사용자의 상태를 조회.
     *
     * @return
     *  - "ACTIVE"        : 현재 접속 중
     *  - "WAITING:n"     : n번째 대기
     *  - "NONE"          : 큐에 없음(접속 X)
     */
    public static synchronized String getStatus(String userId) {
        if (activeUsers.contains(userId)) {
            return "ACTIVE";
        }
        int idx = waitingQueue.indexOf(userId);
        if (idx >= 0) {
            return "WAITING:" + (idx + 1);
        }
        return "NONE";
    }

    public static synchronized int getActiveCount() {
        return activeUsers.size();
    }

    public static synchronized int getWaitingCount() {
        return waitingQueue.size();
    }

    public static synchronized List<String> getWaitingSnapshot() {
        return List.copyOf(waitingQueue);
    }
}
