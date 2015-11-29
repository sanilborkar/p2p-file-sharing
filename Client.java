// CLIENT 2

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Client implements Runnable {
	public final int CHUNK_SIZE = 1024*100;
	public enum Role{TALK_TO_SERVER, DOWNLOADER, UPLOADER};
	
	Socket requestSrSocket;           //socket connect to the server
	Socket requestDwSocket;           //socket connect to the download neighbor
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	ObjectOutputStream outDown;         //stream write to the socket
 	ObjectInputStream inDown;          //stream read from the socket
 	ObjectOutputStream outUp;         //stream write to the socket
 	ObjectInputStream inUp;          //stream read from the socket
	String message;                //message send to the server
	int MESSAGE;                //capitalized message read from the server
	static File[] availableChunks;
	boolean flag;
	int clientNum;
	Role clientRole;
	static int totalChunks;
	File[] requiredChunks;
	static int dwldNeighbor;
	static int dwldNeighborPort;
	static int sPort;					// The port at which this client will listen on (when acting as UPLOADER)
	static int serverPort;				// The port at which the server is listening on
	static ServerSocket uploadSocket;
	Socket upSock;						// UPLOADER: Upload socket
	private static boolean flagFilename;
	private static String filename;

	public Client(int num, Role r) {
		flag = false;
		clientRole = r;
		clientNum = num;
		flagFilename = true;
		if (r == Role.TALK_TO_SERVER) {
			//totalChunks = 0;
			availableChunks = null;
			requiredChunks = null;
		}
	}
	
	public void run()
	{
		switch (clientRole) {

			case TALK_TO_SERVER: 
				try{
					//create a socket to connect to the server
					requestSrSocket = new Socket("localhost", serverPort);
					System.out.println("Connected to server localhost at port " + serverPort);
					
					//initialize inputStream and outputStream
					out = new ObjectOutputStream(requestSrSocket.getOutputStream());
					out.flush();
					in = new ObjectInputStream(requestSrSocket.getInputStream());
					
					while(true)
					{
						System.out.println("Starting to receive chunks");
		      			ReceiveFileChunksFromServer();
						
		      			if (flag)
		      				break;		                
					}
				}
				catch (ConnectException e) {
		    			System.err.println("Connection refused. You need to initiate a server first.");
				} 
				catch ( ClassNotFoundException e ) {
		            		System.err.println("Class not found");
		        	} 
				catch(UnknownHostException unknownHost){
					System.err.println("You are trying to connect to an unknown host!");
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
				catch(Exception e){
					e.printStackTrace();
				}
				finally{
					//Close connections
					try{
						in.close();
						out.close();
						requestSrSocket.close();
					}
					catch(IOException ioException){
						ioException.printStackTrace();
					}
				}
				break;
				
				
			case DOWNLOADER:
				
				try {				
					//create a socket to connect to the server
					while (requestDwSocket == null) {
						try {
							System.out.println(clientNum + ": Downloader connecting to port " + dwldNeighborPort);
							requestDwSocket = new Socket("localhost", dwldNeighborPort);
						}
						catch(Exception e) {
							System.out.println("Waiting for " + dwldNeighbor + " to come up. Retrying in 1 second.");
							Thread.sleep(1000);
						}
					}
					
					System.out.println("D: Connected to localhost at port " + dwldNeighborPort);
					
					//initialize inputStream and outputStream
					outDown = new ObjectOutputStream(requestDwSocket.getOutputStream());
					outDown.flush();
					inDown = new ObjectInputStream(requestDwSocket.getInputStream());
					
					boolean isMerged = false;
					while (true) {
						
						//if (isMerged) continue;
						
						// If there are no requiredChunks --> all the chunks have been received. So, merge!
						if (AllChunksReceived()) {
							MergeChunks(availableChunks);
							if (!isMerged) {
								System.out.println("Received all the chunks and MERGED!");
								isMerged = true;
								break;
							}
							//continue;
						}
						else {
							
							// Request for dlwdNeighbor's chunk ID list
							System.out.println("Requesting for Client " + dwldNeighbor + " chunk ID List");
							outDown.writeObject("REQ");
							outDown.flush();
							
							// Receive neighbor's chunk ID list
							String chunkIDList = (String) inDown.readObject();
							
							System.out.println("Received chunk ID List from Client " + dwldNeighbor);
							System.out.println(chunkIDList);
							
							String requiredChunks = "";
							
							// If this client never contacted the server, then it has not received any chunks
							if (chunkIDList.isEmpty()) {
								
								System.out.println("chunkIDList empty");
								continue;
								
								// We required all the chunks then
								/*for(int i = 0; i < totalChunks; i++) {
									if (requiredChunks == "")
										requiredChunks += i;
									else
										requiredChunks += "," + i;
								}*/
									
							}
							else {
								System.out.println("D: Checking requiredChunks");
								
								for(int i = 0; i < availableChunks.length; i++) {
									if (availableChunks[i] != null)
										System.out.print(i + "\t");
								}
								System.out.println();
								
								String[] chunkList = chunkIDList.split(",");
								for(int i = 0; i < chunkList.length; i++) {
									if (availableChunks[Integer.parseInt(chunkList[i])] == null) {
										if (requiredChunks.isEmpty())
											requiredChunks += chunkList[i];
										else
											requiredChunks += "," + chunkList[i];
									}
								}
							}
							
							/*if (requiredChunks == "")
								continue;*/
							
							// Send the requested chunks list to download neighbor
							System.out.println("Requesting chunks " + requiredChunks + " from Client " + dwldNeighbor);
							outDown.writeObject(requiredChunks);
							outDown.flush();
								
							//Thread.sleep(1000);

							// Receive file chunk from the download neighbor
							if (!requiredChunks.isEmpty()) {
								String[] rcArray = requiredChunks.split(",");
								for (int i = 0; i < rcArray.length; i++)
									ReceiveFileChunkFromNeighbor();
							}
							
								
							Thread.sleep(1000);

							
						}
						
					}
				}
				catch (ConnectException e) {
	    			System.err.println("Connection refused. You need to initiate a server first.");
				} 
				catch(UnknownHostException unknownHost){
					System.err.println("You are trying to connect to an unknown host!");
				}
				catch(IOException ioException){
					ioException.printStackTrace();
				}
				catch(Exception e){
					System.out.println(e);
					e.printStackTrace();
				}
				finally{
					//Close connections
					try{
						inDown.close();
						outDown.close();
						if (requestDwSocket != null)
							requestDwSocket.close();
					}
					catch(IOException ioException){
						ioException.printStackTrace();
					}
				}
				
				break;
				
				
			case UPLOADER:
				
				try {				
					// Listen for requests from Upload Neighbor
					upSock = uploadSocket.accept();
					
					// Initialize inputStream and outputStream
					outUp = new ObjectOutputStream(upSock.getOutputStream());
					outUp.flush();
					inUp = new ObjectInputStream(upSock.getInputStream());
					
					System.out.println(clientNum + ": Uploader listening on port " + sPort);
					
					String upRequested = "";
					while (true) {
						
						// Look for a REQ from downloading peer
						String req = (String) inUp.readObject();
						System.out.println("************ Received " + req);
						if (req.equals("REQ")) {
							
							String chunkIDList = "";
							// Send our chunk ID list
							for (int i = 0; i < availableChunks.length; i++) {
								if (availableChunks[i] != null) {
									if (chunkIDList.isEmpty())
										chunkIDList += i;
									else
										chunkIDList += "," + i;
								}
							}
							
							
							System.out.println("Sending chunk ID List to Client");
							System.out.println(chunkIDList);
							outUp.writeObject(chunkIDList);
							outUp.flush();
						}
						else {
							System.out.println("Excepted REQ: Received something else. Continuing");
							continue;
						}
							
						
						// Get the requestedChunks from upload neighbor. These are the chunks that
						// the upload neighbor needs from this client
						upRequested = (String) inUp.readObject();
						String[] upRequestedChunks = upRequested.split(",");
						
						System.out.println("Received requestedChunks");
						System.out.println(upRequested);
						
						// Check this against availableChunks and send the chunks that we have to Upload Neighbor
						if (availableChunks == null) { 
							//System.out.println("U: availableChunks is NULL");
						}
						else {
							
							if (!upRequested.isEmpty()) {
								for (int i = 0; i < upRequestedChunks.length; i++) {
									
									// If we have the required chunk, send it!
									int reqChunkNum = Integer.parseInt(upRequestedChunks[i]);
									if (availableChunks[reqChunkNum] != null)
										SendChunk(reqChunkNum);
									else
										SendChunk(-1);
								}
							}
						}
						
						Thread.sleep(1000);
					}
				}
				catch (ConnectException e) {
	    			System.err.println("Connection refused. You need to initiate a server first.");
				} 
				catch(UnknownHostException unknownHost){
					System.err.println("You are trying to connect to an unknown host!");
				}
				catch(IOException ioException){
					//ioException.printStackTrace();
					System.out.println("Reached EOF");
				}
				catch(Exception e){
					e.printStackTrace();
				}
				finally{
					//Close connections
					try{
						inUp.close();
						outUp.close();
					}
					catch(IOException ioException){
						ioException.printStackTrace();
					}
				}
				
				break;
				
				
			default:
				break;
			
		}

	}

	
	boolean AllChunksReceived() {
		if (totalChunks > 0) {
			for (int i = 0; i < totalChunks; i++) {
				if (availableChunks[i] == null)
					return false;
			}
			
			return true;
		}
			
		return false;
	}

	void ReceiveFileChunksFromServer() throws Exception, ClassNotFoundException {		
		try {
			if (flagFilename) {
				filename = (String)in.readObject();
				totalChunks = Integer.parseInt((String)in.readObject());
				flagFilename = false;
			}
			
			if (availableChunks == null)
				availableChunks = new File[totalChunks];
			
			if (requiredChunks == null)
				requiredChunks = new File[totalChunks];
			
			int partNumber = Integer.parseInt((String)in.readObject());

			File partFile = new File("chunks/" + filename + "." + partNumber);
			byte[] msg = (byte[]) in.readObject();
			Files.write(partFile.toPath(), msg);
			availableChunks[partNumber] = partFile;
			System.out.println("Received chunk " + partNumber);
		}
		catch (ClassNotFoundException e) {
			flag = true;
		}
		catch (Exception e) {
			flag = true;
		}
	}

	
	void ReceiveFileChunkFromNeighbor() throws Exception, ClassNotFoundException {		
		try {
			int	chunkNum = Integer.parseInt((String)inDown.readObject());
			if (chunkNum == -1)
				return;
			
			byte[] msg = (byte[]) inDown.readObject();
			
			File chunkFile = new File("chunks/" + filename + "." + chunkNum);
			Files.write(chunkFile.toPath(), msg);
			
			availableChunks[chunkNum] = chunkFile;
			System.out.println("Received chunk " + chunkNum + " from Client " + dwldNeighbor);
		}
		catch (ClassNotFoundException e) {
			flag = true;
		}
		catch (Exception e) {
			flag = true;
		}
	}
	
	
	// Merge the availableChunks to build the original file
	void MergeChunks(File[] chunks) throws IOException {
		
		// Safety check
		if (chunks == null) {
			System.err.println("ERROR: No chunks to merge!");
			return;
		}
		
	    FileOutputStream fos = new FileOutputStream("complete/" + filename);
		
		try {
		    FileInputStream fis;
		    byte[] fileBytes;
		    int bytesRead;
		    for (File f : chunks) {
		        fis = new FileInputStream(f);
		        fileBytes = new byte[(int) f.length()];
		        bytesRead = fis.read(fileBytes, 0, (int) f.length());
		        assert(bytesRead == fileBytes.length);
		        assert(bytesRead == (int) f.length());
		        fos.write(fileBytes);
		        fos.flush();
		        fileBytes = null;
		        fis.close();
		        fis = null;
		    }
		} catch (Exception exception){
			exception.printStackTrace();
		}
		finally {
			fos.close();
		    fos = null;
		}
	}
	
	//send a message to the output stream
	void SendChunk(int chunkNum)
	{
		try{
			// Send the chunk number that will follow
			outUp.writeObject(chunkNum + "");
			outUp.flush();
			
			// Send the chunk
			if (chunkNum != -1) {
				byte[] fileContents = Files.readAllBytes(availableChunks[chunkNum].toPath());
				outUp.writeObject(fileContents);
				outUp.flush();
				System.out.println("Sent chunk " + chunkNum + " to Client");
			}
			
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	
	// Read config file
	private static void ReadConfig(int clientNum) {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("resources/config.properties");
			//input = new FileInputStream("../resources/config.properties");

			// load the properties file
			prop.load(input);

			// get the property value
			String propValue = prop.getProperty("client" + clientNum);
			
			/* props[0] = port on which to listen
			 * props[1] = download neighbor
			 * props[2] = upload neighbor
			 */
			String[] props = propValue.split(" ");
			sPort = Integer.parseInt(props[0]);
			dwldNeighbor = Integer.parseInt(props[1]);
			
			// Get port number of download neighbor
			propValue = prop.getProperty("client" + props[1]);
			dwldNeighborPort = Integer.parseInt(propValue.split(" ")[0]);
			
			// Get the server port number on which the server will be listening on.
			propValue = prop.getProperty("server");
			serverPort = Integer.parseInt(propValue);
			
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

	
	// main method
	public static void main(String args[]) throws IOException, InterruptedException
	{		
		// Command-line argument contains the number of clients to start
		int clientNum = Integer.parseInt(args[0]);		

		new File("chunks").mkdir();
		new File("complete").mkdir();
			
		// Read configuration file
		ReadConfig(clientNum);
		
		// Start listening on its own source port for requests from Upload Neighbor
		uploadSocket = new ServerSocket(sPort, 10);
		
		new Thread(new Client(clientNum, Role.TALK_TO_SERVER)).start();
		Thread.sleep(10000);
		new Thread(new Client(clientNum, Role.DOWNLOADER)).start();
		//Thread.sleep(1000);
		new Thread(new Client(clientNum, Role.UPLOADER)).start();
	}

}
