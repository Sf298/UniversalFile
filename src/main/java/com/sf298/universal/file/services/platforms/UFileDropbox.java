package com.sf298.universal.file.services.platforms;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.sf298.universal.file.model.connection.ConnectionDetails;
import com.sf298.universal.file.model.functions.ExceptionNet;
import com.sf298.universal.file.model.inputs.BatchMove;
import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.dropbox.core.v2.files.WriteMode.OVERWRITE;
import static com.sf298.universal.file.model.connection.ConnectionParam.TOKEN;
import static com.sf298.universal.file.services.platforms.UFileDropboxBatch.DROPBOX_BATCH;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class UFileDropbox extends UFile {

    /**
     * The cache of clients {@link DbxClientV2}. Reduces overhead of new connections.
     */
    private static final Map<DbxCredential, DbxClientV2> clients = new ConcurrentHashMap<>();

    private final DbxCredential credential;
    private DbxDownloader<FileMetadata> readDownloader;
    private DbxDownloader<FileMetadata> readThumbnailDownloader;
    private UploadUploader writeUploader;

    public UFileDropbox(DbxCredential credential, String path) {
        super(path = path.toLowerCase());
        if (!getPath().startsWith("/")) {
            throw new IllegalArgumentException("Error: '"+ path +"' doesn't have a valid beginning. Should start with '/'.");
        }

        this.credential = credential;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("photo-trove local").build();
        try {
            credential.refresh(config);
        } catch (DbxException e) {
            throw new RuntimeException(e);
        }
    }
    public UFileDropbox(String dropboxAppKey, String dropboxAppSecret, String refreshToken, String path) {
        super(path = path.toLowerCase());
        if (!getPath().startsWith("/")) {
            throw new IllegalArgumentException("Error: '"+ path +"' doesn't have a valid beginning. Should start with '/'.");
        }

        credential = new DbxCredential("", 0L, refreshToken, dropboxAppKey, dropboxAppSecret);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("photo-trove local").build();
        try {
            credential.refresh(config);
        } catch (DbxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new {@link UFile} located at "/".
     * @param login The {@link ConnectionDetails} configuration to connect to the FTP server.<br>
     *              Required: HOST, USERNAME, PASSWORD<br>
     *              Optional: PORT
     */
    public UFileDropbox(ConnectionDetails login) {
        this(login, "/");
    }

    /**
     * Creates a new {@link UFile} located at the given path.
     * @param login The {@link ConnectionDetails} configuration to connect to the FTP server.<br>
     *              Required: HOST, USERNAME, PASSWORD<br>
     *              Optional: PORT
     * @param path The path of the {@link UFile} object to create. May not exist on the remote server.
     */
    public UFileDropbox(ConnectionDetails login, String path) {
        this(new DbxCredential(login.get(TOKEN)), path);
    }

    /**
     * Creates a new {@link UFile} located at the given path.
     * @param uFile The {@link UFile} to copy the {@link ConnectionDetails} configuration from.
     * @param path The path of the {@link UFile} object to create. May not exist on the remote server.
     */
    private UFileDropbox(UFileDropbox uFile, String path) {
        this(uFile.credential, path);
    }
    private UFileDropbox(DbxCredential credential, Metadata metadata) {
        super(metadata.getPathLower());
        this.credential = credential;
        metadataCache = metadataToUFMetadata(metadata);
    }

    public String getAccessToken() {
        return credential.getAccessToken();
    }

    public DbxClientV2 getClient() {
        DbxClientV2 client = clients.get(credential);
        if (isNull(client)) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("UFile/testing").build();
            client = new DbxClientV2(config, credential);
            clients.put(credential, client);
        }
        return client;
    }

    @Override
    public String getFileSep() {
        return "/";
    }


    @Override
    public UFile getParentUFile() {
        String parentStr = getParent();
        return isNull(parentStr) ? null : new UFileDropbox(this, parentStr);
    }


    @Override
    public UFOperationResult<Boolean> exists() {
        if (getDropboxPath().equals(getFileSep()))
            return UFOperationResult.createBoolOperation(this, true);

        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            return nonNull(metadataCache);
        });
    }

    @Override
    public UFOperationResult<Boolean> isDirectory() {
        if (getDropboxPath().equals(getFileSep()))
            return UFOperationResult.createBoolOperation(this, true);

        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            return nonNull(metadataCache) && metadataCache.isFolder();
        });
    }

    @Override
    public UFOperationResult<Boolean> isFile() {
        if (getDropboxPath().equals(getFileSep()))
            return UFOperationResult.createBoolOperation(this, false);

        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            return nonNull(metadataCache) && metadataCache.isFile();
        });
    }

    @Override
    public UFOperationResult<Date> lastModified() {
        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            if (nonNull(metadataCache)) {
                return metadataCache.getLastModified();
            }
            return null;
        });
    }

    @Override
    public UFOperationResult<Long> length() {
        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            if (nonNull(metadataCache)) {
                return metadataCache.getLength();
            }
            return -1L;
        });
    }


    @Override
    public UFOperationResult<Boolean> delete() {
        return DROPBOX_BATCH.delete(List.of(this)).get(0);
    }

    @Override
    public UFOperationResult<Boolean> deleteRecursive() {
        return DROPBOX_BATCH.deleteRecursive(List.of(this)).get(0);
    }

    @Override
    public UFOperationResult<String[]> list() {
        return new UFOperationResult<>(this, () -> {
            ListFolderResult returned = callApi(5, () -> getClient().files().listFolder(getDropboxPath()));
            return returned.getEntries().stream().map(Metadata::getName).toArray(String[]::new);
        });
    }

    @Override
    public UFOperationResult<UFile[]> listFiles() {
        return new UFOperationResult<>(this, () -> {
            List<ListFolderResult> folderResults = new ArrayList<>();
            folderResults.add(callApi(5, () -> getClient().files().listFolder(getDropboxPath())));
            while (folderResults.get(folderResults.size()-1).getHasMore()) {
                ListFolderResult lastResult = folderResults.get(folderResults.size()-1);
                folderResults.add(callApi(5, () -> getClient().files().listFolderContinue(lastResult.getCursor())));
            }
            return folderResults.stream()
                    .flatMap(r -> r.getEntries().stream())
                    .map(m -> new UFileDropbox(credential, m))
                    .toArray(UFile[]::new);
        });
    }

    @Override
    public UFOperationResult<Boolean> listFilesRecursiveBatch(Consumer<UFile[]> resultCallback) {
        UFOperationResult<ListFolderResult> r = new UFOperationResult<>(this, () -> callApi(5, () -> {
            ListFolderBuilder listFolderBuilder = getClient().files().listFolderBuilder(getDropboxPath());
            return listFolderBuilder.withRecursive(true).start();
        }));
        if (!r.isSuccessful()) {
            return new UFOperationResult<>(this, r.getException());
        }
        resultCallback.accept(r.getResult().getEntries().stream()
                .map(m -> new UFileDropbox(credential, m))
                .toArray(UFile[]::new));


        while (r.getResult().getHasMore()) {
            final ListFolderResult returnedFinal = r.getResult();
            r = new UFOperationResult<>(this, () -> callApi(5, () ->
                getClient().files().listFolderContinue(returnedFinal.getCursor())
            ));
            if (!r.isSuccessful()) {
                return new UFOperationResult<>(this, r.getException());
            }
            resultCallback.accept(r.getResult().getEntries().stream()
                    .map(m -> new UFileDropbox(credential, m))
                    .toArray(UFile[]::new));
        }
        return UFOperationResult.createBoolOperation(this, true);
    }

    @Override
    public UFOperationResult<Boolean> mkdir() {
        if (getDropboxPath().equals(getFileSep()) || exists().getResult() || !getParentUFile().exists().getResult()) {
            return UFOperationResult.createBoolOperation(this, false);
        }

        return new UFOperationResult<>(this, () -> {
            callApi(5, () -> getClient().files().createFolderV2(getDropboxPath()));
            return true;
        });
    }

    @Override
    public UFOperationResult<Boolean> mkdirs() {
        if (getPath().equals(getFileSep()) || exists().getResult()) {
            return UFOperationResult.createBoolOperation(this, false);
        }

        return new UFOperationResult<>(this, () -> {
            CreateFolderResult result = callApi(5, () -> getClient().files().createFolderV2(getDropboxPath()));
            return nonNull(result) && nonNull(result.getMetadata());
        });
    }

    @Override
    public UFOperationResult<Boolean> setLastModified(Date time) {
        return UFOperationResult.createBoolOperation(this, false);
    }


    @Override
    public InputStream read() throws IOException {
        try {
            readDownloader = getClient().files().download(getDropboxPath());
            return readDownloader.getInputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void readClose() {
        if (isNull(readDownloader)) return;
        readDownloader.close();
        readDownloader = null;
    }

    public InputStream readThumbnail() throws IOException {
        try {
            readThumbnailDownloader = getClient().files().getThumbnailBuilder(getDropboxPath())
                    .withSize(ThumbnailSize.W256H256)
                    .withMode(ThumbnailMode.FITONE_BESTFIT)
                    .withFormat(ThumbnailFormat.JPEG)
                    .start();
            return readThumbnailDownloader.getInputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    public void readThumbnailClose() {
        if (isNull(readThumbnailDownloader)) return;
        readThumbnailDownloader.close();
        readThumbnailDownloader = null;
    }

    @Override
    public OutputStream write() throws IOException {
        try {
            writeUploader = getClient().files().uploadBuilder(getDropboxPath()).withMode(OVERWRITE).start();
            return writeUploader.getOutputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeClose() {
        if (isNull(writeUploader)) return;

        try {
            writeUploader.getOutputStream().flush();
            writeUploader.finish();
            writeUploader.close();
            writeUploader = null;
        } catch (DbxException | IOException e) {
            writeUploader = null;
            throw new RuntimeException(e);
        }
    }
/*
    @Override
    public OutputStream append() throws IOException {
        try {
            appendUploader = getClient().files().uploadBuilder(getPath()).withMode(ADD).start();
            return appendUploader.getOutputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void appendClose() {
        if (isNull(appendUploader)) return;

        try {
            appendUploader.finish();
            appendUploader.close();
            appendUploader = null;
        } catch (DbxException e) {
            appendUploader = null;
            throw new RuntimeException(e);
        }
    }
*/
    @Override
    public void close() {
        readClose();
        writeClose();
        appendClose();
    }

    @Override
    public UFOperationResult<Boolean> moveTo(UFile destination) {
        if (destination instanceof UFileDropbox) {
            return DROPBOX_BATCH.moveTo(List.of(new BatchMove(this, destination))).get(0);
        } else {
            return super.moveTo(destination);
        }
    }

    @Override
    public UFOperationResult<Boolean> copyTo(UFile destination) {
        if (destination instanceof UFileDropbox) {
            return DROPBOX_BATCH.copyTo(List.of(new BatchMove(this, destination))).get(0);
        } else {
            return super.moveTo(destination);
        }
    }


    @Override
    public UFile stepInto(String path) {
        return new UFileDropbox(this, join(getFileSep(), getPath(), path));
    }

    @Override
    public UFile goTo(String path) {
        return new UFileDropbox(this, path);
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UFileDropbox uFile)) return false;
        return getPath().equals(uFile.getPath()) && credential.equals(uFile.credential);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath(), credential);
    }


    private void populateMetadataCache() throws DbxException {
        if (isNull(metadataCache)) {
            try {
                Metadata metadata = callApi(0, () -> getClient().files().getMetadata(getDropboxPath()));
                metadataCache = metadataToUFMetadata(metadata);
            } catch (GetMetadataErrorException ignored) {
                // file doesnt exist, do nothing
            } catch (DbxException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    private UFMetadata metadataToUFMetadata(Metadata metadata) {
        if (isNull(metadata)) {
            return UFMetadata.NOT_EXIST;
        }

        return new UFMetadata(
                true,
                metadata instanceof FileMetadata ? ((FileMetadata) metadata).getSize() : null,
                null,
                metadata instanceof FileMetadata ? ((FileMetadata) metadata).getClientModified() : null,
                metadata instanceof FileMetadata,
                metadata instanceof FolderMetadata
        );
    }

    private <T> T callApi(int retryCount, ExceptionNet<T, DbxException> function) throws DbxException {
        int i = 0;
        while (true) {
            try {
                return function.run();
            } catch (DbxException ex) {
                long sleepTime = (ex instanceof RateLimitException) ? ((RateLimitException)ex).getBackoffMillis()+10 : ++i*1000L;

                if (i > retryCount) throw ex;

                if (!(ex instanceof RateLimitException)) {
                    ex.printStackTrace();
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {}
            }
        }


    }
    private String getDropboxPath() {
        return getPath().equals("/") ? "" : getPath();
    }

}
