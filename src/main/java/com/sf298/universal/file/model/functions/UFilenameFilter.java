package com.sf298.universal.file.model.functions;

import com.sf298.universal.file.services.UFile;

@FunctionalInterface
public interface UFilenameFilter {

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param   dir    the directory in which the file was found.
     * @param   name   the name of the file.
     * @return  {@code true} if and only if the name should be
     * included in the file list; {@code false} otherwise.
     */
    boolean accept(UFile dir, String name);

}
