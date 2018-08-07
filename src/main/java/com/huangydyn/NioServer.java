package com.huangydyn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioServer implements Runnable {
    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(1024);

    public NioServer(int port) throws IOException {
        this.port = port;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Server starting on port " + this.port);

            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.serverSocketChannel.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) this.handleAccept(key);
                    if (key.isReadable()) this.handleRead(key);
                }
            }
        } catch (IOException e) {
            System.out.println("IOException, server of port " + this.port + " terminating. Stack trace:");
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        String address = (new StringBuilder(socketChannel.socket().getInetAddress().toString()))
                .append(":")
                .append(socketChannel.socket().getPort()).toString();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ, address);

        ByteBuffer buffer = ByteBuffer.wrap("Welcome to NioServer!\n".getBytes());
        socketChannel.write(buffer);
        buffer.rewind();

        System.out.println("accepted connection from: " + address);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();

        buf.clear();
        int read;
        while ((read = socketChannel.read(buf)) > 0) {
            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            sb.append(new String(bytes));
            buf.clear();
        }
        String msg;
        if (read < 0) {
            msg = key.attachment() + " left the chat.\n";
            socketChannel.close();
        } else {
            msg = key.attachment() + ": " + sb.toString();
        }

        System.out.println(msg);
        broadcastMsg(msg);
    }

    private void broadcastMsg(String msg) throws IOException {
        ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel sch = (SocketChannel) key.channel();
                sch.write(msgBuf);
                msgBuf.rewind();
            }
        }
    }
}
