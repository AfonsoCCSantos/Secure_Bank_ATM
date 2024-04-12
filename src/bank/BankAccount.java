package bank;

import java.math.BigDecimal;
import java.security.PublicKey;

public class BankAccount {
	
	private BigDecimal accountValue;
	private PublicKey publicKey;
	
	public BankAccount(BigDecimal accountValue, PublicKey publicKey) {
		this.accountValue = accountValue;
		this.publicKey = publicKey;
	}
	
	public BigDecimal getAccountValue() {
		return accountValue;
	}

	public void setAccountValue(BigDecimal accountValue) {
		this.accountValue = accountValue;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public void deposit(BigDecimal amount) {
		this.accountValue.add(amount);
	}
	
	public int withdraw(BigDecimal amount) {
		BigDecimal newValue = this.accountValue.subtract(amount); 
		
		
		if (newValue.compareTo(BigDecimal.ONE) == -1) {
			return -1;
		}
		else {
			this.accountValue = newValue;
			return 0;
		}
	}

}
