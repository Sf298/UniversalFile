package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;

import java.util.List;
import java.util.stream.Collectors;

public class UFileLocalDiskBatch {

    public static UFOperationBatchResult<UFOperationResult<Boolean>> exists(List<UFileLocalDisk> targets) {
        return targets.stream()
                .map(UFile::exists)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> isDirectory(List<UFileLocalDisk> targets) {
        return targets.stream()
                .map(UFile::isDirectory)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> isFile(List<UFileLocalDisk> targets) {
        return targets.stream()
                .map(UFile::isFile)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> mkdir(List<UFileLocalDisk> targets) {
        return targets.stream()
                .map(UFile::mkdir)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> mkdirs(List<UFileLocalDisk> targets) {
        return targets.stream()
                .map(UFile::mkdirs)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

}
