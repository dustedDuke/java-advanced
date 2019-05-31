package ru.ifmo.rain.naumkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.net.*;
import java.nio.charset.Charset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;

public class HelloUDPServer implements HelloServer {

    private DatagramSocket socket;
    private ExecutorService workers;
    private AtomicBoolean closed = new AtomicBoolean(false);

    public void start(int port, int threads) {

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println(e.toString());
        }

        workers = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; ++i) {
            workers.submit(() -> {
                while (!closed.get()) {
                    try {

                        byte[] recvBuffer = new byte[socket.getReceiveBufferSize()];
                        DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                        socket.receive(recvPacket);

                        String sendMessage = "Hello, " + new String(recvPacket.getData(), 0, recvPacket.getLength(), "UTF-8");
                        byte[] sendBuffer = sendMessage.getBytes("UTF-8");
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, 0, sendBuffer.length, recvPacket.getSocketAddress());
                        socket.send(sendPacket);

                    } catch (IOException e) {
                        System.err.println(e.toString());
                    }
                }
            });
        }
    }

    public void close() {
        closed.set(true);
        workers.shutdown();
        socket.close();
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Two arguments excepted");
            return;
        }

        try {
            new HelloUDPServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println(e.toString());
        }
    }
}
