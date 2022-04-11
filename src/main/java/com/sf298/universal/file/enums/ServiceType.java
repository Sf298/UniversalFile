package com.sf298.universal.file.enums;

import com.sf298.universal.file.model.connection.ConnectionDetails;
import com.sf298.universal.file.services.*;

import java.util.function.Function;

public enum ServiceType {

    LOCAL_DISK(d -> new UFileLocalDisk("/")),
    FTP(UFileFtp::new),
    DROPBOX(UFileDropbox::new);

    private final Function<ConnectionDetails, UFile> constructor;

    ServiceType(Function<ConnectionDetails, UFile> constructor) {
        this.constructor = constructor;
    }

    public UFile construct(ConnectionDetails details) {
        return constructor.apply(details);
    }

}
