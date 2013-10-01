package events;

import domain.SBFile;

public class AddShareFileEvent extends FileEvent {

	public AddShareFileEvent(SBFile file) {
		super(file);
	}

}
