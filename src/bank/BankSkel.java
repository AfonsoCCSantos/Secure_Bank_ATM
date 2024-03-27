package bank;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class BankSkel {
	
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Map<String, Double> accounts;
	
	public BankSkel(ObjectInputStream in, ObjectOutputStream out, Map<String, Double> accounts) {
		super();
		this.in = in;
		this.out = out;
		this.accounts = accounts;
	}
	
	public int createAccount(String accountName, double balance) {
		if (accounts.get(accountName) != null) return -2;
		
		synchronized (accounts) {
			accounts.put(accountName, balance);
		}

		System.out.println("{\"account\":\"" + accountName + "\",\"initial_balance\":" + balance + "}\n"); 
		return 0;
	}

	public int deposit(String accountName, double amount) {
		if (accounts.get(accountName) == null) return -3;
		
		synchronized (accounts) {
			accounts.put(accountName, accounts.get(accountName) + amount);
		}

		System.out.println("{\"account\":\"" + accountName + "\",\"deposit\":" + amount + "}\n"); 
		return 0;
	}


	
	
	
	
	
	
	
	
	
	

}
