package com.inedo.http;

import java.util.ArrayList;
import java.util.List;

class EventManager {
    private List<HttpEasyListener> listeners = new ArrayList<HttpEasyListener>();
    private boolean isDefaultListener;
	private boolean setLogRequestDetails = false; 
    
    public EventManager() {
        addListener(new LogWriter());
        isDefaultListener = true;
    }
    
    public EventManager(EventManager eventManager) {
        listeners.addAll(eventManager.getListeners());
        isDefaultListener = eventManager.isDefaultListener();
    }

	public void setLogRequestDetails(boolean logRequestDetails) {
		this.setLogRequestDetails  = logRequestDetails;
	}
	
    public void clearListeners() {
        listeners.clear();
    }

    public boolean isDefaultListener() {
        return isDefaultListener;
    }
    
    public boolean hasListeners() {
        return listeners.size() > 0;
    }

    public List<HttpEasyListener> getListeners() {
        return listeners;
    }
    
    public void addListener(HttpEasyListener toAdd) {
        if (isDefaultListener) {
            listeners.clear();
            isDefaultListener = false;
        }
        
        listeners.add(toAdd);
    }

    public void addListeners(List<HttpEasyListener> toAdd) {
        if (isDefaultListener) {
            listeners.clear();
            isDefaultListener = false;
        }
        
        listeners.addAll(toAdd);
    }
    
    public void request(String msg, Object... args) {
        for (HttpEasyListener listener : listeners) {
            listener.request(msg, args);
        }
    }
    
    public void details(String msg, Object... args) {
    	if (!setLogRequestDetails) {
    		return;
    	}
    	
        for (HttpEasyListener listener : listeners) {
            listener.details(msg, args);
        }
    }

    public void error(String message, Throwable t) {
        for (HttpEasyListener listener : listeners) {
            listener.error(message, t);
        }
    }
}
