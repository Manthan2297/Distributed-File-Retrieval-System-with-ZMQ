package csc435.app;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerProcessingEngine {
    private IndexStore store;
    private ZContext context;
    private ExecutorService workerPool;
    private ZMQ.Socket notifySocket; // Socket for shutdown messages

    public ServerProcessingEngine(IndexStore store) {
        this.store = store;
        this.context = new ZContext();
    }

    public void initialize(int serverPort, int numWorkerThreads) {
        // Start the proxy on its own thread
        Thread proxyThread = new Thread(new ZMQProxyWorker(context, serverPort));
        proxyThread.start();

        // Create a worker pool
        workerPool = Executors.newCachedThreadPool();

        // CHANGED: Remove the infinite loop. Just submit one ServerWorker per thread.
        for (int i = 0; i < numWorkerThreads; i++) {
            workerPool.execute(new ServerWorker(store, context));
        }

        // Create a PUB socket to send shutdown notifications, if desired
        notifySocket = context.createSocket(SocketType.PUB);
        notifySocket.bind("tcp://*:5556");

        System.out.println("Server started on port " + serverPort + " with " 
                           + numWorkerThreads + " worker threads.");
    }

    public void shutdown() {
        System.out.println("Shutting down server...");

        // Optionally broadcast a shutdown message
        notifySocket.send("SERVER_SHUTDOWN");
        notifySocket.close();

        // Shut down the worker pool
        workerPool.shutdown();

        // Close the ZeroMQ context (forces all sockets to close)
        context.close();

        System.out.println("Server fully shut down.");
    }
}
