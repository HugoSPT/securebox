/*
 * Cliente SecureBox - Sincronizacao e partilha
 *
 * SBClient.java
 *
 * Created on 9/Fev/2011
 */

package secureboxclient;

import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import domain.*;
import events.*;

/**
 *
 * @author Mario Calha e Dulce Domingos
 */
public class SBClient extends javax.swing.JApplet {

	Preferences prefs;
	CommunicationController cc;
	SBDirectory userDir;
	LinkedList<String> users;
	boolean connected;
	ServerAnswerController sac;
	String current;	

	class UpdateThread extends Thread {

		UpdateThread() {
		}

		@Override
		public void run(){	
			while(true){
				try {
					sleep(1000*60*5);
				} catch (InterruptedException e) {
					System.out.println("Erro nos updates");
				}
				cc.writeObject(new UpdateEvent());

				ServerAnswerEvent sae = null;	

				while(sae == null){
					sae = sac.getAnswer(new UpdateEvent());
					try {
						sleep(10);
					} catch (InterruptedException e) {
						System.out.println("Erro nos updates");
					}
				}
				userDir = (SBDirectory) sae.getObject();

				fillLists();
			}
		}
	}


	/** Initializes the applet JAppletSBClient */
	public void init() {
		try {
			java.awt.EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					initComponents();
				}
			});
		} catch (Exception ex) {
			System.out.println("Erro ao iniciar a aplicacao");
		}

		resize(770,665);

		// Objectos que armazenam o conteúdo das listas do interface visual
		SyncListModel = new javax.swing.DefaultListModel();
		ShareListModel = new javax.swing.DefaultListModel();
		UserShareListModel = new javax.swing.DefaultListModel();

		ShareListModel.add(0, new Share(MySharesMarker));
		jListShareList.setModel(ShareListModel);

		Login login = new Login();

		File workspace = new File("workspace");
		if(!workspace.exists())
			workspace.mkdir();

		current = "workspace"+File.separator+"workspace_"+login.username;
		File dir = new File(current);

		if(!dir.exists())
			dir.mkdir();

		prefs = new Preferences(login.ip, login.port,login.username, login.password);
		verifyLogin(prefs.username, prefs.password);
	}

	private void fillLists(){


		ShareListModel.removeAllElements();

		boolean found = false;
		String homedir = current+File.separator;

		for(SBFile f : userDir.getSyncFiles()){
			if(!SyncListModel.contains(homedir+f.getName()))
				SyncListModel.add(SyncListModel.getSize(), homedir+f.getName());

			LinkedList <SBUser> sharedUsers = userDir.getShareUsers(f);

			found = false;
			for(int i = 0; i < ShareListModel.size(); i++)
				if(ShareListModel.getElementAt(i).equals(homedir+f.getName()))
					found = true;

			if(sharedUsers != null && !found){

				ShareListModel.add(ShareListModel.getSize(), new Share(homedir+f.getName()));

				for(SBUser u : sharedUsers)
					((Share)ShareListModel.elementAt(ShareListModel.getSize()-1)).UserShareListModel.add(
							((Share)ShareListModel.elementAt(ShareListModel.getSize()-1)).UserShareListModel.getSize(), 
							u.getName());
			}
		}

		found = false;
		for(int i = 0; i < ShareListModel.size(); i++)
			if(ShareListModel.getElementAt(i).equals(OtherSharesMarker))
				found = true;

		if(!found)
			ShareListModel.add(ShareListModel.getSize(), new Share(OtherSharesMarker));

		found = false;

		for(SBFile f : userDir.getOwner().getSharedFiles()){
			String name = f.getParent()+File.separator+f.getName();
			found = false;

			for(int i = 0; i < ShareListModel.size(); i++)
				if(ShareListModel.getElementAt(i).equals(name))
					found = true;
			if(!found){
				System.out.println("entrou other shares");
				ShareListModel.add(ShareListModel.getSize(), new Share(name));

			}
		}

		jListSyncList.setModel(SyncListModel);
		jListShareList.setModel(ShareListModel);
		//jListShareList.repaint();
	}

	private void verifyLogin(final String userId, final String passwd){
		//Background task para comunicação com o servidor
		@SuppressWarnings("rawtypes")
		javax.swing.SwingWorker worker = new javax.swing.SwingWorker<Boolean, Object>() {
			String errorMsg = null;
			public Boolean doInBackground() {
				Boolean serverAnswer = null;

				try {
					cc = new CommunicationController(new Socket (prefs.ip, prefs.port));
				} catch (UnknownHostException e) {
					jLabelStatus.setText("Failed to Connect to Server!");
				} catch (IOException e) {
					jLabelStatus.setText("Failed to Create Channels!");
				}

				LoginEvent login = new LoginEvent(new SBSession(userId, passwd));

				if(!cc.writeObject(login)){
					jLabelStatus.setText("Failed Login!");
					connected = false;
				}
				else{
					userDir = (SBDirectory) cc.readObject();


					sac = new ServerAnswerController(cc,current);		
					sac.start();
					
					UpdateThread ut = new UpdateThread();
					ut.start();

					sendEvent(new GetUsersEvent());
					fillLists();
					serverAnswer= (userDir != null);
					connected = true;
				}

				return serverAnswer;
			}
			@Override
			public void done() {
				try {
					if(get()==true)
						jLabelStatus.setText("Login Ok!");
					else if(errorMsg==null)
						jLabelStatus.setText("Login Failed! Check credentials.");
					else
						jLabelStatus.setText(errorMsg);
				} catch (Exception ignore) {
				}
			}

		};
		worker.execute();
	}

	private void sendEvent(final Event event){
		@SuppressWarnings("rawtypes")
		javax.swing.SwingWorker worker = new javax.swing.SwingWorker<Boolean, Object>() {
			String errorMsg = null;
			@SuppressWarnings({ "unchecked", "null" })
			public Boolean doInBackground() {
				Boolean serverAnswer = null;

				if(cc.writeObject(event)){

					System.out.println("Cliente - Enviou: "+event.getClass());

					if(event instanceof AddSyncFileEvent){
						ServerAnswerEvent sae = null;	

						while(sae == null)
							sae = sac.getAnswer(new AddSyncFileEvent(null));

						SBFile file = (SBFile) sae.getObject();

						if(file != null)
							sendFileData(((AddSyncFileEvent) event).getFile(), file);

						if(serverAnswer = (file != null)){
							userDir.insertSyncFile(file);
							SyncListModel.add(SyncListModel.getSize(), current+File.separator+file.getName());
							jListSyncList.setModel(SyncListModel);
							errorMsg = "Success Add Sync Up";
						}
						else
							errorMsg = "Failed To Add File";
					}
					else if(event instanceof GetUsersEvent){
						ServerAnswerEvent sae = null;

						while(sae == null)
							sae = sac.getAnswer(new GetUsersEvent());


						users = (LinkedList<String>) sae.getObject();

						jComboBoxUsers.setModel(new javax.swing.DefaultComboBoxModel(users.toArray()));
						if(!(serverAnswer = (users != null)))
							errorMsg = "Failed to Get Users";
						errorMsg = "Success Get Users";
					}
					else if(event instanceof SyncEvent){
						ServerAnswerEvent sae = null;

						while(sae == null)
							sae = sac.getAnswer(new SyncEvent());

						userDir = (SBDirectory) sae.getObject();
						if(userDir == null){
							errorMsg = "Failed to Sync";
							return false;
						}

						fillLists();
						errorMsg = "Success Sync";
						return true;
					}
					else if(event instanceof GetShareListEvent){
						ServerAnswerEvent sae = null;

						while(sae == null){
							sae = sac.getAnswer(new GetShareListEvent());
						}

						userDir = (SBDirectory) sae.getObject();

						if(userDir == null){
							errorMsg = "Failed to Get Shared";
							return false;
						}

						fillLists();
						errorMsg = "Success Get Shared";
						return true;
					}
					else if(event instanceof GetSyncEvent){

						ServerAnswerEvent sae = null;

						long startTime = System.currentTimeMillis();
						while(sae == null)
							if((System.currentTimeMillis()-startTime) > 20){
								sae = sac.getAnswer(new GetSyncEvent());
								startTime = System.currentTimeMillis();
							}


						LinkedList<SyncDownFileEvent> sdfes = new LinkedList<SyncDownFileEvent>();

						while(sae.getObject() != null){

							sdfes.add(new SyncDownFileEvent(((SBFile)((FileEvent) sae.getObject()).getFile())));
							sae = null;							

							while(sae == null)
								if((System.currentTimeMillis()-startTime) > 20){
									sae = sac.getAnswer(new GetSyncEvent());
									startTime = System.currentTimeMillis();
								}


						}


						for(SyncDownFileEvent sdfe : sdfes)
							sendEvent(sdfe);

						errorMsg = "Get Syncs";
					}
					else if(event instanceof SyncUpFileEvent){
						ServerAnswerEvent sae = null;

						while(sae == null)
							sae = sac.getAnswer(new SyncUpFileEvent(null,0));

						if((Boolean)sae.getObject()){

							SBFile file = ((SyncUpFileEvent) event).getFile();

							sendFileData(new SBFile(current+File.separator+file.getName()), file);

							if((serverAnswer = (Boolean) sae.getObject()))
								errorMsg = "Success Sync Up";
							else
								errorMsg = "Failed To Sync Up File";
						}	
						else
							errorMsg = "More Recent File On Server";
					}else if (event instanceof SyncDownFileEvent)
						errorMsg = "Success Sync Down";


				}
				else{
					connected = false;
					serverAnswer = false;
					errorMsg = "Disconnected!";
					//jLabelStatus.setText("Desligado!");
				}

				return serverAnswer;
			}

			@Override
			public void done() {
				try {					
					if(get()==true)
						jLabelStatus.setText(errorMsg);
					else if(errorMsg==null)
						jLabelStatus.setText("An error Occurred!");
					else
						jLabelStatus.setText(errorMsg);
				} catch (Exception ignore) {}
			}

		};

		worker.execute();
	}

	private void sendFileData(SBFile original, SBFile file) {
		BufferedInputStream inBuf;
		BufferedOutputStream outBuf;
		byte data[] = new byte[SBFileData.MAX_SIZE];
		try{

			inBuf = new BufferedInputStream(new FileInputStream(original.getAbsolutePath()));
			outBuf = new BufferedOutputStream(new FileOutputStream(current+File.separator+file.getName()));
			int toRead = 0;
			int read = 0;

			ServerAnswerEvent sae = null;

			while((toRead = inBuf.read(data, 0, SBFileData.MAX_SIZE)) > -1){
				SBFileData fileData = new SBFileData();
				fileData.setData(data, (read + toRead) >= original.length(), toRead);

				
				cc.writeObject(new FileDataEvent(file, fileData));
				outBuf.write(data, 0, toRead);
				
				
				while(sae == null)
					sae = sac.getAnswer(new FileDataEvent(null,null));

				read += toRead;
			}

			outBuf.close();
			inBuf.close();
		} catch (FileNotFoundException e) {
			//nao foi possivel ler o ficheiro
			System.out.println("Ficheiro n�o encontrado");
		} catch (IOException e) {
			//erro ao ler o ficheiro
			System.out.println("Erro ao ler o ficheiro");
		}

	}

	/** This method is called from within the init() method to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents() {

		jPanel1 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jScrollPane1 = new javax.swing.JScrollPane();
		jListSyncList = new javax.swing.JList();
		jLabel3 = new javax.swing.JLabel();
		jLabelSyncSize = new javax.swing.JLabel();
		jPanel2 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		jListShareList = new javax.swing.JList();
		jPanel4 = new javax.swing.JPanel();
		jScrollPane3 = new javax.swing.JScrollPane();
		jListUserShareList = new javax.swing.JList();
		jLabel4 = new javax.swing.JLabel();
		jLabelNumShares = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jLabelNumUsers = new javax.swing.JLabel();
		jPanel6 = new javax.swing.JPanel();
		jTextFieldFile = new javax.swing.JTextField();
		jButtonAddSync = new javax.swing.JButton();
		jButtonDelSync = new javax.swing.JButton();
		jButtonAddShare = new javax.swing.JButton();
		jButtonDelShare = new javax.swing.JButton();
		jButtonBrowse = new javax.swing.JButton();
		jLabel1 = new javax.swing.JLabel();
		jLabelStatus = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jSeparator5 = new javax.swing.JSeparator();
		jLabel5 = new javax.swing.JLabel();
		jComboBoxUsers = new javax.swing.JComboBox();
		jButtonAddUser = new javax.swing.JButton();
		jButtonDelUser = new javax.swing.JButton();
		jMenuBar1 = new javax.swing.JMenuBar();
		jMenuSync = new javax.swing.JMenu();
		jMenuGetSyncs = new javax.swing.JMenu();
		jMenuSyncUp = new javax.swing.JMenu();
		jMenuSyncDown = new javax.swing.JMenu();
		jMenuGetUsers = new javax.swing.JMenu();
		jMenuShare = new javax.swing.JMenu();

		getContentPane().setLayout(null);

		jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Syncs"));
		jPanel1.setPreferredSize(new java.awt.Dimension(252, 500));

		jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Sync List"));
		jPanel5.setPreferredSize(new java.awt.Dimension(230, 440));

		jListSyncList.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				jListSyncListMouseClicked(evt);
			}
		});

		jScrollPane1.setViewportView(jListSyncList);

		javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
		jPanel5.setLayout(jPanel5Layout);
		jPanel5Layout.setHorizontalGroup(
				jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
		);
		jPanel5Layout.setVerticalGroup(
				jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
		);

		jLabel3.setText("Sync Size (bytes):");

		jLabelSyncSize.setText("0");

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout.setHorizontalGroup(
				jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup()
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGroup(jPanel1Layout.createSequentialGroup()
										.addContainerGap()
										.addComponent(jLabel3)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jLabelSyncSize)))
										.addContainerGap())
		);
		jPanel1Layout.setVerticalGroup(
				jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup()
						.addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel3)
								.addComponent(jLabelSyncSize))
								.addContainerGap())
		);

		getContentPane().add(jPanel1);
		jPanel1.setBounds(5, 5, 252, 500);

		jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Shares"));
		jPanel2.setPreferredSize(new java.awt.Dimension(500, 500));

		jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Share List"));
		jPanel3.setPreferredSize(new java.awt.Dimension(230, 440));

		jListShareList.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				if(evt != null)
					jListShareListMouseClicked(evt);
			}
		});
		jScrollPane2.setViewportView(jListShareList);

		javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
		jPanel3.setLayout(jPanel3Layout);
		jPanel3Layout.setHorizontalGroup(
				jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
		);
		jPanel3Layout.setVerticalGroup(
				jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
		);

		jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("User Share List"));
		jPanel4.setPreferredSize(new java.awt.Dimension(230, 440));

		jScrollPane3.setViewportView(jListUserShareList);

		javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
		jPanel4.setLayout(jPanel4Layout);
		jPanel4Layout.setHorizontalGroup(
				jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
		);
		jPanel4Layout.setVerticalGroup(
				jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
		);

		jLabel4.setText("Num Shares:");

		jLabelNumShares.setText("0");

		jLabel6.setText("Num Users:");

		jLabelNumUsers.setText("0");

		javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
		jPanel2.setLayout(jPanel2Layout);
		jPanel2Layout.setHorizontalGroup(
				jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel2Layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGroup(jPanel2Layout.createSequentialGroup()
										.addGap(10, 10, 10)
										.addComponent(jLabel4)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jLabelNumShares)))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addGroup(jPanel2Layout.createSequentialGroup()
														.addGap(10, 10, 10)
														.addComponent(jLabel6)
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addComponent(jLabelNumUsers))
														.addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE))
														.addContainerGap())
		);
		jPanel2Layout.setVerticalGroup(
				jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel2Layout.createSequentialGroup()
						.addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel2Layout.createSequentialGroup()
										.addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel4)
												.addComponent(jLabelNumShares)))
												.addGroup(jPanel2Layout.createSequentialGroup()
														.addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 440, javax.swing.GroupLayout.PREFERRED_SIZE)
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																.addComponent(jLabel6)
																.addComponent(jLabelNumUsers))))
																.addGap(13, 13, 13))
		);

		getContentPane().add(jPanel2);
		jPanel2.setBounds(262, 5, 500, 500);

		jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Operations"));
		jPanel6.setMinimumSize(new java.awt.Dimension(756, 100));
		jPanel6.setPreferredSize(new java.awt.Dimension(756, 115));

		jButtonAddSync.setText("Add Sync");
		jButtonAddSync.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonAddSyncActionPerformed(evt);
			}
		});

		jButtonDelSync.setText("Del Sync");
		jButtonDelSync.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonDelSyncActionPerformed(evt);
			}
		});

		jButtonAddShare.setText("Add Share");
		jButtonAddShare.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonAddShareActionPerformed(evt);
			}
		});

		jButtonDelShare.setText("Del Share");
		jButtonDelShare.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonDelShareActionPerformed(evt);
			}
		});

		jButtonBrowse.setText("Browse ...");
		jButtonBrowse.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonBrowseActionPerformed(evt);
			}
		});

		jLabel1.setText("Status:");

		jLabelStatus.setText("   ");

		jLabel2.setText("File:");

		jLabel5.setText("User:");

		//jComboBoxUsers.setModel(new javax.swing.DefaultComboBoxModel(new String[]{""}));

		jButtonAddUser.setText("Add User");
		jButtonAddUser.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonAddUserActionPerformed(evt);
			}
		});

		jButtonDelUser.setText("Del User");
		jButtonDelUser.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonDelUserActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
		jPanel6.setLayout(jPanel6Layout);
		jPanel6Layout.setHorizontalGroup(
				jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel6Layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jSeparator5, javax.swing.GroupLayout.DEFAULT_SIZE, 724, Short.MAX_VALUE)
								.addGroup(jPanel6Layout.createSequentialGroup()
										.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
												.addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
														.addGap(21, 21, 21)
														.addComponent(jButtonAddSync)
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
														.addComponent(jButtonDelSync)
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
														.addComponent(jButtonAddShare)
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
														.addComponent(jButtonDelShare)
														.addGap(12, 12, 12))
														.addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
																.addComponent(jLabel2)
																.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addComponent(jTextFieldFile, javax.swing.GroupLayout.PREFERRED_SIZE, 357, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addComponent(jButtonBrowse)))
																.addGap(31, 31, 31)
																.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																		.addGroup(jPanel6Layout.createSequentialGroup()
																				.addComponent(jLabel5)
																				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																				.addComponent(jComboBoxUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE))
																				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
																						.addComponent(jButtonAddUser)
																						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																						.addComponent(jButtonDelUser)
																						.addGap(16, 16, 16))))
																						.addGroup(jPanel6Layout.createSequentialGroup()
																								.addComponent(jLabel1)
																								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																								.addComponent(jLabelStatus)))
																								.addContainerGap())
		);
		jPanel6Layout.setVerticalGroup(
				jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel6Layout.createSequentialGroup()
						.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jTextFieldFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jButtonBrowse)
								.addComponent(jLabel2)
								.addComponent(jLabel5)
								.addComponent(jComboBoxUsers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jButtonAddSync)
										.addComponent(jButtonDelSync)
										.addComponent(jButtonDelShare)
										.addComponent(jButtonAddShare)
										.addComponent(jButtonAddUser)
										.addComponent(jButtonDelUser))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabelStatus)
												.addComponent(jLabel1))
												.addContainerGap())
		);

		getContentPane().add(jPanel6);
		jPanel6.setBounds(6, 510, 756, 130);

		jMenuSync.setText("Get Sync List");
		jMenuSync.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				sendEvent(new SyncEvent());
			}
		});

		javax.swing.JMenu menu = new javax.swing.JMenu("Options");

		/*
		jMenuSetPath.setText("Set WorkSpace Path");
		jMenuSetPath.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				prefs.openPreferences();
			}
		});
		 */
		//jMenuBar1.add(jMenuSetPath);

		javax.swing.JMenuItem connectMenu = new javax.swing.JMenuItem("Connect");
		connectMenu.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				if(!connected){
					Login login = new Login();
					prefs = new Preferences(login.ip, login.port,login.username, login.password);
					verifyLogin(prefs.username, prefs.password);
				}
			}
		});

		menu.add(connectMenu);

		javax.swing.JMenuItem disconnectMenu = new javax.swing.JMenuItem("Disconnect");
		disconnectMenu.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				if(connected){
					connected = false;
					cc.closeConnection();
				}
			}
		});

		menu.add(disconnectMenu);

		jMenuBar1.add(menu);

		jMenuBar1.add(jMenuSync);

		jMenuGetSyncs.setText("Get Syncs");
		jMenuGetSyncs.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				sendEvent(new GetSyncEvent());
			}
		});
		jMenuBar1.add(jMenuGetSyncs);

		jMenuShare.setText("Get Share List");
		jMenuShare.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				sendEvent(new GetShareListEvent());
			}
		});
		jMenuBar1.add(jMenuShare);

		jMenuSyncUp.setText("Sync Up");
		jMenuSyncUp.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				for(int i=0; i<SyncListModel.getSize(); i++)
					if(SyncListModel.elementAt(i).equals(jTextFieldFile.getText())){
						File file = new File(jTextFieldFile.getText());
						SBFile sbf = null;

						for(SBFile f : userDir.getSyncFiles())
							if(f.getName().equals(file.getName())){
								sbf = f;
								break;
							}
						File cFile = new File(current+File.separator+file.getName());
						
						if(cFile.exists())
							sendEvent(new SyncUpFileEvent(sbf, cFile.lastModified()));
					}
			}
		});
		jMenuBar1.add(jMenuSyncUp);

		jMenuSyncDown.setText("Sync Down");
		jMenuSyncDown.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				//for(int i=0; i<SyncListModel.getSize(); i++)
				//if(SyncListModel.elementAt(i).equals(jTextFieldFile.getText())){
				File file = new File(jTextFieldFile.getText());
				SBFile sbf = null;

				for(SBFile f : userDir.getSyncFiles())
					if(f.getName().equals(file.getName())){
						sbf = f;
						break;
					}

				if(sbf == null)
					for(SBFile f : userDir.getOwner().getSharedFiles())
						if(f.getName().equals(file.getName())){
							sbf = f;
							break;
						}

				sendEvent(new SyncDownFileEvent(sbf));
				//}
		}
		});
		jMenuBar1.add(jMenuSyncDown);

		jMenuGetUsers.setText("Get Users");
		jMenuGetUsers.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				sendEvent(new GetUsersEvent());
			}
		});
		jMenuBar1.add(jMenuGetUsers);

		/*
		jMenuSetPath.setText("Set WorkSpace Path");
		jMenuSetPath.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(MouseEvent e){
				prefs.openPreferences();
			}
		});
		//jMenuBar1.add(jMenuSetPath);
		 */
		setJMenuBar(jMenuBar1);
	}// </editor-fold>//GEN-END:initComponents

	private void jListShareListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListShareListMouseClicked
		if(jListShareList.getSelectedValue().equals(MySharesMarker) ||
				((Share)jListShareList.getSelectedValue()).Share.startsWith(OtherSharesMarker)) {
			jListUserShareList.setModel(UserShareListModel);
			jTextFieldFile.setText("");
			return;
		}
		jTextFieldFile.setText(jListShareList.getSelectedValue().toString());
		jListUserShareList.setModel(((Share)jListShareList.getSelectedValue()).UserShareListModel);
	}//GEN-LAST:event_jListShareListMouseClicked

	private void jListSyncListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListSyncListMouseClicked
		jTextFieldFile.setText(jListSyncList.getSelectedValue().toString());
	}//GEN-LAST:event_jListSyncListMouseClicked

	private void jButtonBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonBrowseActionPerformed
		// Selecionar ficheiro
		javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
		chooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
		int returnVal = chooser.showOpenDialog(this);
		if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
			jTextFieldFile.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}//GEN-LAST:event_jButtonBrowseActionPerformed

	private void jButtonAddSyncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddSyncActionPerformed

		String initFile[] = jTextFieldFile.getText().replace(File.separator, "/").split("/");

		// Procurar elemento na lista de syncs
		for(int i=0; i<SyncListModel.getSize(); i++) {
			if(SyncListModel.elementAt(i).equals(current+File.separator+initFile[initFile.length-1])) {
				// Elemento encontrado
				// Nao adicionar
				jLabelStatus.setText("Allready Sync");
				return;
			}
		}
		// Adicionar ficheiro à lista de sincronização

		//NEW

		SBFile file = new SBFile(jTextFieldFile.getText());
		
		//file.setClientPath(file.getAbsolutePath());
		sendEvent(new AddSyncFileEvent(file));

		//END NEW

	}//GEN-LAST:event_jButtonAddSyncActionPerformed


	private void jButtonDelSyncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDelSyncActionPerformed
		// Procurar elemento na lista de shares
		for(int i=0; i<ShareListModel.getSize(); i++) {
			if(ShareListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Retirar elemento
				ShareListModel.remove(i);
				jListShareList.setModel(ShareListModel);
				break;
			}
		}
		// Procurar elemento na lista de syncs
		for(int i=0; i<SyncListModel.getSize(); i++) {
			if(SyncListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Retirar elemento
				SyncListModel.remove(i);
				jListSyncList.setModel(SyncListModel);

				//NEW

				File file = new File(jTextFieldFile.getText());
				SBFile sbf = null;

				for(SBFile f : userDir.getSyncFiles())
					if(f.getName().equals(file.getName())){
						sbf = f;
						break;
					}

				sendEvent(new RemoveSyncFileEvent(sbf));

				//END NEW

				break;
			}
		}
	}//GEN-LAST:event_jButtonDelSyncActionPerformed

	private void jButtonAddShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddShareActionPerformed
		// Procurar elemento na lista de shares
		for(int i=0; i<ShareListModel.getSize(); i++) {
			if(ShareListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Não adicionar
				return;
			}
		}
		// Procurar elemento na lista de syncs
		for(int i=0; i<SyncListModel.getSize(); i++) {
			if(SyncListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Adicionar ficheiro à lista de partilha


				ShareListModel.add(1, new Share(jTextFieldFile.getText()));
				jListShareList.setModel(ShareListModel);


				//NEW
				File file = new File(jTextFieldFile.getText());
				SBFile sbf = null;

				for(SBFile f : userDir.getSyncFiles())
					if(f.getName().equals(file.getName())){
						sbf = f;
						break;
					}

				sendEvent(new AddShareFileEvent(sbf));
				userDir.insertShareFile(sbf);

				//END NEW

				break;
			}
		}
	}//GEN-LAST:event_jButtonAddShareActionPerformed

	private void jButtonDelShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDelShareActionPerformed


		if(jTextFieldFile.getText().startsWith("root")){
			jLabelStatus.setText("Not Your Share");
			return;
		}

		// Procurar elemento na lista de shares
		for(int i=0; i<ShareListModel.getSize(); i++) {
			if(ShareListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Retirar elemento
				ShareListModel.remove(i);

				//NEW
				File file = new File(jTextFieldFile.getText());
				SBFile sbf = null;

				for(SBFile f : userDir.getSyncFiles())
					if(f.getName().equals(file.getName())){
						sbf = f;
						break;
					}	

				sendEvent(new RemoveShareFileEvent(sbf));
				//END NEW

				jListShareList.setModel(ShareListModel);
				jListUserShareList.setModel(UserShareListModel);
				break;
			}
		}
	}//GEN-LAST:event_jButtonDelShareActionPerformed

	private void jButtonAddUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddUserActionPerformed
		// Procurar elemento na lista de shares


		if(jTextFieldFile.getText().startsWith("root")){
			jLabelStatus.setText("Can't Share This");
			return;
		}

		for(int i=0; i<ShareListModel.getSize(); i++) {
			if(ShareListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Procurar utilizador na lista de usershares
				for(int j=0; j<((Share)ShareListModel.elementAt(i)).UserShareListModel.getSize(); j++) {
					if(((Share)ShareListModel.elementAt(i)).UserShareListModel.elementAt(j).equals(jComboBoxUsers.getSelectedItem())) {
						// Elemento encontrado
						// Não adicionar
						return;
					}
				}
				// Adicionar utilizador à partilha
				((Share)ShareListModel.elementAt(i)).UserShareListModel.add(
						((Share)ShareListModel.elementAt(i)).UserShareListModel.getSize(), jComboBoxUsers.getSelectedItem());

				//NEW
				SBFile file = null;
				for(SBFile f : userDir.getSyncFiles())
					if((current+File.separator+f.getName()).equals(((Share)ShareListModel.elementAt(i)).Share))
						file = f;

				sendEvent(new AddUserEvent(new SBUser((String)jComboBoxUsers.getSelectedItem()), file));
				//END NEW

				jListUserShareList.setModel(((Share)ShareListModel.elementAt(i)).UserShareListModel);
				break;
			}
		}
	}//GEN-LAST:event_jButtonAddUserActionPerformed

	private void jButtonDelUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDelUserActionPerformed
		// Procurar elemento na lista de shares
		for(int i=0; i<ShareListModel.getSize(); i++) {
			if(ShareListModel.elementAt(i).equals(jTextFieldFile.getText())) {
				// Elemento encontrado
				// Procurar utilizador na lista de usershares
				for(int j=0; j<((Share)ShareListModel.elementAt(i)).UserShareListModel.getSize(); j++) {
					if(((Share)ShareListModel.elementAt(i)).UserShareListModel.elementAt(j).equals(jComboBoxUsers.getSelectedItem())) {
						// Elemento encontrado
						// Retirar elemento
						((Share)ShareListModel.elementAt(i)).UserShareListModel.remove(j);

						//NEW
						SBFile file = null;
						for(SBFile f : userDir.getSyncFiles())
							if((current+File.separator+f.getName()).equals(((Share)ShareListModel.elementAt(i)).Share))
								file = f;

						sendEvent(new RemoveUserEvent(new SBUser((String)jComboBoxUsers.getSelectedItem()), file));

						//END NEW

						jListUserShareList.setModel(((Share)ShareListModel.elementAt(i)).UserShareListModel);
						break;
					}
				}
			}
		}

	}//GEN-LAST:event_jButtonDelUserActionPerformed

	// Variaveis do projecto
	private javax.swing.DefaultListModel SyncListModel;
	private javax.swing.DefaultListModel ShareListModel;
	private javax.swing.DefaultListModel UserShareListModel;
	private String MySharesMarker = "--- MyShares";
	private String OtherSharesMarker = "--- Other";

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButtonAddShare;
	private javax.swing.JButton jButtonAddSync;
	private javax.swing.JButton jButtonAddUser;
	private javax.swing.JButton jButtonBrowse;
	private javax.swing.JButton jButtonDelShare;
	private javax.swing.JButton jButtonDelSync;
	private javax.swing.JButton jButtonDelUser;
	private javax.swing.JComboBox jComboBoxUsers;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabelNumShares;
	private javax.swing.JLabel jLabelNumUsers;
	private javax.swing.JLabel jLabelStatus;
	private javax.swing.JLabel jLabelSyncSize;
	private javax.swing.JList jListShareList;
	private javax.swing.JList jListSyncList;
	private javax.swing.JList jListUserShareList;
	private javax.swing.JMenuBar jMenuBar1;
	private javax.swing.JMenu jMenuShare;
	private javax.swing.JMenu jMenuGetSyncs;
	private javax.swing.JMenu jMenuSync;	
	private javax.swing.JMenu jMenuSyncUp;
	private javax.swing.JMenu jMenuGetUsers;
	private javax.swing.JMenu jMenuSyncDown;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JSeparator jSeparator5;
	private javax.swing.JTextField jTextFieldFile;
	// End of variables declaration//GEN-END:variables

}
