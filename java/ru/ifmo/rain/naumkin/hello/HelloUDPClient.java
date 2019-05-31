package ru.ifmo.rain.naumkin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class HelloUDPClient implements HelloClient {

    public void run(String host, int port, String message, int threads, int msgCount) {

        try {
            InetAddress ip = InetAddress.getByName(host);
            ExecutorService workers = Executors.newFixedThreadPool(threads);

            for (int i = 0; i < threads; ++i) {
                final InetAddress fin_ip = ip;
                final int fin_i = i;

                workers.submit(() -> {
                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setSoTimeout(400);

                        for (int j = 0; j < msgCount; ++j) {
                            String sendMessage = message + fin_i + "_" + j;

                            byte[] sendBuffer = sendMessage.getBytes("UTF-8");
                            byte[] recvBuffer = new byte[socket.getReceiveBufferSize()];

                            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, 0, sendBuffer.length, fin_ip, port);
                            DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);

                            while (!socket.isClosed() || Thread.currentThread().isInterrupted()) {
                                try {
                                    socket.send(sendPacket);
                                    System.out.println("Sent request: " + sendMessage);

                                    socket.receive(recvPacket);
                                    String recvMessage = new String(recvPacket.getData(), recvPacket.getOffset(), recvPacket.getLength(), "UTF-8");
                                    System.out.println("Received request: " + recvMessage);

                                    if (!recvMessage.contains(sendMessage)) {
                                        System.out.println("Unknown message\n");
                                        continue;
                                    }
                                    System.out.println();
                                    break;

                                } catch (IOException e) {
                                    System.err.println(e.toString());
                                }
                            }
                        }

                        socket.close();

                    } catch (SocketException | UnsupportedEncodingException e) {
                        System.err.println(e.toString());
                    }
                });
            }

            workers.shutdown();

            try {
                workers.awaitTermination(threads * msgCount, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println(e.toString());
            }

        } catch (UnknownHostException e) {
            System.err.println("Cannot find host: " + host + "\n" + e.toString());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Five arguments excepted");
            return;
        }

        for (String arg : args) {
            if(arg == null) {
                System.err.println("Non-null arguments expected");
                return;
            }
        }

        try {

            new HelloUDPClient().run( args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));

        } catch (NumberFormatException e) {
            System.err.println(e.toString());
        }
    }
}
