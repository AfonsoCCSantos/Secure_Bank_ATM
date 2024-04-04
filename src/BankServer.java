import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Base64;
import java.util.Locale;


import bank.BankThread;
import utils.Utils;

public class BankServer {
	
	private static final String DEFAULT_BANK_PORT = "3000";
	private static final int RETURN_VALUE_INVALID = 255;  
	private static final String DEFAULT_AUTH_FILE = "bank.auth";
	private static Map<String, Double> accounts;
	private static PrivateKey privateKey;
	
	public static void main(String[] args) {
		Locale.setDefault(new Locale("en", "US"));
		ServerSocket serverSocket = null;
		addSigtermHook(serverSocket);
		accounts = new HashMap<String, Double>();
		Scanner sc = new Scanner(System.in);
		Map<String, String> finalArgs = new HashMap<String, String>();
		
		finalArgs.put("port", null);
		finalArgs.put("AuthFile", null);
		
		getArgs(args, finalArgs);
		
		//Default parameters
		if (finalArgs.get("port") == null) {
			finalArgs.put("port", DEFAULT_BANK_PORT);
		}
		if (finalArgs.get("AuthFile") == null) {
			finalArgs.put("AuthFile", DEFAULT_AUTH_FILE);
		}
		
		Path path = Paths.get(finalArgs.get("AuthFile"));
		if (Files.exists(path)) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.generateKeyPair();
			privateKey = kp.getPrivate();
			createAuthFile(finalArgs.get("AuthFile"), kp.getPublic());
		} catch (NoSuchAlgorithmException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		
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
				System.exit(RETURN_VALUE_INVALID);
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
	
	private static void createAuthFile(String authFileName, PublicKey publicKey) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(authFileName))) {
			writer.write(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
			Utils.printAndFlush("created\n");
		} catch (IOException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
	}

	private static void getArgs(String[] args, Map<String, String> finalArgs) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].length() >= 4096) {
				System.exit(RETURN_VALUE_INVALID);			
			}
			String currentArg = null;
            String restArg = null;
			if (args[i].length() > 2) {
				currentArg = args[i].substring(0,2);
				restArg = args[i].substring(2);
			}
			currentArg = currentArg == null ? args[i] : currentArg;
			
			if (currentArg.equals("-s")) {
				if (finalArgs.get("AuthFile") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					if (!Utils.verifyFileNames(args[i+1])) 
						System.exit(RETURN_VALUE_INVALID);
					finalArgs.put("AuthFile", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					if (!Utils.verifyFileNames(restArg)) 
						System.exit(RETURN_VALUE_INVALID);
					finalArgs.put("AuthFile", restArg);
				}
			}
			else if (currentArg.equals("-p")) {
				if (finalArgs.get("port") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					if(!Utils.verifyPort(args[i+1])) {
						System.exit(RETURN_VALUE_INVALID);
					}
					finalArgs.put("port", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					if(!Utils.verifyPort(restArg)) {
						System.exit(RETURN_VALUE_INVALID);
					}
					finalArgs.put("port", restArg);
				}
			}
			else {
				System.exit(RETURN_VALUE_INVALID);
			}
		}
	}

}
