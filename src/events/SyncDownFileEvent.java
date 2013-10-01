package events;

import domain.SBFile;

public class SyncDownFileEvent extends FileEvent {

	
	public SyncDownFileEvent(SBFile file) {
		super(file);
	}

}
