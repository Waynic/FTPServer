/**
 * 
 */
package ftp;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import database.DB;

/**
 * @author Jakub Fortunka
 *
 */
public class Server implements Runnable {

	protected int serverPort = 3021;
	protected ServerSocket serverSocket = null;
	protected boolean isStopped = false;
	protected Thread runningThread= null;
	protected ExecutorService threadPool = Executors.newFixedThreadPool(5);
	
	private DB database = null;
	
	private File parentDirectory;

	public Server() throws IOException {
		//createServer(3021);
		getParentDirectory();
	}

	public Server(int port, DB database) throws IOException {
		//createServer(port);
		serverPort = port;
		this.database = database;
		getParentDirectory();
	}

	private void createServer(int port) {
		serverPort = port;
		try {
			serverSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		synchronized(this){
			this.runningThread = Thread.currentThread();
		}
		createServer(serverPort);
		while(! isStopped()){
			Socket clientSocket = null;
			try {
				clientSocket = this.serverSocket.accept();
			} catch (IOException e) {
				if(isStopped()) {
					System.out.println("Server Stopped.") ;
					return;
				}
				throw new RuntimeException("Error accepting client connection", e);
			}
			this.threadPool.execute(new ClientThread(clientSocket, parentDirectory.getAbsolutePath(),database));
		}
		this.threadPool.shutdown();
		System.out.println("Server Stopped.");

	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void stop(){
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}
	
	private void getParentDirectory() {
		parentDirectory = new File(System.getProperty("user.dir") + File.separator + "serverFiles");
		if (!parentDirectory.exists()) parentDirectory.mkdir();
	}



}
