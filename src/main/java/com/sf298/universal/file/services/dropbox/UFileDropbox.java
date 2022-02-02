package com.sf298.universal.file.services.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.sf298.universal.file.model.connection.ConnectionDetails;
import com.sf298.universal.file.model.inputs.BatchMove;
import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.dropbox.core.v2.files.WriteMode.ADD;
import static com.dropbox.core.v2.files.WriteMode.OVERWRITE;
import static com.sf298.universal.file.model.connection.ConnectionParam.*;
import static com.sf298.universal.file.services.dropbox.UFileDropboxBatch.DROPBOX_BATCH;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class UFileDropbox extends UFile {

//    /**
//     * The time format used by {@link FTPClient}.
//     */
//    private static final SimpleDateFormat timeValFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * The cache of clients {@link DbxClientV2}. Reduces overhead of new connections.
     */
    private static final Map<String, DbxClientV2> clients = new ConcurrentHashMap<>();

    private final String accessToken;
    private final String path;
    private Metadata metadataCache;

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
        this.path = (!path.equals(getFileSep()) && path.endsWith(getFileSep())) ? path.substring(0, path.length() - 1) : path;
        if (!this.path.matches("([A-Z]:[\\\\/]|/).*")) {
            throw new IllegalArgumentException("Error: '"+ path +"' doesn't have a valid beginning. Should be like 'C:\\' or '/'.");
        }

        accessToken = login.get(TOKEN);
    }

    /**
     * Creates a new {@link UFile} located at the given path.
     * @param uFile The {@link UFile} to copy the {@link ConnectionDetails} configuration from.
     * @param path The path of the {@link UFile} object to create. May not exist on the remote server.
     */
    private UFileDropbox(UFileDropbox uFile, String path) {
        this(new ConnectionDetails(Map.of(TOKEN, uFile.accessToken)), path);
    }
    private UFileDropbox(String accessToken, Metadata metadata) {
        this.accessToken = accessToken;
        this.metadataCache = metadata;
        this.path = metadata.getPathLower();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public DbxClientV2 getClient() {
        DbxClientV2 client = clients.get(accessToken);
        if (isNull(client)) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("UFile/testing").build();
            client = new DbxClientV2(config, accessToken);
            clients.put(accessToken, client);
        }
        return client;
    }

    @Override
    public String getFileSep() {
        return "/";
    }


    @Override
    public String getName() {
        return path.substring(path.lastIndexOf(getFileSep())+1);
    }

    @Override
    public String getParent() {
        return parent(path, getFileSep());
    }

    @Override
    public UFile getParentUFile() {
        String parentStr = getParent();
        return isNull(parentStr) ? null : new UFileDropbox(this, parentStr);
    }

    @Override
    public String getPath() {
        return path;
    }


    @Override
    public UFOperationResult<Boolean> exists() {
        if (path.equals(getFileSep()))
            return UFOperationResult.createBool(this, true);

        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            return nonNull(metadataCache);
        });
    }

    @Override
    public UFOperationResult<Boolean> isDirectory() {
        if (path.equals(getFileSep()))
            return UFOperationResult.createBool(this, true);

        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            return nonNull(metadataCache) && metadataCache instanceof FolderMetadata;
        });
    }

    @Override
    public UFOperationResult<Boolean> isFile() {
        if (path.equals(getFileSep()))
            return UFOperationResult.createBool(this, false);

        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            return nonNull(metadataCache) && metadataCache instanceof FileMetadata;
        });
    }

    @Override
    public UFOperationResult<Date> lastModified() {
        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            if (nonNull(metadataCache) && metadataCache instanceof FileMetadata) {
                return ((FileMetadata) metadataCache).getClientModified();
            }
            return null;
        });
    }

    @Override
    public UFOperationResult<Long> length() {
        return new UFOperationResult<>(this, () -> {
            populateMetadataCache();
            if (metadataCache instanceof FileMetadata) {
                return ((FileMetadata) metadataCache).getSize();
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
            ListFolderResult returned = getClient().files().listFolder(path.substring(1));
            return returned.getEntries().stream().map(Metadata::getPathLower).toArray(String[]::new);
        });
    }

    @Override
    public UFOperationResult<UFile[]> listFiles() {
        return new UFOperationResult<>(this, () -> {
            ListFolderResult returned = getClient().files().listFolder(path.substring(1));
            return returned.getEntries().stream().map(m -> new UFileDropbox(accessToken, m)).toArray(UFile[]::new);
        });
    }

    @Override
    public UFOperationResult<Boolean> mkdir() {
        if (path.equals(getFileSep()) || exists().getResult()) {
            return UFOperationResult.createBool(this, false);
        }

        return new UFOperationResult<>(this, () -> {
            getClient().files().createFolderV2(path);
            return true;
        });
    }

    @Override
    public UFOperationResult<Boolean> mkdirs() {
        if (path.equals(getFileSep()) || exists().getResult()) {
            return UFOperationResult.createBool(this, false);
        }

        return new UFOperationResult<>(this, () -> {
            int i = 0;
            while (true) {
                try {
                    CreateFolderResult result = getClient().files().createFolderV2(path);
                    return nonNull(result) && nonNull(result.getMetadata());
                } catch (RateLimitException ex) {
                    if (i >= 5) throw ex;
                    Thread.sleep(++i * 1000L);
                }
            }
        });
    }

    @Override
    public UFOperationResult<Boolean> setLastModified(Date time) {
        return UFOperationResult.createBool(this, false);
    }


    @Override
    public InputStream read() throws IOException {
        try {
            return getClient().files().download(path).getInputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void readClose() {}

    @Override
    public OutputStream write() throws IOException {
        try {
            return getClient().files().uploadBuilder(path).withMode(OVERWRITE).start().getOutputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeClose() {}

    @Override
    public OutputStream append() throws IOException {
        try {
            return getClient().files().uploadBuilder(path).withMode(ADD).start().getOutputStream();
        } catch (DbxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void appendClose() {}

    @Override
    public void close() {}

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
    public UFile goTo(String path) {
        return new UFileDropbox(this, join(this.path, path, getFileSep()));
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UFileDropbox)) return false;
        UFileDropbox uFile = (UFileDropbox) o;
        return path.equals(uFile.path) && accessToken.equals(uFile.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, accessToken);
    }


    @Override
    public void clearCache() {
        metadataCache = null;
    }

    private void populateMetadataCache() throws DbxException {
        if (isNull(metadataCache)) {
            try {
                metadataCache = getClient().files().getMetadata(path);
            } catch (GetMetadataErrorException ignored) {
                // file doesnt exist, do nothing
            } catch (DbxException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

}
