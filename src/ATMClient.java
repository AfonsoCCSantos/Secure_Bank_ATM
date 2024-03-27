import java.net.Socket;

import atm.AtmStub;

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
		
		processArgs(args, authFile, bankIP, bankPort, cardFile, account, functionality, amount);
		
		Socket bankSocket = connectToServerSocket(bankIP, bankPort);
		AtmStub atmStub = new AtmStub(bankSocket);
				
		switch(functionality) {
				case "CREATE_ACCOUNT":
					int result = atmStub.createAccount(account, amount, cardFile);
					System.exit(result);
				case "DEPOSIT":
					result = atmStub.depositAmount(account, amount, cardFile);
					System.exit(result);
				case "WITHDRAW":
					result = atmStub.withdrawAmount(account, amount, cardFile);
					System.exit(result);
				case "GET_BALANCE":
					result = atmStub.getBalance(account, cardFile);
					System.exit(result);
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
	
	private static void processArgs(String[] args, String authFile, String bankIP, int bankPort, 
							String cardFile, String account, String functionality, double amount) {
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
				try {
					amount = Double.valueOf(args[i+1]);
					i++;					
				} catch(NumberFormatException e) {
					System.err.println("The balance given must be a number!");
					System.exit(RETURN_VALUE_INVALID);
				}
				
			}
			else if(args[i].equals("-d") && i+1 < args.length) {
				if(functionality != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				functionality = "DEPOSIT";
				try {
					amount = Double.valueOf(args[i+1]);
					i++;				
				} catch(NumberFormatException e) {
					System.err.println("The amount given must be a number!");
					System.exit(RETURN_VALUE_INVALID);
				}
				
			}
			else if(args[i].equals("-w") && i+1 < args.length) {
				if(functionality != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				functionality = "WITHDRAW";
				try {
					amount = Double.valueOf(args[i+1]);
					i++;				
				} catch(NumberFormatException e) {
					System.err.println("The amount given must be a number!");
					System.exit(RETURN_VALUE_INVALID);
				}
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
	}

	

}
