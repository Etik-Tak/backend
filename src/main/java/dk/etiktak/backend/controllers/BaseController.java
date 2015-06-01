package dk.etiktak.backend.controllers;

import dk.etiktak.backend.controllers.json.BaseJsonObject;

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
