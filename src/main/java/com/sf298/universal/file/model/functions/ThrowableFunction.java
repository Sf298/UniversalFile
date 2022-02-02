package com.sf298.universal.file.model.functions;

public interface ThrowableFunction<I,R> {

    R apply(I i) throws Exception;

}
