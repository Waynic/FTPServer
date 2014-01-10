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
 * @author Jakub Fortunka
 *
 */
public class DB {

	private Connection connection = null;
	private Statement statement = null;
	private ResultSet resultSet = null;
	
	public DB() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		connect("fortunka", "XK4uMsjV", "jdbc:mysql://mysql.agh.edu.pl/fortunka");
	}
	
	public DB(String username, String password, String address) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		connect(username, password, address);
	}
	
	private void connect(String username, String password, String address) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		connection = DriverManager.getConnection(address, username, password);
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet checkTables = dbm.getTables(null, null, "files", null);
		if (!checkTables.next()) {
			createTables();
		}
	}
	
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
				+ "`group_read` tinyint(1) NOT NULL DEFAULT '0', "
				+ "`group_write` tinyint(1) NOT NULL DEFAULT '0', "
				+ "PRIMARY KEY  (`id`), "
				+ "UNIQUE KEY `filename` (`filename`) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=2 ;" );
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `usergroup` ("
				+ "`user_id` int(11) NOT NULL, "
				+ "`group_id` int(11) NOT NULL "
				+") ENGINE=MyISAM DEFAULT CHARSET=latin2;");
	}
	
	public void addUser(String username, String password, int salt) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `users`(`username`, `password`, `salt`) "
				+ "VALUES ('" + username + "',PASSWORD(CONCAT(PASSWORD('"+ password + "')," + salt + "))," + salt + ")");
		addGroup(username);
		addUserToGroup(username, username);
	}
	
	public void addGroup(String groupname) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `groups`(`group`) VALUES ('" + groupname + "')");
	}
	
	public void addFile(String filename, String owner) throws SQLException {
		int userID = getUserID(owner);
		int groupID = getGroupIDsOfUser(owner).get(1);
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `files`(`filename`, `owner_id`, `group_id`, `user_read`, `user_write`, `group_read`, `group_write`)"
				+ " VALUES ('" + filename + "','" + userID + "','" + groupID + "',TRUE,TRUE,TRUE,FALSE)");
	}
	
	public void addUserToGroup(String user, String group) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO `usergroup`(`user_id`,`group_id`) VALUES "
				+ "((SELECT `id` FROM `users` WHERE `username`='" + user + "') , (SELECT `id` FROM `groups` WHERE `group`='" + group + "'))");
	}
	
	public void deleteFile(String filename) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `files` WHERE `filename`='" + filename + "'");
	}
	
	public void deleteUser(String username) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `users` WHERE `username`='" + username +"'");
		deleteGroup(username);
		deleteConnectionBetweenUserAndGroup(username, username);
	}
	
	public void deleteGroup(String groupname) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `groups` WHERE `group`='" + groupname + "'");
	}
	
	public void deleteConnectionBetweenUserAndGroup(String username, String groupname) throws SQLException {
		int userID = getUserID(username);
		int groupID = getGroupID(groupname);
		statement = connection.createStatement();
		statement.executeUpdate("DELETE FROM `usergroup` WHERE `user_id`=" + userID + " AND `group_id`=" + groupID);
	}
	
	public void changeFileRights(String filename, String ownerRights, String groupRights) throws SQLException {
		int owner = Integer.parseInt(ownerRights);
		int group = Integer.parseInt(groupRights);
		String[] rights = adjustRights(owner,group);
		statement = connection.createStatement();
		statement.executeUpdate("UPDATE `files` SET (`user_read`,`user_write`,`group_read`,`group_write`)"
		+ " VALUES ('" + rights[0] + "', '" + rights[1] + "', '" + rights[2] + "', '"
				+ rights[3] + "') WHERE `filename`='" + filename + "'");
	}
	
	private String[] adjustRights (int ownerRights, int groupRights) {
		String[] rights = new String[4];
		String[] owner = transformRights(ownerRights);
		String[] group = transformRights(groupRights);
		rights[0]=owner[0];
		rights[1]=owner[1];
		rights[2]=group[0];
		rights[3]=group[1];
		return rights;
	}
	
	private String[] transformRights(int rights) {
		String[] newRights = new String[2];
		if (rights==0) {
			newRights[0]="FALSE";
			newRights[1]="TRUE";
		}
		else if (rights==1) {
			newRights[0]="TRUE";
			newRights[1]="FALSE";
		}
		else if (rights==2) {
			newRights[0]="FALSE";
			newRights[1]="TRUE";
		}
		else {
			newRights[0]="TRUE";
			newRights[1]="TRUE";
		}
		return newRights;
	}
	
	public boolean checkIfUserExists(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `username`='" + username + "' FROM `users` WHERE `username`='" + username +"'");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	public boolean checkUserPassword(String user, String password) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `password`=PASSWORD(CONCAT(PASSWORD('" + password + "'),salt)) FROM "
				+ "`users` WHERE `username`='" + user + "'");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	public boolean checkIfUserHavePermissionToRead(String user, String filename) throws SQLException {
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT `group_read` OR (`user_read` AND `owner_id`=" + userID + ") FROM "
				+ "`files` AS `f` WHERE `f`.`filename`='" + filename + "' AND `f`.`group_id` IN (SELECT `group_id` FROM "
				+ "`usergroup` WHERE `user_id`=" + userID + ")");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	public boolean checkIfUserHavePermissionToWrite(String user, String filename) throws SQLException {
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT `group_write` OR (`user_write` AND `owner_id`=" + userID + ") FROM "
				+ "`files` AS `f` WHERE `f`.`filename`='" + filename + "' AND `f`.`group_id` IN "
				+ "(SELECT `group_id` FROM `usergroup` WHERE `user_id`=" + userID + ")");
		if (resultSet.next()) return check(resultSet.getString(1));
		else return false;
	}
	
	private boolean check(String result) {
		if (Integer.parseInt(result) == 1) return true;
		else return false;
	}
	
	private int getUserID(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `id` FROM `users` WHERE `username`='" + username +"'");
		if (resultSet.next()) return Integer.parseInt(resultSet.getString(1));
		else return -1;
	}
	
	private int getGroupID(String groupname) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `id` FROM `groups` WHERE `groupname`='" + groupname + "'");
		if (resultSet.next()) return Integer.parseInt(resultSet.getString(1));
		else return -1;
	}
	
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
	
	public ArrayList<String> getAllUsernames() throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `username` FROM `users`");
		ArrayList<String> usernames = new ArrayList<String>();
		while (resultSet.next()) {
			usernames.add(resultSet.getString(1));
		}
		return usernames;
	}
	
	public ArrayList<String> getAllGroups() throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `group` FROM `groups`");
		ArrayList<String> groups = new ArrayList<String>();
		while (resultSet.next()) groups.add(resultSet.getString(1));
		return groups;
	}
	
	public ArrayList<String> getInformationAboutUser(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT * FROM `users` WHERE `username`='" + username + "'");
		if (resultSet.next()) {
			ArrayList<String> informations = new ArrayList<String>();
			informations.add(resultSet.getString(1));
			informations.add(resultSet.getString(2));
			//String[] informations = new String[3];
			//informations[0] = resultSet.getString(1);
			//informations[1] = resultSet.getString(2);
			ArrayList<Integer> groupID = getGroupIDsOfUser(username);
			for (Integer i : groupID) {
				statement = connection.createStatement();
				resultSet = statement.executeQuery("SELECT `group` FROM `groups` WHERE `id`=" + i);
				if (resultSet.next()) informations.add(resultSet.getString(1));
			}
			return informations;
			//resultSet = statement.executeQuery("SELECT `group` FROM `groups` WHERE `id`=" + groupID);
			//informations[2] = resultSet.getString(1);
			//return informations;
		}
		else {
			//String[] error = new String[1];
			//error[0] = "error";
			ArrayList<String> error = null;
			return error;
		}
		
	}
	
	public String[] getFileInformations(String filename) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT * FROM `files` WHERE `filename`='" + filename + "'");
		if (resultSet.next()) {
			String[] informations = new String[7];
			informations[0] = resultSet.getString(1);
			informations[1] = getUserByID(resultSet.getString(3));
			informations[2] = getGroupByID(resultSet.getString(4));
			for (int i=0;i<4;i++) {
				informations[3+i] = resultSet.getString(5+i);
			}
			return informations;
		}
		else {
			String[] error = new String[1];
			error[0] = "error";
			return error;
		}
	}
	
	public String getUserByID(String userID) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `username` FROM `users` WHERE `id`=" + userID);
		return resultSet.getString(1);
	}
	
	public String getGroupByID(String groupID) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `group` FROM `groups` WHERE `id`=" + groupID);
		return resultSet.getString(1);
	}

}
