package domain;

import java.io.Serializable;
import java.util.LinkedList;


public class SBUser implements Serializable, Cloneable {
	
	private String username;
	private LinkedList<SBFile> sharedFiles;
	
	public SBUser (String username){
		this.username = username;
		sharedFiles = new LinkedList<SBFile>();
	}
	
	public String getName(){
		return this.username;
	}
	
	public LinkedList <SBFile> getSharedFiles(){
		return sharedFiles;
	}
	
	public boolean isShared (SBFile file){
		return sharedFiles.contains(file);
	}
	
	public boolean insertSharedFile (SBFile file){
		return sharedFiles.add(file);
	}
	
	public boolean removeSharedFile (SBFile file){
		return sharedFiles.remove(file);
	}
	
	public boolean equals(Object other){
		if(this == other)
			return true;
		
		if(!(other instanceof SBUser))
			return false;
		
		SBUser u = (SBUser) other;
		
		return this.username.equals(u.username);
	}
	
	public String toString(){
		String filesString = "Files:\n";
		for(SBFile f : sharedFiles)
			filesString += f.getName() + "\n";
		
		return username + "\n" + filesString;
	}
	
	public Object clone(){
		SBUser u = null;
		try {
			u = (SBUser) super.clone();
			u.sharedFiles = (LinkedList<SBFile>) this.sharedFiles.clone();
			u.username = this.username;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return u;
	}
}
