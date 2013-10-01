package secureboxclient;

class Share {
	
	String Share;
	javax.swing.DefaultListModel UserShareListModel;
	
	Share(String s) {
		Share = s;
		UserShareListModel = new javax.swing.DefaultListModel();
	}
	
	@Override
	public String toString(){
		return Share.toString();
	}
	
	@Override
	public boolean equals(Object s){
				
		if(Share == s)
			return true;
		if(s instanceof String && Share.equals(s))
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return Share.hashCode();
	}
}

