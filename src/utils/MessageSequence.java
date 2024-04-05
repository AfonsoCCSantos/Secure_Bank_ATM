package utils;

import java.io.Serializable;

public class MessageSequence implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private byte[] message;
	private int counter;
	
	public MessageSequence(byte[] message, int counter) {
		super();
		this.message = message;
		this.counter = counter;
	}

	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}
}
