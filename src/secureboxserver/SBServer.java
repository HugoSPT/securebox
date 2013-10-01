/*
 * Servidor SecureBox - Sincronização e partilha
 *
 * SBServer.java
 *
 * Created on 9/Fev/2011
 */

package secureboxserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;

import domain.*;
import events.*;

/**
 *
 * @author Mario Calha e Dulce Domingos
 */
public class SBServer {

	private static LinkedList<SBDirectory> sysDirs;
	private static LinkedList<TransferingSBFile> transferingFiles;	
	private static File root;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SBServer server = new SBServer();
		sysDirs = new LinkedList<SBDirectory>();
		root = new File("root");
		if(!root.exists())
			root.mkdir();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;

		//init server
		initServerStatus();
		transferingFiles = new LinkedList<TransferingSBFile>();

		try {
			sSoc = new ServerSocket(5664);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
				System.out.println(newServerThread);
			}
			catch (IOException e) {			
				//falhou ao aceitar a socket
				System.out.println("Erro ao aceitar a socker - Server");
			}
		}
	}

	private void toFile() {
		try {
			BufferedWriter bw = new BufferedWriter (new FileWriter("status.config"));
			for(SBDirectory dir : sysDirs){
				bw.write("<User>"+dir.getName()+"\n");
				dir.writeFiles(bw);
				bw.write("</User>\n");
			}
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void initServerStatus() {

		String line = "", username = "", fileName = "",fileShare = "",sharedUsers = "";

		SBUser user = null,userShare = null;
		SBFile file = null;
		SBDirectory dir= null, dirShare = null; 

		Scanner leitor;
		try {
			leitor = new Scanner(new FileReader("status.config"));
		} catch (FileNotFoundException e) {
			return;
		}

		while(leitor.hasNext()){

			line = leitor.nextLine();
			if(line.contains("<User>")){
				username = line.split(">")[1].trim();
				user = new SBUser(username);
				dir = new SBDirectory(root, user);
				if(!sysDirs.contains(dir))
					sysDirs.add(dir);
				if(!dir.exists())
					dir.mkdir();
			}

			if(line.contains("<File>")){
				fileName = line.split(">")[1].trim();

				file = new SBFile(dir, fileName);
				dir.insertSyncFile(file);

				String[] path = System.getProperty("user.dir").toString().replace(File.separator, "/").split("/");

				String current = path[path.length-1];

				//file.setClientPath(current+File.separator+"workspace_"+user.getName()+File.separator+fileName);
				file.setServerPath(current+File.separator+user.getName()+File.separator+fileName);


				line = leitor.nextLine();
				fileShare = line.split(">")[1].trim();
				if(fileShare.equals("Y")){
					dir.insertShareFile(file);
					line = leitor.nextLine();
					String[] array = line.split(">");
					if(array.length > 1){
						sharedUsers = array[1].trim();
						String usersArray[] = sharedUsers.split(",");
						for(String s : usersArray){
							userShare = new SBUser(s.trim());
							dirShare = new SBDirectory(root, userShare);
							if(!sysDirs.contains(dirShare)){
								sysDirs.add(dirShare);
								if(!dirShare.exists())
									dirShare.mkdir();
							}
							else{
								dirShare = sysDirs.get(sysDirs.indexOf(dirShare));
							}
							dirShare.getOwner().insertSharedFile(file);
							dir.insertShareUser(file, userShare);

						}
					}
				}

			}
		}
		leitor.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;
		CommunicationController cc;
		SendFileThread sft;

		ServerThread(Socket inSoc) {

			socket = inSoc;
			try {
				cc = new CommunicationController(socket);
				sft = new SendFileThread(cc);
				sft.start();
			} catch (IOException e) {
				System.out.println("Nao foi possivel criar os canais de comunicaco.");
				cc = null;
			}
			System.out.println("thread do server para cada cliente");
		}

		@Override
		public void run(){				
			SBDirectory dir = null;
			Object login = cc.readObject();

			if(login instanceof LoginEvent){						
				SBSession session =((LoginEvent) login).getSession();

				String username = session.getUsername();
				String passwd = session.getPassword();

				//autenticao do cliente
				if (authenticate(username, passwd)){

					for(SBDirectory sbd : sysDirs)
						if(sbd.getName().equals(username))
							dir = sbd;

					if(dir == null){
						SBUser user = new SBUser(username);
						dir = new SBDirectory(root, user);
						if(!dir.exists())
							dir.mkdir();
						sysDirs.add(dir);
					}

					cc.writeObject(dir);

					toFile();
				}
				else
					cc.writeObject(null);
			}

			Object input = null;
			int error = 0;
			do{
				input = cc.readObject();
				if(input == null)
					error++;
				else
					error = 0;

				dispatcher(input, dir);

				dir = dir.merge(sysDirs.get(sysDirs.indexOf(dir)));

			}while(true);

		}

		private boolean authenticate(String username, String pass) {
			return username != null && username.length() != 0;
		}

		private boolean dispatcher(Object input, SBDirectory dir) {

			if(input == null)
				return true;

			System.out.println("Servidor - Recebeu: "+input.getClass());

			if(input instanceof FileEvent){
				SBFile file = ((FileEvent) input).getFile();

				if(input instanceof AddSyncFileEvent){

					file = new SBFile(dir, ((FileEvent) input).getFile().getName());

					//file.setClientPath(((FileEvent) input).getFile().getClientPath());
					file.setServerPath(file.getAbsolutePath());

					try {
						transferingFiles.add(new TransferingSBFile(file, new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()))));
					} catch (FileNotFoundException e) {
						System.out.println("Nao conseguiu criar o ficheiro");
						return false;
					}

					sysDirs.get(sysDirs.indexOf(dir)).insertSyncFile(file);

					toFile();

					cc.writeObject(new ServerAnswerEvent(file, new AddSyncFileEvent(null)));

					return true;
				}
				else if(input instanceof SyncUpFileEvent){

					file = ((SyncUpFileEvent) input).getFile();
					long lastModified = ((SyncUpFileEvent) input).getLastModified();

					
					if(lastModified > file.lastModified()){
											
						try {
							transferingFiles.add(new TransferingSBFile(file, new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()))));
						} catch (FileNotFoundException e) {
							System.out.println("Nao conseguiu criar o ficheiro");
							return false;
						}

						toFile();

						cc.writeObject(new ServerAnswerEvent(new Boolean(true), new SyncUpFileEvent(null, 0)));
					}
					else
						cc.writeObject(new ServerAnswerEvent(new Boolean(false), new SyncUpFileEvent(null, 0)));

					return true;
				}
				else if(input instanceof FileDataEvent){					
					SBFileData fd = ((FileDataEvent) input).getFileData();

					for(TransferingSBFile tf : transferingFiles)
						if(tf.file().equals(file)){
							if(tf.write(fd.data(), 0, fd.size()))
								cc.writeObject(new ServerAnswerEvent(new Boolean(true), new FileDataEvent(null,null)));
							else
								cc.writeObject(new ServerAnswerEvent(new Boolean(false), new FileDataEvent(null,null)));

							if(fd.isLast()){
								tf.close();
								transferingFiles.remove(tf);
							}

							return true;
						}
					return false;
				}
				else if(input instanceof RemoveSyncFileEvent){

					LinkedList<SBUser> users = dir.removeSyncFile(file);

					for(SBDirectory d : sysDirs)
						for(SBUser u : users)
							if(u.equals(d.getOwner()))
								d.getOwner().removeSharedFile(file);

					file.delete();

					toFile();

					if(file != null && file.exists())
						cc.writeObject(new ServerAnswerEvent(new Boolean(true), new RemoveSyncFileEvent(null)));
					else
						cc.writeObject(new ServerAnswerEvent(new Boolean(false), new RemoveSyncFileEvent(null)));

					return true;
				}
				else if (input instanceof AddShareFileEvent){
					dir.insertShareFile(dir.getSyncFile(file));

					toFile();

					cc.writeObject(new ServerAnswerEvent(new Boolean(true), new AddShareFileEvent(null)));

					return true;
				}
				else if (input instanceof RemoveShareFileEvent){
					LinkedList<SBUser> sharedUsers = dir.removeShareFile(file);

					for(SBDirectory d : sysDirs)
						if(sharedUsers.contains(d.getOwner()))
							d.getOwner().removeSharedFile(file);

					toFile();

					cc.writeObject(new ServerAnswerEvent(new Boolean(true), new RemoveShareFileEvent(null)));

					return true;
				}

				else if(input instanceof SyncDownFileEvent){

					cc.writeObject(new ServerAnswerEvent(file, new SyncDownFileEvent(null)));

					if(sft.sendFile(file))
						return true;
					return false;
				}

			}
			else if(input instanceof UserEvent){
				SBFile file = dir.getSyncFile(((UserEvent) input).getFile());
				SBUser user = null;
				for(SBDirectory d : sysDirs)
					if(d.getOwner().getName().equals(((UserEvent) input).getUser().getName()))
						user = d.getOwner();

				if(user == null || file == null)
					cc.writeObject(new ServerAnswerEvent(new Boolean(false), new UserEvent(null,null)));

				if(input instanceof AddUserEvent){
					dir.insertShareUser(file, user);
					for(SBDirectory d : sysDirs)
						if(d.getOwner().equals(user))
							d.getOwner().insertSharedFile(file);

					toFile();

					cc.writeObject(new ServerAnswerEvent(new Boolean(true), new AddUserEvent(null,null)));
					return true;
				}		
				else if(input instanceof RemoveUserEvent){
					dir.removeShareUser(file, user);
					for(SBDirectory d : sysDirs)
						if(d.getOwner().equals(user))
							d.getOwner().removeSharedFile(file);

					toFile();

					cc.writeObject(new ServerAnswerEvent(new Boolean(true), new RemoveUserEvent(null,null)));
					return true;
				}
			}
			else if (input instanceof GetUsersEvent){
				LinkedList<String> users = new LinkedList<String>();
				for(int i = 0; i < sysDirs.size(); i++)
					users.add(sysDirs.get(i).getName());
				cc.writeObject(new ServerAnswerEvent(users, new GetUsersEvent()));
				return true;
			}
			else if(input instanceof SyncEvent){
				cc.writeObject(new ServerAnswerEvent(dir.clone(), new SyncEvent()));
				return true;
			}
			else if(input instanceof GetShareListEvent){

				cc.writeObject(new ServerAnswerEvent(dir.clone(), new GetShareListEvent()));
				return true;
			}
			else if(input instanceof UpdateEvent){
				cc.writeObject(new ServerAnswerEvent(dir.clone(), new UpdateEvent()));
				return true;
			}
			else if(input instanceof GetSyncEvent){
				for(SBFile file : dir.getSyncFiles())
					cc.writeObject(new ServerAnswerEvent(new FileEvent(file), new GetSyncEvent()));

				cc.writeObject(new ServerAnswerEvent(null, new GetSyncEvent()));

				return true;
			}

			return false;

		}

	}

	class SendFileThread extends Thread {

		CommunicationController cc;

		SendFileThread(CommunicationController cc) {
			this.cc = cc;
		}

		public boolean sendFile(SBFile file) {

			BufferedInputStream inBuf;
			byte data[] = new byte[SBFileData.MAX_SIZE];
			try{

				inBuf = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
				int toRead = 0;
				int read = 0;

				while((toRead = inBuf.read(data, 0, SBFileData.MAX_SIZE)) > -1){
					SBFileData fileData = new SBFileData();
					fileData.setData(data, (read + toRead) >= file.length(), toRead);
					cc.writeObject(new ServerAnswerEvent(new FileDataEvent(file, fileData), new SyncDownFileEvent(null)));
					read += toRead;
				}

				inBuf.close();
			} catch (FileNotFoundException e) {
				return false;
				//nao foi possivel ler o ficheiro
			} catch (IOException e) {
				return false;
				//erro ao ler o ficheiro
			}
			return true;
		}

		@Override
		public void run(){	

		}
	}

}
