package server;

import model.Message;
import util.SerializerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerConnection implements Runnable {
    private final String name;
    private final Socket socket;

    public ServerConnection(Socket socket) throws IOException {
        this.socket = socket;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            name = in.readLine();
        }
        Server.openConnections.put(name, this);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = SerializerUtil.deserialize(line);
                //TODO handle message
            }
            onClose();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onClose() throws IOException {
        Server.openConnections.remove(name);
        if (!socket.isClosed()) {
            socket.close();
        }
    }
}
