package utils;

import java.io.Serializable;

public class MessageSequence implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private byte[] message;
	private long counter;
	
	public MessageSequence(byte[] message, long counter) {
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

	public long getCounter() {
		return counter;
	}

	public void setCounter(long counter) {
		this.counter = counter;
	}
}
