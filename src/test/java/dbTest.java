import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Date;

import meaty.db.HibernateUtil;
import meaty.db.models.*;
import meaty.db.types.*;

public class dbTest {
    public static void main(String[] args) {
        SessionFactory factory = HibernateUtil.getSessionFactory();

        // Create Session
        Session session = factory.openSession();

        try {
            // Create a user object
            User tempUser = new User();
            tempUser.setUsername("john_doe");
            tempUser.setPassword("password123");
            tempUser.setBirthDate(new Date());

            // Create a tweet object
            Tweet tempTweet = new Tweet();
            tempTweet.setContent("Hello, world!");
            tempTweet.setUser(tempUser);

            LikesSaves likesSaves = new LikesSaves();
            likesSaves.setUser(tempUser);
            likesSaves.setTweet(tempTweet);
            likesSaves.setType(LikesSavesType.LIKE);

            // Start a transaction
            session.beginTransaction();

            // Save the user and tweet
            session.save(tempUser);
            session.save(tempTweet);
            session.save(likesSaves);

            // Commit transaction
            session.getTransaction().commit();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        }

        session = factory.openSession();
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

            // // get the tweet back
            // Tweet tweet = user.getTweets().iterator().next();

            // Delete both objects
            session.delete(user);
            // session.delete(tweet);
            session.getTransaction().commit();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        } finally {
            factory.close();
        }
    }
}

