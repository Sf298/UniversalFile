package com.sf298.universal.file.services;

import com.sf298.universal.file.model.functions.UFileFilter;
import com.sf298.universal.file.model.functions.UFilenameFilter;
import com.sf298.universal.file.model.inputs.BatchMove;
import com.sf298.universal.file.model.responses.UFOperationBatchResult;
import com.sf298.universal.file.model.responses.UFOperationResult;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class UFileBatch<T extends UFile> {

    public UFOperationBatchResult<Boolean> exists(List<T> targets) {
        return targets.stream()
                .map(UFile::exists)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> isDirectory(List<T> targets) {
        return targets.stream()
                .map(UFile::isDirectory)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> isFile(List<T> targets) {
        return targets.stream()
                .map(UFile::isFile)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }


    public UFOperationBatchResult<Date> lastModified(List<T> targets) {
        return targets.stream()
                .map(UFile::lastModified)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Long> length(List<T> targets) {
        return targets.stream()
                .map(UFile::length)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> createNewFile(List<T> targets) {
        return targets.stream()
                .map(UFile::createNewFile)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> delete(List<T> targets) {
        return targets.stream()
                .map(UFile::delete)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> deleteRecursive(List<T> targets) {
        return targets.stream()
                .map(UFile::deleteRecursive)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }


    public UFOperationBatchResult<String[]> list(List<T> targets) {
        return targets.stream()
                .map(UFile::list)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<String[]> list(List<T> targets, UFilenameFilter filter) {
        return targets.stream()
                .map(UFile::list)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<UFile[]> listFiles(List<T> targets) {
        return targets.stream()
                .map(UFile::listFiles)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<UFile[]> listFiles(List<T> targets, UFilenameFilter filter) {
        return targets.stream()
                .map(UFile::listFiles)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<UFile[]> listFiles(List<T> targets, UFileFilter filter) {
        return targets.stream()
                .map(UFile::listFiles)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> mkdir(List<T> targets) {
        return targets.stream()
                .map(UFile::mkdir)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> mkdirs(List<T> targets) {
        return targets.stream()
                .map(UFile::mkdirs)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }


    public UFOperationBatchResult<Boolean> copyTo(List<BatchMove> targets) {
        return targets.stream()
                .map(t -> t.from.copyTo(t.to))
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public UFOperationBatchResult<Boolean> moveTo(List<BatchMove> targets) {
        return targets.stream()
                .map(t -> t.from.moveTo(t.to))
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

}
