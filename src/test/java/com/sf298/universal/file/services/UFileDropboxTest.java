package com.sf298.universal.file.services;

import com.sf298.universal.file.enums.ServiceType;
import com.sf298.universal.file.model.connection.ConnectionDetails;
import com.sf298.universal.file.services.UFile;
import com.sf298.universal.file.services.UFileTest;
import com.sf298.universal.file.services.UFileDropbox;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.sf298.universal.file.model.connection.ConnectionParam.*;

public class UFileDropboxTest extends UFileTest {

    private static final UFile root;

    static {
        ConnectionDetails login = new ConnectionDetails(
                ServiceType.DROPBOX,
                Map.of(TOKEN, "j1EMzF6_VYAAAAAAAAAAAVGlaSPkSRLhHHayzRPJoYZwfQGVcg792AlzwjEpNg1l")
        );

        root = new UFileDropbox(login);
    }

    public UFileDropboxTest() {
        super(root);
    }

    @Test
    @Override
    public void testLastModified() {}

}
