package secureboxclient;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JPanel;

import domain.SBDirectory;

class Preferences {

	String ip, workspacePath, username, password;
	int port;
	javax.swing.JTextField workspaceField;
	javax.swing.JPanel prefsPanel;
	
	public Preferences(String ip, int port, String username, String password) {
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
		this.port = port;
		workspacePath = "workspace" + File.pathSeparator;
	}
	
	/*
	public void openPreferences(){
				
		
		// Create the labels and text fields.
		javax.swing.JLabel workspaceLabel = new javax.swing.JLabel("Workspace Path:   ", javax.swing.JLabel.RIGHT);
		workspaceField = new javax.swing.JTextField("");		
		
		prefsPanel = new javax.swing.JPanel(false);
		prefsPanel.setLayout(new javax.swing.BoxLayout(prefsPanel, javax.swing.BoxLayout.X_AXIS));
		
		javax.swing.JPanel namePanel = new javax.swing.JPanel(false);
		
		namePanel.setLayout(new java.awt.GridLayout(1, 2));

		namePanel.add(workspaceLabel);
		
		javax.swing.JPanel fieldPanel = new javax.swing.JPanel(false);
		fieldPanel.setAlignmentY(JPanel.BOTTOM_ALIGNMENT);
		
		fieldPanel.setLayout(new java.awt.GridLayout(1, 2));
		
		prefsPanel.add(namePanel);
		prefsPanel.add(fieldPanel);
		javax.swing.JButton jButtonBrowse = new javax.swing.JButton("Browse");
		jButtonBrowse.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonBrowseActionPerformed(evt);
			}
		});
		
		prefsPanel.add(jButtonBrowse);

		javax.swing.JOptionPane.showOptionDialog(null, prefsPanel,
				"Preferences",javax.swing.JOptionPane.OK_CANCEL_OPTION,
				javax.swing.JOptionPane.INFORMATION_MESSAGE,
				null, new String[]{"OK", "Cancel"}, new String[]{"OK"});
		
		this.workspacePath = workspaceField.getText();
		
	}
	*/
	/*
	private void jButtonBrowseActionPerformed(ActionEvent evt) {
		javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
		chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
		int returnVal = chooser.showOpenDialog(prefsPanel);
		if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
			workspaceField.setText(chooser.getSelectedFile().getAbsolutePath());
		}				
	}
	*/
}
