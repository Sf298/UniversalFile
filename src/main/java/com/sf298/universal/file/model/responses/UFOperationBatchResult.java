package com.sf298.universal.file.model.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UFOperationBatchResult<T> extends ArrayList<UFOperationResult<T>> {

    public boolean allSuccessful() {
        return stream().allMatch(UFOperationResult::isSuccessful);
    }

    public List<UFOperationResult<T>> getFailed() {
        return stream().filter(r -> !r.isSuccessful()).collect(Collectors.toList());
    }

}
