package events;

import domain.SBFile;

public class SyncUpFileEvent extends FileEvent {

	private long lastModified;
	
	public SyncUpFileEvent(SBFile file, long lastModified) {
		super(file);
		this.lastModified = lastModified;
	}

	public long getLastModified() {
		return this.lastModified;
	}
	
}
