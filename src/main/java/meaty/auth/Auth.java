package meaty.auth;

import java.util.Date;
import java.util.Base64;
import java.security.SecureRandom;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import meaty.db.HibernateUtil;
import meaty.db.models.User;
import meaty.protocol.*;


public class Auth {
    private static SessionFactory factory = HibernateUtil.getSessionFactory();
    private static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(Date.class, new DateSerializer())
                .create();

    public static Response login(JsonObject data) {
        Response response = new Response();
        
        User user_to_check = gson.fromJson(data, User.class);

        if (user_to_check.getToken() != null) {
            User user = getUserByToken(user_to_check.getToken());

            if (user == null || !user.getToken().equals(user_to_check.getToken())) {
                System.out.println("User token: " + user_to_check.getToken() + " user_to_check token: " + user_to_check.getToken());
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token");
                response.setData(respData);
                return response;
            }

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Already logged in");
            response.setData(respData);
            return response;
        }

        // else: login by password

        User user = getUserByUsername(user_to_check.getUsername());

        if (user == null || !BCrypt.checkpw(user_to_check.getPassword(), user.getPassword())) {
            response.setStatus(401);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Invalid username or password");
            response.setData(respData);
            return response;
        }
        
        Session session = factory.openSession();
        Transaction t = null;
        try {
            t = session.beginTransaction();
            user.setToken(generateRandomToken());
            session.update(user);
            t.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (t != null) {
                t.rollback();
            }
        }
        session.close();

        response.setStatus(200);
        JsonObject respData = new JsonObject();
        respData.addProperty("content", "Logged in successfully");
        respData.addProperty("token", user.getToken());
        response.setData(respData);

        return response;
    }

    public static Response signup(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();

        Transaction t = null;
        try {
            User user = gson.fromJson(data, User.class);
            user.setToken(generateRandomToken());

            t = session.beginTransaction();
            session.save(user);
            t.commit();
            session.close();

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Created your account successfully");
            respData.addProperty("token", user.getToken());
            response.setData(respData);
            return response;

        } catch (Exception e) {
            e.printStackTrace();

            if (t != null) {
                t.rollback();
            }
        }

        response.setStatus(401);
        return response;
    }

    public static User getUserByUsername(String username) {
        Session session = factory.openSession();

        try {
            // get the user by username
            User user = (User) session.createQuery("FROM User U WHERE U.username = :username")
                .setParameter("username", username)
                .uniqueResult();
            session.close();

            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static User getUserByToken(String token) {
        Session session = factory.openSession();

        try {
            // get the user by token
            User user = (User) session.createQuery("FROM User U WHERE U.token = :token")
                .setParameter("token", token)
                .uniqueResult();
            session.close();

            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateRandomToken() {
        byte[] randomBytes = new byte[20]; // 20 bytes = 160 bits
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().encodeToString(randomBytes);
    }
}
