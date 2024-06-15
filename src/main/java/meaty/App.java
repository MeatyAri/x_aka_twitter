package meaty;

import java.io.IOException;


public class App {
    public static void main(String[] args) {
        AsyncSocketManager server = new AsyncSocketManager(5000, true);
        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
