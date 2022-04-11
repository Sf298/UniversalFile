package com.sf298.universal.file.model.responses;

import java.util.Date;

public class UFMetadata {

    public static final UFMetadata NOT_EXIST = new UFMetadata(false, null, null, null, false, false);

    /**
     * Whether the object exists on the filesystem.
     */
    boolean exists;

    /**
     * The size of this file in bytes.
     */
    Long length;

    /**
     * The date this file was created.
     */
    Date created;

    /**
     * The date this file was last modified.
     */
    Date lastModified;

    /**
     * Whether the object is a file.
     */
    boolean isFile;

    /**
     * Whether the object is a folder.
     */
    boolean isFolder;

    public UFMetadata() {}

    public UFMetadata(boolean exists, Long length, Date created, Date lastModified, boolean isFile, boolean isFolder) {
        this.exists = exists;
        this.length = length;
        this.created = created;
        this.lastModified = lastModified;
        this.isFile = isFile;
        this.isFolder = isFolder;
    }

    /**
     * Gets whether the object exists on the filesystem.
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * Gets the size of this file.
     * @return The size of the file in bytes.
     */
    public Long getLength() {
        return length;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isFolder() {
        return isFolder;
    }

}
