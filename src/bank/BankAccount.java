package bank;

import java.security.PublicKey;

public class BankAccount {
	
	private double accountValue;
	private PublicKey publicKey;
	
	public BankAccount(double accountValue, PublicKey publicKey) {
		this.accountValue = accountValue;
		this.publicKey = publicKey;
	}
	
	public double getAccountValue() {
		return accountValue;
	}

	public void setAccountValue(double accountValue) {
		this.accountValue = accountValue;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public void deposit(double amount) {
		this.accountValue += amount;
	}
	
	public int withdraw(double amount) {
		if (this.accountValue - amount < 0) {
			return -1;
		}
		else {
			this.accountValue -= amount;
			return 0;
		}
	}

}
