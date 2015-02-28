package ContentAddressableNetwork;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * File: Peer.java
 * 
 * This class is a Peer which communicates with a Bootstrap server 
 * and join CAN
 * 
 * @author Binit
 *
 */
@SuppressWarnings("serial")
public class Peer extends Thread implements Serializable{

	public final String serverIP = "localhost" ;
	public final int serverPort = 6780;
	public InetAddress InetAddress = null;
	public String nodeIP;
	public int nodePort;
	public Point destPoint;
	public ArrayList<Point> coordinate;
	public Set<Peer> neighbors;
	public Set<String> files;
	public boolean isSquare;
	public String splitZone = "splitZone";
	public String removeNeighbor = "removeNeighbor";
	public String searchFile = "searchFile";
	public String insertFile = "insertFile";
	public String returnTarget = "returnTarget";
	public String updateTarget = "updateTarget";
	public String updateNeighbor = "updateNeighbor";
	public String updateFiles = "updateFiles";

	/**
	 * Initialize all the variables
	 */
	@SuppressWarnings("static-access")
	public Peer(){
		Scanner sc = null;
		try{
			this.InetAddress = InetAddress.getLocalHost();
			this.nodeIP = InetAddress.getHostAddress();
			do{ // get port number between 1000 and 9999
				this.nodePort = (int)(Math.random()*10000);
			}while(this.nodePort < 1000);
			this.isSquare = true;
			this.neighbors = new HashSet<Peer>();
			this.files = new HashSet<String>();
			this.coordinate = new ArrayList<Point>();
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(sc != null)
				sc.close();
		}
	}

	// get owner of CAN
	/**
	 * Communicates with Bootstrap to get owner of CAN
	 * @param toServer	Object Output writer to write to server
	 * @throws Exception
	 */
	public void requestEntryNode(ObjectOutputStream toServer) throws Exception{
		destPoint = new Point(Math.random()*10.0, Math.random()*10.0);

		toServer.writeObject(this);
	}

