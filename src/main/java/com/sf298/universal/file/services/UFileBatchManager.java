package com.sf298.universal.file.services;

public class UFileBatchManager {
/*
    public static UFOperationBatchResult<UFOperationResult<Boolean>> existsBatch(List<UFile> targets) {
        return splitAndReorder(
                targets,
                UFileLocalDiskBatch::exists,
                UFileFtpBatch::exists,
                UFileDropboxBatch::exists
        );
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> isDirectoryBatch(List<UFile> targets) {
        return splitAndReorder(
                targets,
                UFileLocalDiskBatch::isDirectory,
                UFileFtpBatch::isDirectory,
                UFileDropboxBatch::isDirectory
        );
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> isFileBatch(List<UFile> targets) {
        return splitAndReorder(
                targets,
                UFileLocalDiskBatch::isFile,
                UFileFtpBatch::isFile,
                UFileDropboxBatch::isFile
        );
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> mkdirBatch(List<UFile> targets) {
        return splitAndReorder(
                targets,
                UFileLocalDiskBatch::mkdir,
                UFileFtpBatch::mkdir,
                UFileDropboxBatch::mkdir
        );
    }

    public static UFOperationBatchResult<UFOperationResult<Boolean>> mkdirsBatch(List<UFile> targets) {
        return splitAndReorder(
                targets,
                UFileLocalDiskBatch::mkdirs,
                UFileFtpBatch::mkdirs,
                UFileDropboxBatch::mkdirs
        );
    }

    private static <R> UFOperationBatchResult<UFOperationResult<R>> splitAndReorder(List<UFile> targets,
                                                                                    Function<List<UFileLocalDisk>, UFOperationBatchResult<UFOperationResult<R>>> localFilesBatch,
                                                                                    Function<List<UFileFtp>, UFOperationBatchResult<UFOperationResult<R>>> ftpBatch,
                                                                                    Function<List<UFileDropbox>, UFOperationBatchResult<UFOperationResult<R>>> dropboxBatch) {
        if (targets.isEmpty()) {
            return new UFOperationBatchResult<>();
        }

        Map<UFile, UFOperationResult<R>> resultMap = new HashMap<>();

        resultMap.putAll(batchProcessForType(targets, UFileLocalDisk.class, localFilesBatch));
        resultMap.putAll(batchProcessForType(targets, UFileFtp.class, ftpBatch));
        resultMap.putAll(batchProcessForType(targets, UFileDropbox.class, dropboxBatch));

        return targets.stream().map(resultMap::get)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    private static <I,R extends UFOperationResult<?>> Map<I,R> batchProcessForType(List<UFile> unfilteredTargets,
                                                                                   Class<I> inputType,
                                                                                   Function<List<I>, UFOperationBatchResult<R>> resultBatch) {
        List<I> uFilesOfType = unfilteredTargets.stream()
                .filter(inputType::isInstance)
                .map(inputType::cast)
                .collect(Collectors.toList());

        List<R> localResults = resultBatch.apply(uFilesOfType);

        HashMap<I, R> out = new HashMap<>();
        for (int i = 0; i < uFilesOfType.size(); i++) {
            out.put(uFilesOfType.get(i), localResults.get(i));
        }
        return out;
    }
*/
}
