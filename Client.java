// CLIENT 1

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.channels.*;
import java.util.*;

public class Client implements Runnable {
	public final int CHUNK_SIZE = 1024*100;
	public enum Role{TALK_TO_SERVER, DOWNLOADER, UPLOADER};
	
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	int MESSAGE;                //capitalized message read from the server
	File[] partFiles;
	boolean flag;
	int clientNum;
	Role clientRole;
	int totalChunks;

	public Client(int num, Role r) {
		flag = false;
		clientRole = r;
		clientNum = num;
		totalChunks = 0;
		//partFiles = new ArrayList<File>();
	}
	
	public void run()
	{
		int bytesRead = -1;
		int current = -1;

		switch (clientRole) {

			case TALK_TO_SERVER: 
				try{
					//create a socket to connect to the server
					requestSocket = new Socket("localhost", 8000);
					System.out.println("Connected to localhost in port 8000");
					//initialize inputStream and outputStream
					out = new ObjectOutputStream(requestSocket.getOutputStream());
					out.flush();
					in = new ObjectInputStream(requestSocket.getInputStream());
					
					//get Input from standard input
					/*BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
					FileOutputStream fos = null;
				    BufferedOutputStream bos = null;
				    int part = 0;*/
					while(true)
					{
						
						System.out.println("Staring to receive chunks");
		      			ReceiveFileChunks();
						
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
					//err.printStackTrace();
				}
				finally{
					//Close connections
					try{
						in.close();
						out.close();
						requestSocket.close();
					}
					catch(IOException ioException){
						ioException.printStackTrace();
					}
				}
		default:
			break;
			
		}

	}


	void ReceiveFileChunks() throws Exception, ClassNotFoundException {		
		try {
			if (totalChunks == 0) {
				totalChunks = Integer.parseInt((String)in.readObject());
				partFiles = new File[totalChunks];
			}
			
			int partNumber = Integer.parseInt((String)in.readObject());

			File partFile = new File("/home/sanilborkar/Client/Client" + clientNum + "/chunks/" + "File." + partNumber);
			byte[] msg = (byte[]) in.readObject();
			Files.write(partFile.toPath(), msg);
			partFiles[partNumber] = partFile;
			System.out.println("Received chunk " + partNumber);
		}
		catch (ClassNotFoundException e) {
			flag = true;
		}
		catch (Exception e) {
			flag = true;
		}
	}

	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	//main method
	public static void main(String args[])
	{		
		boolean success = false;
		
		// Command-line argument contains the number of clients to start
		int clientNum = Integer.parseInt(args[0]);
		//for (int i = 1; i <= numClients; i++) {
		new File("/home/sanilborkar/Client/Client" + clientNum).mkdir();
		new File("/home/sanilborkar/Client/Client" + clientNum + "/chunks").mkdir();
		new File("/home/sanilborkar/Client/Client" + clientNum + "/complete").mkdir();
			/*Client client = new Client(i, Role.TALK_TO_SERVER);
			client.run();*/
			
		new Thread(new Client(clientNum, Role.TALK_TO_SERVER)).start();		
	}

}
