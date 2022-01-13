package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.model.ConnectionDetails;
import com.sf298.universal.file.services.UFile;
import com.sf298.universal.file.services.UFileTest;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

import static com.sf298.universal.file.model.ConnectionParam.*;
import static org.assertj.core.api.Assertions.assertThat;

public class UFileFtpTest extends UFileTest {

    private static final UFile root;

    static {
        ConnectionDetails login = new ConnectionDetails(Map.of(
                HOST, "192.168.86.31",
                PORT, "21",
                USERNAME, "testuser",
                PASSWORD, "7Jc!r"
        ));

        root = new UFileFtp(login).goTo("UFileFtpTest");

        /*try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public UFileFtpTest() {
        super(root);
    }

    @Test
    @Override
    public void testLastModified() {}

}