	/** 
	 * Keeps listening for other peers
	 */
	public void run(){
		ServerSocket server = null;
		Socket clientSocket = null;
		ObjectInputStream fromClient = null;
		ObjectOutputStream toClient = null;
		System.out.println("Waiting for peers at port... " + this.nodePort);

		try{
			server = new ServerSocket(this.nodePort);

			while(true){
				System.out.println("I AM WAITING");

				clientSocket = server.accept();
				fromClient = new ObjectInputStream(clientSocket.getInputStream());
				toClient = new ObjectOutputStream(clientSocket.getOutputStream());

				System.out.println("Peer 2 peer joined...");

				String flagString = null;
				Peer otherPeer = null;
				// read command from other peers
				flagString = (String)fromClient.readObject();

				// update this peer
				String fileName = null;
				if(flagString.equals(this.updateTarget)){
					otherPeer = (Peer)fromClient.readObject();
					this.coordinate = otherPeer.coordinate;
					this.neighbors = otherPeer.neighbors;
					this.isSquare = otherPeer.isSquare;
				}
				else if(flagString.equals(this.returnTarget)){
					toClient.writeObject(this);
				}
				else if(flagString.equals(this.updateNeighbor)){
					otherPeer = (Peer)fromClient.readObject();
					this.neighbors = otherPeer.neighbors;
					this.removeNeighbors();
					toClient.writeObject(this);
				}
				else if(flagString.equals(this.updateFiles)){
					otherPeer = (Peer)fromClient.readObject();
					this.files = otherPeer.files;
					toClient.writeObject(this);
				}
				else if(flagString.equals(this.splitZone)){
					otherPeer = (Peer)fromClient.readObject();
					this.splitZone(otherPeer);
				}
				else if(flagString.equals(this.removeNeighbor)){
					this.removeNeighbors();
				}
				else if(flagString.equals(this.insertFile)){
					fileName = (String)fromClient.readObject();
					this.files.add(fileName);
				}
				else if(flagString.equals(this.searchFile)){
					fileName = (String)fromClient.readObject();
					if(this.files.contains(fileName))
						toClient.writeObject("File found on " + this.InetAddress.getHostName());
					else
						toClient.writeObject("No such file");
				}
			}
		}catch(Exception ex){
		//	ex.printStackTrace();
		}finally{
			try{
				if(server != null)
					server.close();
				if(clientSocket != null)
					clientSocket.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	/**
	 * join this peer to CAN
	 * 
	 * @param fromServer	Object Input to read from server
	 * @return		Peer that needs to split in coordinate space
	 * @throws Exception
	 */
	public Peer join(ObjectInputStream fromServer) throws Exception{
		Peer entryNode = null;
		Socket nodeSocket = null;
		ObjectInputStream fromClient = null;
		Peer targetNode = null;
		while(true){
			ObjectOutputStream toPeerServer = null;

			// get owner from bootstrap
			entryNode = (Peer)fromServer.readObject();
			entryNode.destPoint = this.destPoint;

			// first peer in CAN
			if(this.nodeIP.equals(entryNode.nodeIP)){
				this.coordinate = entryNode.coordinate;
				System.out.println("Peer joined... ");
				break;
			}
			else{ // add peer to CAN, split zones and update neighbor peers
				try{
					targetNode = null;
					System.out.println("Destination point " + this.destPoint.getX() + " " + this.destPoint.getY());
					System.out.println("Entry Node " + entryNode.nodeIP + " " + entryNode.nodePort);

					nodeSocket = new Socket(entryNode.nodeIP, entryNode.nodePort);
					toPeerServer = new ObjectOutputStream(nodeSocket.getOutputStream());
					fromClient = new ObjectInputStream(nodeSocket.getInputStream());

					// write to other peer
					toPeerServer.writeObject(this.returnTarget);
					toPeerServer.writeObject(this);

					// read from other peer
					entryNode = (Peer)fromClient.readObject();

					Set<String> checkedPeer = new HashSet<String>();
					checkedPeer.add(entryNode.nodeIP);
					// get the target peer which is to split
					targetNode = this.getTargetZone(entryNode, entryNode.getCenter(), checkedPeer);

				}catch(Exception ex){
					ex.printStackTrace();
				}finally{
					try{
						if(nodeSocket != null)
							nodeSocket.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
				// split this peer
				this.assignZone(targetNode);

				// split target peer
				targetNode.splitZone(this);

				// add peers that are neighbor
				for(Peer neighbor : targetNode.neighbors){
					this.updateNeighbors(neighbor);
				}

				// remove peers that are no more neighbors
				targetNode.removeNeighbors();

				// add each other as neighbor
				this.neighbors.add(targetNode);								
				targetNode.neighbors.add(this);

				// update files of each peers
				this.updateFile(targetNode);

				break;
			}
		}
		return targetNode;
	}

	/**
	 * Get the target zone that has the desired point
	 * 
	 * @param targetNode	target node of CAN
	 * @param center	midpoint of target node
	 * @param checkedPeer	peers that has already been checked
	 * @return		target peer that has the desired point
	 */
	public Peer getTargetZone(Peer targetNode, Point center, Set<String> checkedPeer){

		Peer target = targetNode;
		//		route from owner peer to the target peer
		if(targetNode.containsPoint(this.destPoint)){
			return targetNode;
		}
		double minDiff = 9999;
		for(Peer neighbor : targetNode.neighbors){
			double diff = getDifference(neighbor.getCenter(), this.destPoint);
			neighbor.destPoint = this.destPoint;
			if(neighbor.containsPoint(this.destPoint)){
				return targetNode;
			}
			//	System.out.println("Difference.... " + diff);
			if(!checkedPeer.contains(neighbor.nodeIP) && diff <= minDiff){
				System.out.println("NEIGHBOR...");
				minDiff = diff;
				target = neighbor;
			}
			checkedPeer.add(neighbor.nodeIP);
		}
		System.out.println("Route... " + target.InetAddress.getHostName());
		target = getTargetZone(target, target.getCenter(), checkedPeer);

		return target;
	}

	/**
	 * @param targetNode	Node that is to be split
	 */
	public void assignZone(Peer targetNode){
		this.coordinate = new ArrayList<Point>();

		if(targetNode.isSquare){
			System.out.println("Squared... Making Rectangle");
			Point br = new Point(targetNode.coordinate.get(1).getX(),targetNode.coordinate.get(1).getY());
			Point tr = new Point(targetNode.coordinate.get(2).getX(),targetNode.coordinate.get(2).getY());
			Point bl = new Point((br.getX()+targetNode.coordinate.get(0).getX())/2.0, targetNode.coordinate.get(1).getY());
			Point tl = new Point((tr.getX()+targetNode.coordinate.get(3).getX())/2.0, targetNode.coordinate.get(2).getY());
			this.coordinate.add(bl);
			this.coordinate.add(br);
			this.coordinate.add(tr);
			this.coordinate.add(tl);
			this.isSquare = false;

		}else{
			System.out.println("Rectangle... Making Square");
			Point tr = new Point(targetNode.coordinate.get(2).getX(), targetNode.coordinate.get(2).getY());
			Point tl = new Point(targetNode.coordinate.get(3).getX(), targetNode.coordinate.get(3).getY());
			Point br = new Point(targetNode.coordinate.get(2).getX(), (tl.getY()+targetNode.coordinate.get(0).getY())/2.0);
			Point bl = new Point(targetNode.coordinate.get(3).getX(), (tr.getY()+targetNode.coordinate.get(1).getY())/2.0);
			this.coordinate.add(bl);
			this.coordinate.add(br);
			this.coordinate.add(tr);
			this.coordinate.add(tl);
			this.isSquare = true;
		}
		System.out.println("Peer joined: " + this.InetAddress.getHostName() + " " + this.nodeIP + " " + this.nodePort);
	}

	/**
	 * @param newNode	new node that is splitted
	 */
	public void splitZone(Peer newNode){

		if(newNode.isSquare){
			System.out.println("Rectangle... Making Square");
			Point tl = new Point(newNode.coordinate.get(0).getX(), newNode.coordinate.get(0).getY());
			Point tr = new Point(newNode.coordinate.get(1).getX(), newNode.coordinate.get(1).getY());
			Point bl = new Point(this.coordinate.get(0).getX(), this.coordinate.get(0).getY());
			Point br = new Point(this.coordinate.get(1).getX(), this.coordinate.get(1).getY());
			this.coordinate = new ArrayList<Point>();
			this.coordinate.add(bl);
			this.coordinate.add(br);
			this.coordinate.add(tr);
			this.coordinate.add(tl);
			this.isSquare = true;
		}else{
			System.out.println("Squared... Making Rectangle");
			Point br = new Point(newNode.coordinate.get(0).getX(), newNode.coordinate.get(0).getY());
			Point tr = new Point(newNode.coordinate.get(3).getX(), newNode.coordinate.get(3).getY());
			Point bl = new Point(this.coordinate.get(0).getX(), this.coordinate.get(0).getY());
			Point tl = new Point(this.coordinate.get(3).getX(), this.coordinate.get(3).getY());
			this.coordinate = new ArrayList<Point>();
			this.coordinate.add(bl);
			this.coordinate.add(br);
			this.coordinate.add(tr);
			this.coordinate.add(tl);
			this.isSquare = false;
		}
	}

	/**
	 * Removes from list of neighbors that are no more neighbors
	 */
	public void removeNeighbors(){
		Set<Peer> removedNeighbors = new HashSet<Peer>();
		
		for(Peer neighbor : this.neighbors){
			Point A0 = new Point(this.coordinate.get(0).getX(), this.coordinate.get(0).getY());
			Point A1 = new Point(this.coordinate.get(1).getX(), this.coordinate.get(1).getY());
			Point A2 = new Point(this.coordinate.get(2).getX(), this.coordinate.get(2).getY());
			//			Point A3 = new Point(this.coordinate.get(3).getX(), this.coordinate.get(3).getY());

			Socket nodeSocket = null;
			ObjectOutputStream toPeerServer = null;
			ObjectInputStream fromPeerServer = null;
			try{
				nodeSocket = new Socket(neighbor.nodeIP, neighbor.nodePort);
				toPeerServer = new ObjectOutputStream(nodeSocket.getOutputStream());
				fromPeerServer = new ObjectInputStream(nodeSocket.getInputStream());

				toPeerServer.writeObject(this.returnTarget);
				toPeerServer.writeObject(this);
				// get actual neighbor object
				neighbor = (Peer)fromPeerServer.readObject();
			}catch(Exception ex){
				ex.printStackTrace();
			}finally{
				try{
					if(nodeSocket != null)
						nodeSocket.close();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}

			Point B0 = new Point(neighbor.coordinate.get(0).getX(), neighbor.coordinate.get(0).getY());
			//			Point B1 = new Point(neighbor.coordinate.get(1).getX(), neighbor.coordinate.get(1).getY());
			Point B2 = new Point(neighbor.coordinate.get(2).getX(), neighbor.coordinate.get(2).getY());
			Point B3 = new Point(neighbor.coordinate.get(3).getX(), neighbor.coordinate.get(3).getY());

			// check if it is neighbor
			if((((A1.getY() == B3.getY()) || (A2.getY() == B0.getY())) && 
					((B3.getX() <= A1.getX() && A1.getX() <= B2.getX()) || (B3.getX() <= A0.getX() && A0.getX() <= B2.getX()))) ||
					(((A1.getX() == B3.getX()) || (A0.getX() == B2.getX())) &&
							((B0.getY() <= A1.getY() && A1.getY() <= B3.getY()) || (B0.getY() <= A2.getY() && A2.getY() <= B3.getY())))){
			}else{
				System.out.println("Removed " + neighbor.InetAddress.getHostName());
				removedNeighbors.add(neighbor);
			}
		}

		// remove which are no longer neighbors
		for(Peer neighbor : removedNeighbors){
			if(this.neighbors.contains(neighbor))
				this.neighbors.remove(neighbor);
		}
	}

	/**
	 * @param targetNode	whose neighbors need to be updated
	 */
	public void updateNeighbors(Peer targetNode){
		
		Point A0 = new Point(this.coordinate.get(0).getX(), this.coordinate.get(0).getY());
		Point A1 = new Point(this.coordinate.get(1).getX(), this.coordinate.get(1).getY());
		Point A2 = new Point(this.coordinate.get(2).getX(), this.coordinate.get(2).getY());
		//		Point A3 = new Point(this.coordinate.get(3).getX(), this.coordinate.get(3).getY());
		Point B0 = new Point(targetNode.coordinate.get(0).getX(), targetNode.coordinate.get(0).getY());
		//		Point B1 = new Point(targetNode.coordinate.get(1).getX(), targetNode.coordinate.get(1).getY());
		Point B2 = new Point(targetNode.coordinate.get(2).getX(), targetNode.coordinate.get(2).getY());
		Point B3 = new Point(targetNode.coordinate.get(3).getX(), targetNode.coordinate.get(3).getY());


		if((((A1.getY() == B3.getY()) || (A2.getY() == B0.getY())) && 
				((B3.getX() <= A1.getX() && A1.getX() <= B2.getX()) || (B3.getX() <= A0.getX() && A0.getX() <= B2.getX()))) ||
				(((A1.getX() == B3.getX()) || (A0.getX() == B2.getX())) &&
						((B0.getY() <= A1.getY() && A1.getY() <= B3.getY()) || (B0.getY() <= A2.getY() && A2.getY() <= B3.getY())))){
			Socket nodeSocket = null;
			ObjectOutputStream toPeerServer = null;
			ObjectInputStream fromPeerServer = null;
			try{
				System.out.println("Updating NEIGHBORS... " + targetNode.InetAddress.getHostName() + " " + targetNode.nodePort);

				nodeSocket = new Socket(targetNode.nodeIP, targetNode.nodePort);
				toPeerServer = new ObjectOutputStream(nodeSocket.getOutputStream());
				fromPeerServer = new ObjectInputStream(nodeSocket.getInputStream());
				System.out.println("UPDATE");

				targetNode.neighbors.add(this);
				toPeerServer.writeObject(this.updateNeighbor);
				toPeerServer.writeObject(targetNode);
				// get actual object of the neighbor
				targetNode = (Peer)fromPeerServer.readObject();
			}catch(Exception ex){
				ex.printStackTrace();
			}finally{
				if(nodeSocket != null){
					try{
						nodeSocket.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
			System.out.println("Added neighbor " + targetNode.InetAddress.getHostName());
			this.neighbors.add(targetNode);
		}
	}

	/**
	 * @param targetNode	update files of the target neighbor
	 */
	public void updateFile(Peer targetNode){

		for(String file : targetNode.files){			
			this.destPoint = getFileDestinationPoint(file);

			if(this.containsPoint(this.destPoint)){
				this.files.add(file);
			}
		}
		
		for(String file : this.files){			
			if(targetNode.files.contains(file))
				targetNode.files.remove(file);
		}		

		Socket nodeSocket = null;
		ObjectOutputStream toPeerServer = null;
		ObjectInputStream fromPeerServer = null;
		try{
			System.out.println("Updating FILES... " + targetNode.InetAddress.getHostName() + " " + targetNode.nodePort);

			nodeSocket = new Socket(targetNode.nodeIP, targetNode.nodePort);
			toPeerServer = new ObjectOutputStream(nodeSocket.getOutputStream());
			fromPeerServer = new ObjectInputStream(nodeSocket.getInputStream());

			// update files of the target peer
			toPeerServer.writeObject(this.updateFiles);
			toPeerServer.writeObject(targetNode);

			targetNode = (Peer)fromPeerServer.readObject();
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(nodeSocket != null){
				try{
					nodeSocket.close();
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param key		file keyword to search
	 */
	public void search(String key){
		
		this.destPoint = getFileDestinationPoint(key);
		System.out.println("Searching file at... " + this.destPoint.getX() + " " + this.destPoint.getY());

		if(this.containsPoint(this.destPoint)){
			if(this.files.contains(key))
				System.out.println(key + " found on " + this.InetAddress.getHostName());
		}else{
			Set<String> checkedPeer = new HashSet<String>();
			checkedPeer.add(this.nodeIP);
			Peer targetNode = this.getTargetZone(this, this.getCenter(), checkedPeer);

			Socket socket = null;
			ObjectOutputStream toPeerServer = null;
			ObjectInputStream fromPeerServer = null;
			try{
				socket = new Socket(targetNode.nodeIP, targetNode.nodePort);
				toPeerServer = new ObjectOutputStream(socket.getOutputStream());
				fromPeerServer = new ObjectInputStream(socket.getInputStream());

				toPeerServer.writeObject(this.searchFile);
				toPeerServer.writeObject(key);

				System.out.println((String)fromPeerServer.readObject());

			}catch(Exception ex){
				ex.printStackTrace();
			}finally{
				if(socket != null){
					try{	
						socket.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * @param key		file keyword to insert
	 */
	public void insert(String key){
		
		this.destPoint = getFileDestinationPoint(key);
		System.out.println("Insert file at... " + this.destPoint.getX() + " " + this.destPoint.getY());

		if(this.containsPoint(this.destPoint)){
			this.files.add(key);
			System.out.println("File uploaded on " + this.InetAddress.getHostName());
		}else{
			Set<String> checkedPeer = new HashSet<String>();
			checkedPeer.add(this.nodeIP);
			Peer targetNode = this.getTargetZone(this, this.getCenter(), checkedPeer);

			Socket socket = null;
			ObjectOutputStream toPeerServer = null;
			try{
				socket = new Socket(targetNode.nodeIP, targetNode.nodePort);
				toPeerServer = new ObjectOutputStream(socket.getOutputStream());

				toPeerServer.writeObject(this.insertFile);
				toPeerServer.writeObject(key);
				System.out.println("File uploaded on " + targetNode.InetAddress.getHostName());

			}catch(Exception ex){
				ex.printStackTrace();
			}finally{
				if(socket != null){
					try{	
						socket.close();
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}
		}
	}

	/** 
	 * overriding equals method to maintain unique neighbors w.r.t their IP
	 * @return		true if equal
	 */
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Peer)) {
			return false;
		}
		return (this.nodeIP.equals(((Peer)obj).nodeIP));
	}

	/**
	 * overriding hashCode method to keep consistency with equals method
	 * @return		hashcode value
	 */
	public int hashCode() {
		final int prime = 97;
		int result = this.nodePort * prime;
		return result;
	}

	/**
	 * @return	mid-point of a zone
	 */
	public Point getCenter(){
		double x = (this.coordinate.get(2).getX() - this.coordinate.get(0).getX())/2;
		double y = (this.coordinate.get(2).getY() - this.coordinate.get(0).getY())/2;
		return new Point(x, y);
	}

	/**
	 * @param one	one point
	 * @param two	other point
	 * @return		difference between two points
	 */
	public double getDifference(Point one, Point two){
		return Math.sqrt(Math.pow(Math.abs(one.getX() - two.getX()), 2) + Math.pow(Math.abs(one.getY() - two.getY()), 2));
	}

	/**
	 * Displays details of a Peer
	 */
	public void view(){
		
		System.out.println("\n=========== " + this.InetAddress.getHostName() + " ============");
		System.out.println("IP: " + this.nodeIP);
		System.out.println("Port: " + this.nodePort);

		StringBuffer zoneCoord = new StringBuffer();
		for(int index = 0; index < this.coordinate.size(); index++){
			zoneCoord.append("(");
			zoneCoord.append(this.coordinate.get(index).getX());
			zoneCoord.append(",");
			zoneCoord.append(this.coordinate.get(index).getY());
			zoneCoord.append(")");
		}
		System.out.println("My zone: " + zoneCoord);

		System.out.println("\nMy neighbors:");
		for(Peer neighbor : this.neighbors){
			System.out.println(neighbor.InetAddress.getHostName());
		}

		System.out.println("\nFiles:");
		for(String file : this.files){
			System.out.println(file);
		}
		System.out.println("================================\n");
	}

	/**
	 * @param destPoint		Point to check for
	 * @return		true if point lies within this Peer's zone
	 */
	public boolean containsPoint(Point destPoint){
		if(this.coordinate.get(0).getX() <= destPoint.getX() && 
				destPoint.getX() <= this.coordinate.get(2).getX() &&
				this.coordinate.get(0).getY() <= destPoint.getY() && 
				destPoint.getY() <= this.coordinate.get(2).getY()){
			return true;
		}
		return false;
	}

	/**
	 * @param key		File keyword
	 * @return		hash value of key at odd positions
	 */
	public double charAtOdd(String key){
		double x = 0.0;
		for(int index = 0; index < key.length(); index = index + 2){
			x += key.charAt(index);
		}
		return x%10.0;
	}

	/**
	 * @param key		File keyword
	 * @return		hash value of key at even positions
	 */
	public double charAtEven(String key){
		double y = 0.0;
		for(int index = 1; index < key.length(); index = index + 2){
			y += key.charAt(index);
		}
		return y%10.0;
	}

	/**
	 * @param key		File keyword
	 * @return		destination point of key
	 */
	public Point getFileDestinationPoint(String key){
		return new Point(charAtOdd(key), charAtEven(key));
	}

	/**
	 * @param args		Command line arguments(Ignored)
	 */
	public static void main(String[] args) {
		Socket nodeSocket = null;
		ObjectInputStream fromServer = null;
		ObjectOutputStream toServer = null;

		Socket socket = null;
		ObjectOutputStream toPeerServer = null;
		Scanner sc = null;

		try{
			Peer myself = new Peer();

			nodeSocket = new Socket(myself.serverIP, myself.serverPort);
			toServer = new ObjectOutputStream(nodeSocket.getOutputStream());
			fromServer = new ObjectInputStream(nodeSocket.getInputStream());

			// request owner node of CAN
			myself.requestEntryNode(toServer);
			// join the CAN and return target node that required to split
			Peer targetNode = myself.join(fromServer);

			if(targetNode != null){
				socket = new Socket(targetNode.nodeIP, targetNode.nodePort);
				toPeerServer = new ObjectOutputStream(socket.getOutputStream());

				toPeerServer.writeObject(myself.updateTarget);
				toPeerServer.writeObject(targetNode);

				System.out.println("Peer joined...");
			}
			myself.view();
			myself.start();

			while(true){
				try{
					System.out.println("Select an option:");
					System.out.println("1. Insert a file");
					System.out.println("2. Search for a file");
					System.out.println("3. View");
					int ch = 0;
					sc = new Scanner(System.in);
					ch = Integer.parseInt(sc.next());

					String file = null;
					switch(ch){
					case 1:
						System.out.println("Enter a file name to upload");
						file = sc.next();
						myself.insert(file);
						continue;
					case 2: 
						System.out.println("Enter a file name to search");
						file = sc.next();
						myself.search(file);
						continue;
					case 3: 
						myself.view();
						continue;				
					default:
						System.out.println("Please provide valid input");
						continue;
					}
				}catch(Exception ex){
					System.out.println("Please provide valid input");
					continue;
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{
				if(nodeSocket != null)
					nodeSocket.close();
				if(socket != null)
					socket.close();
				if(sc != null)
					sc.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
}
