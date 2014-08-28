/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Martin Lippert
 */
public class ConnectedProject {
	
	private IProject project;
	private Map<String, String> resourceHash;
	private Map<String, Long> resourceTimestamp;
	
	public ConnectedProject(IProject project) {
		this.project = project;
		this.resourceHash = new ConcurrentHashMap<String, String>();
		this.resourceTimestamp = new ConcurrentHashMap<String, Long>();
		
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					String path = resource.getProjectRelativePath().toString();
					ConnectedProject.this.setTimestamp(path, resource.getLocalTimeStamp());
					
					if (resource instanceof IFile) {
						try {
							IFile file = (IFile) resource;
							ConnectedProject.this.setHash(path, DigestUtils.shaHex(file.getContents()));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else if (resource instanceof IFolder) {
						ConnectedProject.this.setHash(path, "0");
					}
					
					return true;
				}
			}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public IProject getProject() {
		return project;
	}
	
	public String getName() {
		return this.project.getName();
	}

	public static ConnectedProject readFromJSON(InputStream inputStream, IProject project) {
		return new ConnectedProject(project);
	}
	
	public void setTimestamp(String resourcePath, long newTimestamp) {
		this.resourceTimestamp.put(resourcePath, newTimestamp);
	}
	
	public long getTimestamp(String resourcePath) {
		return this.resourceTimestamp.get(resourcePath);
	}

	public void setHash(String resourcePath, String hash) {
		this.resourceHash.put(resourcePath, hash);
	}
	
	public String getHash(String resourcePath) {
		return this.resourceHash.get(resourcePath);
	}

	public boolean containsResource(String resourcePath) {
		return this.resourceTimestamp.containsKey(resourcePath);
	}
	
}
