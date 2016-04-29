package hyflow.caesar.replica;

import hyflow.benchmark.AbstractService;
import hyflow.caesar.Caesar;
import hyflow.caesar.DecideCallback;
import hyflow.common.ProcessDescriptor;
import hyflow.common.Request;
import hyflow.common.ThreadDispatcher;
import hyflow.main.ClientManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Queue;

public class Replica {

    private final static Logger logger = LogManager.getLogger(Replica.class);

    private final Caesar caesar;
    private final DecideCallback callback;
    private final AbstractService service;
    private final ThreadDispatcher deliverDispatcher;
    private ClientManager client;

    public Replica(AbstractService service, Caesar caesar) throws IOException {
        this.service = service;
        this.caesar = caesar;
        callback = new InnerDecideCallback();
        deliverDispatcher = new ThreadDispatcher("DeliveryThread", ProcessDescriptor.getInstance().numThreads);
    }

    public void start(ClientManager client) throws IOException {
        this.client = client;
        caesar.startCaesar(callback);
    }

    public void submit(Request request) {
        caesar.propose(request);
    }

    private class InnerDecideCallback implements DecideCallback {

        @Override
        public void deliver(final Request request, final Queue<Runnable> deliverQ) {
            deliverDispatcher.execute(() -> {
                service.executeRequest(request);
                caesar.onDelivery(request, deliverQ);
                client.notifyClient(request);
            });
        }
    }

}
