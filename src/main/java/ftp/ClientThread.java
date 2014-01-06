/**
 * 
 */
package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Jakub Fortunka
 *
 */
public class ClientThread implements Runnable {

	private Socket clientSocket = null;
	private ServerSocket dataSocketServer = null;
	private Socket dataSocket = null;
	
	private final String parentPath;
	private String systemPath = null;
	private String virtualPath = null;
	
	private boolean disconnectClient = false;
	
	private Timer disconnectTimer = null;
	private TimerTask disconnectTask = null;
	
	private PrintWriter messageToClient;

	public ClientThread(Socket client, String parentDirectory) {
		clientSocket = client;
		virtualPath = "/";
		systemPath = parentDirectory;
		parentPath = parentDirectory;
	}


	@Override
	public void run() {
		try {
			disconnectTimer = new Timer();
			disconnectTask = createNewTimerTask();
			disconnectTimer.schedule(disconnectTask, 2*60*1000);
			BufferedReader messageFromClient  =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			messageToClient = new PrintWriter(clientSocket.getOutputStream(), true);
			String inputLine;
			sendWelcomeMessage();
			while (!clientSocket.isClosed() && ((inputLine = messageFromClient.readLine()) != null)) {
				 if (disconnectClient) {
					 break;
				 }
				doRequestedCommand(inputLine);
			}
			cancelDisconectDeamon();
		} catch (IOException e) {
			if (clientSocket.isClosed()) {
				cancelDisconectDeamon();
			}
			else e.printStackTrace();
		}
	}
	
	private void sendWelcomeMessage() {		
		messageToClient.println("220---------- Welcome to Fortun server [privsep] ----------");
		messageToClient.println("220-This is a private system - No anonymous login");
		messageToClient.println("220 You will be disconnected after 3 minutes of inactivity.");
	}
	
	private void doRequestedCommand(String input) throws IOException {
		resetDisconnectionTimer();
		if (input.startsWith("USER")) checkUser(input);
		else if (input.startsWith("PASS")) checkPassword(input);
		else if (input.startsWith("QUIT")) closeConnection();
		else if (input.startsWith("NOOP")) refreshConnection();
		else if (input.startsWith("PASV")) openPassiveMode();
		else if (input.startsWith("STOR")) getFile(input, true);
		else if (input.startsWith("RETR")) retr(input);
		else if (input.startsWith("APPE")) getFile(input, false);
		else if (input.startsWith("ABOR")) stopOperation();
		else if (input.startsWith("DELE")) delete(input);
		else if (input.startsWith("RMD")) delete(input);
		else if (input.startsWith("MKD")) createDirectory(input);
		else if (input.startsWith("PWD")) pwd();
		else if (input.startsWith("LIST")) list();
		else if (input.startsWith("CWD")) cwd(input);
		else if (input.startsWith("CHMOD")) changeRights(input);
		else unknownCommand(input);
	}


	/**
	 * @param input
	 */
	private void checkUser(String input) {
		String username = getContentFromCommand(input);
		if (username.equals("Kuba")) messageToClient.println("331 Password required");
		else messageToClient.println("430 Invalid username");
	}


	/**
	 * @param input
	 */
	private void checkPassword(String input) {
		String pass = getContentFromCommand(input);
		if (pass.equals("test")) messageToClient.println("230 User logged in");
		else messageToClient.println("430 Invalid password");
	}


	/**
	 * @throws IOException 
	 * 
	 */
	private void closeConnection() throws IOException {
		messageToClient.println("221 Goodbye.");
		clientSocket.close();
		cancelDisconectDeamon();
	}


	/**
	 * 
	 */
	private void refreshConnection() {
		messageToClient.println("200 Zzz...");
		//resetDisconnectionTimer();
	}


	/**
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * 
	 */
	private void openPassiveMode() throws UnknownHostException, IOException {
		dataSocketServer = new ServerSocket(3020);
		//dataSocket = dataSocketServer.accept();
		if (!dataSocketServer.isClosed()) {
			messageToClient.println("227 Entering Passive Mode (127,0,0,1,11,204)");
		}
		else {
			dataSocketServer.close();
			messageToClient.println("425 Can't open data connection");
		}
		dataSocket = dataSocketServer.accept();
	}


