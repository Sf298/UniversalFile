package com.sf298.universal.file.model.connection;

import com.sf298.universal.file.enums.ServiceType;
import com.sf298.universal.file.services.UFile;

import java.util.HashMap;
import java.util.Map;

public class ConnectionDetails extends HashMap<ConnectionParam, String> {

    // TODO encrypt variables

    private final ServiceType serviceType;

    public ConnectionDetails(ServiceType serviceType) {
        super();
        this.serviceType = serviceType;
    }

    public ConnectionDetails(ServiceType serviceType, Map<? extends ConnectionParam, ? extends String> m) {
        super(m);
        this.serviceType = serviceType;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

}
