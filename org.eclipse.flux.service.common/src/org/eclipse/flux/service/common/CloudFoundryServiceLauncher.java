package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;

public class CloudFoundryServiceLauncher implements IServiceLauncher {

	private CloudFoundryClient cfClient;
	private String serviceId;
	private int numberOfInstances;	

	public CloudFoundryServiceLauncher(String serviceId, URL cfControllerUrl, String orgName, String spaceName, String cfLogin, String cfPassword, String fluxUrl, String username, String password, 
			File appLocation) throws IOException {
		this.serviceId = serviceId;
		this.numberOfInstances = 0;
		cfClient = new CloudFoundryClient(new CloudCredentials(cfLogin, cfPassword), cfControllerUrl, orgName, spaceName);
		cfClient.login();
		try {
			CloudApplication cfApp = cfClient.getApplication(serviceId);
			if (cfApp != null) {
				cfClient.deleteApplication(serviceId);
			}
		} catch (CloudFoundryException e) {
			e.printStackTrace();
		}
		cfClient.createApplication(serviceId, new Staging(), 1024, null, null);
		cfClient.uploadApplication(serviceId, appLocation, new UploadStatusCallback() {
			
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
		cfClient.updateApplicationEnv(serviceId, createEnv(fluxUrl, username, password));
	}

	@Override
	public void init() {
		cfClient.login();
		/*
		 * One instance will be running once the app starts, so set instances
		 * number to 1
		 */
		numberOfInstances = 1;
		cfClient.startApplication(serviceId);
		boolean started = false;
		while (!started) {
			try {
				cfClient.updateApplicationInstances(serviceId, numberOfInstances);
				started = true;
				System.out.println(serviceId + " app started on CF");
			} catch (Exception e) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public void startService(int n) {
		cfClient.login();
		boolean updated = false;
		while (!updated) {
			try {
				cfClient.updateApplicationInstances(serviceId,
						numberOfInstances + n);
				numberOfInstances += n;
				updated = true;
			} catch (Throwable t) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void dispose() {
		cfClient.login();
		cfClient.stopApplication(serviceId);
	}

	private List<String> createEnv(String fluxUrl, String username, String password) {
		List<String> env = new ArrayList<String>(3);
		env.add("FLUX_HOST=" + fluxUrl);
		env.add("FLUX_USER_ID=" + username.replace("$", "\\$"));
		env.add("FLUX_USER_TOKEN=" + password);
		env.add("FLUX_LAZY_START=true");
		return env;
	}
	
}
