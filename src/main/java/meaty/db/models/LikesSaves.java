package meaty.db.models;

import javax.persistence.*;

import meaty.db.types.LikesSavesType;

@Entity
@Table(name = "likes_saves")
public class LikesSaves {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tweet_id", nullable = false)
    private Tweet tweet;

    @Column(nullable = false)
    private LikesSavesType type;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Tweet getTweet() {
        return tweet;
    }

    public void setTweet(Tweet tweet) {
        this.tweet = tweet;
    }

    public LikesSavesType getType() {
        return type;
    }

    public void setType(LikesSavesType type) {
        this.type = type;
    }
}
