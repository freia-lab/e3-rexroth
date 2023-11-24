import java.io.*;
import java.net.*;
 
import org.apache.commons.lang3.SystemUtils;

import com.boschrexroth.eal.Connection;
import com.boschrexroth.eal.EalDateAndTime;
import com.boschrexroth.eal.EalErrorMemoryTraceLog;
import com.boschrexroth.eal.EalSystemLanguage;
import com.boschrexroth.eal.EalSystemMode;
import com.boschrexroth.eal.Global;
import com.boschrexroth.eal.utils.DiagnosisTableHelper;

import com.boschrexroth.eal.EalAxisCondition;
import com.boschrexroth.eal.Motion;
import com.boschrexroth.eal.EalAxisUnits;

/**
 * This program implements a simple TCP/IP socketStreamDevice server that replies
 * to the message from the client.
 * This server allows MAX_CONN simultaneous client connections.
 *
 * @author K. Gajewski
 */
public class StreamDevServer { 
    static final int MAX_CONN = 1;           // Max number of connected clients
    private int clients = 0;
    
   public synchronized void changeVariable(int newValue) {
        this.clients = newValue;
        System.out.println("Variable changed to: " + newValue);
    }
   public synchronized void decreaseClients() {
        this.clients--;
        System.out.println("Variable changed to: " + this.clients);
    }

    public static void main(String[] args) throws Exception {
	StreamDevServer server = new StreamDevServer();
	
        if (args.length < 1) {
            System.out.println("Specify the port number" );
	    return;
	}
	

	    java.lang.System.out.println("\n\n\n##############################################################");
	    java.lang.System.out.println("System Setter's and Getter's API Sample");
	    java.lang.System.out.println("##############################################################\n\n");
	    Connection con = new Connection(false);
	    try {
		//		Connection con = new Connection(false);
	    // Connects to the drive
	    con.connect("192.168.10.46");
	    Thread.sleep(1000);

	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
	
        int port = Integer.parseInt(args[0]);
	String ipAddress = "192.168.10.108";
	
        try {
	    InetSocketAddress address = new InetSocketAddress(ipAddress, port);
	    ServerSocket serverSocket = new ServerSocket();
	    serverSocket.bind(address);
            System.out.println("Server is listening on port " + port);
	    int n;
	    // Set an infinite timeout (0 milliseconds) for accept
	    serverSocket.setSoTimeout(0);
	    while (true) {
		try {
		    Socket socket = serverSocket.accept();
		    System.out.println("Created a new socket" + server.clients);
		    server.clients++;
		    if (server.clients > MAX_CONN) {
			socket.close();
			server.clients--;
			System.out.println("Connection rejected. Max number of connctions (" + MAX_CONN + ") exceeded");
		    } else {
			System.out.println("New client connected. Number of clients: " + server.clients);
			ServerThread serverThread =  new ServerThread(server, socket, con);
			serverThread.start();
		    }
		} catch (SocketTimeoutException e) {
		    //		    e.printStackTrace();
		}
            }
 
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

/**
 * This thread is responsible to handle client connection.
 *
 * @author www.codejava.net
 */
class ServerThread extends Thread {
    private StreamDevServer srv;
    private Socket socket;
    private Connection con;
    private static Motion motion;
    private EalAxisUnits units;
    
    public ServerThread(StreamDevServer srv, Socket socket, Connection con) {
        this.socket = socket;
	this.srv = srv;
	this.con = con;
	try {
	     motion = con.getAxes(0).motion();
	     units = motion.getAxisUnits();	     
	} catch (Exception e) {
	    System.out.println("Error:"+e.getLocalizedMessage());
	}
    }
    
    @Override
    public void run() {
        try {
	    InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

 
            String text;
 
            do {
                text = reader.readLine();
		if (text == null) break;
		//                String reverseText = new StringBuilder(text).reverse().toString();
		//                writer.println("Server: " + reverseText);
		String reply = this.parseString(con, text);
                writer.println(reply);
 
            } while (!text.equals("bye"));
 
            socket.close();
	    srv.decreaseClients();
	    
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    private static String parseString(Connection con, String input) {
        // Split the input string into words
        String[] words = input.split("\\s+");

        // Check if there are at least one word in the input
        if (words.length > 0) {
            String action = words[0].toLowerCase(); // Assuming actions are case-insensitive

            // Perform different actions based on the first word
            switch (action) {
	    case "*idn?":
		    return getId(con, words);

	    case "pos?":
		    return getPos(words);

	    case "state?":
		    return getMotionState(words);

	    case "stateext?":
		    return getMotionStateExt(words);

	    case "vel?":
		return getVelocity(words);

	    default:
		return "Unknown action: " + action;
            }
        } else {
            return "Input string is empty";
        }
    }

    private static String getId(Connection con, String[] words) {
        // Implement the logic for Action1
        StringBuilder result = new StringBuilder("");
	try {
	    com.boschrexroth.eal.System system = con.getAxes(0).system();
	    result.append(system.getName()).append(",SN:").append(system.getSerialNumber()).append(",Ver:").append(system.getVersionInfo("EAL"));
	    //	    java.lang.System.out.println("Gateway Address-" + system.getGateway());
	    //java.lang.System.out.println("Hardware Details-" + system.getHardwareDetails());
	    //java.lang.System.out.println("Ip Address-" + system.getIpAddress());
	    //java.lang.System.out.println("Operating Hours-" + system.getOperationHours());
	    //java.lang.System.out.println("Serial Number-" + system.getSerialNumber());
	    //java.lang.System.out.println("Version Info-" + system.getVersionInfo("EAL"));
	    //java.lang.System.out.println("\n\n**************************************************\n\n");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }

    private static String getMotionState(String[] words) {
        // Implement the logic for getState
        StringBuilder result = new StringBuilder("state: ");
	try {
	    result.append(Integer.toString(motion.getState()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }
    private static String getMotionStateExt(String[] words) {
        // Implement the logic for getStateExtended
        StringBuilder result = new StringBuilder("stateext: ");
	try {
	    result.append(Integer.toString(motion.getStateExtended()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }
    private static String getPos(String[] words) {
        // Implement the logic for getPos
        StringBuilder result = new StringBuilder("pos: ");
	try {
	    result.append(Double.toString(motion.getActualPosition()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }
    private static String getVelocity(String[] words) {
        // Implement the logic for getVelocity
        StringBuilder result = new StringBuilder("vel: ");
	try {
	    result.append(Double.toString(motion.getActualVelocity()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }

    // Add more methods for other actions as needed

}

