package atm;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import utils.EncryptionUtils;
import utils.MessageSequence;
import utils.RequestMessage;
import utils.RequestType;
import utils.ResponseMessage;
import utils.Utils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.util.Locale;

public class AtmStub {
	
	private static final int RETURN_VALUE_INVALID = 255; 
	private static final int RETURN_CONNECTION_ERROR = 63;  
	private static final double BALANCE_INFERIOR_LIMIT = 10.0; 
	
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	private PrivateKey privateKey;
	private PublicKey bankPublicKey;
	
	
	public AtmStub(Socket bankSocket, PublicKey bankPublicKey) {
		this.outToServer = Utils.gOutputStream(bankSocket);
		this.inFromServer = Utils.gInputStream(bankSocket);
		this.bankPublicKey = bankPublicKey;
	}
	
	public int createAccount(RequestMessage requestMessage, String account) {
		//verify if card file is unique
		int messageCounter = 0;
		System.out.println(requestMessage.getCardFile());
		Path path = Paths.get(requestMessage.getCardFile());
		if (Files.exists(path)) {
			return RETURN_VALUE_INVALID;
		}
		
		PublicKey publicKey = null;
		
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.generateKeyPair();
			privateKey = kp.getPrivate();
			publicKey = kp.getPublic();
			createCardFile(requestMessage.getCardFile(),kp);
		} catch (NoSuchAlgorithmException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		if (requestMessage.getValue() < BALANCE_INFERIOR_LIMIT) {
			return RETURN_VALUE_INVALID;
		} 
		
		try {
			//Sending the request to the bank
			RequestType request = RequestType.CREATE_ACCOUNT;
			MessageSequence messageToSend = new MessageSequence(Utils.serializeData(request), messageCounter);
			byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(messageToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes); //SEND A CREATE_ACCOUNT REQUEST TO SERVER
			messageCounter++;
			
			//Sending my public key to the bank
			messageToSend = new MessageSequence(Utils.serializeData(publicKey), messageCounter);
//			encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(messageToSend), bankPublicKey);
//			byte[] messageToSendBytes = Utils.serializeData(messageToSend);
			outToServer.writeObject(messageToSend);
			messageCounter++;
			
			//Receiving nonce from bank
			byte[] nonceEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence nonceDecrypted = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(nonceEncrypted, privateKey); 
			if (nonceDecrypted.getCounter() != messageCounter) return RETURN_VALUE_INVALID;
			messageCounter++;
			
			//After decrypting the nonce, it sends it back to the bank
			MessageSequence nonceToSend = new MessageSequence(nonceDecrypted.getMessage(), messageCounter);
			encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			MessageSequence messageReceived = (MessageSequence) inFromServer.readObject(); //Think whether it makes sense to encrypt
			ResponseMessage responseMessage = (ResponseMessage) Utils.deserializeData(messageReceived.getMessage());
			System.out.println(responseMessage);
			
//			if (messageReceived.getCounter() != messageCounter || responseMessage.equals(ResponseMessage.AUTHENTICATION_FAILURE)) 
//				return RETURN_VALUE_INVALID;
//			messageCounter++;
			
			// Authentication completed
			
			//encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(account), bankPublicKey);
			//outToServer.writeObject(encryptedBytes);
			
			
			
//			ResponseMessage createAccountResult = (ResponseMessage) inFromServer.readObject();
//			if(createAccountResult.equals(ResponseMessage.ACCOUNT_ALREADY_EXISTS)) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		Utils.printAndFlush("{\"account\":\"" + requestMessage.getAccount() + "\",\"initial_balance\":" + String.format(Locale.ROOT, "%.2f",requestMessage.getValue()) + "}\n");
		return 0;
	}

	public int depositAmount(RequestMessage request) {
		if (request.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		} 

		//verify cardFile is associated to account
		try {
			outToServer.writeObject(request);
			
			ResponseMessage depositResult = (ResponseMessage) inFromServer.readObject();
			if(depositResult.equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);	
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"deposit\":" + String.format(Locale.ROOT, "%.2f",request.getValue()) + "}\n");
		return 0;
	}

	public int withdrawAmount(RequestMessage request) {
		if (request.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		}
		
		//verify cardFile is associated to account
		try {
			outToServer.writeObject(request);
			
			ResponseMessage withdrawResult = (ResponseMessage) inFromServer.readObject();
			if(withdrawResult.equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) return RETURN_VALUE_INVALID;
			if(withdrawResult.equals(ResponseMessage.NEGATIVE_BALANCE)) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"withdraw\":" + String.format(Locale.ROOT, "%.2f",request.getValue()) + "}\n");
		return 0;
	}

	public int getBalance(RequestMessage request) {
		String result = null;
		//verify if account exists and get amount
		//verify cardFile is associated to account
		try {
			outToServer.writeObject(request);
			
			//receive result from bank
			result = (String) inFromServer.readObject();
			if(result.equals("ACCOUNT_DOESNT_EXIST")) return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}

		//print account and amount
		Utils.printAndFlush("{\"account\":\"" + request.getAccount() + "\",\"balance\":" + result + "}\n");
		return 0;
	}
	
	private static void createCardFile(String cardFileName, KeyPair kp) {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cardFileName))) {
			oos.writeObject(kp);
		} catch (IOException e) {
			System.exit(RETURN_VALUE_INVALID);
		} 
		Utils.printAndFlush("Card file created.\n");
	}
	
	
}
