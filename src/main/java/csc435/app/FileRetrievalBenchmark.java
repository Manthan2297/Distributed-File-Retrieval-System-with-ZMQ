package csc435.app;

import java.util.ArrayList;
import java.util.List;

class BenchmarkWorker implements Runnable {

    private ClientProcessingEngine engine;
    private String datasetPath;
    private long bytesRead;
    private double executionTime;

    public BenchmarkWorker(String datasetPath, String serverIP, String serverPort) {
        this.datasetPath = datasetPath;
        this.engine = new ClientProcessingEngine();
        engine.connect(serverIP, serverPort);
    }

    @Override
    public void run() {
        long start = System.nanoTime();
        ClientProcessingEngine.IndexResult result = engine.indexFiles(datasetPath);
        long end = System.nanoTime();

        this.bytesRead = result.totalBytesRead;
        this.executionTime = (end - start) / 1e9; // Convert to seconds
        System.out.println("Indexed " + datasetPath + " in " + this.executionTime + " s, bytes read: " + this.bytesRead);
    }

    public void search(String query) {
        List<String> terms = List.of(query.split(" AND "));
        ClientProcessingEngine.SearchResult result = engine.searchFiles(new ArrayList<>(terms));

        System.out.println("\nSearch query: \"" + query + "\"");
        System.out.println("Search completed in " + result.executionTime + " s");

        boolean found = false;
        // Updated printing to include client ID (docOwner) along with document path and frequency.
        for (ClientProcessingEngine.DocPathFreqPair doc : result.documentFrequencies) {
            if (doc.documentPath != null) {
                System.out.println("Client " + doc.docOwner + " - " + doc.documentPath + " - Frequency: " + doc.wordFrequency);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No valid matches found.");
        }
    }

    public void disconnect() {
        engine.disconnect();
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public double getExecutionTime() {
        return executionTime;
    }
}

public class FileRetrievalBenchmark {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java FileRetrievalBenchmark <serverIP> <serverPort> <numClients> <datasetPath1> ...");
            return;
        }

        String serverIP = args[0];
        String serverPort = args[1];
        int numClients = Integer.parseInt(args[2]);

        List<BenchmarkWorker> workers = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            workers.add(new BenchmarkWorker(args[i], serverIP, serverPort));
        }

        long startTime = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (BenchmarkWorker worker : workers) {
            Thread t = new Thread(worker);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + e.getMessage());
            }
        }

        long endTime = System.nanoTime();
        double totalExecutionTime = (endTime - startTime) / 1e9; // Convert to seconds
        long totalBytesRead = workers.stream().mapToLong(BenchmarkWorker::getBytesRead).sum();

        System.out.println("\nTotal time " + totalExecutionTime + " seconds.");
        System.out.println("Total bytes read: " + totalBytesRead);

        if (!workers.isEmpty()) {
            // Execute specified search queries.
            workers.get(0).search("the");
            workers.get(0).search("child-like");
            workers.get(0).search("child-like AND cats");
            workers.get(0).search("child-like AND cats AND dogs");
        }

        for (BenchmarkWorker worker : workers) {
            worker.disconnect();
        }
    }
}
