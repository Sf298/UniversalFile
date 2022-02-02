package com.sf298.universal.file.services.local;

import com.sf298.universal.file.model.responses.*;
import com.sf298.universal.file.services.UFile;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static java.util.Objects.isNull;

public class UFileLocalDisk extends UFile {

    private final File file;

    /**
     * Creates a new {@link UFile} instance by converting the given {@link File}.
     * @param   file  A {@link File} object.
     */
    public UFileLocalDisk(File file) {
        this.file = file;
    }

    /**
     * Creates a new {@link UFile} instance by converting the given
     * pathname string into an abstract pathname.  If the given string is
     * the empty string, then the result is the empty abstract pathname.
     *
     * @param   pathname  A pathname string
     * @throws  NullPointerException
     *          If the {@code pathname} argument is {@code null}
     */
    public UFileLocalDisk(String pathname) {
        this(new File(pathname));
    }

    /**
     * Creates a new {@link UFile} instance from a parent pathname string
     * and a child pathname string.
     *
     * <p> If {@code parent} is {@code null} then the new
     * {@link UFile} instance is created as if by invoking the
     * single-argument {@link UFile} constructor on the given
     * {@code child} pathname string.
     *
     * <p> Otherwise the {@code parent} pathname string is taken to denote
     * a directory, and the {@code child} pathname string is taken to
     * denote either a directory or a file.  If the {@code child} pathname
     * string is absolute then it is converted into a relative pathname in a
     * system-dependent way.  If {@code parent} is the empty string then
     * the new {@link UFile} instance is created by converting
     * {@code child} into an abstract pathname and resolving the result
     * against a system-dependent default directory.  Otherwise each pathname
     * string is converted into an abstract pathname and the child abstract
     * pathname is resolved against the parent.
     *
     * @param   parent  The parent pathname string
     * @param   child   The child pathname string
     * @throws  NullPointerException
     *          If {@code child} is {@code null}
     */
    public UFileLocalDisk(String parent, String child) {
        this(new File(parent, child));
    }

    /**
     * Creates a new {@link UFile} instance from a parent abstract
     * pathname and a child pathname string.
     *
     * <p> If {@code parent} is {@code null} then the new
     * {@link UFile} instance is created as if by invoking the
     * single-argument {@link UFile} constructor on the given
     * {@code child} pathname string.
     *
     * <p> Otherwise the {@code parent} abstract pathname is taken to
     * denote a directory, and the {@code child} pathname string is taken
     * to denote either a directory or a file.  If the {@code child}
     * pathname string is absolute then it is converted into a relative
     * pathname in a system-dependent way.  If {@code parent} is the empty
     * abstract pathname then the new {@code File} instance is created by
     * converting {@code child} into an abstract pathname and resolving
     * the result against a system-dependent default directory.  Otherwise each
     * pathname string is converted into an abstract pathname and the child
     * abstract pathname is resolved against the parent.
     *
     * @param   parent  The parent abstract pathname
     * @param   child   The child pathname string
     * @throws  NullPointerException
     *          If {@code child} is {@code null}
     */
    public UFileLocalDisk(File parent, String child) {
        this(new File(parent, child));
    }

    /**
     * Creates a new {@link UFile} instance by converting the given
     * {@code file:} URI into an abstract pathname.
     *
     * <p> The exact form of a {@code file:} URI is system-dependent, hence
     * the transformation performed by this constructor is also
     * system-dependent.
     *
     * <p> For a given abstract pathname <i>f</i> it is guaranteed that
     *
     * <blockquote><code>
     * new File(</code><i>&nbsp;f</i><code>.{@link File#toURI()
     * toURI}()).equals(</code><i>&nbsp;f</i><code>.{@link #getPath() getPath}())
     * </code></blockquote>
     *
     * so long as the original abstract pathname, the URI, and the new abstract
     * pathname are all created in (possibly different invocations of) the same
     * Java virtual machine.  This relationship typically does not hold,
     * however, when a {@code file:} URI that is created in a virtual machine
     * on one operating system is converted into an abstract pathname in a
     * virtual machine on a different operating system.
     *
     * @param  uri
     *         An absolute, hierarchical URI with a scheme equal to
     *         {@code "file"}, a non-empty path component, and undefined
     *         authority, query, and fragment components
     *
     * @throws  NullPointerException
     *          If {@code uri} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on the parameter do not hold
     *
     * @see java.net.URI
     * @since 1.4
     */
    public UFileLocalDisk(URI uri) {
        this(new File(uri));
    }

