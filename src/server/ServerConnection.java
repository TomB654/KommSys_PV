package server;

import model.Message;
import util.SerializerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import static server.Server.queues;
import static server.Server.openConnections;

public class ServerConnection implements Runnable {
    private final String name;
    private final Socket socket;

    public ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            name = in.readLine();
        }
        openConnections.put(name, this);
        sendPendingMessages();
    }

    private void sendPendingMessages() {
        if (queues.containsKey(name)) {
            for (Message msg : queues.get(name)) {
                writeMessage(msg);
            }
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = SerializerUtil.deserialize(line);
                handleMessage(msg);
            }
        } catch (IOException e) {
            System.err.printf("Server: error reading from socket: %s%n", e.getMessage());
        } finally {
            onClose();
        }
    }

    private void handleMessage(Message msg) {
        // Empfänger online
        if (openConnections.containsKey(msg.to())) {
            openConnections.get(msg.to()).writeMessage(msg);
            return;
        }

        //Empfänger offline
        synchronized (queues) {
            // Message-Queue für Empfänger existiert bereits
            if (queues.containsKey(msg.to())) {
                queues.get(msg.to()).add(msg);
                return;
            }

            // Message-Queue existiert noch nicht
            Queue<Message> queue = new LinkedList<>();
            queue.add(msg);
            queues.put(msg.to(), queue);
        }
    }

    private synchronized void writeMessage(Message msg) {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String msgStr = SerializerUtil.serialize(msg);
            out.println(msgStr);
        } catch (IOException e) {
            System.err.printf("Server: error writing to socket: %s%n", e.getMessage());
        }
    }

    private void onClose() {
        openConnections.remove(name);
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.printf("Server: error closing socket: %s%n", e.getMessage());
            }
        }
    }
}
