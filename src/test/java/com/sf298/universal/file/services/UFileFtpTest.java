package com.sf298.universal.file.services;

import com.sf298.universal.file.enums.ServiceType;
import com.sf298.universal.file.model.connection.ConnectionDetails;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.sf298.universal.file.model.connection.ConnectionParam.*;
import static org.assertj.core.api.Assertions.assertThat;

public class UFileFtpTest extends UFileTest {

    private static final UFile root;

    static {
        ConnectionDetails login = new ConnectionDetails(
                ServiceType.FTP,
                Map.of(
                        HOST, "localhost",//""192.168.86.31",
                        PORT, "21",
                        USERNAME, "testuser",
                        PASSWORD, "7Jc!r"
                )
        );

        root = new UFileFtp(login).stepInto("UFileFtpTest");
    }

    public UFileFtpTest() {
        super(root);
    }

    @Test
    @Override
    public void testLastModified() {}

}