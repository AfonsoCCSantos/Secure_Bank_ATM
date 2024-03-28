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
		addSigtermHook();
		accounts = new HashMap<String, Double>();
		Scanner sc = new Scanner(System.in);
		
		//Default parameters
		int port = DEFAULT_BANK_PORT;
		String authFileName = DEFAULT_AUTH_FILE;
		
		if (args.length >= 4096) {
			System.exit(RETURN_VALUE_INVALID);			
		}
		
        List<String> filteredArgs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String currentArg = args[i].substring(0,2);
            String restArg = args[i].substring(2);
			if (currentArg.equalsIgnoreCase("-s") || currentArg.equalsIgnoreCase("-p")){
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
        for (int i = 0; i < filteredArgs.size(); i++) {
            String argI = filteredArgs.get(i);
            if (argI.equalsIgnoreCase("-s") || argI.equalsIgnoreCase("-p")) {
                continue;
            }
            else if (filteredArgs.get(i-1).equalsIgnoreCase("-p")) {

                try {
                    Integer.parseInt(argI);
                }
                catch (NumberFormatException e) {
                    System.exit(RETURN_VALUE_INVALID);
                }
                
                if (!argI.matches("(0|[1-9][0-9]*)") || Integer.parseInt(argI) < 1024 || Integer.parseInt(argI) > 65535) {
                    System.exit(RETURN_VALUE_INVALID);                    
                }
            }
            else if (filteredArgs.get(i-1).equalsIgnoreCase("-s")) {
                if(argI.length() < 1 || argI.length() > 127 || argI.equalsIgnoreCase(".") || argI.equalsIgnoreCase("..")) {
                    System.exit(RETURN_VALUE_INVALID);
                }
                else {
                    for (String a : argI.split("")) {
                        if (!a.matches("[_\\-\\.0-9a-z]")) {
                            System.exit(RETURN_VALUE_INVALID);
                        }
                    }
                }
            }
        }
		
		getArgs(filteredArgs, authFileName, port);
		
		Path path = Paths.get(authFileName);
		if (Files.exists(path)) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		createAuthFile(authFileName);
		System.out.println("Auth file created");
		
		ServerSocket serverSocket = initialiseSocket(port);
		
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

	private static void addSigtermHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received SIGTERM, shutting down...");
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
	}

	private static void getArgs(List<String> filteredArgs, String authFileName, int port) {
		for (int i = 0; i < filteredArgs.size(); i++) {
			if (filteredArgs.get(i).equals("-s") && i + 1 < filteredArgs.size()) {
				authFileName = filteredArgs.get(i+1);
				i++;
			}
			else if (filteredArgs.get(i).equals("-p") && i + 1 < filteredArgs.size()) {
				port = Integer.parseInt(filteredArgs.get(i+1));
				i++;
			}
			else {
				System.exit(RETURN_VALUE_INVALID);
			}
		}
	}

}
