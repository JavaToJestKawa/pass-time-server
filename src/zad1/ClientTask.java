package zad1;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ClientTask extends FutureTask<String> {
    private final Client client;
    private final List<String> requests;
    private final boolean showRes;

    private ClientTask(Client client, List<String> requests, boolean showRes) {
        super(new Callable<String>() {
            @Override
            public String call() throws Exception {
                StringBuilder sb = new StringBuilder();

                client.connect();
                String loginRes = client.send("login " + client.id);

                for (String req : requests) {
                    String res = client.send(req);

                    if (showRes) {
//                        String[] lines = res.split("\n", -1);
//                        String resModified = String.join("\n", Arrays.copyOfRange(lines, 2, lines.length));
                        System.out.println(res);
                    }
                }

                String clog = client.send("bye and log transfer");
//                System.out.println(clog);

                return clog;
            }
        });

        this.client = client;
        this.requests = requests;
        this.showRes = showRes;
    }

    public static ClientTask create(Client client, List<String> requests, boolean showRes) {
        return new ClientTask(client, requests, showRes);
    }
}
