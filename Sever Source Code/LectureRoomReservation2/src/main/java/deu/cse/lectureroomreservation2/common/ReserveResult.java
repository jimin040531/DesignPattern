package deu.cse.lectureroomreservation2.common;

import java.io.*;

public class ReserveResult implements Serializable {
    public boolean result;
    public String reason;

    public ReserveResult(boolean result, String reason) {
        this.result = result;
        this.reason = reason;
    }

    public boolean getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }
}