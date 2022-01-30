package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.model.ConnectionDetails;
import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.sf298.universal.file.model.ConnectionParam.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.net.ftp.FTPReply.*;

public class UFileFtp extends UFile {

    /**
     * The time format used by {@link FTPClient}.
     */
    private static final SimpleDateFormat timeValFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * The cache of clients {@link FTPClient}. Reduces overhead of new connections.
     */
    private static final Map<Pair<ConnectionDetails, String>, FTPClient> ftpConnections = new ConcurrentHashMap<>();

    private final String path;
    private final ConnectionDetails login;

    /**
     * Creates a new {@link UFile} located at "/".
     * @param login The {@link ConnectionDetails} configuration to connect to the FTP server.<br>
     *              Required: HOST, USERNAME, PASSWORD<br>
     *              Optional: PORT
     */
    public UFileFtp(ConnectionDetails login) {
        this(login, "/");
    }

    /**
     * Creates a new {@link UFile} located at the given path.
     * @param login The {@link ConnectionDetails} configuration to connect to the FTP server.<br>
     *              Required: HOST, USERNAME, PASSWORD<br>
     *              Optional: PORT
     * @param path The path of the {@link UFile} object to create. May not exist on the remote server.
     */
    public UFileFtp(ConnectionDetails login, String path) {
        this.path = (!path.equals(getFileSep()) && path.endsWith(getFileSep())) ? path.substring(0, path.length() - 1) : path;
        if (!this.path.matches("([A-Z]:[\\\\/]|/).*")) {
            throw new IllegalArgumentException("Error: '"+ path +"' doesn't have a valid beginning. Should be like 'C:\\' or '/'.");
        }
        this.login = login;
    }

    /**
     * Creates a new {@link UFile} located at the given path.
     * @param uFile The {@link UFile} to copy the {@link ConnectionDetails} configuration from.
     * @param path The path of the {@link UFile} object to create. May not exist on the remote server.
     */
    private UFileFtp(UFileFtp uFile, String path) {
        this(uFile.login, path);
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
        return isNull(parentStr) ? null : new UFileFtp(login, parentStr);
    }

    @Override
    public String getPath() {
        return path;
    }


    @Override
    public UFOperationResult<Boolean> exists() {
        return new UFOperationResult<>(this, () -> path.equals(getFileSep()) || nonNull(asFTPFile()));
    }

    @Override
    public UFOperationResult<Boolean> isDirectory() {
        return new UFOperationResult<>(this, () -> {
            FTPFile sftpFile = asFTPFile();
            return nonNull(sftpFile) && sftpFile.isDirectory();
        });
    }

    @Override
    public UFOperationResult<Boolean> isFile() {
        return new UFOperationResult<>(this, () -> {
            FTPFile sftpFile = asFTPFile();
            return nonNull(sftpFile) && sftpFile.isFile();
        });
    }

