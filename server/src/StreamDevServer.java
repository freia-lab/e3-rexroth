import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date; 
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
 * This program implements a simple TCP/IP socketStreamDevice serve that replies
 * to the message from the client.
 * This server allows MAX_CONN simultaneous client connections.
 *
 * @author K. Gajewski
 */
public class StreamDevServer { 
    static final int MAX_CONN = 2;           // Max number of connected clients
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
	java.lang.System.out.println("                     StreamDevice server");
	java.lang.System.out.println("##############################################################\n\n");
	Connection con = new Connection(false);
	String ipAddress = "";

        int port = Integer.parseInt(args[0]);
	if (args.length > 1)  {
	    ipAddress = args[1];
	}
	else {
	    ipAddress = "localhost";
	}
	
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
		    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String timestamp = dateFormat.format(new Date());
		    System.out.println(timestamp + " - Server exception: " + e.getMessage());
		    // e.printStackTrace();
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
    private static EalAxisUnits units;
    private static  SimpleDateFormat dateFormat;
    
    public ServerThread(StreamDevServer srv, Socket socket, Connection con) {
        this.socket = socket;
	this.srv = srv;
	this.con = con;
	dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
    
    @Override
    public void run() {
        try {
	    InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

 
            String text;
	    String reply = "";
 
            do {
                text = reader.readLine();
		if (text == null) break;
		//                String reverseText = new StringBuilder(text).reverse().toString();
		//                writer.println("Server: " + reverseText);
		try {
		    reconnect (con);
		    reply = this.parseString(con, text);
		} catch (Exception e) {
		    //		    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String timestamp = dateFormat.format(new Date());
		    System.err.println(timestamp + " - Connection to server failed. Retrying in 3 seconds...");

		    try {
			Thread.sleep(5000); // Wait for 5 seconds before attempting to reconnect
		    } catch (InterruptedException ignored) {
		    }
		}

                writer.println(reply);
 
            } while (!text.equals("bye"));
 
            socket.close();
	    srv.decreaseClients();
	    
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void reconnect(Connection con) {
	try {
	    // Connects to the drive
	    if (!con.isConnected()) {
		con.connect("192.168.10.46");
		Thread.sleep(1000);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}

	try {
	    motion = con.getAxes(0).motion();
	    units = motion.getAxisUnits();
	} catch (Exception e) {
	    System.out.println("Error:"+e.getLocalizedMessage());
	}
    }

    private static String binaryToDecimal (String binaryString) {
	if (!binaryString.matches("0b[01]{4}\\.[01]{4}\\.[01]{4}\\.[01]{4}")) {
            System.out.println("Invalid binary string format");
            return "";
        }

	String binString = binaryString.substring(2); // Remove the "0b" prefix
        String[] binaryParts = binString.split("\\.");
        StringBuilder binaryValue = new StringBuilder(""); // Add the binary point
	
        for (String part : binaryParts) {
            binaryValue.append(part);
        }
       
        return Long.toString(Long.parseLong(binaryValue.toString(), 2));
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

	    case "acc?":
		return getActAcceleration(words);

	    case "torq?":
		return getActTorque(words);

	    case "diagno?":
		return getDiagNo();

	    case "diagtxt?":
		return getDiagTxt();

	    case "setpm":
		return setMode (EalAxisCondition.EAL_AXIS_CONDITION_ACTIVE_PARAMETERIZATION);

	    case "setom":
		return setMode (EalAxisCondition.EAL_AXIS_CONDITION_ACTIVE);

	    case "driveenable":
		return driveEnable (true);
		
	    case "drivedisable":
		return driveEnable (false);

	    case "clrerr":
		return clearError (con);

	    case "reboot":
		return reboot (con);

	    case "moveabs":
		return move(con, true, words);

	    case "moverel":
		return move(con, false, words);

	    case "stop":
		return stop(con);

	    case "velocity":
		return setPar(con, words, "S-0-0259.0.0");
	       
	    case "accel":
		return setPar(con, words, "S-0-0260.0.0");
	       
	    case "decel":
		return setPar(con, words, "S-0-0359.0.0");
	       
	    case "jerk":
		return setPar(con, words, "S-0-0193.0.0");
	       
	    case "velocity?":
		return getPar(con, "S-0-0259.0.0", "velocity: ");
	       
	    case "accel?":
		return getPar(con, "S-0-0260.0.0", "accel: ");
	       
	    case "decel?":
		return getPar(con, "S-0-0359.0.0", "decel: ");
	       
	    case "jerk?":
		return getPar(con, "S-0-0193.0.0", "jerk: ");
	       
	    case "psstatus?":
		return getPar(con, "P-0-0861.0.0", "psstatus: ");
	       
	    case "posstatus?":
		return getPar(con, "S-0-0437.0.0", "posstatus: ");
	       
	    case "par?":
		return getParAsString(con, words);

	    case "par":
		return setParAsString(con, words);

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
    private static String getActAcceleration(String[] words) {
        // Implement the logic for getActualAcceleration
        StringBuilder result = new StringBuilder("acc: ");
	try {
	    result.append(Double.toString(motion.getActualAcceleration()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }
    private static String getActTorque(String[] words) {
        // Implement the logic for getActualTorque
        StringBuilder result = new StringBuilder("torq: ");
	try {
	    result.append(Double.toString(motion.getActualTorque()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }
    private static String getDiagNo() {
        // Implement the logic for getDiagnosisNumber
        StringBuilder result = new StringBuilder("diagno: ");
	try {
	    //	    System.out.println("diagno="+Integer.toUnsignedLong(motion.getDiagnosisNum()));
	    result.append(Integer.toUnsignedLong(motion.getDiagnosisNum()));	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }
    private static String getDiagTxt() {
        // Implement the logic for getDiagnosisText
        StringBuilder result = new StringBuilder("diagtxt: ");
	try {
	    result.append(motion.getDiagnosisText());	   
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return result.toString();
    }

    private static String setMode(EalAxisCondition code) {
        // Implement the logic for 
	try {
	    motion.setCondition(code);
	    return("OK");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return "";
    }

    private static String driveEnable(boolean powerOn) {
        // Implement the logic for 
	try {
	    motion.movement().power(powerOn);
	    return("OK");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return "";
    }
    
    private static String clearError(Connection con) {
        // Implement the logic for clear error
	try {
	    con.getAxes(0).system().clearError();
	    return("OK");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return "";
    }

    private static String reboot(Connection con) { //doesn't work from Epics
        // Implement the logic for reboot
	try {
	    con.getAxes(0).system().cleanup();
	    return("OK");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return "";
    }

    private static String move(Connection con, boolean absolute, String[] words) {
        // Implement the logic for move absolute/relative
	if (words.length > 1) {
	    System.out.println(Double.parseDouble(words[1]));
	    System.out.println(words[1]);
	    try {
		// Write positioning cmd value
		con.getAxes(0).parameter().writeDataAsString("S-0-0282", words[1]);
		// Get the Positioning control word
		String oldCtrlWord = con.getAxes(0).parameter().readDataAsString("S-0-0346.0.0");
		StringBuffer newCtrlWord = new StringBuffer(oldCtrlWord);
		char bit = oldCtrlWord.charAt(20);
		if  (bit == '0') { bit = '1'; } else { bit = '0'; }
		newCtrlWord.setCharAt(20,bit);
		if (absolute) {
		    // Clear the stop bits, absolute movement
		    newCtrlWord.replace(17, 20, "000");
		//		motion.movement().moveAbsolute(Double.parseDouble(words[1]), 2, 1, 1, 0);
		} else {
		    // Clear the stop bits, relative movement
		    newCtrlWord.replace(17, 20, "100");
		//		motion.movement().moveRelative(Double.parseDouble(words[1]), 2, 1, 1, 0);
		}
		System.out.println("Old Positioning control word: " + oldCtrlWord);
		System.out.println("New Positioning control word: " + newCtrlWord);
		con.getAxes(0).parameter().writeDataAsString("S-0-0346", newCtrlWord.toString());
		return("OK");
	    } catch (Exception e) {
		e.printStackTrace();
		System.out.println("Error:" + e.getMessage());
	    }
	}
        return "";
    }
    
    private static String stop(Connection con) {
        // Implement the logic for stopping the motor movement
	try {
	    // Get the Positioning control word
	    String oldCtrlWord = con.getAxes(0).parameter().readDataAsString("S-0-0346.0.0");
	    StringBuffer newCtrlWord = new StringBuffer(oldCtrlWord);
	    // Set the stop bits
	    newCtrlWord.replace(18, 20, "11");
	    System.out.println("Old Positioning control word: " + oldCtrlWord);
	    System.out.println("New Positioning control word: " + newCtrlWord);
	    con.getAxes(0).parameter().writeDataAsString("S-0-0346", newCtrlWord.toString());
	    return("OK");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return "";
    }
    
    private static String setPar(Connection con, String[] words, String ParId) {
        // Implement the logic for setting Double type parameter
	if (words.length > 1) {
	    System.out.println(Double.parseDouble(words[1]));
	    System.out.println(words[1]);
	    try {
		// Set the parameter
		con.getAxes(0).parameter().writeDataAsString(ParId, words[1]);
		return("OK");
	    } catch (Exception e) {
		e.printStackTrace();
		System.out.println("Error:" + e.getMessage());
	    }
	}
        return "";
    }
    private static String getPar(Connection con, String parId, String replyTag) {
        // Implement the logic for getting  parameter
	//	System.out.println("getPar: parId=" + parId + "   reply tag=" + replyTag);
	StringBuilder result = new StringBuilder(replyTag);
	try {
	    // Get the parameter
	    String par = con.getAxes(0).parameter().readDataAsString(parId);
	    // Check if it's a binary parameter
	    if (par.matches("0b[01]{4}\\.[01]{4}\\.[01]{4}\\.[01]{4}")) {
		//		System.out.println("Par: " + par + "/" + binaryToDecimal(par));
		result.append(binaryToDecimal(par));
	    } else {
		result.append(par);
	    }
	    return result.toString();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	}
        return "";
    }
    private static String getParAsString(Connection con, String[] words) {
        // Implement the logic for getting the parameter
	StringBuilder result = new StringBuilder("par: ");
	if (words.length < 2) return "par: No_parId"; 
	try {
	    // Get the parameter
	    String par = con.getAxes(0).parameter().readDataAsString(words[1]);
	    result.append(words[1]).append(" ");
	    result.append(par);
	    return result.toString();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("Error:" + e.getMessage());
	    return "Error: " + e.getMessage();
	}
    }

     private static String setParAsString(Connection con, String[] words) {
	 // Implement the logic for setting  parameter
	 if (words.length < 3) return "par: Wrong_fmt"; 
	 StringBuilder result = new StringBuilder("");
	 for (int i = 2; i < words.length; i++) {
	     result.append(words[i]);
	     if (i !=  (words.length - 1))
		 result.append(" ");
	 }
	 System.out.println(words[1] + " " + "\"" + result.toString() + "\"");

	 try {
	     // Set the parameter
	     con.getAxes(0).parameter().writeDataAsString(words[1], result.toString());
	 } catch (Exception e) {
	     e.printStackTrace();
	     System.out.println("Error:" + e.getMessage());
	     return "Error: " + e.getMessage();
	 }
	 return "OK";
     }
    
    // Add more methods for other actions as needed

    private static String performAction1(String[] words) {
        // Implement the logic for Action1
        StringBuilder result = new StringBuilder("Performing Action1 with parameters:");
        for (int i = 1; i < words.length; i++) {
            result.append("\nParameter ").append(i).append(": ").append(words[i]);
        }
        return result.toString();
    }
}

