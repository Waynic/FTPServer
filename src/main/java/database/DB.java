/**
 * 
 */
package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Class that connects and sends queries to the database
 * 
 * @author Jakub Fortunka
 *
 */
public class DB {

	/**
	 * Connection with database
	 */
	private Connection connection = null;
	/**
	 * statement to send
	 */
	private Statement statement = null;
	/**
	 * result set
	 */
	private ResultSet resultSet = null;
	
	/**
	 * Constructor. Connects with default database
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DB() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		connect("fortunka", "XK4uMsjV", "jdbc:mysql://mysql.agh.edu.pl/fortunka");
	}
	
	/**
	 * Constructor. Connects with custom database
	 * 
	 * @param username username for database
	 * @param password password for database
	 * @param address address to database
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public DB(String username, String password, String address) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		connect(username, password, address);
	}
	
	/**
	 * Method that connects with database with credentials passed by argumets
	 * 
	 * @param username username
	 * @param password password
	 * @param address address of database
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void connect(String username, String password, String address) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		connection = DriverManager.getConnection(address, username, password);
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet checkTables = dbm.getTables(null, null, "files", null);
		if (!checkTables.next()) {
			createTables();
		}
	}
	
	/**
	 * Method that creates tables in database if they don't exists
	 * 
	 * @throws SQLException
	 */
	private void createTables() throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `users` ("
				+ "`id` int(11) NOT NULL AUTO_INCREMENT, "
				+ "`username` varchar(10) NOT NULL, "
				+ "`password` varchar(45) NOT NULL, "
				+ "`salt` varchar(20) NOT NULL, "
				+ "PRIMARY KEY  (`id`), "
				+ "UNIQUE KEY `username` (`username`) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=3 ;");
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `groups` ("
				+ "`id` int(11) NOT NULL AUTO_INCREMENT, "
				+ "`group` varchar(10) NOT NULL, "
				+ "PRIMARY KEY  (`id`), "
				+ "UNIQUE KEY `group` (`group`) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=2 ;");
		
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `groups` (`id`, `group`) VALUES "
				+ "(1, 'admin');");
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `files` ("
				+ "`id` int(11) NOT NULL AUTO_INCREMENT, "
				+ "`filename` varchar(50) NOT NULL, "
				+ "`owner_id` int(11) NOT NULL, "
				+ "`group_id` int(11) NOT NULL, "
				+ "`user_read` tinyint(1) NOT NULL DEFAULT '1', "
				+ "`user_write` tinyint(1) NOT NULL DEFAULT '1', "
				+ "`user_execute` tinyint(1) NOT NULL DEFAULT '0', "
				+ "`group_read` tinyint(1) NOT NULL DEFAULT '0', "
				+ "`group_write` tinyint(1) NOT NULL DEFAULT '0', "
				+ "`group_execute` tinyint(1) NOT NULL DEFAULT '0', "
				+ "`others_read` tinyint(1) NOT NULL DEFAULT '1', "
				+ "`others_write` tinyint(1) NOT NULL DEFAULT '0', "
				+ "`others_execute` tinyint(1) NOT NULL DEFAULT '0', "
				+ "PRIMARY KEY  (`id`), "
				+ "UNIQUE KEY `filename` (`filename`) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=2 ;" );
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `usergroup` ("
				+ "`user_id` int(11) NOT NULL, "
				+ "`group_id` int(11) NOT NULL "
				+") ENGINE=MyISAM DEFAULT CHARSET=latin2;");
		
		addUser("anonymous", "anonymous", 2);
	}
	
	/**
	 * Method adds user to database
	 * 
	 * @param username username
	 * @param password password
	 * @param salt used to make it harder to decrypt user password
	 * @throws SQLException
	 */
	public void addUser(String username, String password, int salt) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `users`(`username`, `password`, `salt`) "
				+ "VALUES ('" + username + "',PASSWORD(CONCAT(PASSWORD('"+ password + "')," + salt + "))," + salt + ")");
		addGroup(username);
		addUserToGroup(username, username);
	}
	
	/**
	 * Adds group to database
	 *  
	 * @param groupname name of group to add
	 * @throws SQLException
	 */
	public void addGroup(String groupname) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `groups`(`group`) VALUES ('" + groupname + "')");
	}
	
	/**
	 * Adds file to database (with default rights)
	 * 
	 * @param filename name of file (path)
	 * @param owner name of owner of the file (must be in the database)
	 * @throws SQLException
	 */
	public void addFile(String filename, String owner) throws SQLException {
		int userID = getUserID(owner);
		int groupID = getGroupIDsOfUser(owner).get(1);
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `files`(`filename`, `owner_id`, `group_id`, `user_read`, `user_write`, `user_execute`, `group_read`,"
				+ " `group_write`, `group_execute`, `others_read`, `others_write`, `others_execute`)"
				+ " VALUES ('" + filename + "','" + userID + "','" + groupID + "',TRUE,TRUE,TRUE,TRUE,FALSE,FALSE,TRUE,FALSE,FALSE)");
	}
	
	/**
	 * Adds user to group
	 * 
	 * @param user username
	 * @param group groupname
	 * @throws SQLException
	 */
	public void addUserToGroup(String user, String group) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `usergroup`(`user_id`,`group_id`) VALUES "
				+ "((SELECT `id` FROM `users` WHERE `username`='" + user + "') , (SELECT `id` FROM `groups` WHERE `group`='" + group + "'))");
	}
	
	/**
	 * Deletes file from database
	 *  
	 * @param filename name of file to delete
	 * @throws SQLException
	 */
	public void deleteFile(String filename) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `files` WHERE `filename`='" + filename + "'");
	}
	
	/**
	 * Deletes user from database
	 * 
	 * @param username name of user to delete
	 * @throws SQLException
	 */
	public void deleteUser(String username) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `users` WHERE `username`='" + username +"'");
		deleteGroup(username);
		deleteConnectionBetweenUserAndGroup(username, username);
	}
	
	/**
	 * Deletes group from database
	 * 
	 * @param groupname name of group to delete
	 * @throws SQLException
	 */
	public void deleteGroup(String groupname) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `groups` WHERE `group`='" + groupname + "'");
	}
	
	/**
	 * Delets user from group
	 * 
	 * @param username name of user to disconnect with group
	 * @param groupname name of group to disconnect with user
	 * @throws SQLException
	 */
	public void deleteConnectionBetweenUserAndGroup(String username, String groupname) throws SQLException {
		int userID = getUserID(username);
		int groupID = getGroupID(groupname);
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `usergroup` WHERE `user_id`=" + userID + " AND `group_id`=" + groupID);
	}
	
	/**
	 * Changes rigths of file
	 * 
	 * @param filename name of file (path)
	 * @param ownerRights represents owner rights (numeric like 7 or 5)
	 * @param groupRights represents group rights (numeric like 7 or 5)
	 * @param othersRights represents others rights (numeric like 7 or 5)
	 * @throws SQLException
	 */
	public void changeFileRights(String filename, String ownerRights, String groupRights, String othersRights) throws SQLException {
		int owner = Integer.parseInt(ownerRights);
		int group = Integer.parseInt(groupRights);
		int others = Integer.parseInt(othersRights);
		String[] rights = adjustRights(owner,group,others);
		statement = connection.createStatement();
		statement.executeUpdate("UPDATE `files` SET "
				+ "`user_read`=" + rights[0]+ ", "
				+ "`user_write`=" + rights[1] + ", "
				+ "`user_execute`=" + rights[2] + ", "
				+ "`group_read`=" + rights[3] + ", "
				+ "`group_write`=" + rights[4] + ", "
				+ "`group_execute`=" + rights[5] + ", "
				+ "`others_read`=" + rights[6] + ", "
				+ "`others_write`=" + rights[7] + ", "
				+ "`others_execute`=" + rights[8]
				+ " WHERE `filename`='" + filename + "'");
	}
	
	/**
	 * changes rights to 'TRUE', 'FALSE' and distinguish read and write permission
	 * 
	 * @param ownerRights rights of owner of file
	 * @param groupRights rights of group of file
	 * @return array of Strings with adequate rights (TRUE or FALSE)
	 */
	private String[] adjustRights (int ownerRights, int groupRights, int othersRights) {
		String[] rights = new String[9];
		String[] owner = transformRights(ownerRights);
		String[] group = transformRights(groupRights);
		String[] others = transformRights(othersRights);
		rights[0]=owner[0];
		rights[1]=owner[1];
		rights[2]=owner[2];
		rights[3]=group[0];
		rights[4]=group[1];
		rights[5]=group[2];
		rights[6]=others[0];
		rights[7]=others[1];
		rights[8]=others[2];
		return rights;
	}
	
	/**
	 * Transform rights from int to 'TRUE', 'FALSE'
	 * 
	 * @param rights owner or group rights or others rights
	 * @return transformed numeric rights to TRUE/FALSE
	 */
	private String[] transformRights(int rights) {
		String[] newRights = new String[3];
		switch(rights) {
			case 0:
				newRights[0]="FALSE";
				newRights[1]="FALSE";
				newRights[2]="FALSE";
				break;
			case 1:
				newRights[0]="FALSE";
				newRights[1]="FALSE";
				newRights[2]="TRUE";
				break;
			case 2:
				newRights[0]="FALSE";
				newRights[1]="TRUE";
				newRights[2]="FALSE";
				break;
			case 3:
				newRights[0]="FALSE";
				newRights[1]="TRUE";
				newRights[2]="TRUE";
				break;
			case 4:
				newRights[0]="TRUE";
				newRights[1]="FALSE";
				newRights[2]="FALSE";
				break;
			case 5:
				newRights[0]="TRUE";
				newRights[1]="FALSE";
				newRights[2]="TRUE";
				break;
			case 6:
				newRights[0]="TRUE";
				newRights[1]="TRUE";
				newRights[2]="FALSE";
				break;
			case 7:
				newRights[0]="TRUE";
				newRights[1]="TRUE";
				newRights[2]="TRUE";
				break;
		}
		return newRights;
	}
	
	/**
	 * Checks if user is in database
	 * 
	 * @param username name of user to check
	 * @return true if user exists in database; false otherwise
	 * @throws SQLException
	 */
	public boolean checkIfUserExists(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `username`='" + username + "' FROM `users` WHERE `username`='" + username +"'");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	/**
	 * Checks if user entered password properly
	 * 
	 * @param user username
	 * @param password entered password
	 * @return true if password is ok; false otherwise
	 * @throws SQLException
	 */
	public boolean checkUserPassword(String user, String password) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `password`=PASSWORD(CONCAT(PASSWORD('" + password + "'),salt)) FROM "
				+ "`users` WHERE `username`='" + user + "'");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	/**
	 * checks if user can read file
	 * 
	 * @param user name of user which wants to read a file
	 * @param filename file that will (or not) be read
	 * @return true if user can read file; false otherwise
	 * @throws SQLException
	 */
	public boolean checkIfUserHavePermissionToRead(String user, String filename) throws SQLException {
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT `group_read` OR (`user_read` AND `owner_id`=" + userID + ") FROM "
				+ "`files` AS `f` WHERE `f`.`filename`='" + filename + "' AND `f`.`group_id` IN (SELECT `group_id` FROM "
				+ "`usergroup` WHERE `user_id`=" + userID + ")");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	/**
	 * Checks if user can write to file
	 * 
	 * @param user name of user which wants to write to a file
	 * @param filename file that will (or not) be written to
	 * @return true if user can write to specified file; false otherwise
	 * @throws SQLException
	 */
	public boolean checkIfUserHavePermissionToWrite(String user, String filename) throws SQLException {
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT `group_write` OR (`user_write` AND `owner_id`=" + userID + ") FROM "
				+ "`files` AS `f` WHERE `f`.`filename`='" + filename + "' AND `f`.`group_id` IN "
				+ "(SELECT `group_id` FROM `usergroup` WHERE `user_id`=" + userID + ")");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	/**
	 * checks if user have permission to execute file
	 * 
	 * @param user name of user to check
	 * @param filename name of file to check
	 * @return true if user have permission to execute file; false otherwise
	 * @throws SQLException
	 */
	public boolean checkIfUserHavePermissionToExecute(String user, String filename) throws SQLException {
		//TODO javadoc
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT `group_execute` OR (`user_execute` AND `owner_id`=" + userID + ") FROM "
				+ "`files` AS `f` WHERE `f`.`filename`='" + filename + "' AND `f`.`group_id` IN "
				+ "(SELECT `group_id` FROM `usergroup` WHERE `user_id`=" + userID + ")");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	/**
	 * checks if file exists in database
	 * 
	 * @param filename name of file to check
	 * @return true if file exists; false otherwise
	 * @throws SQLException
	 */
	public boolean checkIfFileExists(String filename) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `filename`='" + filename + "' FROM `files` WHERE `filename`='" + filename + "'");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	/**
	 * checks state (used in {@link database.DB#checkIfUserHavePermissionToRead(String, String) and database.DB#checkIfUserHavePermissionToWrite(String, String)}
	 * 
	 * @param result true if database returned 1; false otherwise
	 * @return true if result set returned 1; false otherwise
	 */
	private boolean check(String result) {
		if (Integer.parseInt(result) == 1) return true;
		else return false;
	}
	
	/**
	 * Gets user ID
	 * 
	 * @param username name of user which id we want to have
	 * @return id of user; when something went wrong returns -1
	 * @throws SQLException
	 */
	private int getUserID(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `id` FROM `users` WHERE `username`='" + username +"'");
		if (resultSet.next()) return Integer.parseInt(resultSet.getString(1));
		else return -1;
	}
	
	/**
	 * Gets group ID
	 * 
	 * @param groupname name of group which ID we want to have
	 * @return group ID; when something went wrong returns -1
	 * @throws SQLException
	 */
	private int getGroupID(String groupname) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `id` FROM `groups` WHERE `groupname`='" + groupname + "'");
		if (resultSet.next()) return Integer.parseInt(resultSet.getString(1));
		else return -1;
	}
	
	/**
	 * Gets all groups IDs in which some user is
	 * 
	 * @param username name of user to get all of his group ids
	 * @return {@link ArrayList} of {@link Integer} in which are stored all ids of groups
	 * @throws SQLException
	 */
	private ArrayList<Integer> getGroupIDsOfUser(String username) throws SQLException {
		int userID = getUserID(username);
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `group_id` FROM `usergroup` WHERE `user_id`=" + userID);
		ArrayList<Integer> list = new ArrayList<Integer>();
		while (resultSet.next()) {
			list.add(resultSet.getInt(1));
		}
		return list;
		//return Integer.parseInt(resultSet.getString(1));
	}
	
	/**
	 * Gets all users from database
	 * 
	 * @return {@link ArrayList} of {@link String} in which are all usernames
	 * @throws SQLException
	 */
	public ArrayList<String> getAllUsernames() throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `username` FROM `users`");
		ArrayList<String> usernames = new ArrayList<String>();
		while (resultSet.next()) {
			usernames.add(resultSet.getString(1));
		}
		return usernames;
	}
	
	/**
	 * Gets all groups from database
	 * 
	 * @return all groups that exists in database
	 * @throws SQLException
	 */
	public ArrayList<String> getAllGroups() throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `group` FROM `groups`");
		ArrayList<String> groups = new ArrayList<String>();
		while (resultSet.next()) groups.add(resultSet.getString(1));
		return groups;
	}
	
	/**
	 * Gets informations about user: his ID and all groups that he belongs in.
	 * 
	 * @param username name of user about which we want to get informations
	 * @return informations about user
	 * @throws SQLException
	 */
	public ArrayList<String> getInformationAboutUser(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT * FROM `users` WHERE `username`='" + username + "'");
		if (resultSet.next()) {
			ArrayList<String> informations = new ArrayList<String>();
			informations.add(resultSet.getString(1));
			informations.add(resultSet.getString(2));
			ArrayList<Integer> groupID = getGroupIDsOfUser(username);
			for (Integer i : groupID) {
				statement = connection.createStatement();
				resultSet = statement.executeQuery("SELECT `group` FROM `groups` WHERE `id`=" + i);
				if (resultSet.next()) informations.add(resultSet.getString(1));
			}
			return informations;
		}
		else {
			ArrayList<String> error = null;
			return error;
		}
		
	}
	
	/**
	 * Gets informations about files (for listing): id, owner, group, user_read, user_write, group_read, group_write
	 * 
	 * @param filename name of file to get informations about
	 * @return informations about file (what informations? look up!)
	 * @throws SQLException
	 */
	public String[] getFileInformations(String filename) throws SQLException {
		//TODO javadoc
		String[] informations = new String[12];
		informations[0] = getFileInformation(filename, "id");
		informations[1] = getUserByID(getFileInformation(filename, "owner_id"));
		informations[2] = getGroupByID(getFileInformation(filename, "group_id"));
		informations[3] = getFileInformation(filename, "user_read");
		informations[4] = getFileInformation(filename, "user_write");
		informations[5] = getFileInformation(filename, "user_execute");
		informations[6] = getFileInformation(filename, "group_read");
		informations[7] = getFileInformation(filename, "group_write");
		informations[8] = getFileInformation(filename, "group_execute");
		informations[9] = getFileInformation(filename, "others_read");
		informations[10] = getFileInformation(filename, "others_write");
		informations[11] = getFileInformation(filename, "others_execute");
		return informations;
	}
	
	/**
	 * gets username by his id
	 * 
	 * @param userID id of user which name we want to find
	 * @return name of user who has passed id
	 * @throws SQLException
	 */
	public String getUserByID(String userID) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `username` FROM `users` WHERE `id`=" + userID);
		if (resultSet.next()) return resultSet.getString(1);
		else return null;
	}
	
	/**
	 * Gets group name by hers id
	 * 
	 * @param groupID id of group
	 * @return name of group which has passed id
	 * @throws SQLException
	 */
	public String getGroupByID(String groupID) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `group` FROM `groups` WHERE `id`=" + groupID);
		if (resultSet.next()) return resultSet.getString(1);
		else return null;
	}
	
	/**
	 * gets informations about file - what informations will it be, is passed by columnName
	 * 
	 * @param filename name of file (path)
	 * @param columnName name of column from which we want to get information
	 * @return informations about file
	 * @throws SQLException
	 */
	private String getFileInformation(String filename, String columnName) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `" + columnName + "` FROM `files` WHERE `filename`='" + filename + "'");
		if (resultSet.next()) return resultSet.getString(1);
		else return null;
	}
	
	/**
	 * Checks if others have permission to read file
	 * 
	 * @param filename name of file to check
	 * @return true if others have permission to read file; false otherwise
	 * @throws SQLException
	 */
	public String getOthersReadPermission(String filename) throws SQLException {
		return getFileInformation(filename, "others_read");
	}
	
	/**
	 * Checks if others have permission to write to file
	 * 
	 * @param filename name of file to check
	 * @return true if others have permission to write to file; false otherwise
	 * @throws SQLException
	 */
	public String getOthersWritePermission(String filename) throws SQLException {
		return getFileInformation(filename, "others_write");
	}
	
	/**
	 * Checks if others have permission to execute file
	 * 
	 * @param filename name of file to check
	 * @return true if others have permission to execute file; false otherwise
	 * @throws SQLException
	 */
	public String getOthersExecutePermission(String filename) throws SQLException {
		return getFileInformation(filename, "others_execute");
	}
}
