package csc435.app;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZMQProxyWorker implements Runnable {
    private final ZContext context;
    private final int serverPort;

    public ZMQProxyWorker(ZContext context, int serverPort) {
        this.context = context;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            ZMQ.Socket router = context.createSocket(SocketType.ROUTER);
            ZMQ.Socket dealer = context.createSocket(SocketType.DEALER);

            router.bind("tcp://*:" + serverPort);
            dealer.bind("inproc://backend"); // Internal communication

            System.out.println("Proxy running on port " + serverPort);

            while (!Thread.currentThread().isInterrupted()) {
                ZMQ.proxy(router, dealer, null);
            }

            router.close();
            dealer.close();
        } catch (Exception e) {
            System.err.println("Proxy error: " + e.getMessage());
        }
    }
}
