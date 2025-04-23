# 📂 Distributed File Retrieval System with ZMQ

This project is a distributed file indexing and search engine implemented in Java using **ZeroMQ** (JeroMQ) for networked communication. It enables multiple clients to connect to a central server, index files recursively from local directories, and perform keyword-based searches across documents uploaded by any connected client.

---

## 🧠 Problem Statement

With large-scale systems and growing data, it becomes crucial to design a scalable and concurrent system for searching documents. This system demonstrates:
- Client-side recursive file indexing.
- Server-side centralized storage and search handling.
- ZeroMQ-based messaging for decoupled architecture.
- Benchmarking across multiple clients and datasets.

---

## 🔧 Key Features

✅ **ZeroMQ-Based Communication**  
✅ **Multi-Threaded Server with Worker Pool**  
✅ **Client Document Indexing**  
✅ **Search Queries with AND Matching Logic**  
✅ **Search Result Ranking by Frequency**  
✅ **Document Ownership Tracking by Client ID**  
✅ **Benchmarking Tool for Performance Metrics**  
✅ **Command Line Interface on Both Sides**

---

## 🧱 Architecture Overview

```plaintext
             +-----------------------------+
             |         Server              |
             |  FileRetrievalServer.java   |
             | +-------------------------+ |
             | | ZMQProxyWorker (ROUTER) |<---+
             | +-------------------------+    |
             |         | inproc backend       |
             | +-------------------------+    |
             | |  ServerWorker Thread(s) |<---+
             | +-------------------------+    |
             |       |       |                |
             |   [IndexStore.java]            |
             +-----------------------------+

   Multiple Clients                      Benchmark Tool
+-------------------------+        +------------------------+
| FileRetrievalClient.java|        | FileRetrievalBenchmark |
| ClientAppInterface      |        | (Runs indexing/search) |
| ClientProcessingEngine  |        +------------------------+
+-------------------------+
