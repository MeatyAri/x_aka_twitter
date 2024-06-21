package meaty.handlers.profile;

import java.text.SimpleDateFormat;
// import java.util.Date;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.JsonElement;
// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import meaty.db.HibernateUtil;
import meaty.db.models.*;
import meaty.handlers.auth.*;
import meaty.protocol.Response;

public class ProfileHandler {
    private static SessionFactory factory = HibernateUtil.getSessionFactory();
    // private static final Gson gson = new GsonBuilder()
    //             .registerTypeAdapter(Date.class, new DateDeserializer())
    //             .registerTypeAdapter(Date.class, new DateSerializer())
    //             .create();

    public static Response getProfile(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();

        try {
            JsonElement selfElement = data.get("self");
            boolean self = selfElement == null ? false : selfElement.getAsBoolean();

            User user = Auth.getUserByToken(session, data.get("token").getAsString());

            User targetProfile;
            if (!self) {
                targetProfile = Auth.getUserByUsername(session, data.get("username").getAsString());
            } else {
                targetProfile = user;
            }
            self = user.getUsername().equals(targetProfile.getUsername());

            if (user == null || targetProfile == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token or username");
                response.setData(respData);
                return response;
            }

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Profile retrieved successfully");

            JsonObject profileData = new JsonObject();
            profileData.addProperty("self", self);
            if (!self) {
                profileData.addProperty("amIFollowing", user.getFollows().stream().anyMatch(f -> f.getFollowedUser().getId() == targetProfile.getId()));
                profileData.addProperty("isFollowingBack", targetProfile.getFollows().stream().anyMatch(f -> f.getFollowedUser().getId() == user.getId()));
            } else {
                profileData.addProperty("amIFollowing", false);
                profileData.addProperty("isFollowingBack", false);
            }
            profileData.addProperty("username", targetProfile.getUsername());
            profileData.addProperty("followers_count", targetProfile.getFollowersCount());
            profileData.addProperty("following_count", targetProfile.getFollowingCount());
            profileData.addProperty("email", targetProfile.getEmail());
            profileData.addProperty("phone", targetProfile.getPhone());
            profileData.add("birthDate", new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd").format(targetProfile.getBirthDate())));
            profileData.addProperty("bio", targetProfile.getBio());

            respData.add("profile", profileData);
            response.setData(respData);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            return response;
        } finally {
            session.close();
        }
    }

    public static Response follow(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();
        Transaction t = null;

        try {
            User user = Auth.getUserByToken(session, data.get("token").getAsString());
            User targetProfile = Auth.getUserByUsername(session, data.get("username").getAsString());

            if (user == null || targetProfile == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token or username");
                response.setData(respData);
                return response;
            }

            if (user.getFollows().stream().anyMatch(f -> f.getFollowedUser().getId() == targetProfile.getId())) {
                response.setStatus(400);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Already following");
                response.setData(respData);
                return response;
            }

            Follows follows = new Follows();
            follows.setUser(user);
            follows.setFollowedUser(targetProfile);

            user.setFollowingCount(user.getFollowingCount() + 1);
            targetProfile.setFollowersCount(targetProfile.getFollowersCount() + 1);

            t = session.beginTransaction();
            session.save(follows);
            session.update(user);
            session.update(targetProfile);
            t.commit();

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Followed successfully");
            response.setData(respData);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            if (t != null) {
                t.rollback();
            }
        } finally {
            session.close();
        }

        response.setStatus(500);
        JsonObject respData = new JsonObject();
        respData.addProperty("content", "Failed to follow due to internal server error");
        response.setData(respData);
        return response;
    }

    public static Response unfollow(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();
        Transaction t = null;

        try {
            User user = Auth.getUserByToken(session, data.get("token").getAsString());
            User targetProfile = Auth.getUserByUsername(session, data.get("username").getAsString());

            if (user == null || targetProfile == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token or username");
                response.setData(respData);
                return response;
            }

            Set<Follows> userFollows = user.getFollows();

            if (!userFollows.stream().anyMatch(f -> f.getFollowedUser().getId() == targetProfile.getId())) {
                response.setStatus(400);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Not following");
                response.setData(respData);
                return response;
            }

            Follows follows = userFollows.stream().filter(f -> f.getFollowedUser().getId() == targetProfile.getId()).findFirst().get();
            user.setFollowingCount(user.getFollowingCount() - 1);
            targetProfile.setFollowersCount(targetProfile.getFollowersCount() - 1);

            t = session.beginTransaction();
            session.delete(follows);
            session.update(user);
            session.update(targetProfile);
            t.commit();

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Unfollowed successfully");
            response.setData(respData);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            if (t != null) {
                t.rollback();
            }
        } finally {
            session.close();
        }

        response.setStatus(500);
        JsonObject respData = new JsonObject();
        respData.addProperty("content", "Failed to unfollow due to internal server error");
        response.setData(respData);
        return response;
    }
}
