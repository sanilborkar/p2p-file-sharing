// SERVER (FILE OWNER)

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Server {

	private static int sPort;
	public final int CHUNK_SIZE = 1024*100;
	public static final int PEERS = 3;

	public Server() {}

	// Split the file
	private ArrayList<File> SplitFile(String filepath) throws IOException {
		File fileObj = new File(filepath);
		byte[] buffer = new byte[CHUNK_SIZE];
		ArrayList<File> partFiles = new ArrayList<File>();
		int part = 0;

		System.out.println("File size: " + fileObj.length());
		
		try (BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream(fileObj))) {
            String name = fileObj.getName();

            int tmp = 0;
            while ((tmp = bis.read(buffer)) > 0) {
                File newFile = new File(fileObj.getParent(), name
                        + String.format("%d", part++));
                
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, tmp);
                	partFiles.add(newFile);
                }
            }
        }

        return partFiles;
	}

	// Read config file
	private void ReadConfig() {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("resources/config.properties");

			// load the properties file
			prop.load(input);
			
			// Get the server port number on which the server will be listening on.
			sPort = Integer.parseInt(prop.getProperty("server"));
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {

		String filename = "";
		String filepath = "";
		boolean found = false;
		while (!found) {
			System.out.print("Enter the name of the file: ");
		    Scanner scanner = new Scanner(System.in);
		    filename = scanner.next();
		    filepath = "resources/" + filename;
		    
		    if (new File(filepath).exists()) {
		    	found = true;
		    	scanner.close();
		    }
		    else
		    	System.out.println("File " + filename + " NOT found! Please enter the filename again.");
		}
	    
		Server S = new Server();
		
		// Chop the file into 100KB chunks
		ArrayList<File> partFiles = S.SplitFile(filepath);
		int totalChunks = partFiles.size();
		System.out.println("Total chunks = " + totalChunks);

		S.ReadConfig();
		
		System.out.println("The server is running."); 
        ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 0;
		
		// Listen for client connections. 
        try {
            while(true) {
				clientNum++;
            	new Handler(listener.accept(), clientNum, partFiles, totalChunks, filename).start();
				System.out.println("Client "  + clientNum + " is connected!");
            }
        } finally {
        	listener.close();
        } 
    }


    private static class Handler extends Thread {
	private Socket connection;
    private ObjectInputStream in;		// Stream read from the socket
    private ObjectOutputStream out;    	// Stream write to the socket
	private int no;						// Client number
	private ArrayList<File> partFiles;	// Stores the file chunks objects
	private int totalChunks;			// Total number of chunks
	private static int chunkNum = 0;	// chunk Number
	private static String filename = "";		// Name of the file to be distributed
	private boolean flag;				
	
    public Handler(Socket connection, int no, ArrayList<File> partFiles, int totalChunks, String fname) {
    	this.connection = connection;
		this.no = no;
		this.partFiles = partFiles;
		this.totalChunks = totalChunks;
		filename = fname;
		flag = true;
    }

    @Override
    public void run() {
 		try{
			//initialize Input and Output streams
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());
			
			int msgCount = -1;
			int sendThreshold = (int) (0.25*totalChunks);
			System.out.println("Setting a SEND threshold of " + sendThreshold);
			try{
				while(chunkNum <= partFiles.size())
				{	
					if (msgCount == sendThreshold) {
						out.writeObject("-1");
						out.flush();
						break;
					}
					
					if (chunkNum == partFiles.size()) {
						chunkNum = 0;
					}
					
					sendMessage(partFiles.get(chunkNum), totalChunks, chunkNum, no, filename);
					chunkNum++;
					msgCount++;
					
					Thread.sleep(1000);
				}
			}
			catch(IndexOutOfBoundsException ie) {
				System.out.println(ie);
			}
			catch(Exception e) {
				System.out.println(e);
			}
		}
		catch(IOException ioException){
			System.out.println("Disconnect with Client " + no);
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				connection.close();
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
		}
	}

	//send a message to the client
	public void sendMessage(File msg, int totalChunks, int chunkNum, int clientNum, String filename) {
		try{
			// Before the first message, send the filename and total number of chunks
			if (flag) {
				out.writeObject(filename);
				out.flush();
				out.writeObject(totalChunks + "");
				out.flush();
				flag = false;
			}

			// Send current chunk number
			out.writeObject(chunkNum + "");
			out.flush();

			// Send the chunk
			byte [] mybytearray  = Files.readAllBytes(msg.toPath());
			out.writeObject(mybytearray);
			out.flush();

			System.out.println("Sent chunk " + chunkNum + " to Client " + clientNum);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

    }

}
