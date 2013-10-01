package events;

import java.io.Serializable;

import domain.SBFile;
import domain.SBUser;

public class UserEvent extends Event implements Serializable {

	private SBFile file;
	private SBUser user;
	
	public UserEvent(SBUser user, SBFile file) {
		this.user = user;
		this.file = file;
	}
	
	public SBUser getUser(){
		return this.user;
	}
	
	public SBFile getFile(){
		return this.file;
	}

}
