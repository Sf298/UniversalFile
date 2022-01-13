package com.sf298.universal.file.model;

import java.util.HashMap;
import java.util.Map;

public class ConnectionDetails extends HashMap<ConnectionParam, String> {

    // TODO encrypt variables

    public ConnectionDetails() {
        super();
    }

    public ConnectionDetails(Map<? extends ConnectionParam, ? extends String> m) {
        super(m);
    }

}
