package meaty.handlers.tweets;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import meaty.db.HibernateUtil;
import meaty.protocol.*;
import meaty.db.models.*;
import meaty.db.types.*;
import meaty.handlers.auth.*;

public class TweetHandler {
    private static SessionFactory factory = HibernateUtil.getSessionFactory();
    private static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .registerTypeAdapter(Date.class, new DateSerializer())
                .create();

    public static Response createTweet(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();
        Transaction t = null;

        try {
            Tweet tweet = gson.fromJson(data, Tweet.class);
            tweet.setLikes(0);
            User user = Auth.getUserByToken(data.get("token").getAsString());
            tweet.setUser(user);

            if (user == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token");
                response.setData(respData);
                return response;
            }

            t = session.beginTransaction();
            session.save(tweet);
            t.commit();
            session.close();

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Tweet created successfully");
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

    public static Response getTweets(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();
        List<Tweet> tweets = new ArrayList<>();

        try {
            String token = data.get("token").getAsString();
            User user = Auth.getUserByToken(token);

            if (user == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token");
                response.setData(respData);
                return response;
            }

            JsonElement usernameElement = data.get("username");
            String username = usernameElement == null ? null : usernameElement.getAsString();
            JsonElement bookmarksElement = data.get("bookmarks");
            boolean bookmarks = bookmarksElement == null ? false : bookmarksElement.getAsBoolean();

            if (username == null && !bookmarks) {
                tweets.addAll(session.createQuery("from Tweet ORDER BY rand()", Tweet.class).setMaxResults(10).getResultList());

            } else if (bookmarks) {
                tweets.addAll(
                    session
                    .createQuery("from Tweet T where T.id in (select B.tweet.id from LikesSaves B where B.user.id = :user_id and B.type = :type)", Tweet.class)
                    .setParameter("user_id", user.getId())
                    .setParameter("type", LikesSavesType.SAVE)
                    .getResultList()
                );
            } else {
                tweets.addAll(
                    session
                    .createQuery("from Tweet where user.username = :username", Tweet.class)
                    .setParameter("username", username)
                    .getResultList()
                );
            }
            
            response.setStatus(200);
            JsonObject respData = new JsonObject();
            JsonArray respArray = new JsonArray();
            for (Tweet tweet : tweets) {
                Set<LikesSaves> likesSaves = tweet.getLikesSaves();
                boolean likedByUser = likesSaves.stream().anyMatch(l -> l.getType() == LikesSavesType.LIKE && l.getUser().getId() == user.getId());
                boolean savedByUser = likesSaves.stream().anyMatch(l -> l.getType() == LikesSavesType.SAVE && l.getUser().getId() == user.getId());

                JsonObject tweetData = new JsonObject();
                tweetData.addProperty("id", tweet.getId());
                tweetData.addProperty("content", tweet.getContent());
                tweetData.addProperty("username", tweet.getUser().getUsername());
                tweetData.addProperty("likes", tweet.getLikes());
                tweetData.addProperty("likedByUser", likedByUser);
                tweetData.addProperty("savedByUser", savedByUser);
                respArray.add(tweetData);
            }
            respData.add("tweets", respArray);
            response.setData(respData);
            session.close();
            return response;

        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setStatus(401);
        return response;
    }

    public static Response likeUnlikeTweet(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();
        Transaction t = null;

        try {
            LikesSaves tweetLikesSaves = gson.fromJson(data, LikesSaves.class);
            tweetLikesSaves.setType(LikesSavesType.LIKE);

            Tweet tweet = session.get(Tweet.class, data.get("tweetId").getAsLong());
            tweetLikesSaves.setTweet(tweet);

            User user = Auth.getUserByToken(data.get("token").getAsString());
            tweetLikesSaves.setUser(user);

            if (user == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token");
                response.setData(respData);
                return response;
            }

            LikesSaves record = tweet.getLikesSaves().stream().filter(l -> l.getUser().getId() == user.getId() && l.getType() == LikesSavesType.LIKE).findFirst().orElse(null);

            t = session.beginTransaction();

            if (record != null) {
                tweet.setLikes(tweet.getLikes() - 1);
                session.delete(record);
            } else {
                tweet.setLikes(tweet.getLikes() + 1);
                session.save(tweetLikesSaves);
            }

            session.update(tweet);
            t.commit();
            session.close();

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Tweet liked/unliked successfully");
            respData.addProperty("likes", tweet.getLikes());
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

    public static Response saveUnsaveTweet(JsonObject data) {
        Response response = new Response();
        Session session = factory.openSession();
        Transaction t = null;

        try {
            LikesSaves tweetLikesSaves = gson.fromJson(data, LikesSaves.class);
            tweetLikesSaves.setType(LikesSavesType.SAVE);

            Tweet tweet = session.get(Tweet.class, data.get("tweetId").getAsLong());
            tweetLikesSaves.setTweet(tweet);

            User user = Auth.getUserByToken(data.get("token").getAsString());
            tweetLikesSaves.setUser(user);
            if (user == null) {
                response.setStatus(401);
                JsonObject respData = new JsonObject();
                respData.addProperty("content", "Invalid token");
                response.setData(respData);
                return response;
            }

            LikesSaves record = tweet.getLikesSaves().stream().filter(l -> l.getUser().getId() == user.getId() && l.getType() == LikesSavesType.SAVE).findFirst().orElse(null);

            t = session.beginTransaction();

            if (record != null) {
                session.delete(record);
            } else {
                session.save(tweetLikesSaves);
            }

            t.commit();
            session.close();

            response.setStatus(200);
            JsonObject respData = new JsonObject();
            respData.addProperty("content", "Tweet saved/unsaved successfully");
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
}
