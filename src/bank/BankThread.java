package bank;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

import utils.Utils;

public class BankThread extends Thread {
	
	private static final int SUCCESS = 0;
	private static final int ACCOUNT_ALREADY_EXISTS = -2;
	private static final int ACCOUNT_DOESNT_EXIST = -3;
	private static final int NEGATIVE_BALANCE = -4;
	private static final int RETURN_VALUE_INVALID = 255;  
	private Socket socket;
	private Map<String, Double> accounts;

	public BankThread(Socket socket) {
		super();
		this.socket = socket;
	}
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		ObjectOutputStream out = Utils.gOutputStream(socket);
		BankSkel bankSkel = new BankSkel(in, out, accounts);
		
		while (true) {
			String command;
			try {
				command = (String) in.readObject();
				switch (command) {
					case "CREATE_ACCOUNT":
						String accountName = (String) in.readObject();
						String balanceString = (String) in.readObject();
						double balance = 0.0;
						try {
		                    balance = Double.parseDouble(balanceString);
		                } catch (NumberFormatException e) {
		                    System.exit(RETURN_VALUE_INVALID);
		                }
						int returnCode = bankSkel.createAccount(accountName, balance);
						if (returnCode == ACCOUNT_ALREADY_EXISTS) {
							out.writeObject("ACCOUNT_ALREADY_EXISTS");
						}
						else if (returnCode == SUCCESS) {
							out.writeObject("SUCCESS");
						}
						break;
					case "DEPOSIT":
						accountName = (String) in.readObject();
						String amountString = (String) in.readObject();
						double amount = 0.0;
						try {
							amount = Double.parseDouble(amountString);
						} catch (NumberFormatException e) {
							System.exit(RETURN_VALUE_INVALID);
						}
						returnCode = bankSkel.deposit(accountName, amount);
						if (returnCode == ACCOUNT_DOESNT_EXIST) {
							out.writeObject("ACCOUNT_DOESNT_EXIST");
						}
						else if (returnCode == SUCCESS) {
							out.writeObject("SUCCESS");
						}
						break;
					case "WITHDRAW":
						accountName = (String) in.readObject();
						amountString = (String) in.readObject();
						amount = 0.0;
						try {
							amount = Double.parseDouble(amountString);
						} catch (NumberFormatException e) {
							System.exit(RETURN_VALUE_INVALID);
						}
						returnCode = bankSkel.withdraw(accountName, amount);
						if (returnCode == ACCOUNT_DOESNT_EXIST) {
							out.writeObject("ACCOUNT_DOESNT_EXIST");
						}
						else if(returnCode == NEGATIVE_BALANCE) {
							out.writeObject("NEGATIVE_BALANCE");
						}
						else if (returnCode == SUCCESS) {
							out.writeObject("SUCCESS");
						}
						break;
					case "GET_BALANCE":
						accountName = (String) in.readObject();
						double currentBalance = bankSkel.getBalance(accountName);
						if (currentBalance == ACCOUNT_DOESNT_EXIST) {
							out.writeObject("ACCOUNT_DOESNT_EXIST");
						}
						out.writeObject(currentBalance);
						break;
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}
