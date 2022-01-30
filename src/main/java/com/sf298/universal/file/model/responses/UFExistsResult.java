package com.sf298.universal.file.model.responses;

import com.sf298.universal.file.model.ExceptionNet;
import com.sf298.universal.file.services.UFile;

public class UFExistsResult extends UFOperationResult {

    boolean result;

    public UFExistsResult(UFile actionedFile, ExceptionNet<Boolean> result) {
        super(actionedFile);
        try {
            this.result = result.run();
        } catch (Exception e) {
            super.setException(e);
        }
    }

}
