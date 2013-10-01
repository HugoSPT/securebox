package events;

import domain.SBFile;

public class RemoveShareFileEvent extends FileEvent {

	public RemoveShareFileEvent(SBFile file) {
		super(file);
	}

}
