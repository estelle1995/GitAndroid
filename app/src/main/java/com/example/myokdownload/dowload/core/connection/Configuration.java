package com.example.myokdownload.dowload.core.connection;


import java.net.Proxy;

public class Configuration {
    Proxy proxy;
    Integer readTimeout;
    Integer connectTimeout;

    public Configuration proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public Configuration readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public Configuration connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }
}
