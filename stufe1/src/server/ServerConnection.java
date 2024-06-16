package server;

import model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import static server.Server.openConnections;
import static server.Server.queues;
import static util.SerializerUtil.deserialize;
import static util.SerializerUtil.serialize;

public class ServerConnection implements Runnable {
    private volatile String name;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
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
        try {
            name = in.readLine();
            openConnections.put(name, this);
            System.out.printf("Server: connected to %s%n", name);
            sendPendingMessages();

            String line;
            while ((line = in.readLine()) != null) {
                Message msg = deserialize(line);
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
        String msgStr = serialize(msg);
        out.println(msgStr);
    }

    private void onClose() {
        openConnections.remove(name);
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
