package events;

import domain.SBFile;
import domain.SBUser;

public class AddUserEvent extends UserEvent {

	public AddUserEvent(SBUser user, SBFile file) {
		super(user, file);
	}

}
