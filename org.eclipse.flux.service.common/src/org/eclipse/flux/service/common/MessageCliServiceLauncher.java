package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MessageCliServiceLauncher extends MessageServiceLauncher {
	
	private ProcessBuilder processBuilder;

	public MessageCliServiceLauncher(/*String host*/MessageConnector messageConnector, String serviceID,
			long timeout, File serviceFolder, List<String> command) {
		super(/*host*/messageConnector, serviceID, timeout);
		int random = new Random().nextInt();
		this.processBuilder = new ProcessBuilder(command).directory(serviceFolder)
				.redirectOutput(new File(serviceFolder.getPath() + File.separator + random + ".out"))
				.redirectError(new File(serviceFolder.getPath() + File.separator + random + ".err"));
	}

	@Override
	protected void addServiceToPool() {
		try {
			processBuilder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
