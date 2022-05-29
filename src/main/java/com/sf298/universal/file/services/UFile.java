package com.sf298.universal.file.services;

import com.sf298.universal.file.model.functions.UFileFilter;
import com.sf298.universal.file.model.functions.UFilenameFilter;
import com.sf298.universal.file.model.responses.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public abstract class UFile {

    private final String path;
    private int bufferSize;
    private boolean hasAppended = false;
    public UFMetadata metadataCache;

    public UFile(String path) {
        this(path, 1024);
    }

    public UFile(String path, int bufferSize) {
        this.path = path.endsWith(getFileSep()) && nonNull(parent(path, getFileSep())) ? path.substring(0, path.length() - 1) : path;
        /* Match windows pattern
        if (!getPath().matches("([A-Z]:[\\\\/]|/).*")) {
            throw new IllegalArgumentException("Error: '"+ path +"' doesn't have a valid beginning. Should be like 'C:\\' or '/'.");
        }*/
        this.bufferSize = bufferSize;
    }

    /**
     * Get the buffer size used when copying/moving files.
     * @return The buffer size in bytes.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the buffer size to use when copying/moving files.
     * @param bufferSize The buffer size in bytes.
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Gets the file separator used by this instance of {@link UFile}.
     * @return The file separator.
     */
    public abstract String getFileSep();


    /**
     * Get the pathname of this {@link UFile}.
     * @return A path of <code>/a/b/c.txt</code> would return <code>c.txt</code>.
     */
    public String getName() {
        return path.substring(path.lastIndexOf(getFileSep())+1);
    }

    /**
     * Get the pathname of the parent of this {@link UFile}.
     * @return A path of <code>/a/b/c.txt</code> would return <code>/a/b</code>.
     */
    public String getParent() {
        return parent(path, getFileSep());
    }

    /**
     * Get the parent of this {@link UFile} as a {@link UFile}.
     * @return The parent of this {@link UFile} or null if <code>this</code> is the root.
     */
    public abstract UFile getParentUFile();

    /**
     * Get the pathname of this {@link UFile}.
     * @return The pathname.
     */
    public String getPath() {
        return path;
    }


    /**
     * Checks if this file exists.
     * @return Returns <code>true</code> if the file exists, otherwise <code>false</code>>.
     */
    public abstract UFOperationResult<Boolean> exists();

    /**
     * Checks if this file is a directory.
     * @return Returns <code>true</code> if the file is a directory, otherwise <code>false</code>>.
     */
    public abstract UFOperationResult<Boolean> isDirectory();

    /**
     * Checks if this file is a file.
     * @return Returns <code>true</code> if the file exists, otherwise <code>false</code>>.
     */
    public abstract UFOperationResult<Boolean> isFile();

    /**
     * Gets the datetime that this file was created.
     * @return A {@link Date} object denoting the date and time this file was created.
     */
    public abstract UFOperationResult<Date> dateCreated();

    /**
     * Sets the created datetime of the file or directory named by this
     * abstract pathname.
     *
     * @param  time  The new created datetime, measured in milliseconds since
     *               the epoch (00:00:00 GMT, January 1, 1970)
     *
     * @return {@code true} if and only if the operation succeeded;
     *          {@code false} otherwise
     *
     * @throws  IllegalArgumentException  If the argument is negative
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}
     *          method denies write access to the named file
     */
    public abstract UFOperationResult<Boolean> setDateCreated(Date time);

    /**
     * Gets the datetime that this file was last modified.
     * @return A {@link Date} object denoting the date and time this file was last modified.
     */
    public abstract UFOperationResult<Date> lastModified();

    /**
     * Sets the last-modified time of the file or directory named by this
     * abstract pathname.
     *
     * <p> All platforms support file-modification times to the nearest second,
     * but some provide more precision.  The argument will be truncated to fit
     * the supported precision.  If the operation succeeds and no intervening
     * operations on the file take place, then the next invocation of the
     * {@link #lastModified} method will return the (possibly
     * truncated) {@code time} argument that was passed to this method.
     *
     * @param  time  The new last-modified time, measured in milliseconds since
     *               the epoch (00:00:00 GMT, January 1, 1970)
     *
     * @return {@code true} if and only if the operation succeeded;
     *          {@code false} otherwise
     *
     * @throws  IllegalArgumentException  If the argument is negative
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}
     *          method denies write access to the named file
     */
    public abstract UFOperationResult<Boolean> setLastModified(Date time);

    /**
     * Gets the size of this file.
     * @return The size of the file in bytes.
     */
    public abstract UFOperationResult<Long> length();

    /**
     * Atomically creates a new, empty file named by this abstract pathname if
     * and only if a file with this name does not yet exist.  The check for the
     * existence of the file and the creation of the file if it does not exist
     * are a single operation that is atomic with respect to all other
     * filesystem activities that might affect the file.
     * <P>
     * Note: this method should <i>not</i> be used for file-locking, as
     * the resulting protocol cannot be made to work reliably.
     *
     * @return  {@code true} if the named file does not exist and was
     *          successfully created; {@code false} if the named file
     *          already exists
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}
     *          method denies write access to the file
     */
    public UFOperationResult<Boolean> createNewFile() {
        UFOperationResult<Boolean> existsResult = exists();
        if (!existsResult.isSuccessful()) {
            return existsResult;
        } else if (existsResult.getResult()) {
            return UFOperationResult.createBoolOperation(this, false);
        }

        return new UFOperationResult<>(this, () -> {
            getParentUFile().mkdirs().getResult();
            OutputStream stream = write();
            stream.close();
            writeClose();
            return true;
        });
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname.  If
     * this pathname denotes a directory, then the directory must be empty in
     * order to be deleted.
     *
     * @return  {@code true} if and only if the file or directory is
     *          successfully deleted; {@code false} otherwise
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkDelete} method denies
     *          delete access to the file
     */
    public abstract UFOperationResult<Boolean> delete();

    /**
     * Deletes the file or directory denoted by this abstract pathname.  If
     * this pathname denotes a directory, then the directory must be empty in
     * order to be deleted.
     *
     * @return  {@code true} if and only if the file or directory is
     *          successfully deleted; {@code false} otherwise
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkDelete} method denies
     *          delete access to the file
     */
    public abstract UFOperationResult<Boolean> deleteRecursive();


    /**
     * Returns an array of strings naming the files and directories in the
     * directory denoted by this abstract pathname.
     *
     * <p> If this abstract pathname does not denote a directory, then this
     * method returns {@code null}.  Otherwise an array of strings is
     * returned, one for each file or directory in the directory.  Names
     * denoting the directory itself and the directory's parent directory are
     * not included in the result.  Each string is a file name rather than a
     * complete path.
     *
     * <p> There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @return  An array of strings naming the files and directories in the
     *          directory denoted by this abstract pathname.  The array will be
     *          empty if the directory is empty.  Returns {@code null} if
     *          this abstract pathname does not denote a directory, or if an
     *          I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     */
    public abstract UFOperationResult<String[]> list();

    /**
     * Returns an array of strings naming the files and directories in the
     * directory denoted by this abstract pathname that satisfy the specified
     * filter.  The behavior of this method is the same as that of the
     * {@link #list()} method, except that the strings in the returned array
     * must satisfy the filter.  If the given {@code filter} is {@code null}
     * then all names are accepted.  Otherwise, a name satisfies the filter if
     * and only if the value {@code true} results when the {@link
     * UFilenameFilter#accept UFilenameFilter.accept(File,&nbsp;String)} method
     * of the filter is invoked on this abstract pathname and the name of a
     * file or directory in the directory that it denotes.
     *
     * @param  filter
     *         A filename filter
     *
     * @return  An array of strings naming the files and directories in the
     *          directory denoted by this abstract pathname that were accepted
     *          by the given {@code filter}.  The array will be empty if the
     *          directory is empty or if no names were accepted by the filter.
     *          Returns {@code null} if this abstract pathname does not denote
     *          a directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     *
     * @see java.nio.file.Files#newDirectoryStream(Path,String)
     */
    public UFOperationResult<String[]> list(UFilenameFilter filter) {
        return new UFOperationResult<>(this,
                () -> Arrays.stream(list().getResult())
                        .filter(f -> filter.accept(this, f))
                        .toArray(String[]::new)
        );
    }


    /**
     * Returns an array of abstract pathnames denoting the files in the
     * directory denoted by this abstract pathname.
     *
     * <p> If this abstract pathname does not denote a directory, then this
     * method returns {@code null}.  Otherwise an array of {@code File} objects
     * is returned, one for each file or directory in the directory.  Pathnames
     * denoting the directory itself and the directory's parent directory are
     * not included in the result.
     *
     * <p> There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     */
    public abstract UFOperationResult<UFile[]> listFiles();

    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.  The behavior of this method is the same
     * as that of the {@link #listFiles()} method, except that the pathnames in
     * the returned array must satisfy the filter.  If the given {@code filter}
     * is {@code null} then all pathnames are accepted.  Otherwise, a pathname
     * satisfies the filter if and only if the value {@code true} results when
     * the {@link UFilenameFilter#accept
     * UFilenameFilter.accept(File,&nbsp;String)} method of the filter is
     * invoked on this abstract pathname and the name of a file or directory in
     * the directory that it denotes.
     *
     * @param  filter
     *         A filename filter
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     */
    public UFOperationResult<UFile[]> listFiles(UFilenameFilter filter) {
        return new UFOperationResult<>(this,
                () -> Arrays.stream(listFiles().getResult())
                .filter(f -> filter.accept(this, f.getName()))
                .toArray(UFile[]::new)
        );
    }

    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.  The behavior of this method is the same
     * as that of the {@link #listFiles()} method, except that the pathnames in
     * the returned array must satisfy the filter.  If the given {@code filter}
     * is {@code null} then all pathnames are accepted.  Otherwise, a pathname
     * satisfies the filter if and only if the value {@code true} results when
     * the {@link UFileFilter#accept UFileFilter.accept(File)} method of the
     * filter is invoked on the pathname.
     *
     * @param  filter
     *         A file filter
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          SecurityManager#checkRead(String)} method denies read access to
     *          the directory
     */
    public UFOperationResult<UFile[]> listFiles(UFileFilter filter) {
        return new UFOperationResult<>(this,
                () -> Arrays.stream(listFiles().getResult())
                    .filter(filter::accept)
                    .toArray(UFile[]::new)
        );
    }

    /**
     * List all files recursively in batches.
     */
    public UFOperationResult<Boolean> listFilesRecursiveBatch(Consumer<UFile[]> resultCallback) {
        UFOperationResult<UFile[]> files = listFiles();
        if (files.isSuccessful()) {
            resultCallback.accept(files.getResult());
            return UFOperationResult.createBoolOperation(this, true);
        }
        return new UFOperationResult<>(this, files.getException());
    }


    /**
     * Creates the directory named by this abstract pathname.
     *
     * @return  {@code true} if and only if the directory was
     *          created; {@code false} otherwise
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}
     *          method does not permit the named directory to be created
     */
    public abstract UFOperationResult<Boolean> mkdir();

    /**
     * Creates the directory named by this abstract pathname, including any
     * necessary but nonexistent parent directories.  Note that if this
     * operation fails it may have succeeded in creating some of the necessary
     * parent directories.
     *
     * @return  {@code true} if and only if the directory was created,
     *          along with all necessary parent directories; {@code false}
     *          otherwise
     *
     * @throws  SecurityException
     *          If a security manager exists and its {@link
     *          java.lang.SecurityManager#checkRead(java.lang.String)}
     *          method does not permit verification of the existence of the
     *          named directory and all necessary parent directories; or if
     *          the {@link
     *          java.lang.SecurityManager#checkWrite(java.lang.String)}
     *          method does not permit the named directory and all necessary
     *          parent directories to be created
     */
    public abstract UFOperationResult<Boolean> mkdirs();


    /**
     * Opens an {@link InputStream} from the file denoted by this {@link UFile}.
     * @return Returns the opened stream.
     * @throws IOException If an I/O error occurred.
     */
    public abstract InputStream read() throws IOException;

    /**
     * Finishes up any operations remaining after the {@link InputStream} returned by {@link #read()} is closed.
     */
    public abstract void readClose();

    /**
     * Opens an {@link OutputStream} to the file denoted by this {@link UFile}.
     * Overwrites the existing contents of the file.
     * @return Returns the opened stream.
     * @throws IOException If an I/O error occurred.
     */
    public abstract OutputStream write() throws IOException;

    /**
     * Finishes up any operations remaining after the {@link OutputStream} returned by {@link #write()} is closed.
     */
    public abstract void writeClose();

    /**
     * Opens an {@link OutputStream} to the file denoted by this {@link UFile}.
     * Appends new data to the end of the file.
     * @return Returns the opened stream.
     * @throws IOException If an I/O error occurred.
     */
    public OutputStream append() throws IOException {
        UFile tempFile = goTo(getPath()+".appendtmp");
        hasAppended = true;
        moveTo(tempFile);

        InputStream in = new BufferedInputStream(tempFile.read());
        OutputStream out = new BufferedOutputStream(this.write());

        byte[] buffer = new byte[getBufferSize()];
        int lengthRead;
        while ((lengthRead = in.read(buffer)) > 0) {
            out.write(buffer, 0, lengthRead);
        }
        tempFile.readClose();

        return out;
    }

    /**
     * Finishes up any operations remaining after the {@link OutputStream} returned by {@link #append()} is closed.
     */
    public void appendClose() {
        if (hasAppended) {
            UFile tempFile = goTo(getPath() + ".appendtmp");
            tempFile.delete();
            writeClose();
            hasAppended = false;
        }
    }

    /**
     * Closes the primary connection used by this {@link UFile} to the remote server.
     */
    public abstract void close();

    /**
     * Copies the file denoted by this {@link UFile} to the <code>destination</code> {@link UFile}.
     * Works for copying files between kinds of destinations.
     * @param destination The target destination. Must not exist prior to copy.
     */
    public UFOperationResult<Boolean> copyTo(UFile destination) {
        UFile destParent = destination.getParentUFile();
        UFOperationResult<Boolean> destParentExistsResult = destParent.exists();
        if(!destParentExistsResult.isSuccessful()) {
            return new UFOperationResult<>(this, destParentExistsResult.getException());
        } else if (!destParentExistsResult.getResult()) {
            destParent.mkdirs();
        }

        return new UFOperationResult<>(this, () -> {
                InputStream in = new BufferedInputStream(this.read());
                OutputStream out = new BufferedOutputStream(destination.write());

                byte[] buffer = new byte[getBufferSize()];
                int lengthRead;
                while ((lengthRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, lengthRead);
                }
                this.close();
                readClose();
                out.flush();
                out.close();
                destination.writeClose();

                return true;
        });
    }

    /**
     * Moves the file denoted by this {@link UFile} to the <code>destination</code> {@link UFile}.
     * Works for moving files between kinds of destinations. Moves the file using the native move function if possible.
     * @param destination The target destination. Must not exist prior to move.
     */
    public UFOperationResult<Boolean> moveTo(UFile destination) {
        UFOperationResult<Boolean> copyToResult = copyTo(destination);
        if(!copyToResult.isSuccessful()) {
            return new UFOperationResult<>(this, copyToResult.getException());
        } else if (!copyToResult.getResult()) {
            return new UFOperationResult<>(this, new FileNotFoundException("Copy failed: reason unknown"));
        }

        UFOperationResult<Boolean> deleteResult = deleteRecursive();
        if(!deleteResult.isSuccessful()) {
            return new UFOperationResult<>(this, deleteResult.getException());
        } else if (!deleteResult.getResult()) {
            return new UFOperationResult<>(this, new FileNotFoundException("Move failed: reason unknown"));
        }

        return UFOperationResult.createBoolOperation(this, true);
    }

    /*
    boolean canRead();
    boolean canWrite();
    boolean setReadOnly();
    boolean setWritable(boolean writable, boolean ownerOnly);
    boolean setWritable(boolean writable);
    boolean setReadable(boolean readable, boolean ownerOnly);
    boolean setReadable(boolean readable);
    boolean setExecutable(boolean executable, boolean ownerOnly);
    boolean setExecutable(boolean executable);
    boolean canExecute();

    long getTotalSpace();
    long getFreeSpace();
    long getUsableSpace();*/

    /**
     * Goes to a pathname relative to the pathname of this {@link UFile}.
     * @param path The relative path. Prepending with <code>../</code> causes it to go up a level.
     * @return The created {@link UFile}.
     */
    public abstract UFile stepInto(String path);

    /**
     * Goes to a pathname on the same filesystem.
     * @param path The full path.
     * @return The created {@link UFile}.
     */
    public abstract UFile goTo(String path);

    /**
     * Clears any caching used in this {@link UFile}.
     */
    public void clearCache() {
        metadataCache = null;
    }

    /**
     * Joins a parent and a child pathname with the given <code>fileSep</code>.<br>
     * Checks if the parent/child start/ends with the given <code>fileSep</code> and ensures they are joined by only one <code>fileSep</code>.
     * @param parent The parent pathname.
     * @param child The relative pathname. Prepending with <code>../</code> causes it to go up a level.
     * @param fileSep The file separator. Usually <code>/</code> or <code>\</code>
     * @return Returns the combined pathname.
     */
    public static String join(String fileSep, String parent, String child) {
        while (true) {
            if (child.startsWith("../") || child.startsWith("..\\")) {
                child = child.substring(3);
                parent = parent(parent, fileSep);
                if (isNull(parent)) {
                    throw new RuntimeException(new FileNotFoundException("Could not step up, out of root."));
                }
            } else if (child.startsWith("./") || child.startsWith(".\\")) {
                child = child.substring(2);
            } else {
                break;
            }
        }
        if (parent.endsWith(fileSep)) {
            if(child.startsWith(fileSep)) {
                return parent + child.substring(1);
            } else {
                return parent + child;
            }
        } else {
            if(child.startsWith(fileSep)) {
                return parent + child;
            } else {
                return parent + fileSep + child;
            }
        }
    }

    public static String join(String fileSep, String... names) {
        return Arrays.stream(names)
                .map(s -> {
                    int start = s.startsWith(fileSep) ? 1 : 0;
                    int end = s.endsWith(fileSep) ? s.length()-1 : s.length();
                    return s.substring(start, end);
                })
                .collect(Collectors.joining(fileSep));
    }

    /**
     * Parses the parent pathname from the given <code>path</code>.
     * @param path The pathname to process.
     * @param fileSep The file separator used.
     * @return The parent pathname
     */
    private static String parent(String path, String fileSep) {
        if (fileSep.equals(path) || fileSep.endsWith(":/") || fileSep.endsWith(":\\") || !path.contains(fileSep)) {
            return null;
        }

        int mod = path.endsWith(fileSep) ? 2 : 1;
        int subStringIndex = path.lastIndexOf(fileSep, path.length() - mod);
        String parent = path.substring(0, subStringIndex);
        return  parent.isEmpty() ? fileSep : parent;
    }

}
