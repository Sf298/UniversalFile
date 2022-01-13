package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.services.UFile;
import com.sf298.universal.file.services.UFileTest;

import java.io.IOException;
import java.nio.file.Files;

public class UFileLocalDiskTest extends UFileTest {

    private static UFile root;

    static {
        try {
            root = new UFileLocalDisk(Files.createTempDirectory("UFileLocalDiskTest").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UFileLocalDiskTest() throws IOException {
        super(root);
    }

}