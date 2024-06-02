import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import meaty.models.Post;
import meaty.models.User;

import java.util.Date;

public class dbTest {
    public static void main(String[] args) {
        // Create SessionFactory
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Post.class)
                .buildSessionFactory();

        // Create Session
        Session session = factory.getCurrentSession();

        try {
            // Create a user object
            User tempUser = new User();
            tempUser.setUsername("john_doe");
            tempUser.setPassword("password123");
            tempUser.setBirthDate(new Date());

            // Create a post object
            Post tempPost = new Post();
            tempPost.setContent("Hello, world!");
            tempPost.setUser(tempUser);

            // Start a transaction
            session.beginTransaction();

            // Save the user and post
            session.save(tempUser);
            session.save(tempPost);

            // Commit transaction
            session.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        }

        session = factory.getCurrentSession();
        try {            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            session.beginTransaction();

            // get the user by username
            User user = (User) session.createQuery("FROM User U WHERE U.username = :username")
                .setParameter("username", "john_doe")
                .uniqueResult();

            // get the post back
            Post post = user.getPosts().iterator().next();

            // Delete both objects
            session.delete(user);
            session.delete(post);
            session.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        } finally {
            factory.close();
        }
    }
}
