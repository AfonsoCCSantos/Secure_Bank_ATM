import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import bank.BankThread;

public class BankServer {
	
	private static final int DEFAULT_BANK_PORT = 3000;
	private static final int RETURN_VALUE_INVALID = 255;  
	private static final String DEFAULT_AUTH_FILE = "bank.auth";
	
	public static void main(String[] args) {
		addSigtermHook();
		Scanner sc = new Scanner(System.in);
		
		//Default parameters
		int port = DEFAULT_BANK_PORT;
		String authFileName = DEFAULT_AUTH_FILE;
		
		if (args.length % 2 == 1) {
			System.err.println("Erro");
			System.exit(RETURN_VALUE_INVALID);			
		}
		
		authFileName = getArgs(args, authFileName);
		
		Path path = Paths.get(authFileName);
		if (Files.exists(path)) {
			System.err.println("Auth file already exists.");
			System.exit(RETURN_VALUE_INVALID);
		}
		
		createAuthFile(authFileName);
		System.out.println("Auth file created");
		
		ServerSocket serverSocket = initialiseSocket(port);
		
		while (true) {
			Socket inSocket;
			try {
				inSocket = serverSocket.accept();
				BankThread newServerThread = new BankThread(inSocket);
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

	private static String getArgs(String[] args, String authFileName) {
		int port;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-s") && i + 1 < args.length) {
				authFileName = args[i+1];
				i++;
			}
			else if (args[i].equals("-p") && i + 1 < args.length) {
				try {
                    port = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for -p flag: " + args[i + 1]);
                    System.exit(255);
                }
			}
			else {
				System.err.println("Erro");
				System.exit(RETURN_VALUE_INVALID);
			}
		}
		return authFileName;
	}

}
