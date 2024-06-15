import com.google.gson.Gson;
import com.google.gson.JsonObject;

import meaty.protocol.*;

public class JsonResponsesTest {

    public static void main(String[] args) {
        Response response = new Response();
        response.setStatus(200);
        JsonObject data = new JsonObject();
        data.addProperty("key", "value");
        response.setData(data);

        Gson gson = new Gson();
        String resp1 = gson.toJson(response);
        String resp2 = gson.toJsonTree(response).getAsJsonObject().toString();
        System.out.println(resp1);
        System.out.println(resp2);
        System.out.println(resp1.equals(resp2));

        JsonObject json = gson.fromJson(resp1, JsonObject.class);
        System.out.println(json);
    }
}
