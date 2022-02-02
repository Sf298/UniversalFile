package com.sf298.universal.file.services.dropbox;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteArg;
import com.dropbox.core.v2.files.RelocationPath;
import com.sf298.universal.file.model.functions.ThrowableFunction;
import com.sf298.universal.file.model.inputs.BatchMove;
import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFileBatchDefault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class UFileDropboxBatch extends UFileBatchDefault<UFileDropbox> {

    public static UFileDropboxBatch DROPBOX_BATCH = new UFileDropboxBatch();

    private UFileDropboxBatch() {}

    @Override
    public UFOperationBatchResult<UFOperationResult<Boolean>> delete(List<UFileDropbox> targets) {
        return deleteRecursive(targets);
    }

    @Override
    public UFOperationBatchResult<UFOperationResult<Boolean>> deleteRecursive(List<UFileDropbox> targets) {
        System.out.println("Delete: " + targets);
        Map<UFileDropbox, UFOperationResult<Boolean>> generated = processGroupsByToken(targets, identity(), files -> {
            DbxClientV2 client = files.get(0).getClient();
            List<DeleteArg> toDelete = files.stream().map(uf -> new DeleteArg(uf.getPath())).collect(toList());
            client.files().deleteBatch(toDelete);
            return files.stream().collect(toMap(identity(), uf -> true));
        });

        return order(targets, generated);
    }

    @Override
    public UFOperationBatchResult<UFOperationResult<Boolean>> moveTo(List<BatchMove> transfers) {
        List<BatchMove> sameTokenTransfers = new ArrayList<>();
        List<BatchMove> crossTokenTransfers = new ArrayList<>();
        for (BatchMove transfer : transfers) {
            if (transfer.from instanceof UFileDropbox && transfer.to instanceof UFileDropbox) {
                UFileDropbox from = (UFileDropbox) transfer.from;
                UFileDropbox to = (UFileDropbox) transfer.to;
                if (from.getAccessToken().equals(to.getAccessToken())) {
                    sameTokenTransfers.add(transfer);
                    continue;
                }
            }
            crossTokenTransfers.add(transfer);
        }

        Map<BatchMove, UFOperationResult<Boolean>> generated = processGroupsByToken(sameTokenTransfers, i -> (UFileDropbox)i.from, files -> {
            DbxClientV2 client = ((UFileDropbox)files.get(0).from).getClient();
            List<RelocationPath> toMove = files.stream()
                    .map(bm -> new RelocationPath(bm.from.getPath(), bm.to.getPath()))
                    .collect(toList());
            client.files().moveBatchV2(toMove);
            return files.stream().collect(toMap(identity(), uf -> true));
        });

        UFOperationBatchResult<UFOperationResult<Boolean>> crossTokenResults = super.moveTo(crossTokenTransfers);
        for (int i = 0; i < crossTokenTransfers.size(); i++) {
            generated.put(crossTokenTransfers.get(i), crossTokenResults.get(i));
        }

        return order(transfers, generated);
    }

    @Override
    public UFOperationBatchResult<UFOperationResult<Boolean>> copyTo(List<BatchMove> transfers) {
        List<BatchMove> sameTokenTransfers = new ArrayList<>();
        List<BatchMove> crossTokenTransfers = new ArrayList<>();
        for (BatchMove transfer : transfers) {
            if (transfer.from instanceof UFileDropbox && transfer.to instanceof UFileDropbox) {
                UFileDropbox from = (UFileDropbox) transfer.from;
                UFileDropbox to = (UFileDropbox) transfer.to;
                if (from.getAccessToken().equals(to.getAccessToken())) {
                    sameTokenTransfers.add(transfer);
                    continue;
                }
            }
            crossTokenTransfers.add(transfer);
        }

        Map<BatchMove, UFOperationResult<Boolean>> generated = processGroupsByToken(sameTokenTransfers, i -> (UFileDropbox)i.from, files -> {
            DbxClientV2 client = ((UFileDropbox)files.get(0).from).getClient();
            List<RelocationPath> toMove = files.stream()
                    .map(bm -> new RelocationPath(bm.from.getPath(), bm.to.getPath()))
                    .collect(toList());
            client.files().copyBatchV2(toMove);
            return files.stream().collect(toMap(identity(), uf -> true));
        });

        UFOperationBatchResult<UFOperationResult<Boolean>> crossTokenResults = super.moveTo(crossTokenTransfers);
        for (int i = 0; i < crossTokenTransfers.size(); i++) {
            generated.put(crossTokenTransfers.get(i), crossTokenResults.get(i));
        }

        return order(transfers, generated);
    }

    private <I,R> Map<I, UFOperationResult<R>> processGroupsByToken(List<I> targets,
                                                                    Function<I, UFileDropbox> toUFile,
                                                                    ThrowableFunction<List<I>, Map<I, R>> function) {
        Map<String, List<I>> grouped = targets.stream().collect(groupingBy(i -> toUFile.apply(i).getAccessToken()));
        Map<I, UFOperationResult<R>> results = new HashMap<>();

        for (List<I> files : grouped.values()) {
            try {
                Map<I, R> resultsMap = function.apply(files);
                resultsMap.entrySet().forEach(e -> results.put(
                        e.getKey(),
                        new UFOperationResult<>(toUFile.apply(e.getKey()), e::getValue)
                ));
            } catch (Exception e) {
                files.forEach(i -> results.put(i, new UFOperationResult<>(toUFile.apply(i), e)));
            }
        }

        return results;
    }

    private <I,R> UFOperationBatchResult<UFOperationResult<R>> order(List<I> ordering, Map<I, UFOperationResult<R>> map) {
        return ordering.stream()
                .map(map::get)
                .collect(Collectors.toCollection(UFOperationBatchResult::new));
    }

    private Map<String, List<UFileDropbox>> groupByToken(List<UFileDropbox> targets) {
        return targets.stream().collect(groupingBy(UFileDropbox::getAccessToken));
    }
}
