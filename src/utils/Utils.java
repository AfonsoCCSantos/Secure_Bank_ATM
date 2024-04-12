package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Utils {
	
	private static final int RETURN_VALUE_INVALID = 255; 
	private static final int RETURN_CONNECTION_ERROR = 63;  
	
	public static ObjectOutputStream gOutputStream(Socket socket) {
        ObjectOutputStream outStream = null;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (SocketTimeoutException e) {
			Utils.printAndFlush("protocol_error\n");
			System.exit(RETURN_CONNECTION_ERROR);
        } catch (Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
        return outStream;
    }
	
	public static ObjectInputStream gInputStream(Socket socket) {
        ObjectInputStream inStream = null;
        try {
            inStream = new ObjectInputStream(socket.getInputStream());
        } catch (SocketTimeoutException e) {
			Utils.printAndFlush("protocol_error\n");
			System.exit(RETURN_CONNECTION_ERROR);
        } catch (Exception e) {
			System.exit(RETURN_VALUE_INVALID);
		}
        return inStream;
    }
	
	public static void printAndFlush(String toPrint) {
		System.err.print(toPrint);
		System.err.flush();
	}
	
	public static boolean verifyPort(String port) {
		int portNumber = 0;
		try {
			portNumber = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			return false;
		}
		if (port.charAt(0) == '0' || portNumber < 1024 || portNumber > 65535) {
			return false;
		}
		return true;
	}
	
	public static boolean verifyFileNames(String fileName) {
		if (fileName.length() < 1 || fileName.length() > 127 || fileName.equals(".") || fileName.equals("..")) 
			return false; 
		
		for(String c : fileName.split("")) {
			if (!c.matches("[_\\-\\.0-9a-z]")) 
				return false;
		}
		return true;
	}
	
	public static boolean verifyAccountName(String fileName) {
		if (fileName.length() < 1 || fileName.length() > 122) 
			return false; 
		
		for (String c : fileName.split("")) {
			if(!c.matches("[_\\-\\.0-9a-z]")) 
				return false;
		}
		return true;
	}
	
	public static boolean verifyIPAddress(String ipAddress) {
		String[] addressSeparated = ipAddress.split("\\."); 	
		if (addressSeparated.length != 4) 
			return false;
		
		for (String ipNumber : addressSeparated) {
			int num;
			try {
				num = Integer.parseInt(ipNumber);
			} catch (NumberFormatException e) {
				return false;
			}
			if(num < 0 || num > 255)
				return false;
		}
		return true;
	}

	public static boolean verifyAmount(String amount) {
		
		String[] amountSeparated = amount.split("\\.");
		if (amountSeparated.length != 2) 
			return false;
		
		String wholeAmount = amountSeparated[0];
		String fractionalPart = amountSeparated[1];
		
		if (fractionalPart.length() != 2)
			return false;
			
		double amountInDouble = 0.0;
		try {
			amountInDouble = Double.parseDouble(amount);	
		} catch (NumberFormatException e) {
			return false;
		}
		
		if (amountInDouble >= 1 && wholeAmount.charAt(0) == '0')
			return false;
		
		if (amountInDouble < 0 || amountInDouble > 4294967295.99) {
			return false;
		}

		return true;
	}
	
	public static byte[] serializeData(Object object) {
		byte[] result = null;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			objectOutputStream.flush();
            objectOutputStream.close();
            result = byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		return result;
	}
	
	public static Object deserializeData(byte[] objectInBytes) {
		Object result = null;
		try (ByteArrayInputStream bis = new ByteArrayInputStream(objectInBytes);
				ObjectInputStream ois = new ObjectInputStream(bis)) {
			result = ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			System.exit(RETURN_VALUE_INVALID);
		}
		return result;
	}
	
	public static int deleteFile(String fileName) {
		File myObj = new File(fileName);
		if (myObj.delete()) { 
			return 0;
		} 
		else {
			return -1;
		} 
	}
}
