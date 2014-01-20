/**
 * 
 */
package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import ftp.ConnectionException;

/**
 * Class that is responsible for connection and functions working with FTP server
 * 
 * @author Jakub Fortunka
 *
 */
public class Connector {

	/**
	 * Socket which purpose is to communicate with FTP server
	 */
	private Socket server = null;
	/**
	 * {@see PrinterWriter} that writes to the server (whatever message we want to send)
	 */
	private PrintWriter write = null;
	/**
	 * by this we are reading responses from server
	 */
	private BufferedReader read = null;

	/**
	 * it allows to caught ecxeptions which are thrown by {@link Thread}, and then can do somthing with it
	 */
	private Thread.UncaughtExceptionHandler handler;

	/**
	 * Thread that is used to repeatedly send NOOP command
	 */
	private Thread timer = null;
	/**
	 * field that helps to find out wheter timer should be stop or not
	 */
	private boolean killTimer = false;

	/**
	 * Socket for sending/retrieving to/from server
	 */
	private Socket dataSocket = null;

	/**
	 * input stream
	 */
	private BufferedInputStream input = null;
	/**
	 * output stream (is used in sending/retrieving files) (and listings)
	 */
	private BufferedOutputStream output = null;

	/**
	 * if we are connected to my FTPServer, then there should be different CHMOD command
	 */
	private boolean usingMyOwnSuperServer = false;

	/**
	 * Class handles all communication with FTP Server.
	 * 
	 */
	public Connector() {
	}

	/**
	 * @param h hander for timer thread
	 */
	public Connector(Thread.UncaughtExceptionHandler h) {
		handler = h;		
	}

	/**
	 * this method connects to the server using passed credentials
	 * 
	 * @param host hostname
	 * @param port port number of server
	 * @param user username
	 * @param pass password
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized void connectToServer(String host, int port, String user, String pass) throws UnknownHostException, IOException, ConnectionException {
		startConnectingToServer(host, port);

		timer = newTimerThread();
		timer.start();

		sendUserCommand(user);
		sendPassCommand(pass);
	}

	/**
	 * it only starts connection with the server, in case user wants to login by himself (by commands)
	 * 
	 * @param host hostname
	 * @param port port number of the server
	 * @throws IOException
	 */
	public void startConnectingToServer(String host, int port) throws ConnectionException, IOException {
		if (port == 3021) usingMyOwnSuperServer = true;
		server = new Socket(host,port);
		write = new PrintWriter(server.getOutputStream(), true);
		read = new BufferedReader(new InputStreamReader(server.getInputStream()));
		String response = null;
		response = getAllResponses("220", read.readLine());
		if (!response.startsWith("220 ")) {
			throw new ConnectionException("Unknown response from FTP Server: " + response);
		}
	}

	/**
	 * method sends to the server command USER with passed username
	 * 
	 * @param user username
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public void sendUserCommand(String user) throws IOException, ConnectionException {
		sendLine("USER " + user);
		String response = getAllResponses("331", read.readLine());
		if (!response.startsWith("331 ")) {
			if (timer != null) cancelNOOPDeamon();
			throw new ConnectionException("There is a problem with username:  "	+ response);
		}
	}

	/**
	 * method sends to the server command PASS with passed password (password is hashed, so on System.out it won't be shown)
	 * 
	 * @param pass password
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public void sendPassCommand(String pass) throws IOException, ConnectionException {
		sendLine("PASS " + pass);
		String response = getAllResponses("230", read.readLine());
		if (!response.startsWith("230 ")) {
			if (timer != null) cancelNOOPDeamon();
			throw new ConnectionException("There is a problem with password: " + response);
		}
		if (timer==null) {
			timer = newTimerThread();
			timer.start();
		}
	}

	/**
	 * Method that disconnects from the server (if not connected - throws {@see IOException} ), by sending QUIT command
	 * 
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized void disconnect() throws IOException, ConnectionException {
		try {
			if (!server.isConnected()) {
				throw new ConnectionException("Can't disconnect from server - you haven't connected yet!");
			}
			usingMyOwnSuperServer = false;
			sendLine("QUIT");
			getAllResponses("221", read.readLine());
		} finally {
			cancelNOOPDeamon();
			read.close();
			server.close();
		}
	}

	/**
	 * method that sends PWD command, and retrieve information about working directory
	 * 
	 * @return name of directory in which we are
	 * @throws IOException
	 */
	public synchronized String pwd() throws IOException {
		sendLine("PWD");
		String dir = null;
		String response = read.readLine();
		response = getAllResponses("257", response);
		if (response.startsWith("257 ")) {
			int firstQuote = response.indexOf('\"');
			int secondQuote = response.indexOf('\"', firstQuote + 1);
			if (secondQuote > 0) {
				dir = response.substring(firstQuote + 1, secondQuote);
			}
		}
		return dir;
	}

