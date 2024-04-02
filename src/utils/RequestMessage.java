package utils;

import java.io.Serializable;

public class RequestMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private RequestType requestType;
	private String account;
	private double value;
	
	public RequestMessage(RequestType requestType, String account, double value) {
		super();
		this.requestType = requestType;
		this.account = account;
		this.value = value;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
}
