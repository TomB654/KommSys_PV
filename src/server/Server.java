package server;

import model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    public static final int PORT = 42069;

    public static final ConcurrentMap<String, Queue<Message>> queues = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, ServerConnection> openConnections = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.printf("server.Server: listening on port %s%n", serverSocket.getLocalPort());
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("server.Server: connection established with port %s%n", socket.getPort());
                    executor.execute(new ServerConnection(socket));
                } catch (IOException e) {
                    System.out.printf("server.Server: error accepting connection: %s%n", e.getMessage());
                }
            }
        }
//        } catch (IOException e) {
//            System.out.printf("server.Server: error opening connection: %s%n", e.getMessage());
//        }
    }
}
