package bank;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

import utils.RequestMessage;
import utils.ResponseMessage;
import utils.Utils;

public class BankThread extends Thread {
	
	private static final int SUCCESS = 0;
	private static final int ACCOUNT_ALREADY_EXISTS = -2;
	private static final int ACCOUNT_DOESNT_EXIST = -3;
	private static final int NEGATIVE_BALANCE = -4;
	private Socket socket;
	private Map<String, Double> accounts;

	public BankThread(Socket socket, Map<String, Double> accounts) {
		super();
		this.socket = socket;
		this.accounts = accounts;
	}
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		ObjectOutputStream out = Utils.gOutputStream(socket);
		BankSkel bankSkel = new BankSkel(in, out, accounts);
		
		while (true) {
			RequestMessage request;
			try {
				request = (RequestMessage) in.readObject();
				switch (request.getRequestType()) {
					case CREATE_ACCOUNT:
						int returnCode = bankSkel.createAccount(request.getAccount(), request.getValue());
						if (returnCode == ACCOUNT_ALREADY_EXISTS) {
							out.writeObject(ResponseMessage.ACCOUNT_ALREADY_EXISTS);
						}
						else if (returnCode == SUCCESS) {
							out.writeObject(ResponseMessage.SUCCESS);
						}
						break;
					case DEPOSIT:
						returnCode = bankSkel.deposit(request.getAccount(), request.getValue());
						if (returnCode == ACCOUNT_DOESNT_EXIST) {
							out.writeObject(ResponseMessage.ACCOUNT_DOESNT_EXIST);
						}
						else if (returnCode == SUCCESS) {
							out.writeObject(ResponseMessage.SUCCESS);
						}
						break;
					case WITHDRAW:
						returnCode = bankSkel.withdraw(request.getAccount(), request.getValue());
						if (returnCode == ACCOUNT_DOESNT_EXIST) {
							out.writeObject(ResponseMessage.ACCOUNT_DOESNT_EXIST);
						}
						else if(returnCode == NEGATIVE_BALANCE) {
							out.writeObject(ResponseMessage.NEGATIVE_BALANCE);
						}
						else if (returnCode == SUCCESS) {
							out.writeObject(ResponseMessage.SUCCESS);
						}
						break;
					case GET_BALANCE:
						double currentBalance = bankSkel.getBalance(request.getAccount());
						if (currentBalance == ACCOUNT_DOESNT_EXIST) {
							out.writeObject("ACCOUNT_DOESNT_EXIST"); //This one still sends a string so we can send the 
																	 //currentBalance as a string too
						}
						out.writeObject(String.valueOf(currentBalance));
						break;
				}
			} catch (Exception e) {
				return;
			}
		}
	}
}
