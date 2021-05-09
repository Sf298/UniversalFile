package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.model.Connection;
import com.sf298.universal.file.services.UFileTest;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.sf298.universal.file.model.ConnectionParams.*;

public class UFileFtpTest extends UFileTest {

    private static final Connection login = new Connection(Map.of(
            HOST, "localhost",
            PORT, "21",
            USERNAME, "testuser",
            PASSWORD, "pass"
    ));

    public UFileFtpTest() throws IOException {
        super(new UFileFtp(login));

        Path temp = Files.createTempDirectory("UFileFtpTest");

        FakeFtpServer fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(Integer.parseInt(login.get(PORT)));
        fakeFtpServer.addUserAccount(new UserAccount(
                login.get(USERNAME),
                login.get(PASSWORD),
                temp.toString()
        ));

        FileSystem fileSystem = new WindowsFakeFileSystem();
        fakeFtpServer.setFileSystem(fileSystem);

        fakeFtpServer.start();
    }

}