package meaty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;


public class AsyncSocketManager {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final int port;

    public AsyncSocketManager(int port) {
        this.port = port;
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
                    readFromClient(key);
                }

                iterator.remove();
            }
        }
    }

    private void acceptClient(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted new connection from " + clientChannel.getRemoteAddress());
    }

    private void readFromClient(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            clientChannel.close();
            System.out.println("Connection closed by client");
            return;
        }

        String message = new String(buffer.array()).trim();
        System.out.println("Received message: " + message);

        // Echo the message back to the client (you can replace this with your own logic)
        buffer.flip();
        clientChannel.write(buffer);
    }
}
