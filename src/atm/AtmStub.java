package atm;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import utils.RequestMessage;
import utils.ResponseMessage;
import utils.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Locale;
import java.io.IOException;


public class AtmStub {
	
	private static final int RETURN_VALUE_INVALID = 255; 
	private static final int RETURN_CONNECTION_ERROR = 63;  
	private static final double BALANCE_INFERIOR_LIMIT = 10.0; 
	
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	
	
	public AtmStub(Socket bankSocket) {
		this.outToServer = Utils.gOutputStream(bankSocket);
		this.inFromServer = Utils.gInputStream(bankSocket);
	}
	
	public int createAccount(RequestMessage request) {
		//verify if card file is unique
//		Path path = Paths.get(cardFileName);
//		if (Files.exists(path)) {
//			return RETURN_VALUE_INVALID;
//		}
		
		if (request.getValue() < BALANCE_INFERIOR_LIMIT) {
			return RETURN_VALUE_INVALID;
		} 
		
		try {
			outToServer.writeObject(request);
			
			ResponseMessage createAccountResult = (ResponseMessage) inFromServer.readObject();
			if(createAccountResult.equals(ResponseMessage.ACCOUNT_ALREADY_EXISTS)) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"initial_balance\":" + String.format(Locale.ROOT, "%.2f",request.getValue()) + "}\n");
		//create card file
		createCardFile(request.getCardFile());
		return 0;
	}

	public int depositAmount(RequestMessage request) {
		if (request.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		} 

		//verify cardFile is associated to account

		try {
			outToServer.writeObject(request);
			
			ResponseMessage depositResult = (ResponseMessage) inFromServer.readObject();
			if(depositResult.equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);	
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"deposit\":" + String.format(Locale.ROOT, "%.2f",request.getValue()) + "}\n");
		return 0;
	}

	public int withdrawAmount(RequestMessage request) {
		if (request.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		}
		
		//verify cardFile is associated to account
		try {
			outToServer.writeObject(request);
			
			ResponseMessage withdrawResult = (ResponseMessage) inFromServer.readObject();
			if(withdrawResult.equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) return RETURN_VALUE_INVALID;
			if(withdrawResult.equals(ResponseMessage.NEGATIVE_BALANCE)) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"withdraw\":" + String.format(Locale.ROOT, "%.2f",request.getValue()) + "}\n");
		return 0;
	}

	public int getBalance(RequestMessage request) {
		String result = null;
		//verify if account exists and get amount
		//verify cardFile is associated to account
		try {
			outToServer.writeObject(request);
			
			//receive result from bank
			result = (String) inFromServer.readObject();
			if(result.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}

		//print account and amount
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"balance\":" + result + "}\n");
		return 0;
	}
	
	private static void createCardFile(String cardFileName) {
		Utils.printAndFlush("Card file created.\n");
	}
	
	
}
