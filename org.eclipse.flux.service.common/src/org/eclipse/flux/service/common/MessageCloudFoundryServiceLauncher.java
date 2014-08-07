package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.json.JSONException;

public class MessageCloudFoundryServiceLauncher extends MessageServiceLauncher {
	
	private CloudFoundryClient cfClient;
	
	private AtomicInteger numberOfInstances;
	
	public MessageCloudFoundryServiceLauncher(URL host, URL cfControllerUrl, String orgName, String spaceName, String username, String password, String serviceID, int maxPoolSize, 
			long timeout, File appFolder) throws IOException {
		super(host, serviceID, maxPoolSize, timeout);
		this.numberOfInstances = new AtomicInteger(maxPoolSize);
		cfClient = new CloudFoundryClient(new CloudCredentials(username, password), cfControllerUrl, orgName, spaceName);
		cfClient.uploadApplication(serviceID , appFolder);
	}

	@Override
	protected void addService() {
		CloudApplication cfApp = cfClient.getApplication(serviceID);
		cfApp.setInstances(numberOfInstances.incrementAndGet());
	}

	@Override
	protected boolean removeService(String socketId, String user)
			throws JSONException {
		boolean stopped = super.removeService(socketId, user);
		if (stopped) {
			numberOfInstances.decrementAndGet();
		}
		return stopped;
	}

	@Override
	protected void initServicePool() {
		addService();
	}


}
