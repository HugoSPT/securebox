package domain;

import java.io.Serializable;

public class SBFileData implements Serializable {

	public final static int MAX_SIZE = 100000;
	private byte fileBytes[];
	private boolean last;
	private int size;
	
	public SBFileData() {
	}
	
	public boolean isLast(){
		return this.last;
	}
	
	public byte[] data(){
		return this.fileBytes;
	}
	
	public int size(){
		return this.size;
	}
	
	/**
	 * 
	 * @param bytes
	 * @param last
	 * @param size
	 * @requieres bytes.length  <= MAX_SIZE
	 */
	public void setData(byte[] bytes, boolean last, int size){
		this.last = last;
		this.size = size;
		this.fileBytes = bytes;
	}

}
