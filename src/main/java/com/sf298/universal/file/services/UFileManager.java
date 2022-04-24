package com.sf298.universal.file.services;

import com.sf298.universal.file.model.responses.UFOperationBatchResult;
import com.sf298.universal.file.model.responses.UFOperationResult;
import com.sf298.universal.file.services.platforms.UFileDropbox;
import com.sf298.universal.file.services.platforms.UFileFtp;
import com.sf298.universal.file.services.platforms.UFileLocalDisk;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sf298.universal.file.services.platforms.UFileDropboxBatch.DROPBOX_BATCH;
import static com.sf298.universal.file.services.platforms.UFileFtpBatch.FTP_BATCH;
import static com.sf298.universal.file.services.platforms.UFileLocalDiskBatch.LOCAL_DISK_BATCH;

public class UFileManager {

    public static UFOperationBatchResult<Boolean> existsBatch(List<UFile> targets) {
        if (targets.isEmpty()) {
            return new UFOperationBatchResult<>();
        }
        Stream<UFOperationBatchResult<Boolean>> stream = Stream.of(
                LOCAL_DISK_BATCH.exists(filterByUFileType(targets, UFileLocalDisk.class)),
                FTP_BATCH       .exists(filterByUFileType(targets, UFileFtp.class)),
                DROPBOX_BATCH   .exists(filterByUFileType(targets, UFileDropbox.class))
        );
        return sortResultStream(targets, stream);
    }

    public static UFOperationBatchResult<Boolean> isDirectoryBatch(List<UFile> targets) {
        if (targets.isEmpty()) {
            return new UFOperationBatchResult<>();
        }
        Stream<UFOperationBatchResult<Boolean>> stream = Stream.of(
                LOCAL_DISK_BATCH.isDirectory(filterByUFileType(targets, UFileLocalDisk.class)),
                FTP_BATCH       .isDirectory(filterByUFileType(targets, UFileFtp.class)),
                DROPBOX_BATCH   .isDirectory(filterByUFileType(targets, UFileDropbox.class))
        );
        return sortResultStream(targets, stream);
    }

    public static UFOperationBatchResult<Boolean> isFileBatch(List<UFile> targets) {
        if (targets.isEmpty()) {
            return new UFOperationBatchResult<>();
        }
        Stream<UFOperationBatchResult<Boolean>> stream = Stream.of(
                LOCAL_DISK_BATCH.isFile(filterByUFileType(targets, UFileLocalDisk.class)),
                FTP_BATCH       .isFile(filterByUFileType(targets, UFileFtp.class)),
                DROPBOX_BATCH   .isFile(filterByUFileType(targets, UFileDropbox.class))
        );
        return sortResultStream(targets, stream);
    }

    public static UFOperationBatchResult<Boolean> mkdirBatch(List<UFile> targets) {
        if (targets.isEmpty()) {
            return new UFOperationBatchResult<>();
        }
        Stream<UFOperationBatchResult<Boolean>> stream = Stream.of(
                LOCAL_DISK_BATCH.mkdir(filterByUFileType(targets, UFileLocalDisk.class)),
                FTP_BATCH       .mkdir(filterByUFileType(targets, UFileFtp.class)),
                DROPBOX_BATCH   .mkdir(filterByUFileType(targets, UFileDropbox.class))
        );
        return sortResultStream(targets, stream);
    }

    public static UFOperationBatchResult<Boolean> mkdirsBatch(List<UFile> targets) {
        if (targets.isEmpty()) {
            return new UFOperationBatchResult<>();
        }
        Stream<UFOperationBatchResult<Boolean>> stream = Stream.of(
                LOCAL_DISK_BATCH.mkdirs(filterByUFileType(targets, UFileLocalDisk.class)),
                FTP_BATCH       .mkdirs(filterByUFileType(targets, UFileFtp.class)),
                DROPBOX_BATCH   .mkdirs(filterByUFileType(targets, UFileDropbox.class))
        );
        return sortResultStream(targets, stream);
    }

    private static <T extends UFile> List<T> filterByUFileType(List<UFile> targets, Class<T> clazz) {
        return targets.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    private static <T> UFOperationBatchResult<T> sortResultStream(List<UFile> targets, Stream<UFOperationBatchResult<T>> stream) {
        Map<UFile, UFOperationResult<T>> resultMap = stream.flatMap(Collection::stream)
            .collect(Collectors.toMap(UFOperationResult::getActionedFile, Function.identity()));

        return targets.stream()
                .map(resultMap::get)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

}
