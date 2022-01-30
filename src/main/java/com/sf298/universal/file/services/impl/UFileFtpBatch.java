package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;

import java.util.List;
import java.util.stream.Collectors;

public class UFileFtpBatch {

    public static UFOperationBatchResult<UFExistsResult> exists(List<UFileFtp> targets) {
        return targets.stream()
                .map(UFile::exists)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFIsDirectoryResult> isDirectory(List<UFileFtp> targets) {
        return targets.stream()
                .map(UFile::isDirectory)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFIsFileResult> isFile(List<UFileFtp> targets) {
        return targets.stream()
                .map(UFile::isFile)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFMkdirResult> mkdir(List<UFileFtp> targets) {
        return targets.stream()
                .map(UFile::mkdir)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFMkdirsResult> mkdirs(List<UFileFtp> targets) {
        return targets.stream()
                .map(UFile::mkdirs)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

}
