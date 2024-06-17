import client.Client;
import server.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int PORT = 54321;

    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            executor.execute(() -> Server.listen(PORT));

            // Initialisierung & Start
            Client alice = new Client(ALICE, PORT);
            Client bob = new Client(BOB, PORT);
            executor.execute(alice);
            executor.execute(bob);
            Thread.sleep(500);

            // synchrone Kommunikation
            alice.sendMessage("Hallo Bob!", BOB);
            Thread.sleep(500);
            bob.sendMessage("Hallo Alice!", ALICE);
            Thread.sleep(500);

            // asynchrone Kommunikation
            alice.stop();
            Thread.sleep(500);

            bob.sendMessage("Noch da?", ALICE);
            Thread.sleep(500);
            bob.sendMessage("Noch da?!", ALICE);
            Thread.sleep(500);

            alice.start();
            Thread.sleep(500);
            executor.execute(alice);

            // Stop
            Thread.sleep(500);
            alice.stop();
            bob.stop();
            Server.stop();
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            System.err.printf("Main: error occured: %s%n", e.getMessage());
        }
    }
}
