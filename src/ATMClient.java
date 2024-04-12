import java.net.Socket;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import atm.AtmStub;
import utils.RequestMessage;
import utils.RequestType;
import utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;

public class ATMClient {
	
	private static final int RETURN_VALUE_INVALID = 255;  
	private static final int RETURN_CONNECTION_ERROR = 63;  
	
	public static void main(String[] args) {
		Locale.setDefault(new Locale("en", "US"));
		
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
		
		if (!Utils.verifyAccountName(finalArgs.get("Account"))) 
			System.exit(RETURN_VALUE_INVALID);
		
		if (finalArgs.get("AuthFile") != null && !Utils.verifyFileNames(finalArgs.get("AuthFile"))) 
			System.exit(RETURN_VALUE_INVALID);
		
		if (finalArgs.get("CardFile") != null && !Utils.verifyFileNames(finalArgs.get("CardFile"))) 
			System.exit(RETURN_VALUE_INVALID);	
		
		if (finalArgs.get("Amount") != null && !Utils.verifyAmount(finalArgs.get("Amount"))) 
			System.exit(RETURN_VALUE_INVALID);
		
		if (finalArgs.get("BankPort") != null && !Utils.verifyPort(finalArgs.get("BankPort"))) 
			System.exit(RETURN_VALUE_INVALID);
		
		
		if (finalArgs.get("BankIP") == null) {
			finalArgs.put("BankIP", "127.0.0.1");
		}
		
		if (finalArgs.get("AuthFile") == null) {
			finalArgs.put("AuthFile", "bank.auth");
		}
		
		if (finalArgs.get("CardFile") == null) {
			finalArgs.put("CardFile", finalArgs.get("Account") + ".card");
		}
		
		if (!Utils.verifyIPAddress(finalArgs.get("BankIP"))) 
			System.exit(RETURN_VALUE_INVALID);
		
		int bankPort;
		try {
			bankPort = Integer.parseInt(finalArgs.get("BankPort"));
		} catch (NumberFormatException e) {
			bankPort = 3000;
		}
		
		PublicKey bankPublicKey = getBankPublicKey(finalArgs.get("AuthFile"));
		
		Socket bankSocket = connectToServerSocket(finalArgs.get("BankIP"), bankPort);
		AtmStub atmStub = new AtmStub(bankSocket, bankPublicKey);
		
		BigDecimal amount = BigDecimal.ZERO;
		int result = 0;
		RequestMessage request = null;
		switch(finalArgs.get("Functionality")) {
				case "CREATE_ACCOUNT":
					amount = getAmountInDecimal(finalArgs);
					request = new RequestMessage(RequestType.CREATE_ACCOUNT, finalArgs.get("Account"), finalArgs.get("CardFile"), amount);
					result = atmStub.createAccount(request, finalArgs.get("Account"));
					break;
				case "DEPOSIT":
					amount = getAmountInDecimal(finalArgs);
					request = new RequestMessage(RequestType.DEPOSIT, finalArgs.get("Account"), finalArgs.get("CardFile"), amount);
					result = atmStub.depositAmount(request);
					break;
				case "WITHDRAW":
					amount = getAmountInDecimal(finalArgs);
					request = new RequestMessage(RequestType.WITHDRAW, finalArgs.get("Account"), finalArgs.get("CardFile"), amount);
					result = atmStub.withdrawAmount(request);
					break;
				case "GET_BALANCE":
					request = new RequestMessage(RequestType.GET_BALANCE, finalArgs.get("Account"), finalArgs.get("CardFile"), null);
					result = atmStub.getBalance(request);
					break;
		}
		try {
			bankSocket.close();
		} catch (IOException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		System.exit(result);
	}
	
	private static BigDecimal getAmountInDecimal(Map<String, String> finalArgs) {
		BigDecimal amount = BigDecimal.ZERO;
		try {
			amount = BigDecimal.valueOf(Double.parseDouble(finalArgs.get("Amount")));
		} catch (NumberFormatException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		return amount;
	}
	
	private static Socket connectToServerSocket(String bankIP, int bankPort) {
		Socket socket = null;
		try {
			socket = new Socket(bankIP, bankPort);
			socket.setSoTimeout(10000);
		} catch (IOException e) {
			System.exit(RETURN_CONNECTION_ERROR);
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
	
	private static PublicKey getBankPublicKey(String authFileName) {
		PublicKey publicKey = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(authFileName))) {
			publicKey = (PublicKey) ois.readObject();
		} catch (Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		return publicKey;
	}
	

}
