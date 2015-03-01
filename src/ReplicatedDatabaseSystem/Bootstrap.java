package ReplicatedDatabaseSystem;
/*
 * File: Bootstrap.java
 */
import java.io.ByteArrayOutputStream; 
import java.io.IOException; 
import java.io.ObjectOutput; 
import java.io.ObjectOutputStream; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 
import java.net.InetAddress; 
import java.net.SocketException; 
import java.util.HashMap; 
import java.util.Iterator; 
import java.util.Set; 
  
/** 
 * Bootstrap class which is the first point of contact for client and server
 * in the system. 
 * 
 * @author Binit Shah
 */
public class Bootstrap implements Runnable { 
  
    private HashMap<String, Integer> currentConnection; 
    private HashMap<String, Integer> serverId; 
  
    private boolean isPrimary = false; 
    private String primaryServerIP = null; 
    private int primaryServerPORT = -1; 
    private String CLIENT = "Client"; 
    private String SERVER = "Server"; 
    private String COLON = ":"; 
    private String CLIENT_EXIT = "Client left"; 
    private boolean FAILURE_REPORTED = false; 
    private final String FAILURE = "FAILURE"; 
    private final String ELECT_MESSAGE = "Elect"; 
    private int count = 1; 
    private String NewPrimary = "NewPrimary"; 
    private int SIZE = 2048; 
  
    // Initialization in constructor
    public Bootstrap() { 
        currentConnection = new HashMap<String, Integer>(); 
        serverId = new HashMap<String, Integer>(); 
    } 
  
    /**
     * registers server IP
     * 
     * @param ip	IP address of server along with port separated by ':'
     * @return		reply message
     */
    public String registerIP(String ip) { 
  
        String address[] = ip.split(COLON); 
        int port = Integer.parseInt(address[1]); 
        if (address[0] != null && port > 0) { 
  
            if (!serverId.containsKey(ip)) { 
  
                currentConnection.put(address[0], 0); 
                serverId.put(ip, count++); 
  
                return "Connected"; 
            } else return "IP already Exists"; 
        } else return "Address not received"; 
    } 
  
  
    /**
     * Assign server with less load to a client
     * 
     * @return		reply Ip of server to which client will be connected
     */
    public String assignServer() { 
  
        String value = null;   
        if (!currentConnection.isEmpty()) { 
  
            Set<String> key = serverId.keySet(); 
            Iterator<String> it = key.iterator(); 
  
            int count = 999; 
            String addressPicked = null; 
  
            while (it.hasNext()) { 
  
                String ip = it.next().toString(); 
                String address[] = ip.split(COLON); 
  
                if (count > currentConnection.get(address[0])) { 
                    count = currentConnection.get(address[0]); 
                    value = ip; 
  
                    addressPicked = address[0]; 
                } 
            } 
  
            System.out.println("  Connecting to :  " + addressPicked + " Count was: " + count); 
            currentConnection.put(addressPicked, count + 1); 
        } 
        return value; 
    } 
  
    /**
     * listens on port for client and server
     */
    public void listen(){
    	  
        DatagramSocket socket; 
        DatagramPacket packet; 
        int port = 5000; 
        byte[] buf = null; 
  
        synchronized (this) { 

        	while (true) { 
                try { 
                    boolean localPrimary = false; 
                    buf = new byte[SIZE]; 
                    socket = new DatagramSocket(5000); 
  
                    packet = new DatagramPacket(buf, buf.length); 
                    socket.receive(packet); 
  
                    String check = new String(packet.getData()); 
                    check = check.trim(); 
  
                    InetAddress address = packet.getAddress(); 
                    port = packet.getPort(); 
                    String value = ""; 
  
                    System.out.println("Packet Received: " + check + "  From:  " + address.getHostAddress()); 
  
                    // client contacts
                    if (check.trim().equalsIgnoreCase(CLIENT)) { 
  
                        value = assignServer(); 
                        if (value == null) 
                            value = "No Server Found"; 
  
                    } // server contacts
                    else if (check.trim().equalsIgnoreCase(SERVER)) { 
  
                        if (!isPrimary) { 
                            isPrimary = true; 
                            primaryServerIP = address.getHostAddress(); 
                            primaryServerPORT = port; 
                            localPrimary = true; 
                        } 
  
                        // Adds the received IP address to the hashMap if doesn't exists 
                        value = registerIP(address.getHostAddress() + COLON + port); 
  
                        buf = new byte[SIZE]; 
                        buf = (localPrimary + COLON + primaryServerIP + COLON + primaryServerPORT).getBytes(); 
                        packet = new DatagramPacket(buf, buf.length, address, port); 
                        socket.send(packet);   
                        System.out.println("Table Updated: " + serverId); 
  
                    } // decrement load on server if client exits
                    else if (check.trim().equalsIgnoreCase(CLIENT_EXIT)) { 
  
                        currentConnection.put(address.getHostAddress(), currentConnection.get(address.getHostAddress()) - 1); 
                        System.out.println("Client Left, Count updated: " + currentConnection); 
  
                    } // if server detects failure of primary server, send message to start election
                    else if (check.trim().equalsIgnoreCase(FAILURE)) { 
  
                        if (!FAILURE_REPORTED) { 
  
                            serverId.remove(primaryServerIP + ":" + primaryServerPORT); 
                            currentConnection.remove(primaryServerIP); 
                            isPrimary = false; 
  
                            System.out.println("Table Updated: " + serverId); 
  
                            buf = new byte[SIZE]; 
                            buf = (ELECT_MESSAGE).getBytes(); 
  
                            System.out.println("ELECTION REQUESTED BY: " + address + ":" + port); 
                            packet = new DatagramPacket(buf, buf.length, address, port); 
                            socket.send(packet); 
  
                            buf = new byte[SIZE]; 
  
                            ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
                            ObjectOutput out = new ObjectOutputStream(bos); 
                            out.writeObject(serverId); 
                            buf = bos.toByteArray(); 
  
                            packet = new DatagramPacket(buf, buf.length, address, port); 
                            socket.send(packet); 
                            FAILURE_REPORTED = true; 
                        } 
                    } // register address of new primary server
                    else if (check.trim().equalsIgnoreCase(NewPrimary)) { 
  
                        primaryServerIP = packet.getAddress().getHostAddress().toString(); 
                        primaryServerPORT = packet.getPort(); 
                        isPrimary = true; 
  
                        serverId.put(primaryServerIP + ":" + primaryServerPORT, count++); 
                        currentConnection.put(primaryServerIP, currentConnection.get(primaryServerIP)); 
  
                        System.out.println("Primary Server Selected: " + primaryServerIP + COLON + primaryServerPORT); 
                        FAILURE_REPORTED = false;
                    } else { 
                        value = "No Action Done"; 
                    } 
  
                    buf = value.getBytes(); 
                    packet = new DatagramPacket(buf, buf.length, address, port); 
                    socket.send(packet); 
  
                    socket.close(); 
                    
                } catch (SocketException e) { 
                    e.printStackTrace(); 
                } catch (IOException e) { 
                    e.printStackTrace(); 
                } catch (Exception e) { 
                    e.printStackTrace(); 
                } 
            } 
        } 
    }
    
    public void run() {
    	this.listen();
    } 
  
    /** 
     * The main program.
     * @param args		command line arguments(ignored)
     */
    public static void main(String[] args) { 
  
        Bootstrap bootstrap = new Bootstrap(); 
  
        Thread thread = new Thread(bootstrap); 
        thread.start(); 
    } 
}