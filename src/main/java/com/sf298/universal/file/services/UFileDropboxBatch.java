package com.sf298.universal.file.services;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.*;
import com.sf298.universal.file.model.functions.ExceptionNet;
import com.sf298.universal.file.model.inputs.BatchMove;
import com.sf298.universal.file.model.responses.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.sf298.universal.file.model.responses.UFOperationResult.createBoolOperation;
import static com.sf298.universal.file.utils.ListUtils.zipToPairs;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.groupingBy;

class UFileDropboxBatch extends UFileBatch<UFileDropbox> {

    public static UFileDropboxBatch DROPBOX_BATCH = new UFileDropboxBatch();

    private UFileDropboxBatch() {}

    @Override
    public UFOperationBatchResult<Boolean> delete(List<UFileDropbox> targets) {
        Map<UFileDropbox, UFOperationResult<Boolean>> generated = new HashMap<>();

        // split targets into 'non-empty folders' and 'files or empty folders'
        Map<Boolean, List<UFileDropbox>> grouped = targets.stream().collect(
                groupingBy(uf -> uf.isDirectory().getResultOrDefault(false) &&
                        (uf.listFiles().getResultOrDefault(new UFile[0]).length > 0)));

        // set results for all non-empty folders to false
        grouped.getOrDefault(true, emptyList()).forEach(uf -> generated.put(uf, createBoolOperation(uf, false)));

        // process files and empty folders
        deleteRecursive(grouped.getOrDefault(false, emptyList())).forEach(r -> generated.put((UFileDropbox) r.getActionedFile(), r));

        return order(targets, generated);
    }

    @Override
    public UFOperationBatchResult<Boolean> deleteRecursive(List<UFileDropbox> targets) {
        System.out.println("Delete: " + targets);

        Map<String, List<UFileDropbox>> groupedByToken = targets.stream()
                .collect(groupingBy(UFileDropbox::getAccessToken));

        Map<UFileDropbox, UFOperationResult<Boolean>> generated = new HashMap<>();
        groupedByToken.values().forEach(files -> {
            List<DeleteArg> toDelete = files.stream()
                    .map(UFile::getPath)
                    .map(DeleteArg::new)
                    .collect(toList());
            DbxUserFilesRequests filesService = files.get(0).getClient().files();
            try {
                // start and complete job
                String jobId = filesService.deleteBatch(toDelete).getAsyncJobIdValue();
                DeleteBatchJobStatus status = waitForDeleteJobToComplete(() -> filesService.deleteBatchCheck(jobId));

                // process results
                List<DeleteBatchResultEntry> resultSet = status.getCompleteValue().getEntries();
                zipToPairs(files, resultSet, (uf, res) -> generated.put(uf, createBoolOperation(uf, res.isSuccess())));
            } catch (DbxException e) {
                // set result as 'error' for all BatchMoves in this group
                files.forEach(uf -> generated.put(uf, new UFOperationResult<>(uf, e)));
            }
        });

        return order(targets, generated);
    }

    @Override
    public UFOperationBatchResult<Boolean> moveTo(List<BatchMove> transfers) {
        // split into transfers for same access token vs transfers across accounts
        Map<Integer, List<BatchMove>> groupedTransfers = groupBySameTokens(transfers);
        List<BatchMove> crossTokenTransfers = groupedTransfers.getOrDefault(0, emptyList());
        List<BatchMove> sameTokenTransfers = groupedTransfers.getOrDefault(1, emptyList());

        // process transfers same token with same token
        Map<String, List<BatchMove>> groupedByToken = sameTokenTransfers.stream()
                .collect(groupingBy(t -> ((UFileDropbox) t.from).getAccessToken()));

        Map<BatchMove, UFOperationResult<Boolean>> generated = new HashMap<>();
        groupedByToken.values().forEach(bms -> {
            List<RelocationPath> relocationPaths = batchMovesToRelocationPaths(bms);
            DbxUserFilesRequests files = batchMovesToClientFiles(bms);
            try {
                // start and complete job
                String jobId = files.moveBatchV2(relocationPaths).getAsyncJobIdValue();
                RelocationBatchV2JobStatus status = waitForRelocateJobToComplete(() -> files.moveBatchCheckV2(jobId));

                // process results
                List<RelocationBatchResultEntry> resultSet = status.getCompleteValue().getEntries();
                zipToPairs(bms, resultSet, (bm, res) -> generated.put(bm, createBoolOperation(bm.from, res.isSuccess())));
            } catch (DbxException e) {
                // set result as 'error' for all BatchMoves in this group
                bms.forEach(bm -> generated.put(bm, new UFOperationResult<>(bm.from, e)));
            }
        });

        // use superclass to transfer across tokens
        UFOperationBatchResult<Boolean> crossTokenResults = super.moveTo(crossTokenTransfers);
        zipToPairs(crossTokenTransfers, crossTokenResults, generated::put);

        return order(transfers, generated);
    }

