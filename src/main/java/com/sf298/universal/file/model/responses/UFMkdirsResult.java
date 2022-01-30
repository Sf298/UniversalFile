package com.sf298.universal.file.model.responses;

import com.sf298.universal.file.model.ExceptionNet;
import com.sf298.universal.file.services.UFile;

public class UFMkdirsResult extends UFOperationResult {

    private boolean result;

    public UFMkdirsResult(UFile actionedFile, ExceptionNet<Boolean> result) {
        super(actionedFile);
        try {
            this.result = result.run();
        } catch (Exception e) {
            super.setException(e);
        }
    }

    public boolean getResult() {
        return result;
    }

}
