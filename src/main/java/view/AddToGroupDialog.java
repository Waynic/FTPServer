/**
 * 
 */
package view;

import java.awt.Dialog;

import javax.swing.JDialog;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JButton;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;
import javax.swing.JList;

import database.DB;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jakub Fortunka
 *
 */
public class AddToGroupDialog extends JDialog {

	/**
	 * needed to eventual serialization
	 */
	private static final long serialVersionUID = 3931022955953008137L;
	
	/**
	 * object of {@link database.DB} to use some mehods from it
	 */
	private DB database = null;
	/**
	 * list of groups in database
	 */
	private JList<String> list = null;
	/**
	 * List model for group list
	 */
	private DefaultListModel<String> groupListModel;
	/**
	 * true if user clicked cancel; false otherwise
	 */
	private boolean clickedCancel = false;

	/**
	 * Constructor
	 * 
	 * @param database to connect with database
	 * @param parentFrame frame of invoker of this JDialog
	 * @param username name of user which will be added to groups
	 */
	public AddToGroupDialog(DB database, JFrame parentFrame, String username) {
		super ( parentFrame, "Add user to group(s)", false );
		setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.database = database;
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.9);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		getContentPane().add(splitPane);
		
		JPanel groupPanel = new JPanel();
		splitPane.setLeftComponent(groupPanel);
		groupPanel.setLayout(new BorderLayout(0, 0));
		
		JLabel lblGroups = new JLabel("<html>Add " + username + " to groups: <br> (just select these to which you want this user to belong; there is multiple selection)</html>");
		lblGroups.setHorizontalAlignment(SwingConstants.CENTER);
		groupPanel.add(lblGroups, BorderLayout.NORTH);
		
		list = new JList<String>();
		JScrollPane groupsScrollPane = new JScrollPane(list);
		groupPanel.add(groupsScrollPane, BorderLayout.CENTER);		
		
		//groupPanel.add(list, BorderLayout.SOUTH);
		
		JPanel buttonsPanel = new JPanel();
		splitPane.setRightComponent(buttonsPanel);
		
		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		buttonsPanel.add(btnOk);
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clickedCancel = true;
				setVisible(false);
			}
		});
		buttonsPanel.add(btnCancel);
	}
	
	@Override
	public void setVisible(boolean b) {
		if (b) list.setModel(groupListModel);
		if (clickedCancel) clickedCancel=false;
		super.setVisible(b);
	}
	
	/**
	 * method that generates list of groups from database
	 * 
	 * @throws SQLException
	 */
	public void generateGroupList() throws SQLException {
		ArrayList<String> groups = database.getAllGroups();
		groupListModel = new DefaultListModel<String>();
		for (String g : groups) {
			groupListModel.addElement(g);
		}
	}
	
	/**
	 * returns state of {@link view.AddToGroupDialog#clickedCancel}
	 * 
	 * @return true if user clicked cancel button; false otherwise
	 */
	public boolean clickedCancel() {
		return clickedCancel;
	}
	
	/**
	 * @return list of selected groups from list
	 */
	public List<String> getSelectedGroups() {
		return list.getSelectedValuesList();
	}

}
