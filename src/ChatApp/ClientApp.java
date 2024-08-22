package ChatApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientApp {
    private Socket client;
    private BufferedReader in;
    private volatile PrintWriter out;

    private volatile boolean isConnected;
    private final String host;
    private final int port;

    public ClientApp(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        try {
            client = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);
            isConnected = true;

            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (UnknownHostException e) {
            System.out.println("Client, Server connection error, " + e.getLocalizedMessage());
        } catch (IOException e) {
            System.out.println("Client I/O error, " + e.getLocalizedMessage());
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null && !client.isClosed()) client.close();
            isConnected = false;
        } catch (Exception e) {
            System.out.println("Client shutdown failed, " + e.getLocalizedMessage());
        }
    }

    public class InputHandler implements Runnable {

        @Override
        public void run() {
            try (BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in))) {

                while (isConnected) {
                    String message = inReader.readLine();
                    if (message.contains("/quit"))
                    {
                        shutdown();
                        break;
                    }

                    out.println(message);
                }
            } catch (Exception e) {
                System.out.println("InputHandler error, " + e.getLocalizedMessage());
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        ClientApp client = new ClientApp("127.0.0.1", 9999);
        client.run();
    }
}
