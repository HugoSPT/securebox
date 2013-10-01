package domain;

import java.io.File;
import java.io.Serializable;


public class SBFile extends File implements Serializable {

	private String serverPath;
	
	public SBFile(String pathname){
		super(pathname);
		this.serverPath = "";
	}
	
	public SBFile(SBDirectory directory, String filename) {
		super(directory, filename);
		this.serverPath = "";
	}
	
	public SBUser getOwner(){
		return ((SBDirectory) this.getParentFile()).getOwner();
	}
	
	public void setServerPath(String path){
		this.serverPath = path;
	}
	
	public String getServerPath(){
		return this.serverPath;
	}
	
	public boolean equals(Object other){
		if(this == other)
			return true;
		
		if(!(other instanceof SBFile))
			return false;
		
		SBFile f = (SBFile) other;
		
		return f.serverPath.equals(this.serverPath) 
//				&& f.clientPath.equals(this.clientPath)
				&& f.getName().equals(this.getName());
//				&& f.lastModified() == this.lastModified();
	}
}
