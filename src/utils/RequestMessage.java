package utils;

import java.io.Serializable;
import java.math.BigDecimal;

public class RequestMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private RequestType requestType;
	private String account;
	private String cardFile;
//	private double value;
	private BigDecimal value;
	
	public RequestMessage(RequestType requestType, String account, String cardFile, BigDecimal value) {
		super();
		this.requestType = requestType;
		this.account = account;
		this.cardFile = cardFile;
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

	public String getCardFile() {
		return cardFile;
	}

	public void setCardFile(String cardFile) {
		this.cardFile = cardFile;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

}
