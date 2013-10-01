package events;

import domain.SBFile;

public class AddSyncFileEvent extends FileEvent {
	
	public AddSyncFileEvent(SBFile file) {
		super(file);
	}
}
