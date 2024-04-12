package bank;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Locale;
import java.util.Map;

import utils.Utils;

public class BankSkel {
	
	private Map<String, BankAccount> accounts;
	
	public BankSkel(Map<String, BankAccount> accounts) {
		super();
		this.accounts = accounts;
	}
	
	public int createAccount(String accountName, BigDecimal balance, PublicKey clientPublicKey) {
		if (accounts.get(accountName) != null) return -2;
		
		BankAccount bankAccount = new BankAccount(balance, clientPublicKey);
		synchronized (accounts) {
			accounts.put(accountName, bankAccount);
		}
		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"initial_balance\":" + String.format(Locale.ROOT, "%.2f", balance) + "}\n");
		return 0;
	}

	public int deposit(String accountName, BigDecimal amount) {
		if (accounts.get(accountName) == null) return -3;
		
		synchronized (accounts) {
			BankAccount bankAccount = accounts.get(accountName);
			bankAccount.deposit(amount);
		}

		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"deposit\":" + String.format(Locale.ROOT, "%.2f", amount) + "}\n");
		return 0;
	}

	public int withdraw(String accountName, BigDecimal amount) {
		if (accounts.get(accountName) == null) return -3;
		
		synchronized (accounts) {
			BankAccount bankAccount = accounts.get(accountName);
			int result = bankAccount.withdraw(amount);
			if (result == -1) return -4;
		}
		
		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"withdraw\":" + String.format(Locale.ROOT, "%.2f", amount) + "}\n");
		return 0;
	}
	
	public BigDecimal getBalance(String accountName) {
		if (accounts.get(accountName) == null) return null;
		BigDecimal amount = BigDecimal.ONE;
		
		synchronized (accounts) {
			amount = accounts.get(accountName).getAccountValue();
		}
		Utils.printAndFlush("{\"account\":\"" + accountName + "\",\"balance\":" + String.format(Locale.ROOT, "%.2f", amount) + "}\n");
		return amount;
	}
}
