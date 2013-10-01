package domain;

import java.io.BufferedOutputStream;
import java.io.IOException;


public class TransferingSBFile {
	
	private SBFile file;
	private BufferedOutputStream buf;
	
	public TransferingSBFile(SBFile file, BufferedOutputStream buf) {
		this.file = file;
		this.buf = buf;
	}
	
	public SBFile file(){
		return this.file;
	}
	
	
	public boolean write(byte[] data, int index, int len){
		try {
			buf.write(data, index, len);
			buf.flush();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean close(){
		try {
			buf.close();
		} catch (IOException e) {
			//Nao foipossivel fechar o buffer
			return false;
		}
		return true;
	}
	
}
