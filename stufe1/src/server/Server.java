package server;

import model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final ConcurrentMap<String, Queue<Message>> queues = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, ServerConnection> openConnections = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static volatile ServerSocket serverSocket;
    private static volatile boolean shouldStop = false;

    public static void listen(int port) {
        shouldStop = false;
        try {
            serverSocket = new ServerSocket(port);
            System.out.printf("Server: listening on port %s%n", serverSocket.getLocalPort());
            while (!shouldStop) {
                Socket socket = serverSocket.accept();
                System.out.printf("Server: connection established with port %s%n", socket.getPort());
                executor.execute(new ServerConnection(socket));
            }
        } catch (IOException e) {
            if (!shouldStop) {
                System.err.printf("Server: error opening connection: %s%n", e.getMessage());
            }
        } finally {
            executor.shutdown();
            System.out.println("Server: stopped");
        }
    }

    public static void stop() {
        shouldStop = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.printf("Server: error closing connection: %s%n", e.getMessage());
        }
    }
}
