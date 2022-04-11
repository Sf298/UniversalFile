package com.sf298.universal.file.utils;

import java.util.List;
import java.util.function.BiConsumer;

public class ListUtils {

    public static <A,B> void zipToPairs(List<A> a, List<B> b, BiConsumer<A, B> callback) {
        if (a.size() != b.size()) {
            throw new RuntimeException("Lists are not equal in length");
        }

        for (int i = 0; i < a.size(); i++) {
            callback.accept(a.get(i), b.get(i));
        }
    }

}
