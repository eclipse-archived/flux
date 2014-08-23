package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LocalProcessServiceLauncher implements IServiceLauncher {
	
	private ProcessBuilder processBuilder;
	
	public LocalProcessServiceLauncher(File serviceFolder, List<String> command) {
		this.processBuilder = new ProcessBuilder(command).directory(serviceFolder)
				.redirectOutput(new File(serviceFolder.getPath() + File.separator + "output.out"))
				.redirectError(new File(serviceFolder.getPath() + File.separator + "output.err"));
	}

	@Override
	public void init() {
		// nothing
	}

	@Override
	public void startService(int n) {
		for (int i = 0; i < n; i++) {
			try {
				processBuilder.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void dispose() {
		// nothing
	}

}
