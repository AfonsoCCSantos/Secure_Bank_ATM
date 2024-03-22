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
		//PRINT THE JSON
		return 0;
	}
	
	
	
	
	
	
	
	
	
	

}
