package atm;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	
	public int createAccount(String account, double balance, String cardFileName) {
		
		//verify if card file is unique
		Path path = Paths.get(cardFileName);
		if (Files.exists(path)) {
			return RETURN_VALUE_INVALID;
		}
		
		if (balance < BALANCE_INFERIOR_LIMIT) {
			return RETURN_VALUE_INVALID;
		} 
		
		try {
			outToServer.writeObject("CREATE_ACCOUNT");
			outToServer.writeObject(account);
			outToServer.writeObject(Double.toString(balance));
			
			String createAccountResult = (String) inFromServer.readObject();
			if(createAccountResult.equals("ACCOUNT_ALREADY_EXISTS")) return RETURN_VALUE_INVALID;
						
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		System.out.println("{\"account\":\"" + account + "\",\"initial_balance\":" + balance + "}\n"); 
		
		//create card file
		createCardFile(cardFileName);
		return 0;
	}

	public int depositAmount(String account, double amount, String cardFileName) {
		//verify if amount > 0
		if (amount <= 0) {
			return RETURN_VALUE_INVALID;
		} 

		//verify if account exists and deposit amount
		//verify cardFile is associated to account
		try {
			outToServer.writeObject("DEPOSIT");
			outToServer.writeObject(account);
			outToServer.writeObject(Double.toString(amount));
			
			//receive result from bank
			String depositResult = (String) inFromServer.readObject();
			if(depositResult.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		//print account and amount
		System.out.println("{\"account\":\"" + account + "\",\"deposit\":" + amount + "}\n"); 
		return 0;
	}

	public int withdrawAmount(String account, double amount, String cardFileName) {
		//verify if amount > 0
		if (amount <= 0) {
			return RETURN_VALUE_INVALID;
		} 

		//verify if account exists and withdraw amount
		//verify cardFile is associated to account
		try {
			outToServer.writeObject("WITHDRAW_AMOUNT");
			outToServer.writeObject(account);
			outToServer.writeObject(amount);
			
			//receive result from bank
			String withdrawResult = (String) inFromServer.readObject();
			if(withdrawResult.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
			if(withdrawResult.equals("NEGATIVE_BALANCE")) return RETURN_VALUE_INVALID;
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		//print account and amount
		System.out.println("{\"account\":\"" + account + "\",\"withdraw\":" + amount + "}\n"); 
		return 0;
	}

	public int getBalance(String account, String cardFileName) {
		String result = null;
		//verify if account exists and get amount
		//verify cardFile is associated to account
		try {
			outToServer.writeObject("GET_BALANCE");
			outToServer.writeObject(account);
			
			//receive result from bank
			result = (String) inFromServer.readObject();
			if(result.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
		} catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		//print account and amount
		System.out.println("{\"account\":\"" + account + "\",\"deposit\":" + result + "}\n"); 
		return 0;
	}
	
	private static void createCardFile(String cardFileName) {
		System.out.println("Creating...");
	}
	
	
}
