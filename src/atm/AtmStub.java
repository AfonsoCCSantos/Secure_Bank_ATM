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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
			
			if (messageReceived.getCounter() != messageCounter || responseMessage.equals(ResponseMessage.AUTHENTICATION_FAILURE)) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			//Generate nonce and send it to bank - bank has to authenticate
			byte[] nonce = EncryptionUtils.generateNonce(32);
			MessageSequence nonceMessage = new MessageSequence(nonce, messageCounter);
			byte[] encryptedNonceMessage = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceMessage), bankPublicKey);
			outToServer.writeObject(encryptedNonceMessage);
			messageCounter++;
			
			//Receive nonce back from the bank
			byte[] receivedNonceBytes = (byte[]) inFromServer.readObject();
			MessageSequence receivedNonceMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(receivedNonceBytes, privateKey);
			if (receivedNonceMessage.getCounter() != messageCounter) return RETURN_VALUE_INVALID;
			messageCounter++;
			
			if (!Arrays.equals(nonce, receivedNonceMessage.getMessage())) {
				MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.AUTHENTICATION_FAILURE), messageCounter);
				outToServer.writeObject(returnMessage); //Think whether it makes sense to encrypt
				return RETURN_VALUE_INVALID;
			}
			MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
			outToServer.writeObject(returnMessage);
			messageCounter++;
			
			// Authentication completed
			
			//Start of Diffie Hellman
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
	        keyPairGenerator.initialize(2048);
	        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
	        
	        byte[] clientPublicKey = clientKeyPair.getPublic().getEncoded();
	        
	        //Client receives publicKey DH of server
			MessageSequence receivedPublicKeyDHmessage = (MessageSequence) inFromServer.readObject();
			if (receivedPublicKeyDHmessage.getCounter() != messageCounter) return RETURN_VALUE_INVALID;
			messageCounter++;
			byte[] bankDHPublicKey = receivedPublicKeyDHmessage.getMessage(); //DH public key of the bank
			
			//Client sends its DH publicKey to server
			MessageSequence messageDhPublicKey = new MessageSequence(clientPublicKey, messageCounter);
	        outToServer.writeObject(messageDhPublicKey);
	        messageCounter++;
	        
	        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
	        keyAgreement.init(clientKeyPair.getPrivate());
	        KeyFactory keyFactory = KeyFactory.getInstance("DH");
	        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bankDHPublicKey);
	        PublicKey bankDHPKObject = keyFactory.generatePublic(x509KeySpec);
	        keyAgreement.doPhase(bankDHPKObject, true);
	        byte[] sharedSecret = keyAgreement.generateSecret();
	        SecretKey secretKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
	        
	        //Here the client has a secret key to talk with the server
			
			//Client sends account and value encryoted to server
			MessageSequence requestMessageSequence = new MessageSequence(Utils.serializeData(requestMessage), messageCounter);
			encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(requestMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			//Client receives final response from server
			byte[] resultEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence resultMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(resultEncrypted, secretKey);
			if(resultMessageSequence.getCounter() != messageCounter || 
				Utils.deserializeData(resultMessageSequence.getMessage()).equals(ResponseMessage.ACCOUNT_ALREADY_EXISTS)) 
				return RETURN_VALUE_INVALID;
			
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		
		Utils.printAndFlush("{\"account\":\"" + requestMessage.getAccount() + "\",\"initial_balance\":" + String.format(Locale.ROOT, "%.2f",requestMessage.getValue()) + "}\n");
		return 0;
	}

	public int depositAmount(RequestMessage requestMessage) {
		if (requestMessage.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		} 
		
		int messageCounter = 0;

		try {
			//Sending the request to the bank
			MessageSequence requestTypeToSend = new MessageSequence(Utils.serializeData(requestMessage.getRequestType()), messageCounter);
			byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(requestTypeToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes); //SEND A DEPOSIT REQUEST TO SERVER
			messageCounter++;
			
			//Sending the account to the bank
			MessageSequence accountToSend = new MessageSequence(Utils.serializeData(requestMessage.getAccount()), messageCounter);
			encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(accountToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			//Start of Diffie Hellman
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
	        keyPairGenerator.initialize(2048);
	        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
	        
	        byte[] clientPublicKey = clientKeyPair.getPublic().getEncoded();
	        
	        //Sends its DH PublicKey to Server
			MessageSequence messageDhPublicKey = new MessageSequence(clientPublicKey, messageCounter);
	        outToServer.writeObject(messageDhPublicKey);
	        messageCounter++;
	        
			//Receives DH PublicKey of Server
			MessageSequence receivedPublicKeyDHmessage = (MessageSequence) inFromServer.readObject();
			if (receivedPublicKeyDHmessage.getCounter() != messageCounter) return RETURN_VALUE_INVALID;
			messageCounter++;
			byte[] bankDHPublicKey = receivedPublicKeyDHmessage.getMessage(); //DH public key of the bank
			
	        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
	        keyAgreement.init(clientKeyPair.getPrivate());
	        KeyFactory keyFactory = KeyFactory.getInstance("DH");
	        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bankDHPublicKey);
	        PublicKey bankDHPKObject = keyFactory.generatePublic(x509KeySpec);
	        keyAgreement.doPhase(bankDHPKObject, true);
	        byte[] sharedSecret = keyAgreement.generateSecret();
	        SecretKey secretKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
	        
	       	//Obtain hash of shared key and sends it to server
	        MessageDigest md = MessageDigest.getInstance("SHA3-256");
	        md.update(secretKey.getEncoded());
	        byte[] hashBytes = md.digest();
	        MessageSequence messageSequence = new MessageSequence(hashBytes, messageCounter); 
	        outToServer.writeObject(messageSequence);
	        messageCounter++;
	        
	        //Calculate hash of shared key + Bank PublicKey
	        byte[] secretKeywithBankPK = new byte[secretKey.getEncoded().length + bankPublicKey.getEncoded().length];
	        System.arraycopy(secretKey.getEncoded(), 0, secretKeywithBankPK, 0, secretKey.getEncoded().length);
	        System.arraycopy(bankPublicKey.getEncoded(), 0, secretKeywithBankPK, secretKey.getEncoded().length, bankPublicKey.getEncoded().length);
	        
	        md = MessageDigest.getInstance("SHA3-256");
	        md.update(secretKeywithBankPK);
	        byte[] hashSecretKeyBankPublicKey = md.digest();
	        
	        //Receive hash of shared key + Bank PublicKey from server and confirm hash
	        MessageSequence receivedHashmessage = (MessageSequence) inFromServer.readObject();
			if (receivedPublicKeyDHmessage.getCounter() != messageCounter || 
				Arrays.equals(hashSecretKeyBankPublicKey, receivedHashmessage.getMessage())) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			
			
			System.out.println("OLA");
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			//From this moment the secretShared key is established
			
			//Client sends value to deposit encrypted to Server
			MessageSequence valueMessageSequence = new MessageSequence(Utils.serializeData(requestMessage.getValue()), messageCounter);
			encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(valueMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
	        
	        //Client receives final response from server
			byte[] resultEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence resultMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(resultEncrypted, secretKey);
			if(resultMessageSequence.getCounter() != messageCounter || 
				Utils.deserializeData(resultMessageSequence.getMessage()).equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) 
				return RETURN_VALUE_INVALID;
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);	
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + requestMessage.getAccount() + "\",\"deposit\":" + String.format(Locale.ROOT, "%.2f",requestMessage.getValue()) + "}\n");
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
