import java.security.SecureRandom;
import java.util.Base64;

public class genTokenTest {
    public static String generateRandomToken() {
        byte[] randomBytes = new byte[20]; // 20 bytes = 160 bits
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().encodeToString(randomBytes);
    }

    public static void main(String[] args) {
        System.out.println(generateRandomToken());
    }
}
