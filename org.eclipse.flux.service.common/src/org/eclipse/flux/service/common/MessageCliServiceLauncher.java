package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MessageCliServiceLauncher extends MessageServiceLauncher {
	
	private ProcessBuilder processBuilder;

	public MessageCliServiceLauncher(MessageConnector messageConnector, String serviceID, int maxPoolSize, 
			long timeout, File serviceFolder, List<String> command) {
		super(messageConnector, serviceID, maxPoolSize, timeout);
		int random = new Random().nextInt();
		this.processBuilder = new ProcessBuilder(command).directory(serviceFolder)
				.redirectOutput(new File(serviceFolder.getPath() + File.separator + random + ".out"))
				.redirectError(new File(serviceFolder.getPath() + File.separator + random + ".err"));
	}

	@Override
	protected void addService(int n) {
		for (int i = 0; i < n; i++) {
			try {
				processBuilder.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
