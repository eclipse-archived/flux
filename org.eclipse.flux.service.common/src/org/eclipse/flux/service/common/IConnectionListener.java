package org.eclipse.flux.service.common;

public interface IConnectionListener {
	
	void connected(String userChannel);
	
	void disconnected(String userChannel);

}
