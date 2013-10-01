package events;

import domain.SBFile;
import domain.SBUser;

public class RemoveUserEvent extends UserEvent {

	public RemoveUserEvent(SBUser user, SBFile file) {
		super(user, file);
	}

}
