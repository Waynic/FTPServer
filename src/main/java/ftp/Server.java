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

	/**
	 * port server
	 */
	protected int serverPort = 3021;
	/**
	 * Server socket for client threads
	 */
	protected ServerSocket serverSocket = null;
	/**
	 * true if server is stopped 
	 */
	protected boolean isStopped = false;
	/**
	 * Client thread
	 */
	protected Thread runningThread= null;
	/**
	 * Thread pool
	 */
	protected ExecutorService threadPool = Executors.newFixedThreadPool(5);
	
	/**
	 * class with which there is established connection with database
	 */
	private DB database = null;
	
	/**
	 * File representing parent directory of files on server (default - directory "serverFiles")
	 */
	private File parentDirectory;

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public Server() throws IOException {
		//createServer(3021);
		setParentDirectory();
	}

	/**
	 * Constructor
	 * 
	 * @param port port on which server will work
	 * @param database object of Database class, to work with database
	 * @throws IOException
	 */
	public Server(int port, DB database) throws IOException {
		//createServer(port);
		serverPort = port;
		this.database = database;
		setParentDirectory();
	}

	/**
	 * method that initialize server
	 * 
	 * @param port number of port at which server will run
	 */
	private void createServer(int port) {
		serverPort = port;
		try {
			serverSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** 
	 * main loop which accepts clients on the server socket
	 * 
	 * @see java.lang.Runnable#run()
	 */
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

	/**
	 * @return true if server is stopped
	 */
	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	/**
	 * stops the server
	 */
	public synchronized void stop(){
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}
	
	/**
	 * sets parent directory to "serverFiles" and creates it if it doesn't exists
	 */
	private void setParentDirectory() {
		parentDirectory = new File(System.getProperty("user.dir") + File.separator + "serverFiles");
		if (!parentDirectory.exists()) parentDirectory.mkdir();
	}



}
