package atm;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import utils.RequestMessage;
import utils.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;


public class AtmStub {
	
	private static final int RETURN_VALUE_INVALID = 255; 
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
			
			String createAccountResult = (String) inFromServer.readObject();
			if(createAccountResult.equals("ACCOUNT_ALREADY_EXISTS")) return RETURN_VALUE_INVALID;
						
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		System.out.println("{\"account\":\"" + request.getAccount() + "\",\"initial_balance\":" + request.getValue() + "}\n"); 
		
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
			
			String depositResult = (String) inFromServer.readObject();
			if(depositResult.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("{\"account\":\"" + request.getAccount() + "\",\"deposit\":" + request.getValue() + "}\n"); 
		return 0;
	}

	public int withdrawAmount(RequestMessage request) {
		if (request.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		}
		
		//verify cardFile is associated to account
		try {
			outToServer.writeObject(request);
			
			String withdrawResult = (String) inFromServer.readObject();
			if(withdrawResult.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
			if(withdrawResult.equals("NEGATIVE_BALANCE")) return RETURN_VALUE_INVALID;
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("{\"account\":\"" + request.getAccount() + "\",\"withdraw\":" + request.getValue() + "}\n"); 
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
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		//print account and amount
		System.out.println("{\"account\":\"" + request.getAccount() + "\",\"deposit\":" + result + "}\n"); 
		return 0;
	}
	
	private static void createCardFile(String cardFileName) {
		System.out.println("Card file created.");
	}
	
	
}
