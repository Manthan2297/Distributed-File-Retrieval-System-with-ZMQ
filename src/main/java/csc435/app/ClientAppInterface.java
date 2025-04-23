package csc435.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientAppInterface {

    private ClientProcessingEngine engine;

    public ClientAppInterface(ClientProcessingEngine engine) {
        this.engine = engine;
    }

    public void readCommands() {
        Scanner sc = new Scanner(System.in);
        String command;
        while (true) {
            System.out.print("> ");
            command = sc.nextLine().trim();

            if (command.equals("quit")) {
                engine.disconnect();
                break;
            }

            if (command.length() >= 7 && command.substring(0, 7).compareTo("connect") == 0) {
                String[] parts = command.split(" ");
                if (parts.length == 3) {
                    engine.connect(parts[1], parts[2]);
                } else {
                    System.out.println("Usage: connect <server IP> <server port>");
                }
                continue;
            }

            if (command.length() >= 8 && command.substring(0, 8).compareTo("get_info") == 0) {
                System.out.println("Client ID: " + engine.getInfo());
                continue;
            }

            if (command.length() >= 5 && command.substring(0, 5).compareTo("index") == 0) {
                String[] parts = command.split(" ", 2);
                if (parts.length == 2) {
                    ClientProcessingEngine.IndexResult result = engine.indexFiles(parts[1]);
                    System.out.println("Indexing completed in " + result.executionTime
                            + " s, bytes read: " + result.totalBytesRead);
                } else {
                    System.out.println("Usage: index <folder path>");
                }
                continue;
            }

            if (command.length() >= 6 && command.substring(0, 6).compareTo("search") == 0) {
                String[] parts = command.split(" ", 2);
                if (parts.length == 2) {
                    ArrayList<String> terms = new ArrayList<>(List.of(parts[1].split(" AND ")));
                    ClientProcessingEngine.SearchResult result = engine.searchFiles(terms);
                    System.out.println("Search completed in " + result.executionTime + " s");
                    for (ClientProcessingEngine.DocPathFreqPair doc : result.documentFrequencies) {
                        System.out.println(doc.documentPath + " - Frequency: " + doc.wordFrequency);
                    }
                } else {
                    System.out.println("Usage: search <term1 AND term2>");
                }
                continue;
            }
            System.out.println("Unrecognized command!");
        }
        sc.close();
    }
}
