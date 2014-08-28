/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.service.common;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Launches process for a JDT service. Emulates Cloud Foundry JDT services
 * 
 * @author aboyko
 */
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
