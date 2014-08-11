package org.eclipse.flux.service.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

public class MessageCloudFoundryServiceLauncher extends MessageServiceLauncher {
	
	private CloudFoundryClient cfClient;
	
	private AtomicInteger numberOfInstances;
	
	public MessageCloudFoundryServiceLauncher(MessageConnector messageConnector, URL cfControllerUrl, String orgName, String spaceName, String cfLogin, String cfPassword, String fluxUrl, String username, String password, String serviceID, int maxPoolSize, 
			long timeout, File appFolder) throws IOException {
		super(messageConnector, serviceID, maxPoolSize, timeout);
		this.numberOfInstances = new AtomicInteger(maxPoolSize);
		cfClient = new CloudFoundryClient(new CloudCredentials(cfLogin, cfPassword), cfControllerUrl, orgName, spaceName);
		cfClient.uploadApplication(serviceID , getManifestFile(appFolder, fluxUrl, username, password));
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
	
	private File getManifestFile(File appFolder, String fluxUrl, String username, String password) {
		File manifestFile = new File(System.getProperty(/*"java.io.tmpdir"*/"user.dir") + File.separator + serviceID + ".yml");
		FileWriter fw = null;
		try {
			fw = new FileWriter(manifestFile, false);
			fw.write(createManifestContents(appFolder, fluxUrl, username, password).toString());
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return manifestFile;
	}
	
	private StringBuilder createManifestContents(File appFolder, String fluxUrl, String username, String password) {
		StringBuilder sb = new StringBuilder();
		sb.append("---\n");
		sb.append("applications:\n");
		sb.append("-name: ");
		sb.append(serviceID);
		sb.append("\n");
		sb.append("memory: 1G\n");
		sb.append("instances: 0\n");
		sb.append("no-route: true\n");
		sb.append("path: ");
		sb.append(appFolder.getPath());
		sb.append("\n");
		sb.append("env:\n");
		sb.append("-Dflux-host=");
		sb.append(fluxUrl);
		sb.append("\n");
		sb.append("-Dflux.user.name=");
		sb.append(username);
		sb.append("\n");
		sb.append("-Dflux.user.token=");
		sb.append(password);
		sb.append("\n");		
		return sb;
	}

}
