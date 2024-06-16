package client;

import model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static util.SerializerUtil.deserialize;
import static util.SerializerUtil.serialize;

public class Client implements Runnable {
    /**
     * Must be unique.
     */
    private final String name;
    private final int port;
    private volatile Socket socket;
    private volatile BufferedReader in;
    private volatile PrintWriter out;

    private volatile boolean isRunning = false;
    private volatile boolean shouldClose = false;

    public Client(String name, int port) throws IOException {
        this.name = name;
        this.port = port;
        start();
    }

    public void start() throws IOException {
        if (isRunning) {
            System.err.printf("Client %s is already running%n", name);
            return;
        }
        socket = new Socket("localhost", port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        out.println(name);
        shouldClose = false;
        System.out.printf("Client %s started%n", name);
    }

    @Override
    public void run() {
        if (isRunning) {
            System.err.printf("Client %s is already running%n", name);
            return;
        }
        isRunning = true;
        try {
            String line;
            while (!shouldClose && (line = in.readLine()) != null) {
                Message message = deserialize(line);
                System.out.printf("Client %s received message from %s: %s%n", name, message.from(), message.text());
            }
        } catch (IOException e) {
            if (!shouldClose) {
                System.err.printf("Client %s: error reading from socket: %s%n", name, e.getMessage());
            }
        } finally {
            onClose();
            isRunning = false;
            System.out.printf("Client %s stopped%n", name);
        }
    }

    private void onClose() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            System.err.printf("Client %s: error closing connection: %s%n", name, e.getMessage());
        }
    }

    public void stop() {
        shouldClose = true;
        onClose();
    }

    public void sendMessage(String msg, String to) {
        Message message = new Message(name, to, msg);
        out.println(serialize(message));
    }
}
