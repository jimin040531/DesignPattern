package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;

public class CheckMaxTimeResult implements Serializable {
    private boolean exceeded; // true면 초과, false면 미초과
    private String reason;

    public CheckMaxTimeResult(boolean exceeded, String reason) {
        this.exceeded = exceeded;
        this.reason = reason;
    }

    public boolean isExceeded() {
        return exceeded;
    }

    public String getReason() {
        return reason;
    }
}