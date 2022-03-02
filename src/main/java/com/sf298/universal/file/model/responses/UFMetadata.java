package com.sf298.universal.file.model.responses;

import java.util.Date;

public class UFMetadata {

    /**
     * Whether the object exists on the filesystem.
     */
    boolean exists;

    /**
     * The size of this file in bytes.
     */
    Long size;

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
    Boolean isFile;

    /**
     * Whether the object is a folder.
     */
    Boolean isFolder;

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
    public Long getSize() {
        return size;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public Boolean getFile() {
        return isFile;
    }

    public Boolean getFolder() {
        return isFolder;
    }

}
