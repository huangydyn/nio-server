package com.huangydyn;

import java.io.IOException;

public class Application {
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) throws IOException {
        NioServer server = new NioServer(SERVER_PORT);
        (new Thread(server)).start();
    }
}
