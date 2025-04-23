package csc435.app;

public class FileRetrievalClient {

    public static void main(String[] args) {
        ClientProcessingEngine engine = new ClientProcessingEngine();
        ClientAppInterface appInterface = new ClientAppInterface(engine);
        appInterface.readCommands();
    }
}
