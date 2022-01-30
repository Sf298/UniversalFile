package com.sf298.universal.file.model.responses;

import com.sf298.universal.file.services.UFile;

public class UFOperationResult {

    private Exception exception;
    private final UFile actionedFile;

    public UFOperationResult(UFile actionedFile) {
        this.actionedFile = actionedFile;
    }

    public boolean isSuccessful() {
        return exception == null;
    }

    void setException(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public UFile getActionedFile() {
        return actionedFile;
    }

}
