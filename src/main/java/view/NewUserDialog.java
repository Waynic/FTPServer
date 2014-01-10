/**
 * 
 */
package view;

import java.awt.Dialog;
import javax.swing.JDialog;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author Jakub Fortunka
 *
 */
public class NewUserDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5640810496596198772L;
	
	private JTextField usernameTextField;
	private JPasswordField passwordField;

	/**
	 * 
	 */
	public NewUserDialog(JFrame parentFrame) {
		super ( parentFrame, "New User", false );
		setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel inputPanel = new JPanel();
		getContentPane().add(inputPanel, BorderLayout.CENTER);
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
		
		JPanel usernamePanel = new JPanel();
		inputPanel.add(usernamePanel);
		
		JLabel lblUsername = new JLabel("Username:");
		usernamePanel.add(lblUsername);
		
		usernameTextField = new JTextField();
		usernamePanel.add(usernameTextField);
		usernameTextField.setColumns(10);
		
		JPanel passwordPanel = new JPanel();
		inputPanel.add(passwordPanel);
		
		JLabel lblPassword = new JLabel("Password:");
		passwordPanel.add(lblPassword);
		
		passwordField = new JPasswordField();
		passwordField.setColumns(10);
		passwordPanel.add(passwordField);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		JButton btnOk = new JButton("OK");
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			//	usernameTextField.setText("");
			//	passwordField.setText("");
				setVisible(false);
			}
		});
		buttonsPanel.add(btnOk);
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				usernameTextField.setText("-1");
				passwordField.setText("");
				setVisible(false);
			}
		});
		buttonsPanel.add(btnCancel);
	}
	
	@Override
	public void setVisible(boolean b) {
		if (!getUsername().equals("") && b) {
			usernameTextField.setText("");
			passwordField.setText("");
		}
		super.setVisible(b);
	}
	
	public String getUsername() {
		return usernameTextField.getText();
	}
	
	public String getPassword() {
		return String.copyValueOf(passwordField.getPassword());
	}

}
