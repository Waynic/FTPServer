/**
 * 
 */
package view;

import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.BorderLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JList;

import java.awt.Color;

import javax.swing.JTextField;

import ftp.Server;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * @author Jakub Fortunka
 *
 */
public class ServerMainWindow {

	private JFrame frmFtpServer;
	private JTextField textField;
	JLabel lblServerStatus;
	
	private Server server = null;

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
		databaseButtons.add(btnAddUser);
		
		JButton btnRemoveUser = new JButton("remove user");
		databaseButtons.add(btnRemoveUser);
		
		JList list = new JList();
		usersList.add(list);
		
		JPanel userDetails = new JPanel();
		userDetails.setBackground(new Color(224, 255, 255));
		frmFtpServer.getContentPane().add(userDetails, BorderLayout.CENTER);
		
		JPanel misc = new JPanel();
		frmFtpServer.getContentPane().add(misc, BorderLayout.SOUTH);
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
	
	private void throwException(Exception e) {
		JOptionPane.showMessageDialog(frmFtpServer,
				e.getMessage(),
				"Problem!",
				JOptionPane.WARNING_MESSAGE);
		return ;
	}

}
