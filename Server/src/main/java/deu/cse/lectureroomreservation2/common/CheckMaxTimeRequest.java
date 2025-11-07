package deu.cse.lectureroomreservation2.common;

import java.io.Serializable;

public class CheckMaxTimeRequest implements Serializable {
    private String id;

    public CheckMaxTimeRequest(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
