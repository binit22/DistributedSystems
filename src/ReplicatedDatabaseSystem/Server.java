package ReplicatedDatabaseSystem;
/*
 * File: Server.java
 */
import java.io.*; 
import java.net.*; 
import java.util.HashMap; 
import java.util.Iterator; 
import java.util.Set; 

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/** 
 * Server class that will be connected to dbms server and to which client can 
 * join and request query
 * 
 * @author Binit Shah
 */
public class Server extends Thread { 

	private Mongo mongo; 
	private DB db; 
	private DBCollection items; 
	private String return_data = ""; 
	private DatagramSocket serverSocket = null;
	private ServerSocket server = null;

	private final String SERVER = "Server"; 
	private final String UPDATE_TABLE = "UPDATE_TABLE"; 
	private final String NewPrimary = "NewPrimary"; 
	private final String RETRIEVE = "RETRIEVE";
	private final String DELETE = "DELETE";
	private final String INSERT = "INSERT";
	private final String FIND = "FIND";
	private final String COMMA = ","; 
	private final String COLON = ":"; 
	private final int SERVER_PORT = 6000; 
	private final String BOOTSTRAP_IP = "129.21.95.14"; 
	private final int BOOTSTRAP_PORT = 5000; 
	private final int size = 2048; 
	private final int failure_Port = 3000; 
	private String PRIMARY_SERVER_IP = null; 
	private int PRIMARY_SERVER_PORT = -1; 
	private boolean isPrimary = false; 
	private boolean reportFailure = true;
	private int maxId;

	// stores Ip addresses of other active servers- IP:Port, count
	private static HashMap<String, Integer> IPADDRESS_TABLE = new HashMap<String, Integer>(); 

	public Server() { 

		try { 
			mongo = new Mongo(); 
			db = mongo.getDB("mydb"); 
			items = db.getCollection("items"); 
			serverSocket = new DatagramSocket(SERVER_PORT); 
			server = new ServerSocket(failure_Port); 

		} catch (UnknownHostException ex) { 
			//			ex.printStackTrace(); 
		} catch (SocketException e) {
			//			e.printStackTrace();
		} catch (IOException e) {
			//			e.printStackTrace();
		} 
	} 

	/**
	 * Retrieve data from the database
	 * 
	 * @return		data retrieved from the database
	 */
	public String retrieve() { 

		DBCursor cursor = items.find(); 
		String prev_string = ""; 
		String return_string; 

		while (cursor.hasNext()) { 

			DBObject obj = cursor.next(); 
			return_string = obj.toString(); 
			System.out.println(return_string); 
			prev_string += return_string + "\n"; 
		} 
		return prev_string; 
	} 

	/**
	 * Retrieve data from the database given a search key
	 * 
	 * @param key		key to be searched
	 * @return		result of the query
	 */
	public String find_one(String key) { 

		BasicDBObject allQuery = new BasicDBObject(); 
		BasicDBObject fields = new BasicDBObject(); 
		fields.put(key, 1); 
		String return_string; 
		String prev_string = ""; 

		DBCursor cursor = items.find(allQuery, fields); 
		while (cursor.hasNext()) { 

			DBObject obj = cursor.next(); 
			return_string = obj.toString(); 
			System.out.println(return_string); 
			prev_string += return_string + "\n"; 
		} 

		return prev_string; 
	} 

	/**
	 * Insert data into the database as a name:value pair
	 * 
	 * @param key	attribute name
	 * @param value		value of attribute
	 */
	public void insert(String key, String value) { 
		BasicDBObject doc = new BasicDBObject(); 
		doc.put(key, value); 
		items.insert(doc); 
		System.out.println("Inserted: " + key + " " + value); 
	} 

	/**
	 * Delete record from the database that matches key:value 
	 * 
	 * @param key		attribute name
	 * @param value		value of attribute
	 */
	public void delete(String key, String value) { 
		BasicDBObject doc = new BasicDBObject(); 
		doc.put(key, value); 

		items.remove(doc); 
	} 

