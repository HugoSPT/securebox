package events;

import domain.SBFile;

public class RemoveSyncFileEvent extends FileEvent {

	public RemoveSyncFileEvent(SBFile file) {
		super(file);
	}

}
