package bank;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import utils.EncryptionUtils;
import utils.MessageSequence;
import utils.RequestMessage;
import utils.RequestType;
import utils.ResponseMessage;
import utils.Utils;
import java.io.IOException;

public class BankThread extends Thread {
	
	private static final int SUCCESS = 0;
	private static final int ACCOUNT_ALREADY_EXISTS = -2;
	private static final int ACCOUNT_DOESNT_EXIST = -3;
	private static final int NEGATIVE_BALANCE = -4;
	private static final int RETURN_CONNECTION_ERROR = 63;  
	private Socket socket;
	private Map<String, BankAccount> accounts;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private int messageCounter;
	

	public BankThread(Socket socket, Map<String, BankAccount> accounts, PrivateKey privateKey, PublicKey publicKey) {
		super();
		this.socket = socket;
		this.accounts = accounts;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}
	
	public void run() {
		try {
			socket.setSoTimeout(10000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in = Utils.gInputStream(socket);
			out = Utils.gOutputStream(socket);
			BankSkel bankSkel = new BankSkel(in, out, accounts);
			messageCounter = 0;
			while (true) {
				byte[] commandInBytes = (byte[]) in.readObject();
				MessageSequence commandMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(commandInBytes, privateKey);
				if (commandMessage.getCounter() != messageCounter) return;
				messageCounter++;
				RequestType command = (RequestType) Utils.deserializeData(commandMessage.getMessage());

				switch (command) {
					case CREATE_ACCOUNT:
						//Get public key from client
						MessageSequence clientPublicKeyMessage = (MessageSequence) in.readObject();
						if (clientPublicKeyMessage.getCounter() != messageCounter) return;
						messageCounter++;
						PublicKey clientPublicKey = (PublicKey) Utils.deserializeData(clientPublicKeyMessage.getMessage());
						
						//Mutual authentication between bank and client
						if (!bankAuthenticationChallenge(clientPublicKey)) return;

						//Here the bank has a secret key to use in the communications
						SecretKey secretKey = bankDHExchange();
						if (secretKey == null) return;
				        
				        //Server receives account and value encrypted from client
				        byte[] requestMessageEncrypted = (byte[]) in.readObject();
						MessageSequence requestMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(requestMessageEncrypted, secretKey);
						if(requestMessageSequence.getCounter() != messageCounter) 
							return;
						RequestMessage requestMessageReceived = (RequestMessage) Utils.deserializeData(requestMessageSequence.getMessage());
						int returnCode = bankSkel.createAccount(requestMessageReceived.getAccount(), requestMessageReceived.getValue(), clientPublicKey);
						messageCounter++;
						
						//Bank sends result of operation to client
						MessageSequence operationResultMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
						if (returnCode == ACCOUNT_ALREADY_EXISTS) {
							operationResultMessage.setMessage(Utils.serializeData(ResponseMessage.ACCOUNT_ALREADY_EXISTS));
						}
						byte[] encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(operationResultMessage), secretKey);
						out.writeObject(encryptedBytes);
						messageCounter++;
						break;
					case DEPOSIT:
						byte[] accountNameMsgBytes = (byte[]) in.readObject();
						MessageSequence accountNameMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(accountNameMsgBytes, privateKey);
						if (accountNameMessage.getCounter() != messageCounter)
							return;
						messageCounter++;
						String accountName = (String) Utils.deserializeData(accountNameMessage.getMessage());
						clientPublicKey = accounts.get(accountName).getPublicKey();
						
						//Mutual authentication between bank and client
						if (!bankAuthenticationChallenge(clientPublicKey)) return;
						
						//Here the bank has a secret key to use in the communications
						secretKey = bankDHExchange();
						if (secretKey == null) return;
						
						byte[] valueMessageEncrypted = (byte[]) in.readObject();
						MessageSequence valueMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(valueMessageEncrypted, secretKey);
						if (valueMessageSequence.getCounter() != messageCounter) return;
						messageCounter++;
						
						returnCode = bankSkel.deposit(accountName, (double) Utils.deserializeData(valueMessageSequence.getMessage()));
						operationResultMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
						if (returnCode == ACCOUNT_DOESNT_EXIST) {
							operationResultMessage.setMessage(Utils.serializeData(ResponseMessage.ACCOUNT_DOESNT_EXIST));
						}
						
						encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(operationResultMessage), secretKey);
						out.writeObject(encryptedBytes);
						messageCounter++;
						break;
					case WITHDRAW:
						accountNameMsgBytes = (byte[]) in.readObject();
						accountNameMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(accountNameMsgBytes, privateKey);
						if (accountNameMessage.getCounter() != messageCounter)
							return;
						messageCounter++;
						accountName = (String) Utils.deserializeData(accountNameMessage.getMessage());
						clientPublicKey = accounts.get(accountName).getPublicKey();
						
						//Mutual authentication between bank and client
						if (!bankAuthenticationChallenge(clientPublicKey)) return;
						
						//Here the bank has a secret key to use in the communications
						secretKey = bankDHExchange();
						if (secretKey == null) return;
				        
				        //From this moment the secretShared key is established
				        
				        //Server receives value to withdraw encrypted from client
				        valueMessageEncrypted = (byte[]) in.readObject();
				        valueMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(valueMessageEncrypted, secretKey);
				        if (valueMessageSequence.getCounter() != messageCounter) return;
				        messageCounter++;
				        
				        //Bank sends result of operation to client
						returnCode = bankSkel.withdraw(accountName, (double) Utils.deserializeData(valueMessageSequence.getMessage()));
						operationResultMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
						if (returnCode == ACCOUNT_DOESNT_EXIST) {
							operationResultMessage.setMessage(Utils.serializeData(ResponseMessage.ACCOUNT_DOESNT_EXIST));
						}
						else if(returnCode == NEGATIVE_BALANCE) {
							operationResultMessage.setMessage(Utils.serializeData(ResponseMessage.NEGATIVE_BALANCE));
						}
						encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(operationResultMessage), secretKey);
						out.writeObject(encryptedBytes);
						messageCounter++;
						break;
					case GET_BALANCE:
						accountNameMsgBytes = (byte[]) in.readObject();
						accountNameMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(accountNameMsgBytes, privateKey);
						if (accountNameMessage.getCounter() != messageCounter)
							return;
						messageCounter++;
						accountName = (String) Utils.deserializeData(accountNameMessage.getMessage());
						clientPublicKey = accounts.get(accountName).getPublicKey();
						
						//Mutual authentication between bank and client
						if (!bankAuthenticationChallenge(clientPublicKey)) return;
						
						//Here the bank has a secret key to use in the communications
						secretKey = bankDHExchange();
						if (secretKey == null) return;
				        
				        //From this moment the secretShared key is established
						
						//Bank sends result of operation to client
						double currentBalance = bankSkel.getBalance(accountName);
						operationResultMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
						if (currentBalance == ACCOUNT_DOESNT_EXIST) {
							operationResultMessage.setMessage(Utils.serializeData(ResponseMessage.ACCOUNT_DOESNT_EXIST));
						}
						encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(operationResultMessage), secretKey);
						out.writeObject(encryptedBytes);
						messageCounter++;
						
						// if account exists, bank sends balance encrypted to client
						if (currentBalance != ACCOUNT_DOESNT_EXIST) {
							String balanceString = String.format(Locale.ROOT, "%.2f", currentBalance);
							MessageSequence balanceMessage = new MessageSequence(Utils.serializeData(balanceString), messageCounter);
							encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(balanceMessage), secretKey);
							out.writeObject(encryptedBytes);
							messageCounter++;
						}
						break;
				}
			}
		} catch (SocketTimeoutException e) {
			Utils.printAndFlush("protocol_error\n");
			System.exit(RETURN_CONNECTION_ERROR);
		} catch (Exception e) {
			return;
		} 
	}
	
	
	private boolean bankAuthenticationChallenge(PublicKey clientPublicKey) {
		try {
			//Generate nonce and send it to client
			byte[] nonce = EncryptionUtils.generateNonce(32);
			MessageSequence nonceMessage = new MessageSequence(nonce, messageCounter);
			byte[] encryptedNonceMessage = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceMessage), clientPublicKey);
			out.writeObject(encryptedNonceMessage);
			messageCounter++;
			
			//Receive nonce back from client
			byte[] receivedNonceBytes = (byte[]) in.readObject();
			MessageSequence receivedNonceMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(receivedNonceBytes, privateKey);
			if (receivedNonceMessage.getCounter() != messageCounter) return false;
			messageCounter++;
			
			//Check if client correctly decrypted nonce
			if (!Arrays.equals(nonce, receivedNonceMessage.getMessage())) {
				MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.AUTHENTICATION_FAILURE), messageCounter);
				out.writeObject(returnMessage); //Think whether it makes sense to encrypt
				return false;
			}
			MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
			out.writeObject(returnMessage);
			messageCounter++;
			
			//Receiving nonce from client
			byte[] nonceEncrypted = (byte[]) in.readObject();
			MessageSequence nonceDecrypted = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(nonceEncrypted, privateKey); 
			if (nonceDecrypted.getCounter() != messageCounter) return false;
			messageCounter++;
			
			//Send nonce back to client
			MessageSequence nonceToSend = new MessageSequence(nonceDecrypted.getMessage(), messageCounter);
			byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceToSend), clientPublicKey);
			out.writeObject(encryptedBytes);
			messageCounter++;
			
			//Receive response from client - whether authentication was successful
			MessageSequence messageReceived = (MessageSequence) in.readObject(); //Think whether it makes sense to encrypt
			ResponseMessage responseMessage = (ResponseMessage) Utils.deserializeData(messageReceived.getMessage());
			
			if (messageReceived.getCounter() != messageCounter || responseMessage.equals(ResponseMessage.AUTHENTICATION_FAILURE)) return false;
			messageCounter++;
			
			return true;
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private SecretKey bankDHExchange() {
        try {
        	KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(2048);
            KeyPair serverKeyPair = keyPairGenerator.generateKeyPair();
            
            //Send DH Public key to atm
            byte[] serverPublicKey = serverKeyPair.getPublic().getEncoded();
            MessageSequence dhPublicKeyMsg = new MessageSequence(serverPublicKey, messageCounter);
			out.writeObject(dhPublicKeyMsg);
			
			messageCounter++;
	        
	        //Receive DH Public key from atm
			MessageSequence receivedPublicKeyDHmessage = (MessageSequence) in.readObject();
			if (receivedPublicKeyDHmessage.getCounter() != messageCounter) return null;
			messageCounter++;
			byte[] clientDHPublicKey = receivedPublicKeyDHmessage.getMessage();
			
			SecretKey secretKey = EncryptionUtils.calculateSecretSharedKey(serverKeyPair.getPrivate(), clientDHPublicKey);
			return secretKey;
		} catch (IOException | ClassNotFoundException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
}
