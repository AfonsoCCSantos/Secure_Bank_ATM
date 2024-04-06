package utils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;


public class EncryptionUtils {
	
	private static final int RETURN_VALUE_INVALID = 255; 
	
	public static byte[] rsaEncrypt(byte[] data, PublicKey publicKey) {
		byte[] encryptedBytes = null;
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			encryptedBytes = cipher.doFinal(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encryptedBytes;
	}
	
	public static byte[] rsaDecrypt(byte[] encryptedData, PrivateKey privateKey) {
		byte[] decryptedBytes = null;
		try {
			Cipher d = Cipher.getInstance("RSA");
			d.init(Cipher.DECRYPT_MODE, privateKey);
			decryptedBytes = d.doFinal(encryptedData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decryptedBytes;
	}
	
	public static Object rsaDecryptAndDeserialize(byte[] encryptedData, PrivateKey Privatekey) {
		Object result = null;
		try {
			Cipher d = Cipher.getInstance("RSA");
			d.init(Cipher.DECRYPT_MODE, Privatekey);
			byte[] decryptedBytes = d.doFinal(encryptedData);
			result = (Object) Utils.deserializeData(decryptedBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static byte[] generateNonce(int length) {
        byte[] nonce = new byte[length];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);
        return nonce;
    }
	
    
	public static byte[] aesEncrypt(byte[] data, SecretKey key) {
		byte[] encryptedBytes = null;
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			encryptedBytes = cipher.doFinal(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encryptedBytes;
	}
	
	public static byte[] aesDecrypt(byte[] data, SecretKey key) {
		byte[] decryptedBytes = null;
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key);
			decryptedBytes = cipher.doFinal(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decryptedBytes;
	}
	
	public static Object aesDecryptAndDeserialize(byte[] encryptedData, SecretKey key) {
		Object result = null;
		try {
			byte[] decryptedBytes = aesDecrypt(encryptedData,key);
			result = (Object) Utils.deserializeData(decryptedBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
