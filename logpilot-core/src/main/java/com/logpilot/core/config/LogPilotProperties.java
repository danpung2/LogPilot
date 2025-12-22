package com.logpilot.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logpilot")
@SuppressWarnings("unused")
public class LogPilotProperties {

    private Storage storage = new Storage();
    private Server server = new Server();
    private Grpc grpc = new Grpc();

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Grpc getGrpc() {
        return grpc;
    }

    public void setGrpc(Grpc grpc) {
        this.grpc = grpc;
    }

    public static class Storage {
        private StorageType type = StorageType.SQLITE;
        private String directory = "./data/logs";
        private Sqlite sqlite = new Sqlite();

        public StorageType getType() {
            return type;
        }

        public void setType(StorageType type) {
            this.type = type;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public Sqlite getSqlite() {
            return sqlite;
        }

        public void setSqlite(Sqlite sqlite) {
            this.sqlite = sqlite;
        }

        public static class Sqlite {
            private String path = "./data/logpilot.db";
            private Pooling pooling = new Pooling();

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public Pooling getPooling() {
                return pooling;
            }

            public void setPooling(Pooling pooling) {
                this.pooling = pooling;
            }
        }

        public static class Pooling {
            private int maximumPoolSize = 10;
            private int minimumIdle = 2;
            private long connectionTimeout = 30000;
            private long idleTimeout = 600000;

            public int getMaximumPoolSize() {
                return maximumPoolSize;
            }

            public void setMaximumPoolSize(int maximumPoolSize) {
                this.maximumPoolSize = maximumPoolSize;
            }

            public int getMinimumIdle() {
                return minimumIdle;
            }

            public void setMinimumIdle(int minimumIdle) {
                this.minimumIdle = minimumIdle;
            }

            public long getConnectionTimeout() {
                return connectionTimeout;
            }

            public void setConnectionTimeout(long connectionTimeout) {
                this.connectionTimeout = connectionTimeout;
            }

            public long getIdleTimeout() {
                return idleTimeout;
            }

            public void setIdleTimeout(long idleTimeout) {
                this.idleTimeout = idleTimeout;
            }
        }
    }

    public static class Server {
        private int port = 8080;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class Grpc {
        private int port = 50051;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public enum StorageType {
        FILE,
        SQLITE
    }
}