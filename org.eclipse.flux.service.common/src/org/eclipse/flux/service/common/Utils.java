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
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Utilities
 * 
 * @author aboyko
 *
 */
public class Utils {
	
	public static final String getEquinoxLauncherJar(String eclipseFolder) {
		File directory = new File(eclipseFolder + File.separator + "plugins");
		if (!directory.exists()) {
			throw new IllegalArgumentException("Folder \"" + directory.getAbsolutePath() + "\" does not exist");
		}
		File[] files = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return Pattern.matches("org.eclipse.equinox.launcher_.*\\.jar", name);
			}
			
		});
		File latest = null;
		for (File file : files) {
			if (file.isFile()) {
				if (latest == null || latest.getName().compareTo(file.getName()) < 0) {
					latest = file;
				}
			}
		}
		if (latest == null) {
			throw new IllegalArgumentException("Cannot find 'org.eclipse.equinox.launcher' plug-in in folder: " + directory.getAbsolutePath());
		} else {
			return latest.getAbsolutePath();
		}
	}

}
