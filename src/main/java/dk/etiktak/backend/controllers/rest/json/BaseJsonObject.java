package dk.etiktak.backend.controllers.rest.json;

/**
 * Used as a parent for all json objects.
 */
public class BaseJsonObject {
    private String result = "OK";

    public BaseJsonObject() {
    }

    public BaseJsonObject(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
