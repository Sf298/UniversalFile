package com.sf298.universal.file.model.responses;

import com.sf298.universal.file.model.ExceptionNet;
import com.sf298.universal.file.services.UFile;

public class UFIsFileResult extends UFOperationResult {

    boolean result;

    public UFIsFileResult(UFile actionedFile, ExceptionNet<Boolean> result) {
        super(actionedFile);
        try {
            this.result = result.run();
        } catch (Exception e) {
            super.setException(e);
        }
    }

}
