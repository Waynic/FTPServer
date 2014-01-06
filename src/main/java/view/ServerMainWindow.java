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

/**
 * @author Jakub Fortunka
 *
 */
public class ServerMainWindow {

	private JFrame frmFtpServer;
	private JTextField textField;
	JLabel lblServerStatus;
	
	private Server server = null;
	
	private DefaultListModel<String> listModel;
	private JList<String> list;
	
	private DB database;
	
	private ArrayList<String> usernames;
	
	private NewUserDialog userDialog = null;

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
		textField.setText("9000");
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
		
		JLabel lblDatabase = new JLabel("database");
		usersList.add(lblDatabase);
		
		JPanel databaseButtons = new JPanel();
		usersList.add(databaseButtons);
		
		JButton btnAddUser = new JButton("add user");
		btnAddUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					addUser();
				} catch (SQLException e) {
					throwException(e);
				}
				
			}
		});
		databaseButtons.add(btnAddUser);
		
		/*JButton btnRemoveUser = new JButton("remove user");
		btnRemoveUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeUser();
			}
		});
		databaseButtons.add(btnRemoveUser);*/
		
		list = new JList<String>();
		listModel = new DefaultListModel<String>();
		list.setModel(listModel);
		usersList.add(list);
		
		list.addMouseListener(new MouseAdapter() {
       	 public void mouseClicked(MouseEvent e) {
       	        if (e.getClickCount() == 2) {
       	        	if (list.getModel().getSize()==0) {
       	        		JOptionPane.showMessageDialog(frmFtpServer, "Lista jest pusta!");
       					return ;
       	        	}
       	        	else {
       	        		try {
							showDetailInformationAboutUser(list.getSelectedValue());
						} catch (SQLException e1) {
							throwException(e1);
						}
       	        	}
       	         }
       	    }
		});
		
		JPanel userDetails = new JPanel();
		userDetails.setBackground(new Color(224, 255, 255));
		frmFtpServer.getContentPane().add(userDetails, BorderLayout.CENTER);
		
		JPanel misc = new JPanel();
		frmFtpServer.getContentPane().add(misc, BorderLayout.SOUTH);
		
		try {
			database = new DB();
			usernames = database.getAllUsernames();
			initializeUsersList();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e2) {
			e2.printStackTrace();
		}
		
	}
	
	private void startServer() {
		try {
			server = new Server(Integer.parseInt(textField.getText()));
			new Thread(server).start();
		} catch (NumberFormatException e) {
			throwException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throwException(e);
		}
		lblServerStatus.setText("Server status: working!");
	}
	
	private void stopServer() {
		server.stop();
		lblServerStatus.setText("Server status: not working");
	}
	
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
			usernames.add(username);
			refreshUsersList();
		}
	}
	
	/*private void removeUser() {
		List<String> listOfUsersToDelete = list.getSelectedValuesList();
		for (String username : listOfUsersToDelete) {
			database.removeUser(username);
		}
	}*/
	
	private void showDetailInformationAboutUser(String user) throws SQLException {
		String[] info = database.getInformationAboutUser(user);
		if (!info[0].equals("Nothing")) {
			//do something
		}
	}
	
	private void initializeUsersList() {
		if (!usernames.isEmpty()) {
			for (String u : usernames) {
				listModel.addElement(u);
			}
		}
	}
	
	private void refreshUsersList() {
		listModel.addElement(usernames.get(usernames.size()-1));
	}

	private void throwException(Exception e) {
		JOptionPane.showMessageDialog(frmFtpServer,
				e.getMessage(),
				"Problem!",
				JOptionPane.WARNING_MESSAGE);
		return ;
	}
	
}
