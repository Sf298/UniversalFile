package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.services.UFileTest;

public class UFileLocalDiskTest extends UFileTest {

    private static final String root = "F:\\temptest";

    public UFileLocalDiskTest() {
        super(new UFileLocalDisk(root));
    }

}