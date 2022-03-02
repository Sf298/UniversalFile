package com.sf298.universal.file.model.functions;

public interface ExceptionNet<T, E extends Exception> {

    T run() throws E;

}
