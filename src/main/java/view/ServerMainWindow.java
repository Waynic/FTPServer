/**
 * 
 */
package view;

import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JList;

import java.awt.Color;

import javax.swing.JTextField;

import database.DB;
import ftp.Server;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Main class of program. Initialize graphical interface and all of the fancy stuff.
 * 
 * @author Jakub Fortunka
 *
 */
public class ServerMainWindow {

	/**
	 * main frame
	 */
	private JFrame frmFtpServer;
	/**
	 * text field in which is entered port number of server
	 */
	private JTextField textField;
	/**
	 * label for showing a server status
	 */
	private JLabel lblServerStatus;
	/**
	 * label showing user id
	 */
	private JLabel lblUserid;
	/**
	 * label showing username
	 */
	private JLabel lblUsername;
	/**
	 * list of groups
	 */
	private JList<String> groupList;
	
	/**
	 * main class for "server action"
	 */
	private Server server = null;
	
	/**
	 * list model for list of groups
	 */
	private DefaultListModel<String> userListModel;
	/**
	 * list of users in database
	 */
	private JList<String> userList;
	
	/**
	 * main class for connecting and managing database
	 * @see database.DB
	 */
	private DB database;
	
//	private ArrayList<String> usernames;
	
	/**
	 * @see view.NewUserDialog
	 */
	private NewUserDialog userDialog = null;
	/**
	 * @see view.AddToGroupDialog
	 */
	private AddToGroupDialog groupDialog = null;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerMainWindow window = new ServerMainWindow();
					window.frmFtpServer.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ServerMainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmFtpServer = new JFrame();
		frmFtpServer.setTitle("FTP Server");
		frmFtpServer.setBounds(100, 100, 709, 461);
		frmFtpServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmFtpServer.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel menu = new JPanel();
		frmFtpServer.getContentPane().add(menu, BorderLayout.NORTH);
		
		JLabel lblPort = new JLabel("Port:");
		menu.add(lblPort);
		
		textField = new JTextField();
		textField.setText("3021");
		menu.add(textField);
		textField.setColumns(10);
		
