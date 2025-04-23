package csc435.app;

import java.util.*;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ServerWorker implements Runnable {
    private IndexStore store;
    private ZContext context;
    private static long clientCounter = 0;
    private static final Object clientLock = new Object();

    public ServerWorker(IndexStore store, ZContext context) {
        this.store = store;
        this.context = context;
    }

    @Override
    public void run() {
        ZMQ.Socket workerSocket = context.createSocket(SocketType.REP);
        workerSocket.connect("inproc://backend"); // Connect to the proxy backend

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String message = workerSocket.recvStr(ZMQ.DONTWAIT);
                if (message == null) {
                    Thread.sleep(10);
                    continue;
                }

                if (message.startsWith("REGISTER REQUEST")) {
                    long clientID;
                    synchronized (clientLock) {
                        clientID = ++clientCounter;
                    }
                    workerSocket.send(String.valueOf(clientID));

                } else if (message.startsWith("INDEX REQUEST")) {
                    handleIndexRequest(message, workerSocket);

                } else if (message.startsWith("SEARCH REQUEST")) {
                    handleSearchRequest(message, workerSocket);

                } else if (message.equals("QUIT")) {
                    break;
                }

            } catch (org.zeromq.ZMQException e) {
                // Thrown if context is closed or socket is forcibly terminated
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        workerSocket.close();
    }

    private void handleIndexRequest(String message, ZMQ.Socket workerSocket) {
        String[] parts = message.split("\\|");
        if (parts.length < 4) {
            workerSocket.send("INDEX REPLY: ERROR");
            return;
        }

        // parts[1] = client ID, parts[2] = docPath, parts[3] = frequencies
        long clientID = Long.parseLong(parts[1]); // ADDED: parse client ID
        long docNum = store.putDocument(parts[2]);
        store.setDocumentOwner(docNum, clientID); // ADDED: associate doc with client

        Map<String, Long> wordFrequencies = new HashMap<>();
        String[] words = parts[3].split(",");
        for (String word : words) {
            String[] keyValue = word.split(":");
            if (keyValue.length == 2) {
                wordFrequencies.put(keyValue[0], Long.parseLong(keyValue[1]));
            }
        }

        store.updateIndex(docNum, wordFrequencies);
        workerSocket.send("INDEX REPLY: OK");
    }

    private void handleSearchRequest(String message, ZMQ.Socket workerSocket) {
        String[] split = message.split("\\|");
        if (split.length < 2) {
            workerSocket.send("SEARCH REPLY|");
            return;
        }

        String[] terms = split[1].split(",");
        Map<Long, Long> docResults = new HashMap<>();

        // Aggregate frequencies for all matched terms
        for (String term : terms) {
            for (IndexStore.DocFreqPair pair : store.lookupIndex(term)) {
                docResults.put(pair.documentNumber,
                        docResults.getOrDefault(pair.documentNumber, 0L) + pair.wordFrequency);
            }
        }

        // Sort documents by frequency (descending)
        List<Map.Entry<Long, Long>> sortedResults = new ArrayList<>(docResults.entrySet());
        sortedResults.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        // Build a response that includes: ownerID:docPath:frequency
        StringBuilder response = new StringBuilder("SEARCH REPLY|");
        for (int i = 0; i < Math.min(10, sortedResults.size()); i++) {
            long docNum = sortedResults.get(i).getKey();
            long docOwner = store.getDocumentOwner(docNum); // ADDED: get the client ID
            String docPath = store.getDocument(docNum);

            if ("UNKNOWN_DOCUMENT".equals(docPath)) {
                System.err.println("WARNING: Document number " + docNum + " not found in store!");
            }

            response.append(docOwner)
                    .append(":")
                    .append(docPath)
                    .append(":")
                    .append(sortedResults.get(i).getValue())
                    .append("|");
        }

        workerSocket.send(response.toString());
    }
}