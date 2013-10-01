package events;

import domain.SBFile;

public class ServerAnswerEvent extends Event {

	private Object obj;
	private Event event; //respota a 1 evento deste tipo
	
	public ServerAnswerEvent(Object obj, Event event) {
		this.obj = obj;
		this.event = event;
	}

	public Event getEvent() {
		return this.event;
	}
	
	public Object getObject(){
		return this.obj;
	}

}
