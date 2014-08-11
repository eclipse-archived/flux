package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

public class MessageCloudFoundryServiceLauncher extends MessageServiceLauncher {
	
	private CloudFoundryClient cfClient;
	
	private AtomicInteger numberOfInstances;
	
	public MessageCloudFoundryServiceLauncher(MessageConnector messageConnector, URL cfControllerUrl, String orgName, String spaceName, String username, String password, String serviceID, int maxPoolSize, 
			long timeout, File appFolder) throws IOException {
		super(messageConnector, serviceID, maxPoolSize, timeout);
		this.numberOfInstances = new AtomicInteger(maxPoolSize);
		cfClient = new CloudFoundryClient(new CloudCredentials(username, password), cfControllerUrl, orgName, spaceName);
		cfClient.uploadApplication(serviceID , appFolder);
	}

	@Override
	protected void addService(int n) {
		CloudApplication cfApp = cfClient.getApplication(serviceID);
		cfApp.setInstances(numberOfInstances.addAndGet(n));
	}

	@Override
	protected boolean removeService(String socketId) {
		boolean stopped = super.removeService(socketId);
		if (stopped) {
			numberOfInstances.decrementAndGet();
		}
		return stopped;
	}

}
