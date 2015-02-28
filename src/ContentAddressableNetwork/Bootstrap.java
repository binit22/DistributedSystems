package ContentAddressableNetwork;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * File: Bootstrap.java
 * 
 * This is a Bootstrap server which is the first point of 
 * communication for all the Peers.
 * 
 * @author Binit
 *
 */
@SuppressWarnings("serial")
public class Bootstrap implements Serializable{

	public static Peer owner;
	public static final int port = 6780;
	public static int nodeCount;

	/**
	 * @param node		One of the peer
	 * @return		owner of the CAN
	 */
	public Peer getCANOwner(Peer node){
		// first peer joins CAN
		if(owner == null){
			Point bl = new Point(0,0);
			Point br = new Point(10,0);
			Point tr = new Point(10,10);
			Point tl = new Point(0,10);
			node.coordinate.add(bl);
			node.coordinate.add(br);
			node.coordinate.add(tr);
			node.coordinate.add(tl);
			owner = node;
		}
		return owner;
	}

	/**
	 * Get the entry node of CAN
	 * @throws Exception
	 */
	public void getEntryNode() throws Exception{
		ServerSocket server = null;
		ObjectInputStream fromClient = null;
		ObjectOutputStream toClient = null;
		try{
			server = this.getInstance();
			while(true){
				System.out.println("Listening on port " + port);

				Socket clientSocket = server.accept();
				nodeCount++;
				fromClient = new ObjectInputStream(clientSocket.getInputStream());
				toClient = new ObjectOutputStream(clientSocket.getOutputStream());

				Peer node = null;
				if(fromClient != null && (node = (Peer)fromClient.readObject()) != null){
					Peer pointOfContact = getCANOwner(node);
					System.out.println("Bootstrap listening " + pointOfContact.nodeIP + " " + node.nodeIP);

					toClient.writeObject(pointOfContact);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{
				if(server != null)
					server.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	/**
	 * @return		Instance of ServerSocket
	 * @throws IOException
	 */
	public ServerSocket getInstance() throws IOException{
		return new ServerSocket(port);
	}

	/**
	 * @param args	Command line arguments(Ignored)
	 */
	public static void main(String[] args) {
		System.out.println("Entering");
		try{
			Bootstrap server = new Bootstrap();
			server.getEntryNode();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
