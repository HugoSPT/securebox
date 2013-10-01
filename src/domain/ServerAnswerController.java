package domain;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;

import events.Event;
import events.FileDataEvent;
import events.ServerAnswerEvent;
import events.SyncDownFileEvent;


public class ServerAnswerController extends Thread{


	private LinkedList<ServerAnswerEvent> answers;
	private LinkedList<TransferingSBFile> transferingFiles;
	private CommunicationController cc;
	private String current;
	
	public ServerAnswerController(CommunicationController cc, String current){
		answers = new LinkedList<ServerAnswerEvent>();
		transferingFiles = new LinkedList<TransferingSBFile>();
		this.cc = cc;
		this.current = current;
	}

	@Override
	public void run(){
		ServerAnswerEvent sa = null;
		
		int error = 0;
		do {
			sa = (ServerAnswerEvent) cc.readObject();
			
			if(sa != null){
				
				System.out.println("Cliente - Recebeu resposta a: "+sa.getEvent().getClass());
				
				if(sa.getObject() instanceof FileDataEvent)
					processFileData((FileDataEvent) sa.getObject());
				else if(sa.getEvent() instanceof SyncDownFileEvent){
					for(TransferingSBFile tf : transferingFiles)
						if(tf.file().equals((SBFile) sa.getObject()))
							continue;
					SBFile file = (SBFile) sa.getObject();
					try {
						//if(file.exists() && e' mais recente)
						System.out.println("FILE: "+current+File.separator+file.getName());
						transferingFiles.add(new TransferingSBFile(file, new BufferedOutputStream(new FileOutputStream(current+File.separator+file.getName()))));
						// else -> colocar na pasta workspace
					} catch (FileNotFoundException e) {
						//File not found
						System.out.println("N‹o possivel transferir o ficheiro (Servidor->Cliente).");
					}
					
				}
				else
					answers.add(sa);
				
				error = 0;

			}
			else
				error++;


		}while(true);
	}

	public ServerAnswerEvent getAnswer(Event expected){
		ServerAnswerEvent toReturn = null;
		for(ServerAnswerEvent sae : answers)
			if(sae.getEvent().getClass().equals((expected.getClass()))){
				toReturn = sae;
				break;
			}

		if(toReturn != null)
			answers.remove(toReturn);
		
		return toReturn;
	}

	public boolean processFileData(FileDataEvent fde) {	
				
		SBFileData fd = fde.getFileData();
		SBFile file = fde.getFile();
		for(TransferingSBFile tf : transferingFiles)
			if(tf.file().equals(file)){
				tf.write(fd.data(), 0, fd.size());

				if(fd.isLast()){
					tf.close();
					transferingFiles.remove(tf);
					return true;
				}

				break;
			}
		
		return false;

		//SE nao encotnrar o ficheiro (pode ainda nao ter sido adicionado) carregar variavel auxiliar
		//FALTA FAZER
	}

}
