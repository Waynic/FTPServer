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
 * Class which objects communicates with clients, do commands etc.
 * 
 * @author Jakub Fortunka
 *
 */
public class ClientThread implements Runnable {

	/**
	 * Socket for communicating with client
	 */
	private Socket clientSocket = null;
	/**
	 * ServerSocket for accepting PASV command
	 */
	private ServerSocket dataSocketServer = null;
	/**
	 * Socket for transfering files
	 */
	private Socket dataSocket = null;

	BufferedInputStream inputStream = null;
	BufferedOutputStream outputStream = null;

	/**
	 * parent path of server files (everything before "serverFiles" directory)
	 */
	private final String parentPath;
	/**
	 * working full path (for writing files etc.) 
	 */
	private String systemPath = null;
	/**
	 * wroking virtual path (for showing user his position etc.)
	 */
	private String virtualPath = null;

	/**
	 * when true, then server will disconnect client
	 */
	private boolean disconnectClient = false;

	/**
	 * timer for server disconnection
	 */
	private Timer disconnectTimer = null;
	/**
	 * task for disconnecting
	 */
	private TimerTask disconnectTask = null;

	/**
	 * manages database
	 */
	private DB database = null;

	/**
	 * sends messages to client side
	 */
	private PrintWriter messageToClient;

	/**
	 * name of user which is logged to the server by this thread
	 */
	private String user = null;

	/**
	 * Constructor
	 * 
	 * @param client socket connected to the client (for communication)
	 * @param parentDirectory parent directory on local machine
	 * @param database manages connection and queries with database
	 */
	public ClientThread(Socket client, String parentDirectory, DB database) {
		clientSocket = client;
		virtualPath = "/";
		systemPath = parentDirectory;
		parentPath = parentDirectory;
		this.database = database;
	}


	/** 
	 * main "loop" - reads commands from clients and executes them
	 * 
	 * @see java.lang.Runnable#run()
	 */
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
				if (disconnectClient) break;
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

	/**
	 * sends welcome message
	 */
	private void sendWelcomeMessage() {		
		messageToClient.println("220---------- Welcome to Fortun server [privsep] ----------");
		messageToClient.println("220-This is a private system - No anonymous login");
		messageToClient.println("220 You will be disconnected after 1 minute of inactivity.");
	}

	/**
	 * Many if's - manages to do what it have to when client sends command
	 * 
	 * @param input line from client
	 * @throws IOException
	 * @throws SQLException
	 */
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
	 * check if username passed by USER exists in database
	 * 
	 * @param input line from client
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
	 * Checks if user entered right password
	 * 
	 * @param input line from client
	 * @throws SQLException 
	 */
	private void checkPassword(String input) throws SQLException {
		String pass = getContentFromCommand(input);
		if (database.checkUserPassword(user, pass)) messageToClient.println("230 User logged in");
		else messageToClient.println("430 Invalid password");
	}


	/**
	 * closes connection with client
	 * 
	 * @throws IOException 
	 * 
	 */
	private void closeConnection() throws IOException {
		messageToClient.println("221 Goodbye.");
		clientSocket.close();
		cancelDisconectDeamon();
	}


	/**
	 * refresh the timer (react to NOOP command)
	 */
	private void refreshConnection() {
		messageToClient.println("200 Zzz...");
		resetDisconnectionTimer();
	}


