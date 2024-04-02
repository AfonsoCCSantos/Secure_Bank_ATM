import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;

import atm.AtmStub;
import utils.RequestMessage;
import utils.RequestType;

import java.io.IOException;

public class ATMClient {
	
	private static final int RETURN_VALUE_INVALID = 255;  
	
	public static void main(String[] args) {
		
		Map<String, String> finalArgs = new HashMap<>();
		
		if (args.length < 2) {
			System.err.println("Not enough arguments were presented.");
			System.exit(RETURN_VALUE_INVALID);
		}
		
		finalArgs.put("BankIP", "127.0.0.1");
		finalArgs.put("BankPort", "3000");
		finalArgs.put("AuthFile", "bank.auth");
		finalArgs.put("CardFile", null);
		finalArgs.put("Account", null);
		finalArgs.put("Functionality", null);
		finalArgs.put("Amount", null);
		
		finalArgs = processArgs(args, finalArgs);
		
		int bankPort = 3000;
		try {
			bankPort = Integer.parseInt(finalArgs.get("BankPort"));
		} catch (NumberFormatException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		Socket bankSocket = connectToServerSocket(finalArgs.get("BankIP"), bankPort);
		AtmStub atmStub = new AtmStub(bankSocket);
		
		double amount = 0.0;
		int result = 0;
		RequestMessage request = null;
		switch(finalArgs.get("Functionality")) {
				case "CREATE_ACCOUNT":
					amount = getAmountInDouble(finalArgs);
					request = new RequestMessage(RequestType.CREATE_ACCOUNT, finalArgs.get("Account"), finalArgs.get("CardFile"), amount);
					result = atmStub.createAccount(request);
					break;
				case "DEPOSIT":
					amount = getAmountInDouble(finalArgs);
					request = new RequestMessage(RequestType.DEPOSIT, finalArgs.get("Account"), finalArgs.get("CardFile"), amount);
					result = atmStub.depositAmount(request);
					break;
				case "WITHDRAW":
					amount = getAmountInDouble(finalArgs);
					request = new RequestMessage(RequestType.WITHDRAW, finalArgs.get("Account"), finalArgs.get("CardFile"), amount);
					result = atmStub.withdrawAmount(request);
					break;
				case "GET_BALANCE":
					request = new RequestMessage(RequestType.GET_BALANCE, finalArgs.get("Account"), finalArgs.get("CardFile"), -1);
					result = atmStub.getBalance(request);
					break;
		}
		try {
			bankSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(result);
	}

	private static double getAmountInDouble(Map<String, String> finalArgs) {
		double amount = 0.0;
		try {
			amount = Double.parseDouble(finalArgs.get("Amount"));
		} catch (NumberFormatException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		return amount;
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
	
	private static Map<String,String> processArgs(String[] args, Map<String,String> finalArgs) {
		int i = 0; 
		while (i < args.length) {
			if(args[i].equals("-s") && i+1 < args.length) {
				finalArgs.put("AuthFile", args[i+1]);
				i++;
			}
			else if(args[i].equals("-i") && i+1 < args.length) {
				finalArgs.put("BankIP", args[i+1]);
				i++;
			}
			else if(args[i].equals("-p") && i+1 < args.length) {
				try {
					finalArgs.put("BankPort", args[i+1]);
					i++;					
				} catch(NumberFormatException e) {
					System.err.println("The port presented is not an integer.");
					System.exit(RETURN_VALUE_INVALID);
				}
			}
			else if(args[i].equals("-c") && i+1 < args.length) {
				finalArgs.put("CardFile", args[i+1]);
				i++;
			}
			else if(args[i].equals("-a") && i+1 < args.length) {
				finalArgs.put("Account", args[i+1]);
				i++;
			}
			else if(args[i].equals("-n") && i+1 < args.length) {
				if(finalArgs.get("Functionality") != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				finalArgs.put("Functionality", "CREATE_ACCOUNT");
				try {
					finalArgs.put("Amount", args[i+1]);
					i++;					
				} catch(NumberFormatException e) {
					System.err.println("The balance given must be a number!");
					System.exit(RETURN_VALUE_INVALID);
				}
				
			}
			else if(args[i].equals("-d") && i+1 < args.length) {
				if(finalArgs.get("Functionality") != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				finalArgs.put("Functionality", "DEPOSIT");
				try {
					finalArgs.put("Amount", args[i+1]);
					i++;				
				} catch(NumberFormatException e) {
					System.err.println("The amount given must be a number!");
					System.exit(RETURN_VALUE_INVALID);
				}
				
			}
			else if(args[i].equals("-w") && i+1 < args.length) {
				if(finalArgs.get("Functionality") != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				finalArgs.put("Functionality", "WITHDRAW");
				try {
					finalArgs.put("Amount", args[i+1]);
					i++;				
				} catch(NumberFormatException e) {
					System.err.println("The amount given must be a number!");
					System.exit(RETURN_VALUE_INVALID);
				}
			}
			else if(args[i].equals("-g")) {
				if(finalArgs.get("Functionality") != null) {
					System.err.println("Only one mode of operation must be chosen!");
					System.exit(RETURN_VALUE_INVALID);
				} 
				finalArgs.put("Functionality", "GET_BALANCE");
				i++;
			}
			i++; 
		}
		
		if(finalArgs.get("Account") == null) {
			System.err.println("An account must be given!");
			System.exit(RETURN_VALUE_INVALID);
		} 
		if(finalArgs.get("Functionality") == null) {
			System.err.println("One mode of operation must be given!");
			System.exit(RETURN_VALUE_INVALID);
		}
		
		return finalArgs;
	}

	

}
