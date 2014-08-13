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
		this.numberOfInstances = new AtomicInteger(0);
		cfClient = new CloudFoundryClient(new CloudCredentials(cfLogin, cfPassword), cfControllerUrl, orgName, spaceName);
		CloudApplication cfApp = cfClient.getApplication(serviceID);
		if (cfApp != null) {
			cfClient.deleteApplication(serviceID);
		}
		cfClient.createApplication(serviceID, new Staging(), 1024, null, null);
		cfApp = cfClient.getApplication(serviceID);
		cfApp.setEnv(createEnv(fluxUrl, username, password));
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
//		System.out.println(getManifestFile(appFolder, fluxUrl, username, password).getPath());
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
	
//	private File getManifestFile(File appFolder, String fluxUrl, String username, String password) {
//		File manifestFile = new File(System.getProperty(/*"java.io.tmpdir"*/"user.dir") + File.separator + serviceID + ".yml");
//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(manifestFile, false);
//			fw.write(createManifestContents(appFolder, fluxUrl, username, password).toString());
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return manifestFile;
//	}
	
	private List<String> createEnv(String fluxUrl, String username, String password) {
		List<String> env = new ArrayList<String>(3);
		env.add("-Dflux-host=" + fluxUrl);
		env.add("-Dflux.user.name=" + username);
		env.add("-Dflux.user.name=" + password);
		return env;
	}
	
//	private StringBuilder createManifestContents(File appFolder, String fluxUrl, String username, String password) {
//		StringBuilder sb = new StringBuilder();
//		sb.append("---\n");
//		sb.append("applications:\n");
//		sb.append("-name: ");
//		sb.append(serviceID);
//		sb.append("\n");
//		sb.append("memory: 1G\n");
//		sb.append("instances: 0\n");
//		sb.append("no-route: true\n");
//		sb.append("path: ");
//		sb.append(appFolder.getPath());
//		sb.append("\n");
//		sb.append("env:\n");
//		sb.append("-Dflux-host=");
//		sb.append(fluxUrl);
//		sb.append("\n");
//		sb.append("-Dflux.user.name=");
//		sb.append(username);
//		sb.append("\n");
//		sb.append("-Dflux.user.token=");
//		sb.append(password);
//		sb.append("\n");		
//		return sb;
//	}

}
