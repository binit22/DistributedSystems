package ReplicatedDatabaseSystem;
/*
 * File: FailureHandler.java
 */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Instance of this class is used to continuously ping primary server to check for its failure 
 *
 * @author Binit Shah
 */
public class FailureHandler  {

	private String serverIP;

	public Socket clientSocket = null;
	private ObjectInputStream fromServer = null;
	private ObjectOutputStream toServer = null;

	public FailureHandler(String ip) {
		serverIP = ip;
	}

	/**
	 * Checks if primary server is alive
	 * 
	 * @throws Exception
	 */
	public void checkIfAlive() throws Exception {

		try {
			clientSocket = new Socket(serverIP, 3000);
			clientSocket.setSoTimeout(15000);
			toServer = new ObjectOutputStream(clientSocket.getOutputStream());
			fromServer = new ObjectInputStream(clientSocket.getInputStream());

			toServer.writeObject("IsAlive");
			fromServer.readObject();

		} catch(ConnectException ex){
			throw new Exception();
		
		} catch (SocketTimeoutException ex) {
			throw new Exception();
		
		} catch (SocketException ex) {
			throw new Exception();
		
		} catch (IOException ex) {
			throw new Exception();
		
		} catch (ClassNotFoundException e) {
			throw new Exception();
			
		} catch (Exception e) {
			throw new Exception();
		} 
	}
}
