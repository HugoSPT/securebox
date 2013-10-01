package secureboxclient;

import java.io.File;

class Login {
	
	String username, password, ip;
	int port;
	
	// modal dialog to get user ID and password
	private String[] ConnectOptionNames = { "Login", "Cancel" };
	private String   ConnectTitle = "SecureBox connection";

	Login() {
		getUsernamePassword();
	}
	
	void getUsernamePassword() {
		javax.swing.JPanel connectionPanel;

		javax.swing.JLabel ipLabel = new javax.swing.JLabel("IP:   ", javax.swing.JLabel.RIGHT);
		javax.swing.JTextField ipField = new javax.swing.JTextField("127.0.0.1");
		javax.swing.JLabel portLabel = new javax.swing.JLabel("Port:   ", javax.swing.JLabel.RIGHT);
		javax.swing.JTextField portField = new javax.swing.JTextField("5664");
		
		// Create the labels and text fields.
		javax.swing.JLabel userNameLabel = new javax.swing.JLabel("Username:   ", javax.swing.JLabel.RIGHT);
		javax.swing.JTextField userNameField = new javax.swing.JTextField("Hugo");
		javax.swing.JLabel passwordLabel = new javax.swing.JLabel("Password:   ", javax.swing.JLabel.RIGHT);
		javax.swing.JTextField passwordField = new javax.swing.JPasswordField("");
		
		connectionPanel = new javax.swing.JPanel(false);
		connectionPanel.setLayout(new javax.swing.BoxLayout(connectionPanel, javax.swing.BoxLayout.X_AXIS));
		
		javax.swing.JPanel namePanel = new javax.swing.JPanel(false);
		
		namePanel.setLayout(new java.awt.GridLayout(0, 1));
		namePanel.add(ipLabel);
		namePanel.add(portLabel);
		namePanel.add(userNameLabel);
		namePanel.add(passwordLabel);
		
		javax.swing.JPanel fieldPanel = new javax.swing.JPanel(false);
		
		fieldPanel.setLayout(new java.awt.GridLayout(0, 1));
		fieldPanel.add(ipField);
		fieldPanel.add(portField);
		fieldPanel.add(userNameField);
		fieldPanel.add(passwordField);
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);

		// Connect or quit
		if(javax.swing.JOptionPane.showOptionDialog(null, connectionPanel,
				ConnectTitle,javax.swing.JOptionPane.OK_CANCEL_OPTION,
				javax.swing.JOptionPane.INFORMATION_MESSAGE,
				null, ConnectOptionNames, ConnectOptionNames[0]) != 0)
					System.exit(0);
		
		
		ip = ipField.getText();
		port = Integer.parseInt(portField.getText());
		username = userNameField.getText();
		password = passwordField.getText();
		
	}
}
