package meaty;

import meaty.models.User;
import meaty.models.Post;

import java.io.IOException;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


public class App {
    private static final int port = 12345;

    public static void main(String[] args) {
        // Create SessionFactory
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Post.class)
                .buildSessionFactory();

        AsyncSocketManager server = new AsyncSocketManager(port);
        try {
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}