    @Override
    public UFOperationBatchResult<Boolean> copyTo(List<BatchMove> transfers) {
        // split into transfers for same access token vs transfers across accounts
        Map<Integer, List<BatchMove>> groupedTransfers = groupBySameTokens(transfers);
        List<BatchMove> sameTokenTransfers = groupedTransfers.getOrDefault(1, emptyList());
        List<BatchMove> crossTokenTransfers = groupedTransfers.getOrDefault(0, emptyList());

        // process same token transfers
        Map<String, List<BatchMove>> groupedByToken = sameTokenTransfers.stream()
                .collect(groupingBy(t -> ((UFileDropbox) t.from).getAccessToken()));

        Map<BatchMove, UFOperationResult<Boolean>> generated = new HashMap<>();
        groupedByToken.values().forEach(bms -> {
            List<RelocationPath> relocationPaths = batchMovesToRelocationPaths(bms);
            DbxUserFilesRequests files = batchMovesToClientFiles(bms);
            try {
                // start and complete job
                String jobId = files.copyBatchV2(relocationPaths).getAsyncJobIdValue();
                RelocationBatchV2JobStatus status = waitForRelocateJobToComplete(() -> files.copyBatchCheckV2(jobId));

                // process results
                List<RelocationBatchResultEntry> resultSet = status.getCompleteValue().getEntries();
                zipToPairs(bms, resultSet, (bm, res) -> generated.put(bm, createBoolOperation(bm.from, res.isSuccess())));
            } catch (DbxException e) {
                // set result as 'error' for all BatchMoves in this group
                bms.forEach(bm -> generated.put(bm, new UFOperationResult<>(bm.from, e)));
            }
        });

        // use superclass to transfer across tokens
        UFOperationBatchResult<Boolean> crossTokenResults = super.copyTo(crossTokenTransfers);
        zipToPairs(crossTokenTransfers, crossTokenResults, generated::put);

        return order(transfers, generated);
    }

    private Map<Integer, List<BatchMove>> groupBySameTokens(List<BatchMove> transfers) {
        return transfers.stream()
                .filter(t -> t.from instanceof UFileDropbox && t.to instanceof UFileDropbox)
                .collect(groupingBy(t -> {
                    UFileDropbox from = (UFileDropbox) t.from;
                    UFileDropbox to = (UFileDropbox) t.to;
                    return from.getAccessToken().equals(to.getAccessToken()) ? 1 : 0;
                }));
    }
    private DbxUserFilesRequests batchMovesToClientFiles(List<BatchMove> batchMoves) {
        return ((UFileDropbox)batchMoves.get(0).from).getClient().files();
    }
    private List<RelocationPath> batchMovesToRelocationPaths(Collection<BatchMove> batchMoves) {
        return batchMoves.stream()
                .map(bm -> new RelocationPath(bm.from.getPath(), bm.to.getPath()))
                .collect(toList());
    }
    private RelocationBatchV2JobStatus waitForRelocateJobToComplete(ExceptionNet<RelocationBatchV2JobStatus, DbxException> isComplete) throws DbxException {
        while (true) {
            RelocationBatchV2JobStatus status = isComplete.run();
            if (status.isComplete()) return status;
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {}
        }
    }
    private DeleteBatchJobStatus waitForDeleteJobToComplete(ExceptionNet<DeleteBatchJobStatus, DbxException> isComplete) throws DbxException {
        while (true) {
            DeleteBatchJobStatus status = isComplete.run();
            if (status.isComplete()) return status;
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {}
        }
    }

    private <I,R> UFOperationBatchResult<R> order(List<I> ordering, Map<I, UFOperationResult<R>> map) {
        return ordering.stream()
                .map(map::get)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

}
