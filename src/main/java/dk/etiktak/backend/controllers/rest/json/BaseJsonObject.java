package dk.etiktak.backend.controllers.rest.json;

/**
 * Used as a parent for all json objects.
 */
public class BaseJsonObject {
    public static final String RESULT_OK = "OK";

    private String result = RESULT_OK;

    public BaseJsonObject() {
    }

    public BaseJsonObject(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
