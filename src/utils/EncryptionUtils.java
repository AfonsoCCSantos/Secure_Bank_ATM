package utils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


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
	
	public static SecretKey calculateSecretSharedKey(PrivateKey ownPrivateKey, byte[] othersPublicKey) {
		SecretKey secretKey = null;
		try {
			KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
			keyAgreement.init(ownPrivateKey);
	        KeyFactory keyFactory = KeyFactory.getInstance("DH");
	        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(othersPublicKey);
	        PublicKey bankDHPKObject = keyFactory.generatePublic(x509KeySpec);
	        keyAgreement.doPhase(bankDHPKObject, true);
	        byte[] sharedSecret = keyAgreement.generateSecret();
	        secretKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return secretKey;
	}
	
	public static byte[] createHmac(SecretKey secretKey, byte[] message) {
		Mac hmacSha256;
		try {
			hmacSha256 = Mac.getInstance("HmacSHA256");
			hmacSha256.init(secretKey);
			byte[] hmacBytes = hmacSha256.doFinal(message);
			return hmacBytes;
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] createHash(byte[] message) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(message);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] sign(byte[] hash, PrivateKey privateKey) {
        Signature signature;
		try {
			signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(privateKey);
	        signature.update(hash);
	        return signature.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			e.printStackTrace();
		}
		return null;
    }
	
	public static boolean verifySignature(byte[] hash, byte[] signature, PublicKey publicKey) {
        Signature verifier;
		try {
			verifier = Signature.getInstance("SHA256withRSA");
			verifier.initVerify(publicKey);
	        verifier.update(hash);
	        return verifier.verify(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			e.printStackTrace();
		}
		return false;
        
    }
}
