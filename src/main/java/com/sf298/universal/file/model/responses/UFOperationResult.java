package com.sf298.universal.file.model.responses;

import com.sf298.universal.file.model.ExceptionNet;
import com.sf298.universal.file.services.UFile;

public class UFOperationResult<T> {

    private final UFile actionedFile;
    private T result;
    private Exception exception;

    public UFOperationResult(UFile actionedFile, ExceptionNet<T> exceptionNet) {
        this.actionedFile = actionedFile;
        try {
            this.result = exceptionNet.run();
        } catch (Exception e) {
            this.exception = e;
        }
    }

    public UFile getActionedFile() {
        return actionedFile;
    }

    public boolean isSuccessful() {
        return exception == null;
    }

    public T getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }

}
