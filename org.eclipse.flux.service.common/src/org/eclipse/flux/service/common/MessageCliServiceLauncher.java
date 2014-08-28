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
import java.util.Random;

import org.eclipse.flux.client.MessageConnector;

/**
 * Launches process for a JDT service. Emulates Cloud Foundry JDT services
 * 
 * @author aboyko
 *
 */
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
	protected void addService() {
		try {
			processBuilder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
