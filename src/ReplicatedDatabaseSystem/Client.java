package ReplicatedDatabaseSystem;
/*
 * File: Client.java
 */
import java.io.BufferedReader; 
import java.io.InputStreamReader; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 

/** 
 * A Client class which will get address of one of the server in the system
 * from Bootstrap and connects to that server in the system.
 * 
 * @author Binit Shah
 */
public class Client { 

	private final String BOOTSTRAP = "129.21.95.14"; 
	private final int BOOTSTRAP_PORT = 5000; 
	private final int PORT = 7000;
	private final String RETRIEVE = "RETRIEVE";
	private final String DELETE = "DELETE";
	private final String INSERT = "INSERT";
	private final String FIND = "FIND";
	private final int size = 2048; 
	private final String COLON = ":"; 
	private String SERVERIP = null; 
	private int SERVER_PORT = -1; 
	private DatagramSocket server = null; 

	/**
	 * Get the server address from Bootstrap to which client will connect
	 */
	public void getServerIP() { 
		try { 
			server = new DatagramSocket(PORT); 
			byte buf[] = "Client".getBytes(); 
			InetAddress aInetAddress = InetAddress.getByName(BOOTSTRAP); 

			System.out.println(aInetAddress.toString()); 
			DatagramPacket packet = new DatagramPacket(buf, buf.length, aInetAddress, BOOTSTRAP_PORT); 

			server.send(packet); 

			buf = new byte[size]; 
			packet = new DatagramPacket(buf, buf.length); 
			server.receive(packet); 

			String[] addr = new String(packet.getData()).split(COLON); 
			SERVERIP = addr[0].trim(); 
			SERVER_PORT = Integer.parseInt(addr[1].trim()); 

			System.out.println("Client Received IP of server: " + SERVERIP + COLON + SERVER_PORT); 

		} catch (Exception ex) { 
			ex.printStackTrace(); 
		} finally { 
			if (server != null) 
				server.close(); 
		} 
	} 


	/**
	 * Access the server to which it is connected and 
	 * Retrieve/Insert/Find/Delete data from the database server
	 */
	public void accessDB(){
		try{
			getServerIP(); 

			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
			DatagramPacket sendPacket; 
			while (true) { 

				DatagramSocket clientSocket = new DatagramSocket(); 
				InetAddress IPAddress = InetAddress.getByName(SERVERIP); 
				byte[] sendData = new byte[size]; 
				byte[] receiveData = new byte[size]; 

				// take input from client
				String sentence = inFromUser.readLine(); 

				// when client exits
				if (sentence.equalsIgnoreCase("exit")) { 

					System.out.println("Closing Connection!!"); 
					sendData = new byte[size]; 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

					System.exit(0); 

				} // client wants to insert some data
				else if (sentence.equalsIgnoreCase(INSERT)) { 

					sendData = new byte[size]; 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

					sendData = new byte[size]; 
					sentence = inFromUser.readLine(); 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

					System.out.println("Insertion Done!"); 

				} // client retrieves data from database
				else if (sentence.equalsIgnoreCase(RETRIEVE)) { 

					sendData = new byte[size]; 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
					clientSocket.receive(receivePacket); 

					String modifiedSentence = new String(receivePacket.getData()); 
					System.out.println("Data Receievd:\n" + modifiedSentence); 

				} // client wants to delete some data
				else if (sentence.equalsIgnoreCase(DELETE)) { 

					sendData = new byte[size]; 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

					sendData = new byte[size]; 
					sentence = inFromUser.readLine(); 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

				} // client wants to find some data
				else if (sentence.equalsIgnoreCase(FIND)) { 

					sendData = new byte[size]; 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket); 

					sendData = new byte[size]; 
					sentence = inFromUser.readLine(); 
					sendData = sentence.getBytes(); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, SERVER_PORT); 
					clientSocket.send(sendPacket);

					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
					clientSocket.receive(receivePacket);

					String modifiedSentence = new String(receivePacket.getData()); 
					System.out.println("FROM SERVER:" + modifiedSentence); 

				} 
				clientSocket.close(); 
			}
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}

	/**
	 * The main program.
	 * @param args		command line arguments(Ignored)
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception { 

		Client client = new Client();
		client.accessDB();    
	}
}