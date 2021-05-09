package com.sf298.universal.file.model;

import java.util.HashMap;
import java.util.Map;

public class Connection extends HashMap<ConnectionParams, String> {

    public Connection() {
        super();
    }

    public Connection(Map<? extends ConnectionParams, ? extends String> m) {
        super(m);
    }

}
