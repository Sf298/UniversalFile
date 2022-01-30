package com.sf298.universal.file.services.impl;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.RelocationBatchV2Result;
import com.dropbox.core.v2.files.RelocationPath;
import com.sf298.universal.file.model.dto.BatchMove;
import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UFileDropboxBatch {

    public static UFOperationBatchResult<UFExistsResult> exists(List<UFileDropbox> targets) {
        return targets.stream()
                .map(UFile::exists)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFIsDirectoryResult> isDirectory(List<UFileDropbox> targets) {
        return targets.stream()
                .map(UFile::isDirectory)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFIsFileResult> isFile(List<UFileDropbox> targets) {
        return targets.stream()
                .map(UFile::isFile)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFMkdirResult> mkdir(List<UFileDropbox> targets) {
        return targets.stream()
                .map(UFile::mkdir)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public static UFOperationBatchResult<UFMkdirsResult> mkdirs(List<UFileDropbox> targets) {
        return targets.stream()
                .map(UFile::mkdirs)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    public void moveTo(List<BatchMove<UFileDropbox>> files) throws IOException {
        try {
            Map<String, List<BatchMove<UFileDropbox>>> grouped = groupByToken(files);
            for (Map.Entry<String, List<BatchMove<UFileDropbox>>> entry : grouped.entrySet()) {
                DbxClientV2 client = entry.getValue().get(0).from.getClient();

                List<RelocationPath> list = entry.getValue().stream()
                        .map(uf -> new RelocationPath(uf.from.getPath(), uf.to.getPath()))
                        .collect(Collectors.toList());
                RelocationBatchV2Result results = client.files().moveBatchV2(list).getCompleteValue();
            }
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    private Map<String, List<BatchMove<UFileDropbox>>> groupByToken(List<BatchMove<UFileDropbox>> targets) {
        return targets.stream().collect(Collectors.groupingBy(uf -> uf.from.getAccessToken()));
    }

}
