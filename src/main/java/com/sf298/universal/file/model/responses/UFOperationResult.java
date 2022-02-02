package com.sf298.universal.file.model.responses;

import com.sf298.universal.file.model.functions.ExceptionNet;
import com.sf298.universal.file.services.UFile;

import static java.util.Objects.nonNull;

public class UFOperationResult<T> {

    public static UFOperationResult<Boolean> createBool(UFile file, boolean value) {
        return new UFOperationResult<>(file, () -> value);
    }

    private final UFile actionedFile;
    private T result;
    private Exception exception;

    public UFOperationResult(UFile actionedFile, Exception exception) {
        this.actionedFile = actionedFile;
        this.exception = exception;
    }

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
        if (nonNull(exception)) {
            throw new RuntimeException(exception);
        }
        return result;
    }

    public T getResultOrDefault(T defaultValue) {
        return isSuccessful() ? result : defaultValue;
    }


    public Exception getException() {
        return exception;
    }


    @Override
    public String toString() {
        if (isSuccessful()) {
            return '{'+result.toString()+'}';
        } else {
            return '{'+exception.toString()+'}';
        }
    }

}
