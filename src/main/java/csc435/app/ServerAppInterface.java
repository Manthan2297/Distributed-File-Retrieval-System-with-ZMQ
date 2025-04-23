package csc435.app;

import java.util.Scanner;

public class ServerAppInterface {

    private ServerProcessingEngine engine;

    public ServerAppInterface(ServerProcessingEngine engine) {
        this.engine = engine;
    }

    public void readCommands() {
        Scanner sc = new Scanner(System.in);
        String command;
        while (true) {
            System.out.print("> ");
            command = sc.nextLine().trim();
            if (command.equals("quit")) {
                engine.shutdown();
                break;
            }
            System.out.println("Unrecognized command!");
        }
        sc.close();
    }
}