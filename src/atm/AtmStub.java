package atm;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.FileInputStream;
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

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

public class AtmStub {
	
	private static final int RETURN_VALUE_INVALID = 255; 
	private static final int RETURN_CONNECTION_ERROR = 63;  
	private static final double BALANCE_INFERIOR_LIMIT = 10.0; 
	
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	private PrivateKey privateKey;
	private PublicKey bankPublicKey;
	private long messageCounter;
	
	public AtmStub(Socket bankSocket, PublicKey bankPublicKey) {
		this.outToServer = Utils.gOutputStream(bankSocket);
		this.inFromServer = Utils.gInputStream(bankSocket);
		this.bankPublicKey = bankPublicKey;
	}
	
	public int createAccount(RequestMessage requestMessage, String account) {
		//verify if card file is unique
		byte[] nonce = EncryptionUtils.generateNonce(8);
		ByteBuffer bb = ByteBuffer.allocate(nonce.length);
		bb.put(nonce);
		bb.flip();
		messageCounter = bb.getLong();
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
			outToServer.writeObject(messageToSend);
			messageCounter++;
			
			if (!clientAuthenticationChallenge()) return RETURN_VALUE_INVALID;
			// Authentication completed
			
			//Here the client has a secret key to talk with the server
	        SecretKey secretKey = clientDHExchange();
	        if (secretKey == null) return RETURN_VALUE_INVALID;
			
			//Client sends account and value encrypted to server
			MessageSequence requestMessageSequence = new MessageSequence(Utils.serializeData(requestMessage), messageCounter);
			encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(requestMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			// Create HMAC and send it to bank
            byte[] hmacBytes = EncryptionUtils.createHmac(secretKey, Utils.serializeData(requestMessage));
            MessageSequence hmacMessageSequence = new MessageSequence(hmacBytes, messageCounter);
            encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(hmacMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			//Client receives final response from server
			byte[] resultEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence resultMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(resultEncrypted, secretKey);
			if(resultMessageSequence.getCounter() != messageCounter || 
				Utils.deserializeData(resultMessageSequence.getMessage()).equals(ResponseMessage.ACCOUNT_ALREADY_EXISTS)) 
				return RETURN_VALUE_INVALID;
			messageCounter++;	
			
			//Client receives and confirms HMAC of operation result
			byte[] hmacMessageEncrypted = (byte[]) inFromServer.readObject();
			hmacMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(hmacMessageEncrypted, secretKey);
			if(hmacMessageSequence.getCounter() != messageCounter) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			hmacBytes = EncryptionUtils.createHmac(secretKey, resultMessageSequence.getMessage());
			if(!Arrays.equals(hmacBytes, hmacMessageSequence.getMessage())) return RETURN_VALUE_INVALID;
			
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
		
		messageCounter = 0;
		try {
			Path path = Paths.get(requestMessage.getCardFile());
			if (!Files.exists(path)) {
				return RETURN_VALUE_INVALID;
			}
			KeyPair keyPair = loadCardFile(requestMessage.getCardFile());
			if (keyPair == null) return RETURN_VALUE_INVALID;
			privateKey = keyPair.getPrivate();
			
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
			
			if (!clientAuthenticationChallenge()) return RETURN_VALUE_INVALID;
			// Authentication completed
			
	        SecretKey secretKey = clientDHExchange();
	        if (secretKey == null) return RETURN_VALUE_INVALID;
	        		
			//From this moment the secretShared key is established
			
			//Client sends value to deposit encrypted to Server
			MessageSequence valueMessageSequence = new MessageSequence(Utils.serializeData(requestMessage.getValue()), messageCounter);
			encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(valueMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			// Create HMAC and send it to bank
            byte[] hmacBytes = EncryptionUtils.createHmac(secretKey, Utils.serializeData(requestMessage.getValue()));
            MessageSequence hmacMessageSequence = new MessageSequence(hmacBytes, messageCounter);
            encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(hmacMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
	        
	        //Client receives final response from server
			byte[] resultEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence resultMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(resultEncrypted, secretKey);
			if(resultMessageSequence.getCounter() != messageCounter || 
				Utils.deserializeData(resultMessageSequence.getMessage()).equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) 
				return RETURN_VALUE_INVALID;
			messageCounter++;	
			
			//Client receives and confirms HMAC of operation result
			byte[] hmacMessageEncrypted = (byte[]) inFromServer.readObject();
			hmacMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(hmacMessageEncrypted, secretKey);
			if(hmacMessageSequence.getCounter() != messageCounter) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			hmacBytes = EncryptionUtils.createHmac(secretKey, resultMessageSequence.getMessage());
			if(!Arrays.equals(hmacBytes, hmacMessageSequence.getMessage())) return RETURN_VALUE_INVALID;
			
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);	
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + requestMessage.getAccount() + "\",\"deposit\":" + String.format(Locale.ROOT, "%.2f",requestMessage.getValue()) + "}\n");
		return 0;
	}

	public int withdrawAmount(RequestMessage requestMessage) {
		if (requestMessage.getValue() <= 0) {
			return RETURN_VALUE_INVALID;
		}
		messageCounter = 0;

		try {
			KeyPair keyPair = loadCardFile(requestMessage.getCardFile());
			if (keyPair == null) return RETURN_VALUE_INVALID;
			privateKey = keyPair.getPrivate();
			
			//Sending the request to the bank
			MessageSequence requestTypeToSend = new MessageSequence(Utils.serializeData(requestMessage.getRequestType()), messageCounter);
			byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(requestTypeToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes); //SEND A WITHDRAW REQUEST TO SERVER
			messageCounter++;
			
			//Sending the account to the bank
			MessageSequence accountToSend = new MessageSequence(Utils.serializeData(requestMessage.getAccount()), messageCounter);
			encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(accountToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			if (!clientAuthenticationChallenge()) return RETURN_VALUE_INVALID;
			
			// Authentication completed
			
	        SecretKey secretKey = clientDHExchange();
	        if (secretKey == null) return RETURN_VALUE_INVALID;
	        		
			//From this moment the secretShared key is established
			
			//Client sends value to withdraw encrypted to Server
			MessageSequence valueMessageSequence = new MessageSequence(Utils.serializeData(requestMessage.getValue()), messageCounter);
			encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(valueMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			// Create HMAC and send it to bank
            byte[] hmacBytes = EncryptionUtils.createHmac(secretKey, Utils.serializeData(requestMessage.getValue()));
            MessageSequence hmacMessageSequence = new MessageSequence(hmacBytes, messageCounter);
            encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(hmacMessageSequence), secretKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			//Client receives final response from server
			byte[] resultEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence resultMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(resultEncrypted, secretKey);
			ResponseMessage withdrawResult = (ResponseMessage) Utils.deserializeData(resultMessageSequence.getMessage());
			if(resultMessageSequence.getCounter() != messageCounter || withdrawResult.equals(ResponseMessage.ACCOUNT_DOESNT_EXIST) 
					|| withdrawResult.equals(ResponseMessage.NEGATIVE_BALANCE)) 
				return RETURN_VALUE_INVALID;
			messageCounter++;	
			
			//Client receives and confirms HMAC of operation result
			byte[] hmacMessageEncrypted = (byte[]) inFromServer.readObject();
			hmacMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(hmacMessageEncrypted, secretKey);
			if(hmacMessageSequence.getCounter() != messageCounter) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			hmacBytes = EncryptionUtils.createHmac(secretKey, resultMessageSequence.getMessage());
			if(!Arrays.equals(hmacBytes, hmacMessageSequence.getMessage())) return RETURN_VALUE_INVALID;
			
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		Utils.printAndFlush("{\"account\":\"" + requestMessage.getAccount() + "\",\"withdraw\":" + String.format(Locale.ROOT, "%.2f",requestMessage.getValue()) + "}\n");
		return 0;
	}

	public int getBalance(RequestMessage requestMessage) {
		String balance = null;
		messageCounter = 0;
		try {
			KeyPair keyPair = loadCardFile(requestMessage.getCardFile());
			if (keyPair == null) return RETURN_VALUE_INVALID;
			privateKey = keyPair.getPrivate();
			
			//Sending the request to the bank
			MessageSequence requestTypeToSend = new MessageSequence(Utils.serializeData(requestMessage.getRequestType()), messageCounter);
			byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(requestTypeToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes); //SEND A GET_BALANCE REQUEST TO SERVER
			messageCounter++;
			
			//Sending the account to the bank
			MessageSequence accountToSend = new MessageSequence(Utils.serializeData(requestMessage.getAccount()), messageCounter);
			encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(accountToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			if (!clientAuthenticationChallenge()) return RETURN_VALUE_INVALID;
			
			// Authentication completed
			
	        SecretKey secretKey = clientDHExchange();
	        if (secretKey == null) return RETURN_VALUE_INVALID;
	        		
			//From this moment the secretShared key is established
			
			//Client receives result of operation from server
			byte[] resultEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence resultMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(resultEncrypted, secretKey);
			ResponseMessage withdrawResult = (ResponseMessage) Utils.deserializeData(resultMessageSequence.getMessage());
			if(resultMessageSequence.getCounter() != messageCounter || withdrawResult.equals(ResponseMessage.ACCOUNT_DOESNT_EXIST)) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			//Client receives and confirms HMAC of operation result
			byte[] hmacMessageEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence hmacMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(hmacMessageEncrypted, secretKey);
			if(hmacMessageSequence.getCounter() != messageCounter) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			byte[] hmacBytes = EncryptionUtils.createHmac(secretKey, resultMessageSequence.getMessage());
			if(!Arrays.equals(hmacBytes, hmacMessageSequence.getMessage())) return RETURN_VALUE_INVALID;
			
			//If operation is a success, client receives balance from bank
			byte[] balanceEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence balanceMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(balanceEncrypted, secretKey);
			if(balanceMessageSequence.getCounter() != messageCounter) 
				return RETURN_VALUE_INVALID;
			balance = (String) Utils.deserializeData(balanceMessageSequence.getMessage());
			messageCounter++;	
			
			//Client receives and confirms HMAC of operation result
			hmacMessageEncrypted = (byte[]) inFromServer.readObject();
			hmacMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(hmacMessageEncrypted, secretKey);
			if (hmacMessageSequence.getCounter() != messageCounter) 
				return RETURN_VALUE_INVALID;
			messageCounter++;
			
			hmacBytes = EncryptionUtils.createHmac(secretKey, balanceMessageSequence.getMessage());
			if(!Arrays.equals(hmacBytes, hmacMessageSequence.getMessage())) return RETURN_VALUE_INVALID;
			
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		} catch(Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}

		//print account and amount
		Utils.printAndFlush("{\"account\":\"" + requestMessage.getAccount() + "\",\"balance\":" + balance + "}\n");
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
	
	private static KeyPair loadCardFile(String cardFileName) {
		KeyPair keypair = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cardFileName))) {
			keypair = (KeyPair) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		return keypair; 
	}
	
	private boolean clientAuthenticationChallenge() {
		try {
			//Receiving nonce from bank
			byte[] nonceEncrypted = (byte[]) inFromServer.readObject();
			MessageSequence nonceDecrypted = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(nonceEncrypted, privateKey); 
			if (nonceDecrypted.getCounter() != messageCounter) return false;
			messageCounter++;
			
			//After decrypting the nonce, it sends it back to the bank
			MessageSequence nonceToSend = new MessageSequence(nonceDecrypted.getMessage(), messageCounter);
			byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceToSend), bankPublicKey);
			outToServer.writeObject(encryptedBytes);
			messageCounter++;
			
			MessageSequence messageReceived = (MessageSequence) inFromServer.readObject(); //Think whether it makes sense to encrypt
			ResponseMessage responseMessage = (ResponseMessage) Utils.deserializeData(messageReceived.getMessage());
			
			if (messageReceived.getCounter() != messageCounter || responseMessage.equals(ResponseMessage.AUTHENTICATION_FAILURE)) 
				return false;
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
			if (receivedNonceMessage.getCounter() != messageCounter) return false;
			messageCounter++;
			
			if (!Arrays.equals(nonce, receivedNonceMessage.getMessage())) {
				MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.AUTHENTICATION_FAILURE), messageCounter);
				outToServer.writeObject(returnMessage); //Think whether it makes sense to encrypt
				return false;
			}
			MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
			outToServer.writeObject(returnMessage);
			messageCounter++;
		 } catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
		 } catch(Exception e) {
			return false; 
		 } 
			
		return true;
	}
	
	private SecretKey clientDHExchange() {
		SecretKey secretKey = null;
		try {
			//Start of Diffie Hellman
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
	        keyPairGenerator.initialize(2048);
	        KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
	        
	        byte[] clientPublicKey = clientKeyPair.getPublic().getEncoded();
	        
	        //Client receives publicKey DH of server
			MessageSequence receivedPublicKeyDHmessage = (MessageSequence) inFromServer.readObject();
			if (receivedPublicKeyDHmessage.getCounter() != messageCounter) return null;
			messageCounter++;
			byte[] bankDHPublicKey = receivedPublicKeyDHmessage.getMessage(); //DH public key of the bank
			byte[] dhPubKeyHash = EncryptionUtils.createHash(bankDHPublicKey);
			
			//Receive signed hash of the server's DH public key
			MessageSequence dHPubKeyHashMessage = (MessageSequence) inFromServer.readObject();
			if (dHPubKeyHashMessage.getCounter() != messageCounter) return null;
			messageCounter++;
			byte[] bankDHPublicKeySignedHash = dHPubKeyHashMessage.getMessage();
			
			//Check if it matches the signature from the bank
			if (!EncryptionUtils.verifySignature(dhPubKeyHash, bankDHPublicKeySignedHash, bankPublicKey)) return null;
			
			//Client sends its DH publicKey to server
			MessageSequence messageDhPublicKey = new MessageSequence(clientPublicKey, messageCounter);
	        outToServer.writeObject(messageDhPublicKey);
	        messageCounter++;
	        
	        //Send a signed hash of the public key to confirm it is correct
	        byte[] dhPublicKeyHash = EncryptionUtils.createHash(clientPublicKey);
	        byte[] dhPublicKeyHashSigned = EncryptionUtils.sign(dhPublicKeyHash, privateKey);
	        MessageSequence messageDhPublicKeySignedHash = new MessageSequence(dhPublicKeyHashSigned, messageCounter);
	        outToServer.writeObject(messageDhPublicKeySignedHash);
	        messageCounter++;
	        
	        secretKey = EncryptionUtils.calculateSecretSharedKey(clientKeyPair.getPrivate(), bankDHPublicKey);
		} catch(SocketTimeoutException e) {
			System.exit(RETURN_CONNECTION_ERROR);
	    } catch (Exception e) {
			e.printStackTrace();
		}
		return secretKey;
	}
	
	
}
