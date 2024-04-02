import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import atm.AtmStub;
import utils.RequestMessage;
import utils.RequestType;

import java.io.IOException;

public class ATMClient {
	
	private static final int RETURN_VALUE_INVALID = 255;  
	
	public static void main(String[] args) {
		
		Map<String, String> finalArgs = new HashMap<>();
		
		if (args.length < 2) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		finalArgs.put("BankIP", null); //127.0.0.1
		finalArgs.put("BankPort", null); //3000
		finalArgs.put("AuthFile", null); //bank.auth
		finalArgs.put("CardFile", null);
		finalArgs.put("Account", null);
		finalArgs.put("Functionality", null);
		finalArgs.put("Amount", null);
		
		finalArgs = processArgs(args, finalArgs);
		
		if (finalArgs.get("BankIP") == null) {
			finalArgs.put("BankIP", "127.0.0.1");
		}
		
		int bankPort;
		try {
			bankPort = Integer.parseInt(finalArgs.get("BankPort"));
		} catch (NumberFormatException e) {
			bankPort = 3000;
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
			
			if(currentArg.equals("-a")) {
				if (finalArgs.get("Account") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("Account", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("Account", restArg);
				}
			}
			else if(currentArg.equals("-s")) {
				if (finalArgs.get("AuthFile") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("AuthFile", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("AuthFile", restArg);
				}
			}
			else if(currentArg.equals("-i")) {
				if (finalArgs.get("BankIP") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("BankIP", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("BankIP", restArg);
				}
			}
			else if(currentArg.equals("-p")) {
				if (finalArgs.get("BankPort") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("BankPort", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("BankPort", restArg);
				}
			}
			else if(currentArg.equals("-c")) {
				if (finalArgs.get("CardFile") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("CardFile", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("CardFile", restArg);
				}
			}
			else if(currentArg.equals("-n")) {
				
				if (finalArgs.get("Functionality") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				finalArgs.put("Functionality", "CREATE_ACCOUNT");
				if (restArg == null && i+1 < args.length) {
					System.out.println("a");
					finalArgs.put("Amount", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("Amount", restArg);
				}
			}
			else if(currentArg.equals("-d")) {
				if (finalArgs.get("Functionality") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				finalArgs.put("Functionality", "DEPOSIT");
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("Amount", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("Amount", restArg);
				}
			}
			else if(currentArg.equals("-w")) {
				if (finalArgs.get("Functionality") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				finalArgs.put("Functionality", "WITHDRAW");
				if (restArg == null && i+1 < args.length) {
					finalArgs.put("Amount", args[i+1]);
					i++;
				}
				else if (i + 1 >= args.length && restArg == null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				else {
					finalArgs.put("Amount", restArg);
				}
			}
			else if(currentArg.equals("-g")) {
				if (finalArgs.get("Functionality") != null) {
					System.exit(RETURN_VALUE_INVALID);
				}
				finalArgs.put("Functionality", "GET_BALANCE");
			}
//			System.out.println(finalArgs.get("Account"));
//			System.out.println(finalArgs.get("Functionality"));
//			System.out.println(finalArgs.get("Amount"));
//			System.out.println();
			
			i++; 
		}
		
		if(finalArgs.get("Account") == null) {
			System.exit(RETURN_VALUE_INVALID);
		} 
		if(finalArgs.get("Functionality") == null) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		return finalArgs;
	}
	

}