    @Override
    public String getFileSep() {
        return File.separator;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getParent() {
        return file.getParent();
    }

    @Override
    public UFile getParentUFile() {
        File parent = file.getParentFile();
        return isNull(parent) ? null : new UFileLocalDisk(parent);
    }

    @Override
    public String getPath() {
        return file.getAbsolutePath();
    }


    @Override
    public UFOperationResult<Boolean> exists() {
        return new UFOperationResult<>(this, file::exists);
    }

    @Override
    public UFOperationResult<Boolean> isDirectory() {
        return new UFOperationResult<>(this, file::isDirectory);
    }

    @Override
    public UFOperationResult<Boolean> isFile() {
        return new UFOperationResult<>(this, file::isFile);
    }

    @Override
    public UFOperationResult<Date> lastModified() {
        return new UFOperationResult<>(this, () -> new Date(file.lastModified()));
    }

    @Override
    public UFOperationResult<Long> length() {
        return new UFOperationResult<>(this, file::length);
    }


    @Override
    public UFOperationResult<Boolean> createNewFile() {
        return new UFOperationResult<>(this, file::createNewFile);
    }

    @Override
    public UFOperationResult<Boolean> delete() {
        return new UFOperationResult<>(this, file::delete);
    }

    @Override
    public UFOperationResult<Boolean> deleteRecursive() {
        if (isDirectory().getResultOrDefault(false)) {
            UFOperationResult<UFile[]> children = listFiles();
            if (!children.isSuccessful()) {
                return new UFOperationResult<>(this, children.getException());
            }

            Arrays.stream(children.getResult()).forEach(UFile::deleteRecursive);
        }
        return delete();
    }

    @Override
    public UFOperationResult<String[]> list() {
        return new UFOperationResult<>(this, file::list);
    }

    @Override
    public UFOperationResult<UFile[]> listFiles() {
        return new UFOperationResult<>(this,
                () -> Arrays.stream(list().getResult()).map(this::goTo).toArray(UFile[]::new));
    }

    @Override
    public UFOperationResult<Boolean> mkdir() {
        return new UFOperationResult<>(this, file::mkdir);
    }

    @Override
    public UFOperationResult<Boolean> mkdirs() {
        return new UFOperationResult<>(this, file::mkdirs);
    }

    @Override
    public UFOperationResult<Boolean> setLastModified(Date time) {
        return new UFOperationResult<>(this, () -> file.setLastModified(time.getTime()));
    }

    @Override
    public InputStream read() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void readClose() {}

    @Override
    public OutputStream write() throws IOException {
        return new FileOutputStream(file, false);
    }

    @Override
    public void writeClose() {}

    @Override
    public OutputStream append() throws IOException {
        return new FileOutputStream(file, true);
    }

    @Override
    public void appendClose() {}

    @Override
    public void close() {}

    @Override
    public UFOperationResult<Boolean> moveTo(UFile destination) {
        if (destination instanceof UFileLocalDisk) {
            return new UFOperationResult<>(this, () -> {
                boolean result = file.renameTo(((UFileLocalDisk)destination).file);
                if(!result) {
                    throw new RuntimeException("Unknown error occurred. Could not move '"+getPath()+"' to '"+destination.getPath()+"'");
                }
                return true;
            });
        } else {
            return super.moveTo(destination);
        }
    }


    @Override
    public UFile goTo(String path) {
        return new UFileLocalDisk(new File(file, path));
    }

    @Override
    public void clearCache() {}

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UFileLocalDisk)) return false;
        UFileLocalDisk that = (UFileLocalDisk) o;
        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFile());
    }
}