		JButton btnStartServer = new JButton("start server");
		btnStartServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startServer();
			}
		});
		menu.add(btnStartServer);
		
		JButton btnStopServer = new JButton("stop server");
		btnStopServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopServer();
			}
		});
		menu.add(btnStopServer);
		
		lblServerStatus = new JLabel("Server status: not working");
		menu.add(lblServerStatus);
		
		JPanel usersList = new JPanel();
		frmFtpServer.getContentPane().add(usersList, BorderLayout.WEST);
		usersList.setLayout(new BoxLayout(usersList, BoxLayout.Y_AXIS));
		
		JScrollPane listScrollPane = new JScrollPane();
		usersList.add(listScrollPane);
		
		JLabel lblDatabase = new JLabel("database");
		lblDatabase.setHorizontalAlignment(SwingConstants.CENTER);
		//usersList.add(lblDatabase);
		
		userList = new JList<String>();
		userListModel = new DefaultListModel<String>();
		userList.setModel(userListModel);
		//usersList.add(list);
		
		listScrollPane.setViewportView(userList);
		userList.setVisibleRowCount(10);
		
		listScrollPane.setColumnHeaderView(lblDatabase);
		
		userList.addMouseListener(new MouseAdapter() {
       	 public void mouseClicked(MouseEvent e) {
       	        if (e.getClickCount() == 2) {
       	        	if (userList.getModel().getSize()==0) {
       	        		JOptionPane.showMessageDialog(frmFtpServer, "Lista jest pusta!");
       					return ;
       	        	}
       	        	else {
       	        		try {
							showDetailInformationAboutUser(userList.getSelectedValue());
						} catch (SQLException e1) {
							throwException(e1);
						}
       	        	}
       	         }
       	    }
		});
		
		JPanel dbButtonsPanel = new JPanel();
		usersList.add(dbButtonsPanel);
		
		JButton btnAddUser = new JButton("add user");
		dbButtonsPanel.add(btnAddUser);
		
		JButton btnRemoveUser = new JButton("remove user");
		dbButtonsPanel.add(btnRemoveUser);
		btnRemoveUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					removeUser();
				} catch (SQLException e1) {
					throwException(e1);
				}
			}
		});
		btnAddUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					addUser();
				} catch (SQLException e) {
					throwException(e);
				}
				
			}
		});
		
		
		JPanel userDetails = new JPanel();
		userDetails.setBackground(new Color(224, 255, 255));
		frmFtpServer.getContentPane().add(userDetails, BorderLayout.CENTER);
		userDetails.setLayout(new BoxLayout(userDetails, BoxLayout.Y_AXIS));
		
		lblUserid = new JLabel("UserID:");
		userDetails.add(lblUserid);
		
		lblUsername = new JLabel("Username:");
		userDetails.add(lblUsername);
		
		groupList = new JList<String>();
		groupList.setVisibleRowCount(10);
		
		JScrollPane groupListScrollPane = new JScrollPane(groupList);

		userDetails.add(groupListScrollPane);
		
		JLabel lblGroups = new JLabel("Groups:");
	//	userDetails.add(lblGroups);
		
		
	//	groupListScrollPane.add(groupList);
		groupListScrollPane.setColumnHeaderView(lblGroups);
		groupListScrollPane.setViewportView(groupList);
		
		JButton btnAddGroups = new JButton("Add group(s)");
		btnAddGroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					showAddGroupDialog(userList.getSelectedValue());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		userDetails.add(btnAddGroups);
	//	userDetails.add(groupList);
		
		JPanel misc = new JPanel();
		frmFtpServer.getContentPane().add(misc, BorderLayout.SOUTH);
		
		try {
			database = new DB();
	//		usernames = database.getAllUsernames();
			initializeUsersList();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e2) {
			e2.printStackTrace();
			JOptionPane.showMessageDialog(frmFtpServer,
					"Database is not working!",
					"Problem!",
					JOptionPane.WARNING_MESSAGE);
			return ;
		}
		
	}
	
	/**
	 * starts the server
	 */
	private void startServer() {
		try {
			if (database == null) {
				throw new IOException("Can't work without Database!");
			}
			else {
				server = new Server(Integer.parseInt(textField.getText()), database);
				new Thread(server).start();
				lblServerStatus.setText("Server status: working!");
			}
		} catch (NumberFormatException e) {
			throwException(e);
		} catch (IOException e) {
			throwException(e);
		}
	}
	
	/**
	 * stops the server
	 */
	private void stopServer() {
		if (server != null) {
			server.stop();
			lblServerStatus.setText("Server status: not working");
		}
	}
	
	/**
	 * method that manage adding user to database.
	 * Shows {@link view.NewUserDialog}, gets informations from it and uses {@link database.DB#addUser(String, String, int)} to add user to database
	 * 
	 * @throws SQLException
	 */
	private void addUser() throws SQLException {
		if (userDialog == null) {
			userDialog = new NewUserDialog(frmFtpServer);
		}
		userDialog.pack();
		userDialog.setSize(userDialog.getPreferredSize().width+100, userDialog.getPreferredSize().height);
		userDialog.setLocationRelativeTo(null);
		userDialog.setVisible(true);
		String username = userDialog.getUsername();
		String pass = userDialog.getPassword();
		if (!username.equals("-1")) {
			Random rndGen = new Random();
			int salt = rndGen.nextInt();
			database.addUser(username, pass, salt);
			//usernames.add(username);
			userListModel.addElement(username);
		//	refreshUsersList();
		}
	}
	
	/**
	 * Method that shows (creates if necessary) {@link view.AddToGroupDialog} and manages adding user to choosen groups. 
	 * 
	 * @param username name of user that will be added to groups
	 * @throws SQLException
	 */
	private void showAddGroupDialog(String username) throws SQLException {
		if (groupDialog == null) {
			groupDialog = new AddToGroupDialog(database, frmFtpServer, username);
		}
		groupDialog.pack();
		groupDialog.setSize(groupDialog.getPreferredSize().width+100, groupDialog.getPreferredSize().height);
		groupDialog.setLocationRelativeTo(null);
		groupDialog.generateGroupList();
		groupDialog.setVisible(true);
		if (!groupDialog.clickedCancel()) {
			List<String> groups = groupDialog.getSelectedGroups();
			for (String g : groups) {
				database.addUserToGroup(username, g);
			}
		}
		showDetailInformationAboutUser(username);
	}
	
	/**
	 * removes user from database using {@link database.DB#deleteUser(String)}
	 * 
	 * @throws SQLException
	 */
	private void removeUser() throws SQLException {
		List<String> listOfUsersToDelete = userList.getSelectedValuesList();
		int[] indexes = userList.getSelectedIndices();
		int i = 0;
		for (String username : listOfUsersToDelete) {
			database.deleteUser(username);
			//list.remove(indexes[i]);
			userListModel.remove(indexes[i]);
			i++;
		}
	}
	
	/**
	 * Shows information about user in another JPanel using {@link database.DB#getInformationAboutUser(String)}
	 * 
	 * @param user name of user about which informations we want to have
	 * @throws SQLException
	 */
	private void showDetailInformationAboutUser(String user) throws SQLException {
		ArrayList<String> info = database.getInformationAboutUser(user);
		if (info != null) {
			//TODO try to set fixed size for list (not important but...)
			lblUserid.setText("UserID: " + info.get(0));
			lblUsername.setText("Username: " + info.get(1));
			DefaultListModel<String> groupListModel = new DefaultListModel<String>();
			for (int i=2;i<info.size();i++) groupListModel.addElement(info.get(i));
			groupList.setModel(groupListModel);
		//	groupList.setSize(new Dimension(300,300));
		}
	}
	
	/**
	 * method that initialize list of users from database
	 * 
	 * @throws SQLException
	 */
	private void initializeUsersList() throws SQLException {
		for (String u : database.getAllUsernames()) {
			userListModel.addElement(u);
		}
	}

	/**
	 * shows exception message in JOptionPane
	 * 
	 * @param e exception to throw
	 */
	private void throwException(Exception e) {
		JOptionPane.showMessageDialog(frmFtpServer,
				e.getMessage(),
				"Problem!",
				JOptionPane.WARNING_MESSAGE);
		return ;
	}
	
}
