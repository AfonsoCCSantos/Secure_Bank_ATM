package bank;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.Map;

import utils.Utils;

public class BankSkel {
	
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Map<String, BankAccount> accounts;
	
	public BankSkel(ObjectInputStream in, ObjectOutputStream out, Map<String, BankAccount> accounts) {
		super();
		this.in = in;
		this.out = out;
		this.accounts = accounts;
	}
	
	public int createAccount(String accountName, double balance) {
		if (accounts.get(accountName) != null) return -2;
		
		BankAccount bankAccount = new BankAccount(balance, null); //put here publicKey
		synchronized (accounts) {
			accounts.put(accountName, bankAccount);
		}
		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"initial_balance\":" + String.format(Locale.ROOT, "%.2f", balance) + "}\n");
		return 0;
	}

	public int deposit(String accountName, double amount) {
		if (accounts.get(accountName) == null) return -3;
		
		synchronized (accounts) {
			BankAccount bankAccount = accounts.get(accountName);
			bankAccount.deposit(amount);
		}

		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"deposit\":" + String.format(Locale.ROOT, "%.2f", amount) + "}\n");
		return 0;
	}

	public int withdraw(String accountName, double amount) {
		if (accounts.get(accountName) == null) return -3;
		
		synchronized (accounts) {
			BankAccount bankAccount = accounts.get(accountName);
			int result = bankAccount.withdraw(amount);
			if (result == -1) return -4;
		}
		
		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"withdraw\":" + String.format(Locale.ROOT, "%.2f", amount) + "}\n");
		return 0;
	}
	
	public double getBalance(String accountName) {
		if (accounts.get(accountName) == null) return -3;
		double amount = 0;
		
		synchronized (accounts) {
			amount = accounts.get(accountName).getAccountValue();
		}
		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"balance\":" + String.format(Locale.ROOT, "%.2f", amount) + "}\n");
		return amount;
	}


	
	
	
	
	
	
	
	
	
	

}
