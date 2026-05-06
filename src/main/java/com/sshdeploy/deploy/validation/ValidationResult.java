package com.sshdeploy.deploy.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {
    private final List<String> errors = new ArrayList<>();

    public static ValidationResult ok() {
        return new ValidationResult();
    }

    public void addError(String message) {
        if (message != null && !message.isBlank()) {
            errors.add(message);
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
