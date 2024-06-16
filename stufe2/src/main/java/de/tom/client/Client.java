package de.tom.client;

import de.tom.Messenger.*;

import java.io.*;
import java.net.Socket;

public class Client implements Runnable {
    private final ClientId id;
    private final int port;
    private volatile Socket socket;
    private volatile InputStream in;
    private volatile OutputStream out;

    private volatile boolean isRunning = false;
    private volatile boolean shouldClose = false;

    public Client(String name, int port) throws IOException {
        this.id = ClientId.newBuilder()
                .setName(name)
                .build();
        this.port = port;
        start();
    }

    public void start() throws IOException {
        if (isRunning) {
            System.err.printf("Client %s is already running%n", id.getName());
            return;
        }
        socket = new Socket("localhost", port);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        id.writeDelimitedTo(out);
        shouldClose = false;
        System.out.printf("Client %s started%n", id.getName());
    }

    @Override
    public void run() {
        if (isRunning) {
            System.err.printf("Client %s is already running%n", id.getName());
            return;
        }
        isRunning = true;
        try {
            Message message;
            while (!shouldClose && (message = Message.parseDelimitedFrom(in)) != null) {
                System.out.printf("Client %s received message from %s: %s%n", id.getName(), message.getFrom().getName(), message.getText());
            }
        } catch (IOException e) {
            if (!shouldClose) {
                System.err.printf("Client %s: error reading from socket: %s%n", id.getName(), e.getMessage());
            }
        } finally {
            onClose();
            isRunning = false;
            System.out.printf("Client %s stopped%n", id.getName());
        }
    }

    private void onClose() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            System.err.printf("Client %s: error closing connection: %s%n", id.getName(), e.getMessage());
        }
    }

    public void stop() {
        shouldClose = true;
        onClose();
    }

    public void sendMessage(String text, String to) {
        ClientId toId = ClientId.newBuilder()
                .setName(to)
                .build();
        Message message = Message.newBuilder()
                .setFrom(id)
                .setTo(toId)
                .setText(text)
                .build();
        try {
            message.writeDelimitedTo(out);
        } catch (IOException e) {
            System.err.printf("Client %s: error writing to socket: %s%n", id.getName(), e.getMessage());
        }
    }
}
