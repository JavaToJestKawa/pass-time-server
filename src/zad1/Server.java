package zad1;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Server implements Runnable {
    private final List<String> serverLog = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> clientLogs = Collections.synchronizedMap(new HashMap<>());
    //    private final Map<String, List<String>> clientLogs = new HashMap<>();
//    private final Map<String, String> clientLogs = new HashMap<>();
    private final Map<SocketChannel, String> clientIds = new HashMap<>();
    private final Map<SocketChannel, List<String>> clientRequests = new HashMap<>();
    private Thread serverThread;
    private String host;
    private int port;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private volatile boolean running;
    private volatile boolean ready = false;
//    private final CountDownLatch readySignal = new CountDownLatch(1);

    public Server(String host, int port) {
        Locale.setDefault(new Locale("pl", "PL"));
        this.host = host;
        this.port = port;
    }

    public void startServer() {
        running = true;
        serverThread = new Thread(this);
        serverThread.start();

        while (!isReady()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //    public void stopServer() {
//        running = false;
//        if (selector != null) {
//            try {
//                selector.wakeup();
//                selector.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        if (serverSocketChannel != null) {
//            try {
//                serverSocketChannel.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        if (serverThread != null) {
//            serverThread.interrupt();
//        }
//    }
    public void stopServer() {
        if (!running) {
            return;
        }

        running = false;

        if (serverThread != null && serverThread.isAlive()) {
            try {
                serverThread.interrupt();
                serverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (selector != null) {
            try {
                selector.wakeup();
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    String getServerLog() {
//        logs
        StringBuilder stringBuilder = new StringBuilder();
        for (String log : serverLog) {
            stringBuilder.append(log).append('\n');
        }
        return stringBuilder.toString();
    }

    public void run() {
        try {
//            System.out.println("Elo");
            serverSocketChannel = ServerSocketChannel.open();
            selector = Selector.open();

            serverSocketChannel.bind(new InetSocketAddress(host, port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//            readySignal.countDown();


            ready = true;

            while (running) {
                if (!selector.isOpen()) {
                    break;
                }

                try {
                    selector.select();
                } catch (ClosedSelectorException e) {
                    break;
                }

                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                for (SelectionKey selectionKey : selectionKeys) {
                    if (selectionKey.isAcceptable()) {
//                        System.out.println("Acceptable");
                        acceptClientKey(selectionKey);
                    }
                    if (selectionKey.isReadable()) {
//                        System.out.println("Readable");
                        readClientInput(selectionKey);
                    }
                }

                selectionKeys.clear();
            }
        } catch (IOException e) {
            if (running) {
                throw new RuntimeException("Server error", e);
            } else {
//                System.out.println("Server stopped.");
            }
        } finally {
            stopServer();
        }
    }

    private void acceptClientKey(SelectionKey key) throws IOException {
//        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void readClientInput(SelectionKey selectionKey) throws IOException {
        SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int bytesRead;
        while ((bytesRead = clientChannel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            baos.write(bytes);
            buffer.clear();
        }

        if (bytesRead == -1) {
            selectionKey.cancel();
            clientChannel.close();
            return;
        }

        String clientRequest = new String(baos.toByteArray(), "UTF-8").trim();
        if (!clientRequest.isEmpty()) {
            processClientRequest(clientChannel, clientRequest);
        }
    }


    private void processClientRequest(SocketChannel clientChannel, String clientRequest) {
        String clientId;
        String toSend = "";

        if (clientRequest.startsWith("login ")) {
            clientId = clientRequest.replace("login ", "");
            clientIds.put(clientChannel, clientId);
            clientLogs.put(clientId, "");

            serverLog.add(clientId + " logged in at " + getHour());
            appendToClientLog(clientId, "=== " + clientId + " log start ===\nlogged in\n");
            toSend = "=== " + clientId + " log start ===\nlogged in";

        } else if (clientRequest.equals("bye and log transfer") && clientIds.containsKey(clientChannel)) {
            clientId = clientIds.get(clientChannel);

            serverLog.add(clientId + " logged out at " + getHour());
            appendToClientLog(clientId, "logged out\n=== " + clientId + " log end ===\n");

            toSend = clientLogs.get(clientId);

        } else if (clientIds.containsKey(clientChannel)) {
            clientId = clientIds.get(clientChannel);

            serverLog.add(clientId + " request at " + getHour() + ": \"" + clientRequest + "\"");

            String[] d = clientRequest.split(" +");
            if (d.length >= 2) {
                String info = Time.passed(d[0], d[1]);

                String entryForLog = "Request: " + clientRequest + "\nResult:\n" + info + "\n";
                appendToClientLog(clientId, entryForLog);

                toSend = info;
            } else {
                String entry = "Malformed request: " + clientRequest + "\n";
                appendToClientLog(clientId, entry);
                toSend = entry;
            }
        } else {
            serverLog.add("??? Unidentified client ???");
            return;
        }

        try {
            ByteBuffer buf = ByteBuffer.wrap(toSend.getBytes());
            while (buf.hasRemaining()) {
                clientChannel.write(buf);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void appendToClientLog(String clientId, String entry) {
        String current = clientLogs.getOrDefault(clientId, "");
        clientLogs.put(clientId, current + entry);
    }

    private String getHour() {
        Locale.setDefault(new Locale("pl", "PL"));
        LocalDateTime timeNow = LocalDateTime.now();
        return DateTimeFormatter.ofPattern("H:mm:ss.SSS").format(timeNow);
    }

    public boolean isReady() {
        return ready;
    }
}
