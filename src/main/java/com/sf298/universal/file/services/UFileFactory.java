package com.sf298.universal.file.services;

import com.sf298.universal.file.enums.ServiceType;
import com.sf298.universal.file.model.connection.ConnectionDetails;
import com.sf298.universal.file.model.connection.ConnectionParam;
import com.sf298.universal.file.services.platforms.UFileDropbox;
import com.sf298.universal.file.services.platforms.UFileFtp;
import com.sf298.universal.file.services.platforms.UFileLocalDisk;

public class UFileFactory {

    public static UFile parse(String url) {
        String[] split1 = url.split("://");
        String urlPrefix = split1[0].toLowerCase();
        switch (urlPrefix) {
            case "file" -> {
                return new UFileLocalDisk(split1[1]);
            }
            case "ftp" -> {
                ConnectionDetails connection = new ConnectionDetails(ServiceType.FTP);
                StringConsumer sc = new StringConsumer(split1[1]);
                int dToAt = sc.dTo("@");
                if (dToAt > 0) {
                    String credentials = sc.consume(dToAt);
                    String[] userAndPassword = credentials.split(":", 2);
                    connection.put(ConnectionParam.USERNAME, userAndPassword[0]);
                    if (userAndPassword.length > 1) {
                        connection.put(ConnectionParam.PASSWORD, userAndPassword[1]);
                    }
                }
                int hostLength = Math.min(sc.dToMax("/"), sc.dToEnd());
                String[] hostAndPort = sc.consume(hostLength).split(":", 2);
                connection.put(ConnectionParam.HOST, hostAndPort[0]);
                if (hostAndPort.length > 1) {
                    connection.put(ConnectionParam.PORT, hostAndPort[1]);
                }

                String path = sc.consume(sc.dToEnd());

                return new UFileFtp(connection, path);
            }
            case "dbx" -> {
                ConnectionDetails connection = new ConnectionDetails(ServiceType.DROPBOX);
                String[] tokenAndPath = split1[1].split("/", 2);
                connection.put(ConnectionParam.TOKEN, tokenAndPath[0]);
                return new UFileDropbox(connection, tokenAndPath[1]);
            }
        }
        throw new RuntimeException("Unsupported url prefix '"+urlPrefix+"'");
    }

    public static UFile parse(String url, String relativePath) {
        UFile file = parse(url);
        return file.stepInto(relativePath);
    }

}
