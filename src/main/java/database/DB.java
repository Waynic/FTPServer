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
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
				+ "id int(1) NOT NULL AUTO_INCREMENT, "
				+ "username varchar(10) NOT NULL, "
				+ "password varchar(45) NOT NULL, "
				+ "salt varchar(20) NOT NULL, "
				+ "RIMARY KEY  (id), "
				+ "UNIQUE KEY username (username) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=3 ;");
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS groups ("
				+ "id int(1) NOT NULL AUTO_INCREMENT, "
				+ "group varchar(10) NOT NULL, "
				+ "PRIMARY KEY  (id), "
				+ "UNIQUE KEY group (group) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=2 ;");
		
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO groups (id, group) VALUES "
				+ "(1, 'admin');");
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS files ("
				+ "id int(11) NOT NULL AUTO_INCREMENT, "
				+ "filename varchar(50) NOT NULL, "
				+ "owner_id int(11) NOT NULL, "
				+ "group_id int(11) NOT NULL, "
				+ "user_read tinyint(1) NOT NULL DEFAULT '1', "
				+ "user_write tinyint(1) NOT NULL DEFAULT '1', "
				+ "group_read tinyint(1) NOT NULL DEFAULT '0', "
				+ "group_write tinyint(1) NOT NULL DEFAULT '0', "
				+ "PRIMARY KEY  (id), "
				+ "UNIQUE KEY filename (filename) "
				+ ") ENGINE=MyISAM  DEFAULT CHARSET=latin2 AUTO_INCREMENT=2 ;" );
		
		statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS usergroup ("
				+ "user_id int(11) NOT NULL, "
				+ "group_id int(11) NOT NULL "
				+") ENGINE=MyISAM DEFAULT CHARSET=latin2;");
	}
	
	public void addUser(String username, String password, int salt) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO users(username, password, salt) "
				+ "VALUES ('" + username + "',PASSWORD(CONCAT(PASSWORD('"+ password + "')," + salt + "))," + salt + ")");
	}
	
	public void addFile(String filename, String owner) throws SQLException {
		int userID = getUserID(owner);
		int groupID = getGroupIDOfUser(owner);
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO files(filename, owner_id, group_id, user_read, user_write, group_read, group_write)"
				+ " VALUES ('" + filename + "','" + userID + "','" + groupID + "',TRUE,TRUE,TRUE,FALSE)");
	}
	
	public void addUserToGroup(String user, String group) throws SQLException {
		statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO usergroup(user_id,group_id) VALUES "
				+ "(SELECT id FROM users WHERE username='" + user + "',SELECT id FROM groups WHERE group='" + group + "')");
	}
	
	public boolean checkUserPassword(String user, String password) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT password=PASSWORD(CONCAT(PASSWORD('" + password + "'),salt)) FROM "
				+ "users WHERE username='" + user + "'");
		return check(resultSet.getString(1));
	}
	
	public boolean checkIfUserHavePermissionToRead(String user, String filename) throws SQLException {
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT group_read OR (user_read AND owner_id=" + userID + ") FROM "
				+ "files AS f WHERE f.filename='" + filename + "' AND f.group_id IN (SELECT group_id FROM "
				+ "usergroup WHERE user_id=" + userID + ")");
		return check(resultSet.getString(1));
		
	}
	
	public boolean checkIfUserHavePermissionToWrite(String user, String filename) throws SQLException {
		statement = connection.createStatement();
		int userID = getUserID(user);
		resultSet = statement.executeQuery("SELECT group_write OR (user_write AND owner_id=" + userID + ") FROM "
				+ "files AS f WHERE f.filename='" + filename + "' AND f.group_id IN "
				+ "(SELECT group_id FROM usergroup WHERE user_id=" + userID + ")");
		return check(resultSet.getString(1));
	}
	
	private boolean check(String result) {
		if (Integer.parseInt(result) == 1) return true;
		else return false;
	}
	
	private int getUserID(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `id` FROM `users` WHERE `username`='" + username +"'");
		return Integer.parseInt(resultSet.getString(1));
	}
	
	private int getGroupIDOfUser(String username) throws SQLException {
		int userID = getUserID(username);
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT `group_id` FROM `usergroup` WHERE `user_id`=" + userID);
		return Integer.parseInt(resultSet.getString(1));
	}
	
	public ArrayList<String> getAllUsernames() throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT username FROM users");
		ArrayList<String> usernames = new ArrayList<String>();
		while (resultSet.next()) {
			usernames.add(resultSet.getString(1));
		}
		return usernames;
	}
	
	public String[] getInformationAboutUser(String username) throws SQLException {
		statement = connection.createStatement();
		resultSet = statement.executeQuery("SELECT * FROM users");
		if (resultSet.isBeforeFirst()) {
			String[] informations = new String[3];
			informations[0] = resultSet.getString(1);
			informations[1] = resultSet.getString(2);
			int groupID = getGroupIDOfUser(username);
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SELECT `group` FROM `groups` WHERE `id`=" + groupID);
			informations[2] = resultSet.getString(1);
			return informations;
		}
		else {
			String[] error = new String[1];
			error[0] = "Nothing";
			return error;
		}
		
	}

}
