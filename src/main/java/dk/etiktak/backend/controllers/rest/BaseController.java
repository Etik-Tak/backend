package dk.etiktak.backend.controllers.rest;

import dk.etiktak.backend.controllers.rest.json.BaseJsonObject;

/**
 * Used as a parent for all controllers.
 */
public class BaseController {
    public BaseJsonObject ok() {
        return new BaseJsonObject();
    }

    public BaseJsonObject message(String message) {
        return new BaseJsonObject(message);
    }
}
