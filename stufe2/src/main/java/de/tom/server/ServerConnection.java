package de.tom.server;

import de.tom.Messenger.*;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import static de.tom.server.Server.*;

public class ServerConnection implements Runnable {
    private volatile ClientId id;
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    private void sendPendingMessages() {
        if (queues.containsKey(id)) {
            for (Message msg : queues.get(id)) {
                writeMessage(msg);
            }
        }
    }

    @Override
    public void run() {
        try {
            id = ClientId.parseDelimitedFrom(in);
            openConnections.put(id, this);
            System.out.printf("Server: connected to %s%n", id.getName());
            sendPendingMessages();

            Message msg;
            while ((msg = Message.parseDelimitedFrom(in)) != null) {
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
        if (openConnections.containsKey(msg.getTo())) {
            openConnections.get(msg.getTo()).writeMessage(msg);
            return;
        }

        //Empfänger offline
        synchronized (queues) {
            // Message-Queue für Empfänger existiert bereits
            if (queues.containsKey(msg.getTo())) {
                queues.get(msg.getTo()).add(msg);
                return;
            }

            // Message-Queue existiert noch nicht
            Queue<Message> queue = new LinkedList<>();
            queue.add(msg);
            queues.put(msg.getTo(), queue);
        }
    }

    private synchronized void writeMessage(Message msg) {
        try {
            msg.writeDelimitedTo(out);
        } catch (IOException e) {
            System.err.printf("Server: error writing to client: %s%n", id.getName());
        }
    }

    private void onClose() {
        openConnections.remove(id);
        if (!socket.isClosed()) {
            try {
                socket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                System.err.printf("Server: error closing connection: %s%n", e.getMessage());
            }
        }
    }
}