	/**
	 * opens passive mode 
	 * 
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * 
	 */
	private void openPassiveMode() throws UnknownHostException, IOException {
		dataSocketServer = new ServerSocket(3020);
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
	 * reaction for RETR command
	 * 
	 * @param input line from client
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	private void retr(String input) throws IOException {
		String filename = getContentFromCommand(input);
		messageToClient.println("150 Opening binary mode data connection for '" + filename + "'");
		File f = new File(systemPath + File.separator + filename);
		inputStream = new BufferedInputStream(new FileInputStream(f));
		outputStream = new BufferedOutputStream(dataSocket.getOutputStream());
		moveFile(inputStream,outputStream);
		outputStream.close();
		inputStream.close();
		dataSocket.close();
		dataSocketServer.close();
		messageToClient.println("226 Transfer complete");
		inputStream = null;
		outputStream = null;
	}


	/**
	 * manages of saving file on server
	 * 
	 * @param input line from client
	 * @param b if true command is STOR; command APPE otherwise
	 * @throws IOException 
	 * @throws SQLException 
	 */
	private void getFile(String input, boolean b) throws IOException, SQLException {
		// TODO append and filename
		String filename = getContentFromCommand(input);
		File f = new File(systemPath + File.separator + filename);
		if (virtualPath.equals("/")) database.addFile(virtualPath + filename, user);
		else database.addFile(virtualPath + "/" + filename, user);
		inputStream = new BufferedInputStream(dataSocket.getInputStream());
		outputStream = new BufferedOutputStream(new FileOutputStream(f));
		messageToClient.println("150 FILE: " + filename);
		moveFile(inputStream, outputStream);
		outputStream.close();
		inputStream.close();
		dataSocket.close();
		dataSocketServer.close();
		messageToClient.println("226 Transfer complete");
		inputStream = null;
		outputStream = null;
	}


	/**
	 * stops current operation on server (command ABOR)
	 * 
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
	 * deletes file from the server files
	 * 
	 * @param input line from client
	 * @throws SQLException 
	 */
	private void delete(String input) throws SQLException {
		// 550 Permission denied
		boolean badUser = false;
		String operation = input.substring(0,1);
		String file = getContentFromCommand(input);
		File f;
		if (file.startsWith("/")) {
			if (!database.checkIfUserHavePermissionToWrite(user, file)) {
				badUser=true;
				messageToClient.println("550 Permission denied");
			}
			else file = parentPath + file;
		}
		else {
			if (!database.checkIfUserHavePermissionToWrite(user, file)) {
				badUser=true;
				messageToClient.println("550 Permission denied");
			}
			else file = systemPath + File.separator + file;
		}
		if (!badUser) {
			f = new File(file);
			database.checkIfUserHavePermissionToWrite(user, file);
			if (virtualPath.equals("/")) database.deleteFile(virtualPath + f.getName());
			else database.deleteFile(virtualPath + "/" + f.getName());
			f.delete();
			if (operation.equals("R")) messageToClient.println("250 RMD was successful");
			else messageToClient.println("250 DELE was successful");
		}
	}


	/**
	 * creates directory on server
	 * 
	 * @param input line from client
	 * @throws SQLException 
	 */
	private void createDirectory(String input) throws SQLException {
		String directoryName = getContentFromCommand(input);
		File f;
		if (directoryName.startsWith("/")) f = new File(parentPath + directoryName.substring(1));
		else f = new File(systemPath + File.separator + directoryName);
		if (virtualPath.equals("/")) database.addFile(virtualPath + f.getName(), user);
		else database.addFile(virtualPath + "/" + f.getName(), user);
		f.mkdir();
		messageToClient.println("257 Directory was succefully created");
	}


	/**
	 * sends to client currently working directory
	 * 
	 * @param input line from client
	 */
	private void pwd() {
		messageToClient.println("257 \"" + virtualPath + "\" is your current location");
	}


	/**
	 * list files from current directory
	 * 
	 * @throws IOException 
	 * @throws SQLException 
	 */
	private void list() throws IOException, SQLException {
		messageToClient.println("150 Accepted data connection");
		PrintWriter sendList = new PrintWriter(dataSocket.getOutputStream(), true);
		File[] list = new File(systemPath).listFiles();
		for (File f : list) {
			if (f.getName().equals("..") || f.getName().equals(".")) continue;
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
			//TODO filename - full path
			String[] info = null;
			if (virtualPath.equals("/")) info = database.getFileInformations(virtualPath + f.getName());
			else info = database.getFileInformations(virtualPath + "/" + f.getName());
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
		dataSocket.close();
		dataSocketServer.close();
		messageToClient.println("226 Transfer complete");
	}

	/**
	 * comes to directory passed in line
	 * 
	 * @param input line from client
	 */
	private void cwd(String input) {
		//TODO Check if directory exists
		boolean directoryExists = true;
		String path = getContentFromCommand(input);
		if (path.startsWith("/")) {
			File f = new File(parentPath + path);
			if (!f.exists()) {
				messageToClient.println("550 Can't change directory to " + path + ": No such file or directory");
				directoryExists = false;
			}
			if (directoryExists) {
				virtualPath = path;
				systemPath = parentPath + path;
				messageToClient.println("250 OK. Current directory is" + virtualPath);
			}
		}
		else {
			if (path.equals("..")) {
				virtualPath = virtualPath.substring(0,virtualPath.lastIndexOf("/"));
				systemPath = systemPath.substring(0,systemPath.lastIndexOf("/"));
			}
			else {
				File f = new File(systemPath + File.separator + path);
				if (!f.exists()) {
					messageToClient.println("550 Can't change directory to " + virtualPath + "/" + path + ": No such file or directory");
					directoryExists = false;
				}
				if (directoryExists) {
					virtualPath += "/" + path;
					systemPath += virtualPath;
					messageToClient.println("250 OK. Current directory is" + virtualPath);
				}
			}
		}
	}

	/**
	 * changes rights of file on server (in database)
	 * 
	 * @param input line from client
	 * @throws SQLException 
	 */
	private void changeRights(String input) throws SQLException {
		String line = getContentFromCommand(input);
		String filename = line.substring(0, line.indexOf(" ") +1 );
		String rights = getContentFromCommand(line);
		String ownerRights = String.valueOf(rights.charAt(0));
		String groupRights = String.valueOf(rights.charAt(1));
		database.changeFileRights(filename, ownerRights, groupRights);
		messageToClient.println("200 Permissions changed on " + filename);
	}


	/**
	 * reaction for unknown command
	 * 
	 * @param input line from client
	 */
	private void unknownCommand(String input) {
		messageToClient.println("502 Command not implemented");

	}

	/**
	 * @param input
	 * @param output
	 * @throws IOException
	 */
	private void moveFile(BufferedInputStream input, BufferedOutputStream output) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.flush();
	}

	/**
	 * gets content from command, for example: command: USER username; method returns username
	 * 
	 * @param input line from client
	 * @return string with content of line from client without command name
	 */
	private String getContentFromCommand(String input) {
		return input.substring(input.indexOf(" ")+1);
	}

	/**
	 * creates new timerTask
	 * 
	 * @return new TimerTask
	 */
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

	/**
	 * resets timer
	 */
	private void resetDisconnectionTimer() {
		disconnectTimer.cancel();
		disconnectTimer = new Timer();
		disconnectTask = createNewTimerTask();
		disconnectTimer.schedule(disconnectTask, 60*1000);
	}

	/**
	 * cancels timer
	 */
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