	/**
	 * Server updates itself as primary and broadcasts other server to update
	 * the primary server
	 */
	public void update_self() { 

		PRIMARY_SERVER_IP = null; 
		PRIMARY_SERVER_PORT = -1; 
		isPrimary = true; 
		reportFailure = true;

		// replicate to other servers including bootstrap
		Set<String> key = IPADDRESS_TABLE.keySet(); 
		Iterator<String> it = key.iterator(); 
		try { 

			byte[] sendData = new byte[size];
			sendData = NewPrimary.getBytes();

			// send to bootstrap server
			InetAddress IPAddress = InetAddress.getByName(BOOTSTRAP_IP); 
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, BOOTSTRAP_PORT); 
			serverSocket.send(sendPacket); 

			while (it.hasNext()) { 
				String ip = it.next(); 
				String ip_port[] = ip.split(COLON); 

				// if IP address in the list is not its own then broadcast 
				InetAddress myAddress = InetAddress.getLocalHost(); 
				if (!ip_port[0].equalsIgnoreCase(myAddress.getHostAddress())) { 
					// broadcast to other active servers
					IPAddress = InetAddress.getByName(ip_port[0]); 
					int port = Integer.parseInt(ip_port[1]); 
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 
					serverSocket.send(sendPacket); 
				} 
			} 
		} catch (Exception e) { 
			//			e.printStackTrace(); 
		} 
	} 

	/**
	 * listens on port for client and other servers
	 */
	public void listen(){ 

		byte[] receiveData = null; 
		byte[] sendData = null; 

		String message = ""; 

		while (true) { 
			try { 
				return_data = ""; 

				receiveData = new byte[size]; 
				sendData = new byte[size]; 

				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
				System.out.println("Waiting for message..."); 

				serverSocket.receive(receivePacket); 

				// message command received from either client or server or bootstrap
				message = new String(receivePacket.getData()).trim(); 

				// retrieves all data from dbms server
				if (message.trim().equalsIgnoreCase(RETRIEVE)) { 
					return_data = retrieve(); 

				} // delete operation to delete record given a string
				else if (message.trim().equalsIgnoreCase(DELETE)) { 

					receiveData = new byte[size]; 
					DatagramPacket receivePacket_temp = new DatagramPacket(receiveData, receiveData.length); 
					serverSocket.receive(receivePacket_temp); 
					String received = new String(receivePacket_temp.getData()); 
					String[] fullString = received.split(COMMA); 
					delete(fullString[0], fullString[1]); 

					return_data = "DELETED"; 

				} // insert operation to insert record given a string
				else if ((message.trim().equalsIgnoreCase(INSERT))) { 

					receiveData = new byte[size]; 
					DatagramPacket receivePacket_temp = new DatagramPacket(receiveData, receiveData.length); 

					serverSocket.receive(receivePacket_temp); 

					String received = new String(receivePacket_temp.getData());
					System.out.println("Inserting " + received);
					String[] input = received.split(COMMA); 

					// if this is primary server, insert in its dbms server and replicate same to other servers
					if (isPrimary) {
						insert(input[0].trim(), input[1].trim()); 

						Set<String> key = IPADDRESS_TABLE.keySet(); 
						Iterator<String> it = key.iterator(); 
						// replicate to other servers 
						while (it.hasNext()) { 
							String ip = it.next(); 
							String address[] = ip.split(COLON);

							if(!address[0].equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress())){
								sendData = "insert_yourself".getBytes(); 

								InetAddress IPAddress = InetAddress.getByName(address[0]); 
								int port = Integer.parseInt(address[1]); 
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 

								serverSocket.send(sendPacket); 

								sendData = received.getBytes(); 
								sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 

								serverSocket.send(sendPacket);
							}
						} 

					} else { // send insert request to primary server 
						sendData = message.getBytes(); 
						InetAddress aInetAddress = InetAddress.getByName(PRIMARY_SERVER_IP); 
						DatagramPacket packet = new DatagramPacket(sendData, sendData.length, aInetAddress, PRIMARY_SERVER_PORT); 

						serverSocket.send(packet); 

						sendData = received.getBytes(); 
						packet = new DatagramPacket(sendData, sendData.length, aInetAddress, PRIMARY_SERVER_PORT); 

						serverSocket.send(packet); 
					} 
					return_data = "INSERTED"; 

				} // find operation to find record given a string
				else if (message.trim().equalsIgnoreCase(FIND)) {

					System.out.println("In FIND"); 
					receiveData = new byte[size]; 
					DatagramPacket receivePacket_temp = new DatagramPacket(receiveData, receiveData.length); 
					serverSocket.receive(receivePacket_temp); 
					String received = new String(receivePacket_temp.getData()); 
					System.out.println("In find received string" + received); 
					return_data = find_one(received.trim()); 

				} // primary server updates its table when a new server comes into the system 
				else if (message.trim().equalsIgnoreCase(UPDATE_TABLE)) {

					System.out.println("New Server Joined: " + receivePacket.getAddress().getHostAddress().toString());
					IPADDRESS_TABLE.put(receivePacket.getAddress().getHostAddress().toString() + COLON + receivePacket.getPort(), maxId + 1); 
					reportFailure = true;
					return_data = "";

				} // insert data into this server's dbms
				else if (message.trim().equalsIgnoreCase("insert_yourself")) {

					receiveData = new byte[size]; 
					DatagramPacket receivePacket_temp = new DatagramPacket(receiveData, receiveData.length); 

					serverSocket.receive(receivePacket_temp); 

					String received = new String(receivePacket_temp.getData()); 
					String[] input = received.split(COMMA); 

					insert(input[0].trim(), input[1].trim());
					return_data = "";

				} // inform bootstrap server that client has left 
				else if (message.trim().equalsIgnoreCase("exit")) {

					return_data = "Client left"; 
					sendData = new byte[size]; 
					sendData = return_data.getBytes(); 
					InetAddress aInetAddress = InetAddress.getByName(BOOTSTRAP_IP); 
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, aInetAddress, BOOTSTRAP_PORT); 
					serverSocket.send(sendPacket);

				} // bootstrap asks this server to start election algorithm
				else if (message.trim().equalsIgnoreCase("Elect")) {

					System.out.println("start election");
					receiveData = new byte[size]; 
					receivePacket = new DatagramPacket(receiveData, receiveData.length); 
					serverSocket.receive(receivePacket); 

					ByteArrayInputStream baos1 = new ByteArrayInputStream(receivePacket.getData()); 
					ObjectInputStream oos1 = new ObjectInputStream(baos1); 

					IPADDRESS_TABLE = (HashMap<String, Integer>)(oos1.readObject()); 

					Set<String> key = IPADDRESS_TABLE.keySet(); 
					Iterator<String> it = key.iterator(); 
					int final_id = -999; 
					String ip = ""; 
					InetAddress myAddress = InetAddress.getLocalHost(); 
					InetAddress final_Address = null; 
					while (it.hasNext()) { 

						String ip_toForward = it.next().toString(); 
						int id_temp = IPADDRESS_TABLE.get(ip_toForward); 

						if (id_temp > final_id) { 
							final_id = id_temp; 
							ip = ip_toForward; 
						} 
					} 

					maxId = final_id;
					String ip_port[] = ip.split(COLON); 
					final_Address = InetAddress.getByName(ip_port[0]); 

					// current server has max Id
					if (ip_port[0].equalsIgnoreCase(myAddress.getHostAddress())) { 
						IPADDRESS_TABLE.remove(myAddress.getHostAddress() + COLON + SERVER_PORT);
						update_self(); 

					} else { 
						sendData = new byte[size]; 
						sendData = "URPrimary".getBytes(); 
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, final_Address, Integer.parseInt(ip_port[1])); 
						serverSocket.send(sendPacket); 

						sendData = new byte[size]; 
						ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
						ObjectOutput out = new ObjectOutputStream(bos); 
						out.writeObject(IPADDRESS_TABLE); 
						sendData = bos.toByteArray(); 

						sendPacket = new DatagramPacket(sendData, sendData.length, final_Address, Integer.parseInt(ip_port[1])); 
						serverSocket.send(sendPacket); 
					} 
					return_data = "";

				} // update new primary server
				else if (message.trim().equalsIgnoreCase(NewPrimary)) { 

					System.out.println("New primary: " + receivePacket.getAddress().getHostName());
					PRIMARY_SERVER_IP = receivePacket.getAddress().getHostName(); 
					PRIMARY_SERVER_PORT = receivePacket.getPort(); 
					reportFailure = true;
					return_data = "";

				} // this server is elected as new primary server
				else if (message.trim().equalsIgnoreCase("URPrimary")) {

					receiveData = new byte[size]; 
					receivePacket = new DatagramPacket(receiveData, receiveData.length); 
					serverSocket.receive(receivePacket); 

					ByteArrayInputStream bais = new ByteArrayInputStream(receivePacket.getData()); 
					ObjectInputStream ois = new ObjectInputStream(bais); 

					IPADDRESS_TABLE = (HashMap<String, Integer>) (ois.readObject());
					System.out.println("I am primary");
					update_self();
					return_data = "";
				}


				// return the result of query to client
				if ("INSERTED".equalsIgnoreCase(return_data) || "DELETED".equalsIgnoreCase(return_data)) 
					return_data = ""; 
				if (!return_data.equalsIgnoreCase("")) { 
					// send retrieved data from db to the client
					sendData = new byte[size]; 
					InetAddress IPAddress = receivePacket.getAddress(); 
					int port = receivePacket.getPort(); 
					sendData = return_data.getBytes(); 
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); 
					serverSocket.send(sendPacket); 
				} 

			} catch (SocketException socEx) { 
				//				socEx.printStackTrace(); 
			} catch (SocketTimeoutException socTEx) { 
				//				socTEx.printStackTrace(); 
			} catch (Exception ex) { 
				//				ex.printStackTrace(); 
			} 
		} 
	}
	
	public void run() {
		this.listen();
	} 

	/**
	 * Server registers itself to the bootstrap and either becomes primary server
	 * or updates address of primary server with itself 
	 */
	public void registerToBootstrap() { 

		try { 
			byte buf[] = SERVER.getBytes(); 
			InetAddress aInetAddress = InetAddress.getByName(BOOTSTRAP_IP); 

			DatagramPacket packet = new DatagramPacket(buf, buf.length, aInetAddress, BOOTSTRAP_PORT); 

			serverSocket.send(packet); 

			buf = new byte[size]; 
			packet = new DatagramPacket(buf, buf.length); 

			serverSocket.receive(packet); 

			String data = new String(packet.getData()); 

			// data will be true:primaryServerIp:primaryServerPort for primary server
			String[] message = data.trim().split(COLON); 
			isPrimary = Boolean.parseBoolean(message[0]); 
			PRIMARY_SERVER_IP = message[1]; 
			PRIMARY_SERVER_PORT = Integer.parseInt(message[2]); 

			// if this server is not the primary server then inform primary server to update its table 
			// that maintains record of each server currently active in the system
			if (!isPrimary) { 

				byte string[] = UPDATE_TABLE.getBytes(); 
				InetAddress aInetAddressPrimary = InetAddress.getByName(PRIMARY_SERVER_IP); 
				DatagramPacket packet4Primary = new DatagramPacket(string, string.length, aInetAddressPrimary, PRIMARY_SERVER_PORT); 

				serverSocket.send(packet4Primary); 
			} else{
				System.out.println("I am primary server");
			}

			buf = new byte[size]; 
			packet = new DatagramPacket(buf, buf.length); 

			serverSocket.receive(packet); 

			data = new String(packet.getData()); 

			buf = new byte[size]; 
			packet = new DatagramPacket(buf, buf.length); 

		} catch (Exception ex) { 
			//			ex.printStackTrace(); 
		}
	} 

	/**
	 * Responses to other secondary servers that it is alive
	 */
	public void checkIfAlive() { 

		ObjectInputStream fromClient = null; 
		ObjectOutputStream toClient = null; 

		try { 
			String sentence2 = ""; 
			Socket clientSocket = null; 

			clientSocket = server.accept(); 

			fromClient = new ObjectInputStream(clientSocket.getInputStream()); 
			toClient = new ObjectOutputStream(clientSocket.getOutputStream()); 

			sentence2 = (String) fromClient.readObject(); 
			if (sentence2.trim().equalsIgnoreCase("IsAlive")) { 
				toClient.writeObject("IamAlive"); 
			} 
		} catch (Exception ex) { 
			//			ex.printStackTrace(); 
		} 
	}

	public void checkFailure() throws Exception{
		FailureHandler handle = null; 

		while (true) {

			if(reportFailure){
				handle = new FailureHandler(PRIMARY_SERVER_IP);
				
				// if this is primary server then respond to other servers that it is alive
				if (isPrimary) {
					checkIfAlive(); 

				} else { // check if primary server is alive by continuously pinging it
					try {
						handle.checkIfAlive(); 

					} catch (Exception ex) {

						// inform bootstrap about failure 
						byte string[] = "FAILURE".getBytes(); 
						InetAddress aInetAddressPrimary = InetAddress.getByName(BOOTSTRAP_IP); 
						DatagramPacket packet4Primary = new DatagramPacket(string, string.length, aInetAddressPrimary, BOOTSTRAP_PORT); 

						serverSocket.send(packet4Primary); 
						reportFailure = false;

						if(handle.clientSocket != null)
							handle.clientSocket.close();
					} 
					continue;
				}
			} 
		} 
	}
	/**
	 * The main program.
	 * 
	 * @param args		command line arguments(ignored)
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception { 

		Server updServer = new Server(); 
		updServer.registerToBootstrap(); 
		updServer.start(); 

		updServer.checkFailure();
	} 
}