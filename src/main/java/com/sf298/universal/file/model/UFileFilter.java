package com.sf298.universal.file.model;

import com.sf298.universal.file.services.UFile;

@FunctionalInterface
public interface UFileFilter {

    /**
     * Tests whether or not the specified abstract pathname should be
     * included in a pathname list.
     *
     * @param  pathname  The abstract pathname to be tested
     * @return  {@code true} if and only if {@code pathname}
     *          should be included
     */
    boolean accept(UFile pathname);

}