	/**
	 * Changes the working directory (like cd). Returns true if successful.
	 * 
	 * @param dir String in which is directorypath to which we want to go.
	 */
	public synchronized boolean cwd(String dir) throws IOException {
		sendLine("CWD " + dir);
		String response = read.readLine();
		response = getAllResponses("250", response);
		return (response.startsWith("250 "));
	}

	/**
	 * Lists current directory. Returns ArrayList of {@link FTPFile} in which is stored information about content of current directory.
	 * Executes {@link client.pasv()} method.
	 * 
	 * @return ArrayList<{@link FTPFile}> in which are the information about files in the current directory on the server
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized ArrayList<FTPFile> list() throws IOException, ConnectionException {
		//	Object[] o = pasv();
		//	String ip=(String)o[0];
		//	int port = (int)o[1];
		//	dataSocket = new Socket(ip,port);
		if (!pasv()) throw new ConnectionException("Problem with PASV");
		return sendListLine();
	}

	/**
	 * Sends LIST command, and gets listing of current working directory, but IT DOESN'T SENDS {@link client.pasv} METHOD!
	 * 
	 * @return {@see ArrayList} of {@link FTPFile} which represents file on the server 
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	
	public synchronized ArrayList<FTPFile> sendListLine() throws IOException, ConnectionException {
		sendLine("LIST");
		BufferedReader readList = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
		String response = getAllResponses("150", read.readLine());
		String file = null;
		ArrayList<FTPFile> files = new ArrayList<FTPFile>();
		files.add(new FTPFile());
		if (response.startsWith("150 ")) {
			while ((file=readList.readLine()) != null) {
				System.out.println(file);
				if (!(file.endsWith(".")
						&& (file.substring(file.lastIndexOf(".")-1, file.lastIndexOf(".")).equals(" ")
						|| file.substring(file.lastIndexOf(".")-1, file.lastIndexOf(".")).equals("."))))
					files.add(new FTPFile(file));
				//&& file.substring(file.lastIndexOf(".")-1, file.lastIndexOf(".")).equals("."))
			}
			dataSocket.close();
		}
		else {
			dataSocket.close();
			throw new ConnectionException("List Problem" + response);
		}
		response = read.readLine();
		response = getAllResponses("226", response);
		if (!response.startsWith("226 ")) {
			throw new ConnectionException("List Problem" + response);
		}
		return files;
	}

	/**
	 * Method sends STOR command (it doesn't send PASV command though)
	 * 
	 * @param file file which will be sended to server
	 * @param isStor true if sending STOR; false if sending APPE
	 * @return true if everything went well
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ConnectionException 
	 */
	public boolean sendStorCommand(File file, boolean isStor) throws IOException, FileNotFoundException, ConnectionException {
		String filename = file.getName();
		if (isStor) sendLine("STOR " + filename);
		else sendLine("APPE " + filename);
		String response = getAllResponses("150", read.readLine());
		if (!response.startsWith ("150 ")) {
			dataSocket.close();
			throw new ConnectionException("Can't send the file: " + response);
		}
		input = new BufferedInputStream(new FileInputStream(file));
		output = new BufferedOutputStream(dataSocket.getOutputStream());
		boolean status = moveFile();
		return status;
	}

