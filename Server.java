import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.channels.*;
import java.util.*;

public class Server {

	private static final int sPort = 8000;
	public final int CHUNK_SIZE = 1024*100;

	public void Server() {}

/*	private class Data {
		File fileObj;
		ArrayList<Int> chunksAvailable;

		public void Data(File f) {
			fileObj = null;
			chunksAvailable = new ArrayList<Int>();
		}
	}*/

	// Split the file
	private ArrayList<File> SplitFile(String filepath) throws IOException {
		File fileObj = new File(filepath);
		byte[] buffer = new byte[CHUNK_SIZE];
		ArrayList<File> partFiles = new ArrayList<File>();
		int part = 0;

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
                    //System.out.println(part);
                }
            }
        }

        return partFiles;
	}

	public static void main(String[] args) throws Exception {

		System.out.print("Enter the name of the file: ");
	    Scanner scanner = new Scanner(System.in);
	    String filepath = "/home/sanilborkar/Server/src/data.pdf"; //scanner.next();

		Server S = new Server();
		
		// Chop the file into 100KB chunks
		ArrayList<File> partFiles = S.SplitFile(filepath);
		int totalChunks = partFiles.size();
		System.out.println("Total chunks = " + totalChunks);

		/*for (int k=0;k<totalChunks ;k++ ) {
			System.out.println(partFiles.get(k));
		}*/

		System.out.println("The server is running."); 
        ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 0;
        
        try {
            while(true) {
				clientNum++;
            	new Handler(listener.accept(), clientNum, partFiles, totalChunks).start();
				System.out.println("Client "  + clientNum + " is connected!");
            }
        } finally {
        	listener.close();
        } 
    }


    private static class Handler extends Thread {
	private File MESSAGE;    // Message to send to the client
	private Socket connection;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
	private int no;		//The index number of the client
	private ArrayList<File> partFiles;	// Stores the file chunks objects
	private int totalChunks;

    public Handler(Socket connection, int no, ArrayList<File> partFiles, int totalChunks) {
    	this.connection = connection;
		this.no = no;
		this.partFiles = partFiles;
		this.totalChunks = totalChunks;
    }

    @Override
    public void run() {
 		try{
			//initialize Input and Output streams
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());

			int chunkNum = 0;
			int i = 0;
			try{
				while(i < partFiles.size())
				{
					// Send totalChunks only before the 1st message, send -1 to sendMessage() otherwise
					if (i != 0)
						totalChunks = -1;
					
					// Once atleast 2 clients join, start sending them chunks
					if (no == 1) {
						//for (int i = 0; i <  .size(); i+=2) {
						if (i%2 == 0) {
							//System.out.println("Sending chunk " + i + " to client " + no);
							MESSAGE = partFiles.get(i);
							sendMessage(MESSAGE, totalChunks, i, no);
						}
						//}
						
					}
					else if (no == 2) {
						//for (int j = 1; j < partFiles.size(); j+=2) {
						if (i%2 == 1) {
							System.out.println("Sending chunk " + i + " to client " + no);
							MESSAGE = partFiles.get(i);
							sendMessage(MESSAGE, totalChunks, i, no);
						}
					}

					i++;
				}
			}
			catch(Exception e) {}
			/*catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}*/
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

	//send a message to the output stream
	public void sendMessage(File msg, int totalChunks, int chunkNum, int clientNum)
	{
		try{
			// Total chunks
			if (totalChunks > 0) {
				out.writeObject(totalChunks + "");
				out.flush();
			}

			// Current chunk number
			out.writeObject(chunkNum + "");
			out.flush();

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
