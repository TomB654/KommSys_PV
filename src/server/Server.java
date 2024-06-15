package server;

import model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    public static final ConcurrentMap<String, Queue<Message>> queues = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, ServerConnection> openConnections = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static volatile boolean shouldStop = false;
    private static Thread currentThread;

    public static void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Server: listening on port %s%n", serverSocket.getLocalPort());
            currentThread = Thread.currentThread();
            while (!shouldStop) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Server: connection established with port %s%n", socket.getPort());
                    executor.execute(new ServerConnection(socket));
                } catch (IOException e) {
                    System.err.printf("Server: error accepting connection: %s%n", e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.printf("Server: error opening connection: %s%n", e.getMessage());
        }
    }

    public static void stop() {
        shouldStop = true;
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }
}
