package csc435.app;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IndexStore {
    private final Map<String, Long> documentMap = new HashMap<>();
    private final Map<String, List<DocFreqPair>> termIndex = new HashMap<>();
    private final Lock docLock = new ReentrantLock();
    private final Lock indexLock = new ReentrantLock();
    private long docCounter = 0;

    // ADDED: track which client owns each document
    private final Map<Long, Long> docOwnerMap = new HashMap<>();

    public static class DocFreqPair {
        public long documentNumber;
        public long wordFrequency;

        public DocFreqPair(long documentNumber, long wordFrequency) {
            this.documentNumber = documentNumber;
            this.wordFrequency = wordFrequency;
        }
    }

    // Clears the index before re-indexing to prevent duplicate counts
    public void clearIndex() {
        docLock.lock();
        indexLock.lock();
        try {
            documentMap.clear();
            termIndex.clear();
            docCounter = 0;
            docOwnerMap.clear(); // ADDED: Clear owners as well
            System.out.println("Index cleared before server start.");
        } finally {
            indexLock.unlock();
            docLock.unlock();
        }
    }

    // Ensures documents are indexed only once
    public long putDocument(String documentPath) {
        docLock.lock();
        try {
            return documentMap.computeIfAbsent(documentPath, k -> ++docCounter);
        } finally {
            docLock.unlock();
        }
    }

    public String getDocument(long documentNumber) {
        docLock.lock();
        try {
            for (Map.Entry<String, Long> entry : documentMap.entrySet()) {
                if (entry.getValue() == documentNumber) {
                    return entry.getKey();
                }
            }
            return "UNKNOWN_DOCUMENT";
        } finally {
            docLock.unlock();
        }
    }

    public void updateIndex(long documentNumber, Map<String, Long> wordFrequencies) {
        indexLock.lock();
        try {
            for (Map.Entry<String, Long> entry : wordFrequencies.entrySet()) {
                List<DocFreqPair> docList = termIndex
                        .computeIfAbsent(entry.getKey(), k -> Collections.synchronizedList(new ArrayList<>()));

                boolean found = false;
                for (DocFreqPair pair : docList) {
                    if (pair.documentNumber == documentNumber) {
                        pair.wordFrequency += entry.getValue(); // Merge frequency
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    docList.add(new DocFreqPair(documentNumber, entry.getValue()));
                }
            }
        } finally {
            indexLock.unlock();
        }
    }

    public List<DocFreqPair> lookupIndex(String term) {
        indexLock.lock();
        try {
            return new ArrayList<>(termIndex.getOrDefault(term, Collections.emptyList()));
        } finally {
            indexLock.unlock();
        }
    }

    // ADDED: store/retrieve client ID for each document number
    public void setDocumentOwner(long docNumber, long clientID) {
        docOwnerMap.put(docNumber, clientID);
    }

    public long getDocumentOwner(long docNumber) {
        return docOwnerMap.getOrDefault(docNumber, -1L);
    }
}
