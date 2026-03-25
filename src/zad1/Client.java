package zad1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class Client {
    final String id;
    private final String host;
    private final int port;
    private SocketChannel socketChannel;
    private Selector selector;

    public Client(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void connect() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String send(String req) {
        try {
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
                socketChannel.register(selector, SelectionKey.OP_READ);
            }

            socketChannel.write(ByteBuffer.wrap(req.getBytes(StandardCharsets.UTF_8)));

            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> it = keys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (key.isReadable()) {
                    ByteBuffer buffer = ByteBuffer.allocate(4096);
                    int readBytes = socketChannel.read(buffer);

                    if (readBytes > 0) {
                        buffer.flip();
                        return StandardCharsets.UTF_8.decode(buffer).toString();
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}
