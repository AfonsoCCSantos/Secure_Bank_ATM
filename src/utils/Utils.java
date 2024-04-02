package utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Utils {
	
	public static ObjectOutputStream gOutputStream(Socket socket) {
        ObjectOutputStream outStream = null;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return outStream;
    }
	
	public static ObjectInputStream gInputStream(Socket socket) {
        ObjectInputStream inStream = null;
        try {
            inStream = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return inStream;
    }
	
	public static void printAndFlush(String toPrint) {
		System.err.println(toPrint);
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
	
}