    @Override
    public Date lastModified() {
        try {
            return timeValFormat.parse(getClient().getModificationTime(path));
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long length() {
        /*FTPFile sftpFile = asFTPFile();
        if(isNull(sftpFile)) {
            throw new RuntimeException("Unable to find file: " + path);
        }
        return sftpFile.getSize();*/
        return -1;
    }


    @Override
    public boolean createNewFile() {
        if (exists().isSuccessful()) {
            return false;
        }

        try {
            getParentUFile().mkdirs();
            OutputStream stream = write();
            stream.close();
            writeClose();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean delete(boolean recursive) {/*
        try {
            if (isDirectory()) {
                if (recursive) {
                    Arrays.stream(listFiles()).forEach(uf -> uf.delete(true));
                }
                return getClient().removeDirectory(path);
            } else {
                return getClient().deleteFile(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        return false;
    }

    @Override
    public String[] list() {
        try {
            return Arrays.stream(getClient().listNames(path))
                    .map(f -> {
                        int index = f.lastIndexOf("/");
                        return index==-1 ? f : f.substring(index+1);
                    })
                    .filter(p -> !p.equals(".") && !p.equals(".."))
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UFile[] listFiles() {
        try {
            return Arrays.stream(getClient().listFiles(path))
                    .map(this::toUFile)
                    .filter(uf -> !uf.getPath().endsWith(".") && !uf.getPath().endsWith(".."))
                    .toArray(UFile[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UFOperationResult<Boolean> mkdir() {
        return new UFOperationResult<>(this, () -> {
            if (!getParentUFile().exists().isSuccessful()) {
                return false;
            }
            return getClient().makeDirectory(path);
        });
    }

    @Override
    public UFOperationResult<Boolean> mkdirs() {
        return new UFOperationResult<>(this, () -> {
            UFile parent = getParentUFile();
            if (!parent.exists().isSuccessful()) {
                parent.mkdirs();
            }
            return getClient().makeDirectory(path);
        });
    }

    @Override
    public boolean setLastModified(Date time) {
        try {
            return getClient().setModificationTime(path, timeValFormat.format(time));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public InputStream read() throws IOException {
        return getClient("read").retrieveFileStream(path);
    }

    @Override
    public void readClose() {
        try {
            getClient("read").completePendingCommand();
            removeClient("read");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OutputStream write() throws IOException {
        return getClient("write").storeFileStream(path);
    }

    @Override
    public void writeClose() {
        try {
            getClient("write").completePendingCommand();
            removeClient("write");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OutputStream append() throws IOException {
        return getClient("append").appendFileStream(path);
    }

    @Override
    public void appendClose() {
        try {
            getClient("append").completePendingCommand();
            removeClient("append");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        FTPClient client = getClient();
        try {
            client.logout();
        } catch (IOException e) {
            if(client.isConnected()) {
                try {
                    client.disconnect();
                } catch(IOException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
        removeClient();
    }

    @Override
    public void moveTo(UFile destination) {
        try {
            boolean result = getClient().rename(this.getPath(), destination.getPath());
            if(!result) {
                throw new RuntimeException("Unknown error occurred. Could not move '"+getPath()+"' to '"+destination.getPath()+"'");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public UFile goTo(String path) {
        return new UFileFtp(this, join(this.path, path, getFileSep()));
    }

    @Override
    public void clearCache() {}

    @Override
    public String toString() {
        String username = nonNull(login.get(USERNAME)) ? login.get(USERNAME) + "@" : "";
        String port = nonNull(login.get(PORT)) ? ":" + login.get(PORT) : "";
        return "ftp://" + username + login.get(HOST) + port + path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UFileFtp)) return false;
        UFileFtp uFileFtp = (UFileFtp) o;
        return path.equals(uFileFtp.path) &&
                login.equals(uFileFtp.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, login);
    }

    /**
     * Converts the {@link FTPFile} into a {@link UFile}.
     * @param ftpFile The {@link FTPFile} to convert.
     * @return The created {@link UFile}.
     */
    private UFile toUFile(FTPFile ftpFile) {
        return new UFileFtp(this, path + (path.endsWith(getFileSep()) ? "" : getFileSep()) + ftpFile.getName());
    }

    /**
     * Gets the existing {@link FTPFile} from this {@link UFile}.
     * @return The retrieved {@link FTPFile} or null if not found.
     */
    private FTPFile asFTPFile() throws IOException {
        return Arrays.stream(getClient().listFiles(getParent()))
                .filter(ftp -> path.endsWith(ftp.getName()))
                .findFirst().orElse(null);
    }

    /**
     * Gets a {@link FTPClient} for this {@link UFile}. Creates and connects if required.
     * @param kind The specific version of the client. Allows for multiple connections to a single server to be maintained.
     * @return The {@link FTPClient}.
     */
    private FTPClient getClient(String kind) {
        Pair<ConnectionDetails, String> key = Pair.of(login, kind);
        var client = ftpConnections.get(key);

        if(isNull(client) || !client.isConnected()) {
            client = new FTPClient();
            FTPClientConfig config = new FTPClientConfig();
            client.configure(config);
            client.setListHiddenFiles(true);

            try {
                if (login.containsKey(PORT)) {
                    client.connect(login.get(HOST), Integer.parseInt(login.get(PORT)));
                } else {
                    client.connect(login.get(HOST));
                }

                if (client.getReplyCode() != SERVICE_READY) {
                    System.out.println("Failed to connect to " + login.get(HOST) + ". Unknown error");
                    System.out.print(client.getReplyString());
                }

                if (nonNull(login.get(USERNAME))) {
                    client.login(login.get(USERNAME), login.get(PASSWORD));
                    if(client.getReplyCode() == NOT_LOGGED_IN) {
                        throw new RuntimeException("Bad login! code: "+client.getReplyCode());
                    } else if (client.getReplyCode() != USER_LOGGED_IN) {
                        System.out.println("Failed to login as " + login.get(USERNAME) + ". Unknown error");
                        System.out.print(client.getReplyString());
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
                if(client.isConnected()) {
                    try {
                        client.disconnect();
                    } catch(IOException ignored) {}
                }
            }

            ftpConnections.put(key, client);
        }

        return client;
    }

    /**
     * Gets a default {@link FTPClient} for this {@link UFile}. Creates and connects if required.
     * @return The {@link FTPClient}.
     */
    public FTPClient getClient() {
        return getClient("");
    }

    /**
     * Removes a {@link FTPClient} from the cache map. Does not close connections.
     * @param kind The specific version of the client. Allows for multiple connections to a single server to be maintained.
     */
    private void removeClient(String kind) {
        Pair<ConnectionDetails, String> key = Pair.of(login, kind);

        try {
            ftpConnections.get(key).disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ftpConnections.remove(key);
    }

    /**
     * Removes the default {@link FTPClient} from the cache map. Does not close connections.
     */
    private void removeClient() {
        removeClient("");
    }

}
