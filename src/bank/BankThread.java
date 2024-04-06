package bank;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
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
	

	public BankThread(Socket socket, Map<String, BankAccount> accounts, PrivateKey privateKey) {
		super();
		this.socket = socket;
		this.accounts = accounts;
		this.privateKey = privateKey;
	}
	
	public void run() {
		try {
			socket.setSoTimeout(10000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			ObjectInputStream in = Utils.gInputStream(socket);
			ObjectOutputStream out = Utils.gOutputStream(socket);
			BankSkel bankSkel = new BankSkel(in, out, accounts);
			int messageCounter = 0;
			while (true) {
				byte[] commandInBytes = (byte[]) in.readObject();
				MessageSequence commandMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(commandInBytes, privateKey);
				if (commandMessage.getCounter() != messageCounter) return;
				messageCounter++;
				RequestType command = (RequestType) Utils.deserializeData(commandMessage.getMessage());

				switch (command) {
					case CREATE_ACCOUNT:
						//Get public key from client
//						byte[] clientPublicKeyBytes = (byte[]) in.readObject();
						MessageSequence clientPublicKeyMessage = (MessageSequence) in.readObject();
						if (clientPublicKeyMessage.getCounter() != messageCounter) return;
						messageCounter++;
						PublicKey clientPublicKey = (PublicKey) Utils.deserializeData(clientPublicKeyMessage.getMessage());
						
						//Generate nonce and send it to client
						byte[] nonce = EncryptionUtils.generateNonce(32);
						MessageSequence nonceMessage = new MessageSequence(nonce, messageCounter);
						byte[] encryptedNonceMessage = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceMessage), clientPublicKey);
						out.writeObject(encryptedNonceMessage);
						messageCounter++;
						
						//Receive nonce back from client
						byte[] receivedNonceBytes = (byte[]) in.readObject();
						MessageSequence receivedNonceMessage = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(receivedNonceBytes, privateKey);
						if (receivedNonceMessage.getCounter() != messageCounter) {
							System.out.println(messageCounter);
							System.out.println(receivedNonceMessage.getCounter());
							return;
						}
						messageCounter++;
						
						//Check if client correctly decrypted nonce
						if (!Arrays.equals(nonce, receivedNonceMessage.getMessage())) {
							MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.AUTHENTICATION_FAILURE), messageCounter);
							out.writeObject(returnMessage); //Think whether it makes sense to encrypt
							return;
						}
						MessageSequence returnMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
						out.writeObject(returnMessage);
						messageCounter++;
						
						//Receiving nonce from client
						byte[] nonceEncrypted = (byte[]) in.readObject();
						MessageSequence nonceDecrypted = (MessageSequence) EncryptionUtils.rsaDecryptAndDeserialize(nonceEncrypted, privateKey); 
						if (nonceDecrypted.getCounter() != messageCounter) return;
						messageCounter++;
						
						//Send nonce back to client
						MessageSequence nonceToSend = new MessageSequence(nonceDecrypted.getMessage(), messageCounter);
						byte[] encryptedBytes = EncryptionUtils.rsaEncrypt(Utils.serializeData(nonceToSend), clientPublicKey);
						out.writeObject(encryptedBytes);
						messageCounter++;
						
						//Receive response from client - whether authentication was successful
						MessageSequence messageReceived = (MessageSequence) in.readObject(); //Think whether it makes sense to encrypt
						ResponseMessage responseMessage = (ResponseMessage) Utils.deserializeData(messageReceived.getMessage());
						
						if (messageReceived.getCounter() != messageCounter || responseMessage.equals(ResponseMessage.AUTHENTICATION_FAILURE)) return;
						messageCounter++;

						//Begin DH
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
						if (receivedPublicKeyDHmessage.getCounter() != messageCounter) return;
						messageCounter++;
						byte[] clientDHPublicKey = receivedPublicKeyDHmessage.getMessage();
 						
						KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
				        keyAgreement.init(serverKeyPair.getPrivate());
				        KeyFactory keyFactory = KeyFactory.getInstance("DH");
				        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientDHPublicKey);
				        PublicKey clientDHPKObject = keyFactory.generatePublic(x509KeySpec);
				        keyAgreement.doPhase(clientDHPKObject, true);
				        byte[] sharedSecret = keyAgreement.generateSecret();
				        SecretKey secretKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
				        
				        //Here the bank has a secret key to use in the communications
				        
				        //Server receives account and value encrypted from client
				        byte[] requestMessageEncrypted = (byte[]) in.readObject();
						MessageSequence requestMessageSequence = (MessageSequence) EncryptionUtils.aesDecryptAndDeserialize(requestMessageEncrypted, secretKey);
						if(requestMessageSequence.getCounter() != messageCounter) 
							return;
						RequestMessage requestMessageReceived = (RequestMessage) Utils.deserializeData(requestMessageSequence.getMessage());
						int returnCode = bankSkel.createAccount(requestMessageReceived.getAccount(), requestMessageReceived.getValue());
						messageCounter++;
						
						//Bank sends result of operation to client
						MessageSequence operationResultMessage = new MessageSequence(Utils.serializeData(ResponseMessage.SUCCESS), messageCounter);
						if (returnCode == ACCOUNT_ALREADY_EXISTS) {
							operationResultMessage.setMessage(Utils.serializeData(ResponseMessage.ACCOUNT_ALREADY_EXISTS));
						}
						encryptedBytes = EncryptionUtils.aesEncrypt(Utils.serializeData(operationResultMessage), secretKey);
						out.writeObject(encryptedBytes);
						messageCounter++;
						
						System.out.println("Fim da operação");
						break;
					case DEPOSIT:
//						returnCode = bankSkel.deposit(request.getAccount(), request.getValue());
//						if (returnCode == ACCOUNT_DOESNT_EXIST) {
//							out.writeObject(ResponseMessage.ACCOUNT_DOESNT_EXIST);
//						}
//						else if (returnCode == SUCCESS) {
//							out.writeObject(ResponseMessage.SUCCESS);
//						}
						break;
					case WITHDRAW:
//						returnCode = bankSkel.withdraw(request.getAccount(), request.getValue());
//						if (returnCode == ACCOUNT_DOESNT_EXIST) {
//							out.writeObject(ResponseMessage.ACCOUNT_DOESNT_EXIST);
//						}
//						else if(returnCode == NEGATIVE_BALANCE) {
//							out.writeObject(ResponseMessage.NEGATIVE_BALANCE);
//						}
//						else if (returnCode == SUCCESS) {
//							out.writeObject(ResponseMessage.SUCCESS);
//						}
						break;
					case GET_BALANCE:
//						double currentBalance = bankSkel.getBalance(request.getAccount());
//						if (currentBalance == ACCOUNT_DOESNT_EXIST) {
//							out.writeObject("ACCOUNT_DOESNT_EXIST"); //This one still sends a string so we can send the 
//																	 //currentBalance as a string too
//						}
//						out.writeObject(String.format(Locale.ROOT, "%.2f", currentBalance));
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
}
