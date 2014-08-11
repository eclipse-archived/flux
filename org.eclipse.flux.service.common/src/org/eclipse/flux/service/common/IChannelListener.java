package org.eclipse.flux.service.common;

public interface IChannelListener {
	
	void connected(String userChannel);
	
	void disconnected(String userChannel);

}
