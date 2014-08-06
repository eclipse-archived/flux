package org.eclipse.flux.core;

public interface IServiceConnector {
	
	void startService(String user, String token);
	
	void stopService();

}
