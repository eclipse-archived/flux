package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;

public class MessageCloudFoundryServiceLauncher extends MessageServiceLauncher {
	
	private CloudFoundryClient cfClient;
	
	private AtomicInteger numberOfInstances;
	
	public MessageCloudFoundryServiceLauncher(MessageConnector messageConnector, URL cfControllerUrl, String orgName, String spaceName, String cfLogin, String cfPassword, String fluxUrl, String username, String password, String serviceID, int maxPoolSize, 
			long timeout, File appLocation) throws IOException {
		super(messageConnector, serviceID, maxPoolSize, timeout);
		this.numberOfInstances = new AtomicInteger(maxPoolSize);
		cfClient = new CloudFoundryClient(new CloudCredentials(cfLogin, cfPassword), cfControllerUrl, orgName, spaceName);
		cfClient.login();
		CloudApplication cfApp = cfClient.getApplication(serviceID);
		if (cfApp != null) {
			cfClient.deleteApplication(serviceID);
		}
		cfClient.createApplication(serviceID, new Staging(), 1024, null, null);
		cfClient.uploadApplication(serviceID , appLocation, new UploadStatusCallback() {
			
			@Override
			public boolean onProgress(String arg0) {
				System.out.println("Progress: " + arg0);
				return false;
			}
			
			@Override
			public void onProcessMatchedResources(int arg0) {
				System.out.println("Matching Resources: " + arg0);
			}
			
			@Override
			public void onMatchedFileNames(Set<String> arg0) {
				System.out.println("Matching file names: " + arg0);
			}
			
			@Override
			public void onCheckResources() {
				System.out.println("Check resources!");
			}
		});		
		cfClient.updateApplicationEnv(serviceID, createEnv(fluxUrl, username, password));
		cfClient.updateApplicationInstances(serviceID, maxPoolSize);
	}

	@Override
	protected void addService() {
		cfClient.updateApplicationInstances(serviceID, numberOfInstances.incrementAndGet());
	}

	@Override
	protected void initServices() {
		cfClient.startApplication(serviceID);
	}

	@Override
	protected boolean removeService(String socketId) {
		boolean stopped = super.removeService(socketId);
		if (stopped) {
			numberOfInstances.decrementAndGet();
		}
		return stopped;
	}
	
	private List<String> createEnv(String fluxUrl, String username, String password) {
		List<String> env = new ArrayList<String>(3);
		env.add("flux-host=" + fluxUrl);
		env.add("flux.user.name=" + username);
		env.add("flux.user.token=" + password);
		env.add("flux.jdt.lazyStart=true");
		return env;
	}

	@Override
	public void dispose() {
		super.dispose();
		cfClient.stopApplication(serviceID);
		cfClient.logout();
	}
	
}
