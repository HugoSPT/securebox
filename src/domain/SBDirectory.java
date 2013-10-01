package domain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;



public class SBDirectory extends File implements Cloneable {
	
	private SBUser owner;
	private LinkedList <SBFile> sync;
	private HashMap <SBFile, LinkedList<SBUser>> share;
	
	public SBDirectory(File root, SBUser owner) {
		super(root.getName()+File.separator+owner.getName());
		this.owner = owner;
		sync = new LinkedList <SBFile>();
		share = new HashMap <SBFile, LinkedList<SBUser>>();
	}
	
	public SBUser getOwner(){
		return this.owner;
	}
	
	public boolean createPath(){
		return super.mkdir();
	}
	
	public boolean insertSyncFile(SBFile file){
		return sync.add(file);
	}
	
	public boolean deleteSyncFile(SBFile file){
		return sync.remove(file);
	}
	
	public boolean insertShareFile(SBFile file){
		return share.put(file, new LinkedList <SBUser>()) != null;
	}
	
	public LinkedList<SBUser> removeShareFile(SBFile file){
		return share.remove(file);
	}
	
	public boolean insertShareUser(SBFile file, SBUser user){
		return share.get(file).add(user);
	}
	
	public boolean removeShareUser(SBFile file, SBUser user){
		return share.get(file).remove(user);
	}
	
	
	public LinkedList <SBUser> getShareUsers(SBFile file){
		if(share.containsKey(file))
			return (LinkedList<SBUser>) share.get(file).clone();
		return null;
	}
	
	public LinkedList <SBFile> getSyncFiles(){
		return (LinkedList<SBFile>) this.sync.clone();
	}
	
	public SBFile getSyncFile(SBFile file){
		return this.sync.get((this.sync.indexOf(file)));
	}
	
	public LinkedList<SBUser> removeSyncFile(SBFile file){		
		this.sync.remove(file);
		
		if(share.containsKey(file))
			return share.remove(file);
		
		return new LinkedList<SBUser>();
	}
	
	public void writeFiles(BufferedWriter bw) {	
		
		for(SBFile file : sync){
			try {
				
				bw.write("   <File>"+file.getName()+"\n");/*+
						"\n      "+file.getClientPath()+
						"\n      "+file.getServerPath()+"\n");*/

				if(this.share.containsKey(file)){
					bw.write("      <Share>Y\n         <SharedUsers>");
					for(SBUser user : this.share.get(file))
						bw.write(user.getName()+",");
					bw.write("\n         </SharedUsers>\n      </Share>");
				}
				else
					bw.write("      <Share>N\n      </Share>");
				bw.write("\n   </File>\n");
			} catch (IOException e) {
				return;
			}
			
		}
	}
	
	public boolean equals(Object other){
		if(this == other)
			return true;
		
		if(!(other instanceof SBDirectory))
			return false;
		
		SBDirectory d = (SBDirectory) other;
		
		return this.owner.equals(d.owner);
	}
	
	public String toString(){
		String ownedFiles = "";
		for(SBFile f : sync){
			ownedFiles += f.getName() + "\nPartilha:\n";
			LinkedList<SBUser> users = getShareUsers(f);
			if(users != null)
				for(SBUser u : users)
					ownedFiles += u.getName() + "\n";
		}
			
		
		return this.owner.toString() + "\nOwned:\n" + ownedFiles;
	}

	public SBDirectory merge(SBDirectory sbd) {
		for(int i = 0; i < sbd.owner.getSharedFiles().size(); i++)
			if(!this.owner.isShared(sbd.owner.getSharedFiles().get(i)))
				this.owner.insertSharedFile(sbd.owner.getSharedFiles().get(i));
		return this;
	}

	@Override
	public Object clone(){
		SBDirectory sbd = null;
		try {
			sbd = (SBDirectory) super.clone();	
			sbd.owner = (SBUser) this.owner.clone();
			sbd.share = (HashMap<SBFile, LinkedList<SBUser>>) this.share.clone();
			sbd.sync = (LinkedList<SBFile>) this.sync.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Erro ao clonar Directory");
			return null;
		}	
		
		return sbd;
		
	}

}
