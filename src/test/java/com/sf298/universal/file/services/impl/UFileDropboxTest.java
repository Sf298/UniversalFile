package com.sf298.universal.file.services.impl;

import com.sf298.universal.file.model.ConnectionDetails;
import com.sf298.universal.file.services.UFile;
import com.sf298.universal.file.services.UFileTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.sf298.universal.file.model.ConnectionParam.*;

public class UFileDropboxTest extends UFileTest {

    private static final UFile root;

    static {
        ConnectionDetails login = new ConnectionDetails(Map.of(
                TOKEN, "j1EMzF6_VYAAAAAAAAAAAVGlaSPkSRLhHHayzRPJoYZwfQGVcg792AlzwjEpNg1l"
        ));

        root = new UFileDropbox(login);
    }

    public UFileDropboxTest() {
        super(root);
    }

}