	/**
	 * Sends RETR command (doesn't send PASV !)
	 * 
	 * @param file file in which will be saved file from server
	 * @param filename name of the file on server (which we want download)
	 * @return true if everything well as expected
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public boolean sendRetrCommand(File file, String filename) throws IOException, ConnectionException {
		sendLine("RETR " + filename);
		String response = getAllResponses("150", read.readLine());
		if (!response.startsWith ("150 ")) {
			dataSocket.close();
			throw new ConnectionException("Problem with downloading a file:" + response);
		}
		input = new BufferedInputStream(dataSocket.getInputStream());
		output = new BufferedOutputStream(new FileOutputStream(file));
		return moveFile();
	}

	/**
	 * Sends APPE command (PASV must be initialized first!)
	 * 
	 * @param file file which we want to append
	 * @return true if everything worked
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public boolean sendAppeCommand(File file) throws FileNotFoundException, IOException, ConnectionException {
		return sendStorCommand(file, false);
	}

	/**
	 * Sends file to the server (takes care of PASV and all this stuff)
	 * 
	 * @param file file we want to send to the server
	 * @param isStor true if we want to send STOR line; false if line should be APPE
	 * @return true if everything is fine (sending was completed)
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean sendFile(File file, boolean isStor) throws IOException, ConnectionException {
		if (pasv()) {
			if (isStor) return sendStorCommand(file, true);
			else return sendAppeCommand(file);
		}
		else return false;
	}

	/**
	 * Retrieves file from server (takes care of PASV and all this stuff)
	 * 
	 * @param file file at computer in which we want to save file from server
	 * @param filename full path to the file which we want to download from server
	 * @return true if everything is fine (sending was completed)
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean getFile(File file, String filename) throws IOException, ConnectionException {
		if (pasv()) return sendRetrCommand(file, filename);
		else return false;
	}

	/**
	 * Method copies file from {@link input} to {@link output}
	 * 
	 * @return true if everything went as expected
	 * @throws IOException
	 */
	private boolean moveFile() throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		if (output != null) {
			output.flush();
			output.close();
		}
		if (input != null) input.close();
		dataSocket.close();
		return transferingFileCompleted();
	}

	/**
	 * Checks if transfer is completed
	 * 
	 * @return true if is completed; false otherwise
	 * @throws IOException
	 */
	private boolean transferingFileCompleted() throws IOException {
		String response = getAllResponses("226", read.readLine());
		if (response.startsWith("226 ")) return true;
		else return false;
	}

	/**
	 * Sends directory to the server. Uses {@link client.sendFile} and {@link makeDirectory} to achieve it. Is recursive.
	 * 
	 * @param directoryToSend {@see File} which represents directory that we want to send to the server.
	 * @return true if all went as expected; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean sendDirectory(File directoryToSend) throws IOException, ConnectionException {
		File[] list = directoryToSend.listFiles();
		ArrayList<File> directoriesList = new ArrayList<File>();
		String directoryName = directoryToSend.getName();
		boolean status = true;
		if (!cwd(directoryName)) {
			makeDirectory(directoryName);
			cwd(directoryName);
		}
		for (File f : list) {
			if (f.isFile()) sendFile(f, true);
			if (f.isDirectory()) directoriesList.add(f);
		}
		for (File directory : directoriesList) {
			status = sendDirectory(directory);
		}
		cwd("..");
		return status;
	}

	/**
	 * Gets directory from server. 
	 * 
	 * @param localDirectory {@see File} which represents directory to which we want write directory from server
	 * @param directorypath path of directory we want to get (on the server)
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean getDirectory(File localDirectory, String directorypath) throws IOException, ConnectionException {
		if(!localDirectory.exists()) localDirectory.mkdir();
		boolean status = true;
		cwd(directorypath);
		String localFilepath = localDirectory.getAbsolutePath() + File.separator;
		ArrayList<FTPFile> list = list();
		ArrayList<FTPFile> directories = new ArrayList<FTPFile>();
		for (FTPFile f : list) {
			if (f.isDirectory()) directories.add(f);
			else {
				File newFile = new File(localFilepath + f.getFilename());
				status = getFile(newFile, f.getFilename());
			}
		}
		for (FTPFile d : directories) {
			if (!d.getFilename().equals("..")) getDirectory(new File(localFilepath + d.getFilename()), d.getFilename());
		}
		cwd("..");
		return status;
	}

	/**
	 * Sends PASV command. Connects dataSocket to the Socket that server want to use for data connection
	 * 
	 * @return if dataSocket connected to server - true; else - false
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean pasv() throws IOException, ConnectionException {
		sendLine("PASV");
		String response = getAllResponses("227", read.readLine());
		if (!response.startsWith("227 ")) {
			throw new ConnectionException("Can't access passive mode " + response);
		}

		String ip = null;
		int port = -1;
		int opening = response.indexOf('(');
		int closing = response.indexOf(')', opening + 1);
		if (closing > 0) {
			String dataLink = response.substring(opening + 1, closing);
			StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
			try {
				ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken();
				port = Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken());
			} catch (Exception e) {
				throw new IOException("Received bad data link connection: " + response);
			}
		}
		dataSocket = new Socket(ip,port);
		if (dataSocket.isConnected()) return true;
		else return false;
	}

	/**
	 * Makes directory on the server - sends MKD command
	 * 
	 * @param dirpath path of the directory to be made (if it will be created in current working directory, can be only name of directory)
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean makeDirectory(String dirpath) throws IOException, ConnectionException {
		sendLine("MKD " + dirpath);
		String response = getAllResponses("257", read.readLine());
		if (!response.startsWith("257 ")) {
			throw new ConnectionException("There is a problem with making directory: " + response);
		}
		else return true;
	}

	/**
	 * Removes directory from the server (and all the files that are in it)
	 * 
	 * @param dirname name (or path) of the directory that we want to remove
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean removeDirectory(String dirname) throws IOException, ConnectionException {
		cwd(dirname);
		ArrayList<FTPFile> list = list();
		ArrayList<FTPFile> directoriesList = new ArrayList<FTPFile>();
		for (FTPFile f : list) {
			if (f.isDirectory()) directoriesList.add(f);
			else removeFile(f.getFilename());
		}
		for (FTPFile directory : directoriesList) {
			if (!(directory.getFilename().equals(".."))) removeDirectory(directory.getFilename());
		}
		list = list();
		directoriesList.clear();
		for (FTPFile f : list) {
			if (f.isDirectory()) directoriesList.add(f);
			else removeFile(f.getFilename());
		}
		if (directoriesList.size()==1) {
			cwd("..");
			sendLine("RMD " + dirname);
			String response = getAllResponses("250", read.readLine());
			return response.startsWith("250 ");
		}
		return true;
	}

	/**
	 * Removes file from the server. Sends DELE line
	 * 
	 * @param filename name of the file that we want to delete
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean removeFile(String filename) throws IOException, ConnectionException {
		sendLine("DELE " + filename);
		String response = getAllResponses("250", read.readLine());
		if (!response.startsWith("250 ")) {
			throw new ConnectionException("There is a problem with removing file: " + response);
		}
		else return true;
	}

	/**
	 * Makes empty file on the server (creates temporary local file and sends it)
	 * 
	 * @param filename name of the file we want to create
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean makeFile(String filename) throws IOException, ConnectionException {
		File f = new File(filename);
		f.createNewFile();
		boolean test = sendFile(f, true);
		f.delete();
		return test;
	}

	/**
	 * Changes name of the file (or directory) on the server. Uses RNFR and RNTO commands to achieve that
	 * 
	 * @param oldFilename name of the file(or directory) from which we want to change
	 * @param newFilename name of the file(or directory) to which we want to change
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean changeName(String oldFilename, String newFilename) throws IOException, ConnectionException {
		sendLine("RNFR " + oldFilename);
		String response = getAllResponses("350", read.readLine());
		if (!response.startsWith("350 ")) {
			throw new ConnectionException("There is a problem with renaming file: " + response);
		}
		sendLine("RNTO " + newFilename);
		response = getAllResponses("250", read.readLine());
		if (!response.startsWith("250 ")) {
			throw new ConnectionException("There is a problem with renaming file: " + response);
		}
		return true;
	}

	/**
	 * Changes rights of the file (or directory) on the server. Uses SITE CHMOD xxx filename syntax, or, for my FTPServer, CHMOD filename xxx
	 * 
	 * @param filename name of file (or directory) which rights we want to change
	 * @param rights rights to which we want to change (must be numeric, like 777 or 644 etc.)
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 * @throws ConnectionException 
	 */
	public synchronized boolean changeRights(String filename, String rights) throws IOException, ConnectionException {
		if (usingMyOwnSuperServer) sendLine("CHMOD " + filename + " " + rights);
		else sendLine("SITE CHMOD " + rights + " " + filename);
		String response = getAllResponses("200", read.readLine());
		if (!response.startsWith("200 ")) {
			throw new ConnectionException("There is a problem with changing rights to file/directory: " + response);
		}
		return true;
	}

	/**
	 * Sends NOOP which keeps connection with server
	 * 
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 */
	public synchronized boolean noop() throws IOException {
		sendLine("NOOP");
		if (getAllResponses("200", read.readLine()).startsWith("200 ")) return true;
		else return false;
	}

	/**
	 * Sends ABOR which aborts currently operation
	 * 
	 * @return true if everything went well; false otherwise
	 * @throws IOException
	 */
	public synchronized boolean abort() throws IOException {
		sendLine("ABOR");
		//sendLine("NOOP");
		if (!dataSocket.isClosed()) {
			output.close();
			input.close();
			getAllResponses("426", read.readLine());
		}
		dataSocket.close();
		if (getAllResponses("200", read.readLine()).startsWith("200 ")) return true;
		else return false;
	}

	/**
	 * Sends a raw command to the FTP server.
	 * 
	 * @param line line that will be send to server
	 * @throws IOException
	 */
	private synchronized void sendLine(String line) throws IOException {
		if (server == null || server.isClosed()) {
			throw new IOException("There is no connection to any FTP server or you've been inactive for too long. If so, please connect again");
		}
		write.write(line + "\r\n");
		write.flush();
		if (!line.startsWith("PASS ")) System.out.println(line);
		else System.out.println("PASS *****");
		if (!line.equals("NOOP")) resetNoopTimer();
	}

	/**
	 * gets all responses from the server (because sometimes server sends additional lines with the same type of number status, but with '-')
	 * 
	 * @param code code we want to read all lines (like 200 or 257)
	 * @param response first response from the server
	 * @return last line of the response from the server (which we want)
	 * @throws IOException
	 */
	private String getAllResponses(String code, String response) throws IOException {
		String r = response;
		while (r.startsWith(code + "-")) {
			System.out.println(r);
			System.out.flush();
			r=read.readLine();
		}
		System.out.println(r);
		System.out.flush();
		return r;
	}

	/**
	 * Method creates new Thread (is used to reset timer)
	 * 
	 * @return Thread that is used like a timer to sends NOOP line
	 */
	private synchronized Thread newTimerThread() {
		return new Thread("Timer-Thread") {
			@Override
			public void run() {
				try {
					sleep(1000*30);
					while(!killTimer) {
						noop();
						sleep(1000*30);
					}
					disconnect();
				} catch (InterruptedException e) {
					if (killTimer) return ;
					else {
						timer = newTimerThread();
						timer.setUncaughtExceptionHandler(handler);
						timer.start();
					}
				} catch (IOException e) {
					throw new RuntimeException("Server disconnected!");
				} catch (NullPointerException e) {
					throw new RuntimeException("Server disconnected!");
				} catch (ConnectionException e) {
					throw new RuntimeException("Problem with disconnecting");
				}
			}
		};
	}
	/**
	 * Resets timer, mostly after the line is send to the server (because it means that we are active)
	 */
	private synchronized void resetNoopTimer() {
		timer.interrupt();
	}

	/**
	 * Cancels timer (for example, if we disconnect from the server)
	 */
	public synchronized void cancelNOOPDeamon() {
		killTimer=true;
	}

}
