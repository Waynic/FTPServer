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
	 * 
	 */
	private static final long serialVersionUID = 3931022955953008137L;
	
	private DB database = null;
	private JList<String> list = null;
	private DefaultListModel<String> groupListModel;
	private boolean clickedCancel = false;
	/**
	 * 
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
	
	public void generateGroupList(String username) throws SQLException {
		ArrayList<String> groups = database.getAllGroups();
		groupListModel = new DefaultListModel<String>();
		for (String g : groups) {
			groupListModel.addElement(g);
		}
	}
	
	public boolean clickedCancel() {
		return clickedCancel;
	}
	
	public List<String> getSelectedGroups() {
		return list.getSelectedValuesList();
	}

}
