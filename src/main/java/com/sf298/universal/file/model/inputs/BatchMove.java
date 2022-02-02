package com.sf298.universal.file.model.inputs;

import com.sf298.universal.file.services.UFile;

public class BatchMove {

    public UFile from;
    public UFile to;

    public BatchMove() {}

    public BatchMove(UFile from, UFile to) {
        this.from = from;
        this.to = to;
    }

}
