package csc435.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ClientProcessingEngine {

    private ZContext context;
    private ZMQ.Socket requestSocket;
    private long clientID;

    public ClientProcessingEngine() {
        this.context = new ZContext();
        this.requestSocket = context.createSocket(SocketType.REQ);
    }

    // This inner class now tracks the document's client owner.
    public static class DocPathFreqPair {
        public String documentPath;
        public long wordFrequency;
        public long docOwner;

        public DocPathFreqPair(String documentPath, long wordFrequency) {
            this(documentPath, wordFrequency, -1);
        }

        public DocPathFreqPair(String documentPath, long wordFrequency, long docOwner) {
            this.documentPath = documentPath;
            this.wordFrequency = wordFrequency;
            this.docOwner = docOwner;
        }
    }

    public static class IndexResult {
        public double executionTime;
        public long totalBytesRead;

        public IndexResult(double executionTime, long totalBytesRead) {
            this.executionTime = executionTime;
            this.totalBytesRead = totalBytesRead;
        }
    }

    public static class SearchResult {
        public double executionTime;
        public ArrayList<DocPathFreqPair> documentFrequencies;

        public SearchResult(double executionTime, ArrayList<DocPathFreqPair> documentFrequencies) {
            this.executionTime = executionTime;
            this.documentFrequencies = documentFrequencies;
        }
    }

    public IndexResult indexFiles(String folderPath) {
        long totalBytesRead = 0;
        long startTime = System.currentTimeMillis();

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("ERROR: Invalid directory path: " + folderPath);
            return new IndexResult(0, 0);
        }

        // Index all files in folder (recursively)
        totalBytesRead = traverseAndIndex(folder, totalBytesRead);

        long endTime = System.currentTimeMillis();
        return new IndexResult((endTime - startTime) / 1000.0, totalBytesRead);
    }

    private long traverseAndIndex(File folder, long totalBytesRead) {
        File[] files = folder.listFiles();
        if (files == null) {
            return totalBytesRead;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                totalBytesRead = traverseAndIndex(file, totalBytesRead);
            } else if (file.isFile()) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    totalBytesRead += content.length();

                    Map<String, Long> wordFrequencies = new HashMap<>();
                    for (String word : content.split("[^a-zA-Z0-9_-]+")) {
                        if (word.length() > 3) {
                            wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0L) + 1);
                        }
                    }

                    // Format: "INDEX REQUEST|<clientID>|<docPath>|<word1:freq1,word2:freq2,...>"
                    StringBuilder message = new StringBuilder("INDEX REQUEST|");
                    message.append(clientID).append("|")
                           .append(file.getAbsolutePath()).append("|");
                    for (Map.Entry<String, Long> entry : wordFrequencies.entrySet()) {
                        message.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
                    }
                    if (message.charAt(message.length() - 1) == ',') {
                        message.deleteCharAt(message.length() - 1);
                    }

                    requestSocket.send(message.toString());
                    requestSocket.recv(); // Expect "INDEX REPLY: OK"
                } catch (IOException e) {
                    System.err.println("ERROR: Failed to read file: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
        return totalBytesRead;
    }

    public SearchResult searchFiles(ArrayList<String> terms) {
        long startTime = System.currentTimeMillis();

        // Build search message: "SEARCH REQUEST|term1,term2,..."
        StringBuilder searchMessage = new StringBuilder("SEARCH REQUEST|");
        for (String term : terms) {
            searchMessage.append(term).append(",");
        }
        if (searchMessage.charAt(searchMessage.length() - 1) == ',') {
            searchMessage.deleteCharAt(searchMessage.length() - 1);
        }
        requestSocket.send(searchMessage.toString());

        String response = new String(requestSocket.recv());
        ArrayList<DocPathFreqPair> results = new ArrayList<>();
        String[] parts = response.split("\\|");

        // parts[0] is "SEARCH REPLY"
        // Each subsequent part is "ownerID:docPath:freq"
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            String[] docData = parts[i].split(":");
            if (docData.length >= 3) {
                long docOwner = Long.parseLong(docData[0]);
                String docPath = docData[1];
                long freq = Long.parseLong(docData[2]);
                results.add(new DocPathFreqPair(docPath, freq, docOwner));
            }
        }

        long endTime = System.currentTimeMillis();
        return new SearchResult((endTime - startTime) / 1000.0, results);
    }

    public long getInfo() {
        return clientID;
    }

    public void connect(String serverIP, String serverPort) {
        requestSocket.connect("tcp://" + serverIP + ":" + serverPort);
        requestSocket.send("REGISTER REQUEST");

        byte[] reply = requestSocket.recv();
        this.clientID = Long.parseLong(new String(reply));
        System.out.println("Connected to server. Client ID: " + clientID);
    }

    public void disconnect() {
        // Here, if you prefer to send DISCONNECT rather than QUIT, update accordingly.
        requestSocket.send("QUIT");
        requestSocket.close();
        context.close();
        System.out.println("Disconnected from server.");
    }
}
