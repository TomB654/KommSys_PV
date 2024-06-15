package client;

import model.Message;
import util.SerializerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {
    /**
     * Must be unique.
     */
    private final String name;
    private final Socket socket;

    private volatile boolean shouldClose = false;
    private Thread currentThread;

    public Client(String name, int port) throws IOException {
        this.name = name;
        socket = new Socket("localhost", port);
        writeName();
    }

    private void writeName() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(name);
        } catch (IOException e) {
            System.err.printf("Client %s: error writing to socket: %S%n", name, e.getMessage());
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            currentThread = Thread.currentThread();
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = SerializerUtil.deserialize(line);
                System.out.printf("Client %s received message: %s%n", name, msg);
            }
        } catch (IOException e) {
            if (!shouldClose) {
                System.err.printf("Client %s: error reading from socket: %s%n", name, e.getMessage());
            }
        } finally {
            onClose();
        }
    }

    private void onClose() {
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.printf("Client %s: error closing socket: %s%n", name, e.getMessage());
            }
        }
    }

    public void close() {
        shouldClose = true;
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

    public void writeMessage(String msg, String to) {
        Message message = new Message(name, to, msg);
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(SerializerUtil.serialize(message));
        } catch (IOException e) {
            System.err.printf("Client %s: error writing to socket: %S%n", name, e.getMessage());
        }
    }
}
