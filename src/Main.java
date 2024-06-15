import server.Server;

public class Main {
    public static final int PORT = 42069;

    public static void main(String[] args) {
        Server.listen(PORT);
        //TODO Demo
        Server.stop();
    }
}