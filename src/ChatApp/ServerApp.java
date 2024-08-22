package ChatApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private final ExecutorService pool;

    private final int port;
    private boolean serverDown;

    public ServerApp(int port) {
        this.port = port;
        connections = new ArrayList<>();
        pool = Executors.newCachedThreadPool();
    }

    public void run() {
        try {
            server = new ServerSocket(port);
            serverDown = false;
            System.out.println("Server is up and running: " + !serverDown);
            while (!serverDown) {
                ConnectionHandler handler = new ConnectionHandler(server.accept());
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void shutdown() {
        try {
            if (!server.isClosed())
                server.close();
            serverDown = true;

            for (ConnectionHandler conch: connections)
                conch.shutdown();

            if (!pool.isShutdown())
                pool.shutdown();
        } catch (Exception e) {
            System.out.println("Server got shut down!");
        }
    }

    public void broadcast(String message, Socket exceptClient) {
        System.out.println(message);
        for (ConnectionHandler conch: connections)
            if (conch.credentialsSatisfied && conch.client != exceptClient)
                conch.sendMessage(message);
    }

    public class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;

        private boolean credentialsSatisfied;
        private String userName;

        public ConnectionHandler(Socket client) {
            this.client = client;
            credentialsSatisfied = false;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);

                askForCredentials();

                broadcast(userName + " got connected!", client);
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.contains("/quit")) {
                        broadcast(userName + " has left the chat.", client);
                        out.println("You got disconnected!!!");
                        shutdown();
                        return;
                    }

                    broadcast(userName + ": " + message, client);
                }
            } catch (Exception e) {
                criticalMessage("Client handler failed, " + e.getLocalizedMessage());
                shutdown();
            }
        }

        private void askForCredentials() throws IOException {
            out.println("Commands: \n/quit: to quit.");
            do {
                out.println("Enter credentials to enter the chat!");
                out.println("In-chat name: ");
                userName = in.readLine();
            } while (userName.isEmpty() || userName.equals("/quit"));

            credentialsSatisfied = true;
        }
        public void shutdown() {
            try {
                in.close();
                out.close();
                if (client.isClosed())
                    client.close();
            } catch (Exception e) {
                criticalMessage("Client Shutdown failed, " + e.getLocalizedMessage());
            }
        }

        public void criticalMessage(String message) {
            if (out != null)
                out.println(message);
            else
                System.out.println(message);
        }
        public void sendMessage(String message) {
            out.println(message);
        }
    }

    public static void main(String[] args) {
        ServerApp server = new ServerApp(9999);
        server.run();
    }
}
