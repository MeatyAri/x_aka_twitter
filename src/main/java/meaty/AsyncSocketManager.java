package meaty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.google.gson.Gson;
// import com.google.gson.JsonObject;

import meaty.handlers.auth.Auth;
import meaty.handlers.profile.ProfileHandler;
import meaty.handlers.tweets.TweetHandler;
import meaty.protocol.*;


public class AsyncSocketManager {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final int port;
    private boolean DEBUG;

    private static final Gson gson = new Gson();


    public AsyncSocketManager(int port, boolean DEBUG) {
        this.port = port;
        this.DEBUG = DEBUG;
    }

    public AsyncSocketManager(int port) {
        this.port = port;
        this.DEBUG = false;
    }

    public void startServer() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started on port " + port);

        // Loop to handle I/O events
        while (true) {
            // Wait for events
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                // Handle new client connections
                if (key.isAcceptable()) {
                    acceptClient(key);
                }

                // Handle data read from clients
                if (key.isReadable()) {
                    try {
                        handleRead(key);
                    } catch (IOException e) {
                        System.out.println("Error reading from client: " + e.getMessage());;
                    }
                }

                iterator.remove();
            }
        }
    }

    private void acceptClient(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4));  // Buffer for message length
        System.out.println("Accepted new connection from " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer lengthBuffer = (ByteBuffer) key.attachment();

        if (lengthBuffer.hasRemaining()) {
            int bytesRead = socketChannel.read(lengthBuffer);
            if (bytesRead == -1) {
                socketChannel.close();
                System.out.println("Client disconnected");
                return;
            }
            if (!lengthBuffer.hasRemaining()) {
                lengthBuffer.flip();
                int messageLength = lengthBuffer.getInt();
                ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
                bytesRead = socketChannel.read(messageBuffer);
                if (bytesRead == -1) {
                    socketChannel.close();
                    System.out.println("Client disconnected");
                    return;
                }
                if (!messageBuffer.hasRemaining()) {
                    messageBuffer.flip();
                    String message = StandardCharsets.UTF_8.decode(messageBuffer).toString();
                    if (DEBUG) {
                        System.out.println("Received: " + message);
                    }
                    String response = handleRequest(message);
                    sendMessage(socketChannel, response);
                    lengthBuffer.clear();
                }
            }
        }
    }

    private static String handleRequest(String message) {
        Request request = gson.fromJson(message, Request.class);
        Response response;

        switch (request.getType()) {
            case LOGIN:
                response = Auth.login(request.getData());
                break;
            case SIGNUP:
                response = Auth.signup(request.getData());
                break;
            case CREATE_TWEET:
                response = TweetHandler.createTweet(request.getData());
                break;
            case GET_TWEETS:
                response = TweetHandler.getTweets(request.getData());
                break;
            case LIKE_UNLIKE_TWEET:
                response = TweetHandler.likeUnlikeTweet(request.getData());
                break;
            case SAVE_UNSAVE_TWEET:
                response = TweetHandler.saveUnsaveTweet(request.getData());
                break;
            case GET_PROFILE:
                response = ProfileHandler.getProfile(request.getData());
                break;
            case FOLLOW:
                response = ProfileHandler.follow(request.getData());
                break;
            case UNFOLLOW:
                response = ProfileHandler.unfollow(request.getData());
                break;
            default:
                response = error404();
                break;
        }

        response.setId(request.getId());
        return gson.toJson(response);
    }

    private static Response error404() {
        Response error = new Response();
        error.setStatus(404);
        return error;
    }

    private static void sendMessage(SocketChannel socketChannel, String message) throws IOException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + messageBytes.length);
        buffer.putInt(messageBytes.length);
        buffer.put(messageBytes);
        buffer.flip();
        socketChannel.write(buffer);
    }
}
