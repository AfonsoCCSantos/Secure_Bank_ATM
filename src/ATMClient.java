import java.net.Socket;
import java.io.IOException;

public class ATMClient {
	
	private static final int RETURN_VALUE_INVALID = 255;  
	
	public static void main(String[] args) {
		
		if (args.length < 2) {
			System.err.println("Not enough arguments were presented.");
			System.exit(RETURN_VALUE_INVALID);
		}
		
		String bankIP = "127.0.0.1";
		int bankPort = 3000;
		String authFile = "bank.auth";
		String cardFile = null;
		String account = null;
		
		String functionality = null;
		double amount = 0.0;
		
		
		int i = 0; 
		while (i < args.length) {
			if(args[i].equals("-s") && i+1 < args.length) {
				authFile = args[i+1];
				i++;
			}
			else if(args[i].equals("-i") && i+1 < args.length) {
				bankIP = args[i+1];
				i++;
			}
			else if(args[i].equals("-p") && i+1 < args.length) {
				try {
					bankPort = Integer.valueOf(args[i+1]);
					i++;					
				} catch(NumberFormatException e) {
					System.err.println("The port presented is not an integer.");
					System.exit(RETURN_VALUE_INVALID);
				}
			}
			else if(args[i].equals("-c") && i+1 < args.length) {
				cardFile = args[i+1];
				i++;
			}
			else if(args[i].equals("-a") && i+1 < args.length) {
				account = args[i+1];
				i++;
			}
			else if(args[i].equals("-n") && i+1 < args.length) {
				if(functionality != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				functionality = "CREATE_ACCOUNT";
				amount = Double.valueOf(args[i+1]);
				i++;
			}
			else if(args[i].equals("-d") && i+1 < args.length) {
				if(functionality != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				functionality = "DEPOSIT";
				amount = Double.valueOf(args[i+1]);
				i++;
			}
			else if(args[i].equals("-w") && i+1 < args.length) {
				if(functionality != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				functionality = "WITHDRAW";
				amount = Double.valueOf(args[i+1]);
				i++;
			}
			else if(args[i].equals("-g")) {
				if(functionality != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				functionality = "GET_BALANCE";
				i++;
			}
			i++; 
		}
		
		if(account == null) {
			System.err.println("An account must be given!");
			System.exit(RETURN_VALUE_INVALID);
		} 
		if(functionality == null) {
			System.err.println("One mode of operation must be given!");
			System.exit(RETURN_VALUE_INVALID);
		} 
		
		Socket bankSocket = connectToServerSocket(bankIP, bankPort);
		
		
		
		switch(functionality) {
				case "CREATE_ACCOUNT":
					break;
				case "DEPOSIT":
					break;
				case "WITHDRAW":
					break;	
				case "GET_BALANCE":
					break;
		}
			
	}
	
	
	private static Socket connectToServerSocket(String bankIP, int bankPort) {
		Socket socket = null;
		try {
			socket = new Socket(bankIP, bankPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return socket;
	}

	

}
