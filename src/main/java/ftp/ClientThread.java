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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import database.DB;

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
	
	private DB database = null;
	
	private PrintWriter messageToClient;
	
	String user = null;

	public ClientThread(Socket client, String parentDirectory, DB database) {
		clientSocket = client;
		virtualPath = "/";
		systemPath = parentDirectory;
		parentPath = parentDirectory;
		this.database = database;
	}


	@Override
	public void run() {
		try {
			disconnectTimer = new Timer();
			disconnectTask = createNewTimerTask();
			disconnectTimer.schedule(disconnectTask, 60*1000);
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
			clientSocket.close();
		} catch (IOException e) {
			if (clientSocket.isClosed()) {
				cancelDisconectDeamon();
			}
			else e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void sendWelcomeMessage() {		
		messageToClient.println("220---------- Welcome to Fortun server [privsep] ----------");
		messageToClient.println("220-This is a private system - No anonymous login");
		messageToClient.println("220 You will be disconnected after 1 minute of inactivity.");
	}
	
	private void doRequestedCommand(String input) throws IOException, SQLException {
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
	 * @throws SQLException 
	 */
	private void checkUser(String input) throws SQLException {
		String username = getContentFromCommand(input);
		if (database.checkIfUserExists(username)) {
			this.user = username;
			messageToClient.println("331 Password required");
		}
		else messageToClient.println("430 Invalid username");
	}


	/**
	 * @param input
	 * @throws SQLException 
	 */
	private void checkPassword(String input) throws SQLException {
		String pass = getContentFromCommand(input);
		if (database.checkUserPassword(user, pass)) messageToClient.println("230 User logged in");
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
		//database.addFile(filename, user);
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
	 * @throws IOException 
	 * 
	 */
	private void stopOperation() throws IOException {
		if (dataSocket == null || dataSocket.isClosed()) {
			messageToClient.println("226 Abort accepted and completed");
		}
		else {
			//TODO
			//close buffers (have to make them into fields)
			//ask how we should different files
			dataSocket.close();
			messageToClient.println("426 service request terminated abnormally");
			messageToClient.println("226 Abort accepted and completed");
		}
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
		//database.deleteFile(f.getName());
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
	//	database.addFile(f.getName(), user);
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
	 * @throws SQLException 
	 */
	private void list() throws IOException, SQLException {
		messageToClient.println("150 Accepted data connection");
		PrintWriter sendList = new PrintWriter(dataSocket.getOutputStream(), true);
		File[] list = new File(systemPath).listFiles();
		for (File f : list) {
			/*String rigths;
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
				format = new SimpleDateFormat("MMM  dd H:m");
			}
			else {
				format = new SimpleDateFormat("MM dd YYYY");
			}
			Date date = new Date(f.lastModified());
			time = format.format(date);
			String filename = f.getName();
			String line = rigths + "  " + hardLinks + "  " + owner + "  " + group + "  " + size + "  " + time + "  " + filename;
			sendList.println(line);*/
			String[] info = database.getFileInformations(f.getName());
			String rights = null;
			if (f.isDirectory()) rights="d";
			else rights="-";
			if (info[3].equals("1")) rights+="r";
			else rights+="-";
			if (info[4].equals("1")) rights+="w";
			else rights+="-";
			if (info[5].equals("1")) rights+="r";
			else rights+="-";
			if (info[6].equals("1")) rights+="w";
			else rights+="-";
			
			Calendar fileTime = Calendar.getInstance();
			fileTime.setTimeInMillis(f.lastModified());
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			String time;
			SimpleDateFormat format;
			if (fileTime.get(Calendar.YEAR) == currentYear) {
				format = new SimpleDateFormat("MMM  dd H:m");
			}
			else {
				format = new SimpleDateFormat("MM dd YYYY");
			}
			Date date = new Date(f.lastModified());
			time = format.format(date);
			
			sendList.println(rights + "  " + info[0] + "  " + info[1] + "  " + info[2] + "  " + f.length() + "  " + time + "  " + f.getName());
		}
		//sendList.println("drwx--x--x    3 a4417886   a4417886         4096 Jan  3 18:27 ..");
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
	 * @throws SQLException 
	 */
	private void changeRights(String input) throws SQLException {
		String line = getContentFromCommand(input);
		String filename = line.substring(0, line.indexOf(" ") +1 );
		String rights = getContentFromCommand(line);
		String ownerRights = String.valueOf(rights.charAt(0));
		String groupRights = String.valueOf(rights.charAt(1));
		database.changeFileRights(filename, ownerRights, groupRights);
		
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
				//	messageToClient.print(-1);
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
