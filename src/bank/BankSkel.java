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

	public int withdraw(String accountName, double amount) {
		if (accounts.get(accountName) == null) return -3;
		
		synchronized (accounts) {
			double remainingAmount = accounts.get(accountName) - amount;
			if(remainingAmount < 0) return -4; 
			accounts.put(accountName, remainingAmount);
		}

		System.out.println("{\"account\":\"" + accountName + "\",\"withdraw\":" + amount + "}\n"); 
		return 0;
	}
	
	public double getBalance(String accountName) {
		if (accounts.get(accountName) == null) return -3;
		double amount = 0;
		
		synchronized (accounts) {
			amount = accounts.get(accountName);
		}
		System.out.println("{\"account\":\"" + accountName + "\",\"balance\":" + amount + "}\n"); 
		return amount;
	}


	
	
	
	
	
	
	
	
	
	

}
