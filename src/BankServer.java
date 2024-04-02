import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

import bank.BankThread;

public class BankServer {
	
	private static final int DEFAULT_BANK_PORT = 3000;
	private static final int RETURN_VALUE_INVALID = 255;  
	private static final String DEFAULT_AUTH_FILE = "bank.auth";
	private static Map<String, Double> accounts;
	
	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		addSigtermHook(serverSocket);
		accounts = new HashMap<String, Double>();
		Scanner sc = new Scanner(System.in);
		Map<String, String> finalArgs = new HashMap<String, String>();
		
		//Default parameters
		finalArgs.put("port", String.valueOf(DEFAULT_BANK_PORT));
		finalArgs.put("authFileName", DEFAULT_AUTH_FILE);
		
		if (args.length >= 4096) {
			System.exit(RETURN_VALUE_INVALID);			
		}
		
        List<String> filteredArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String currentArg = args[i].substring(0,2);
            String restArg = args[i].substring(2);
			if (currentArg.equals("-s") || currentArg.equals("-p")) {
                filteredArgs.add(currentArg);
                if (restArg.isEmpty()){
                    continue;
                }
                else {
                    filteredArgs.add(restArg); 
                }    
			}
            else {
                filteredArgs.add(args[i]); 
            }
		}
		
		getArgs(args, finalArgs);
		
		Path path = Paths.get(finalArgs.get("authFileName"));
		if (Files.exists(path)) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		createAuthFile(finalArgs.get("authFileName"));
		System.out.println("Auth file created");
		System.out.flush();
		
		serverSocket = initialiseSocket(Integer.parseInt(finalArgs.get("port")));
		
		while (true) {
			Socket inSocket;
			try {
				inSocket = serverSocket.accept();
				BankThread newServerThread = new BankThread(inSocket, accounts);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static void addSigtermHook(ServerSocket serverSocket) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received SIGTERM, shutting down...");
            System.out.flush();
            try {
				serverSocket.close();
			} catch (IOException e) {
				System.exit(0);
			}
            // Perform cleanup tasks or any necessary actions before exiting
            System.exit(0);
        }));
	}
	
	public static ServerSocket initialiseSocket(int portNumber) {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serverSocket;
	}
	
	private static void createAuthFile(String authFileName) {
		System.out.println("Creating...");
		System.out.flush();
	}

	private static void getArgs(String[] args, Map<String, String> finalArgs) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s") && i + 1 < args.length) {
				finalArgs.put("authFileName", args[i+1]);
				i++;
			}
			else if (args[i].equals("-p") && i + 1 < args.length) {
				try {
                    finalArgs.put("port", args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    System.exit(RETURN_VALUE_INVALID);
                }
			}
			else {
				System.exit(RETURN_VALUE_INVALID);
			}
		}
	}

}