	/**
	 * @param input
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	private void retr(String input) throws IOException {
		String filename = getContentFromCommand(input);
		messageToClient.println("150 Opening binary mode data connection for '" + filename + "'");
		File f = new File(systemPath + File.separator + filename);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
		BufferedOutputStream out = new BufferedOutputStream(dataSocket.getOutputStream());
		moveFile(in,out);
		out.close();
		in.close();
		dataSocket.close();
		dataSocketServer.close();
		messageToClient.println("226 Transfer complete");
	}


	/**
	 * @param input
	 * @param b
	 * @throws IOException 
	 */
	private void getFile(String input, boolean b) throws IOException {
		String filename = getContentFromCommand(input);
		File f = new File(systemPath + File.separator + filename);
		BufferedInputStream in = new BufferedInputStream(dataSocket.getInputStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
		messageToClient.println("150 FILE: " + filename);
		moveFile(in, out);
		out.close();
		in.close();
		dataSocket.close();
		dataSocketServer.close();
		messageToClient.println("226 Transfer complete");
	}


	/**
	 * 
	 */
	private void stopOperation() {
		// TODO Auto-generated method stub
		
	}


	/**
	 * @param input
	 */
	private void delete(String input) {
		String operation = input.substring(0,1);
		String file = getContentFromCommand(input);
		File f;
		if (file.startsWith("/")) f = new File(parentPath + file.substring(1));
		else f = new File(systemPath + File.separator + file);
		f.delete();
		if (operation.equals("R")) messageToClient.println("250 RMD was successful");
		else messageToClient.println("250 DELE was successful");
	}


	/**
	 * @param input
	 */
	private void createDirectory(String input) {
		String directoryName = getContentFromCommand(input);
		File f;
		if (directoryName.startsWith("/")) f = new File(parentPath + directoryName.substring(1));
		else f = new File(systemPath + File.separator + directoryName);
		f.mkdir();
	}


	/**
	 * @param input
	 */
	private void pwd() {
		messageToClient.println("257 \"" + virtualPath + "\" is your current location");
	}


	/**
	 * @throws IOException 
	 */
	private void list() throws IOException {
		messageToClient.println("150 Accepted data connection");
		PrintWriter sendList = new PrintWriter(dataSocket.getOutputStream(), true);
		/*File[] list = new File(systemPath).listFiles();
		for (File f : list) {
			String rigths;
			if (f.isDirectory()) rigths="d";
			else rigths="-";
			Path file = Paths.get(f.getAbsolutePath());
			PosixFileAttributes att = Files.getFileAttributeView(file, PosixFileAttributeView.class).readAttributes();
			rigths += PosixFilePermissions.toString(att.permissions());
			//to do
			String hardLinks = "2";
			UserPrincipal o = att.owner();
			GroupPrincipal g = att.group();
			String owner = o.getName();
			String group = g.getName();
			String size = String.valueOf(f.length());
			Calendar fileTime = Calendar.getInstance();
			fileTime.setTimeInMillis(f.lastModified());
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			String time;
			SimpleDateFormat format;
			if (fileTime.get(Calendar.YEAR) == currentYear) {
				format = new SimpleDateFormat("MM  dd H:m");
			}
			else {
				format = new SimpleDateFormat("MM dd YYYY");
			}
			Date date = new Date(f.lastModified());
			time = format.format(date);
			String filename = f.getName();
			String line = rigths + "  " + hardLinks + "  " + owner + "  " + group + "  " + size + "  " + time + "  " + filename;
			sendList.println(line);
		}*/
		sendList.println("drwx--x--x    3 a4417886   a4417886         4096 Jan  3 18:27 ..");
		dataSocket.close();
		dataSocketServer.close();
		messageToClient.println("226 Transfer complete");
	}


	/**
	 * @param input
	 */
	private void cwd(String input) {
		String path = getContentFromCommand(input);
		if (path.startsWith("/")) {
			virtualPath = path;
			systemPath = parentPath + path;
		}
		else {
			virtualPath += File.separator + path;
			systemPath += virtualPath;
		}
	}


	/**
	 * @param input
	 */
	private void changeRights(String input) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * @param input
	 */
	private void unknownCommand(String input) {
		messageToClient.println("502 Command not implemented");
		
	}
	
	private void moveFile(BufferedInputStream input, BufferedOutputStream output) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.flush();
	}
	
	private String getContentFromCommand(String input) {
		return input.substring(input.indexOf(" ")+1);
	}
	
	private TimerTask createNewTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
					disconnectClient = true;
					messageToClient.print(-1);
					//messageToClient.println("-1");
			}
		};
	}
	
	private void resetDisconnectionTimer() {
		disconnectTimer.cancel();
		disconnectTimer = new Timer();
		disconnectTask = createNewTimerTask();
		disconnectTimer.schedule(disconnectTask, 60*1000);
	}
	
	public void cancelDisconectDeamon() {
		if (disconnectTimer != null) {
			disconnectTimer.cancel();
			disconnectTimer = null;
		}
		if (disconnectTask != null) {
			disconnectTask.cancel();
			disconnectTask = null;
		}		
	}

}
