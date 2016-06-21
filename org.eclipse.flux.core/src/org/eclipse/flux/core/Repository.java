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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flux.client.CallbackIDAwareMessageHandler;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageBus;
import org.eclipse.flux.watcher.core.RepositoryModule;
import org.eclipse.flux.watcher.core.spi.Project;
import org.eclipse.flux.watcher.fs.JDKProjectModule;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Martin Lippert
 */
public class Repository {

	private String username;
	private MessageConnector messagingConnector;
	private Collection<IMessageHandler> messageHandlers;

	private ConcurrentMap<String, ConnectedProject> syncedProjects;
	private Collection<IRepositoryListener> repositoryListeners;
	
	private static int GET_PROJECT_CALLBACK = "Repository - getProjectCallback".hashCode();
	private static int GET_RESOURCE_CALLBACK = "Repository - getResourceCallback".hashCode();
	
	private AtomicBoolean connected;
	
	private org.eclipse.flux.watcher.core.Repository repository;
	private FluxMessageBus messageBus;

	public Repository(MessageConnector messagingConnector, String user) {
		Injector injector = Guice.createInjector(new RepositoryModule(), new JDKProjectModule());
		this.username = user;
		this.repository = injector.getInstance(org.eclipse.flux.watcher.core.Repository.class);
		this.messageBus = injector.getInstance(FluxMessageBus.class);
		this.connected = new AtomicBoolean(true);
		this.messagingConnector = messagingConnector;
		this.syncedProjects = new ConcurrentHashMap<String, ConnectedProject>();
		this.repositoryListeners = new ConcurrentLinkedDeque<>();
		
		this.messageHandlers = new ArrayList<IMessageHandler>(9);
		
		IMessageHandler resourceChangedHandler = new MessageHandler("resourceChanged") {
			@Override
			public void handle(String messageType, JSONObject message) {
				updateResource(message);
			}
		};
		this.messagingConnector.addMessageHandler(resourceChangedHandler);
		messageHandlers.add(resourceChangedHandler);
		
		IMessageHandler resourceCreatedHandler = new MessageHandler("resourceCreated") {
			@Override
			public void handle(String messageType, JSONObject message) {
				createResource(message);
			}
		};
		this.messagingConnector.addMessageHandler(resourceCreatedHandler);
		this.messageHandlers.add(resourceCreatedHandler);
		
		IMessageHandler resourceDeletedHandler = new MessageHandler("resourceDeleted") {
			@Override
			public void handle(String messageType, JSONObject message) {
				deleteResource(message);
			}
		};
		this.messagingConnector.addMessageHandler(resourceDeletedHandler);
		this.messageHandlers.add(resourceDeletedHandler);
		
		IMessageHandler getResourceRequestHandler = new MessageHandler("getResourceRequest") {
			@Override
			public void handle(String messageType, JSONObject message) {
				try {
					final String resourcePath = message.getString("resource");
					
					if (resourcePath.startsWith("classpath:")) {
						getClasspathResource(message);
					}
					else {
						getResource(message);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		this.messagingConnector.addMessageHandler(getResourceRequestHandler);
		this.messageHandlers.add(getResourceRequestHandler);
		
		IMessageHandler getResourceResponseHandler = new CallbackIDAwareMessageHandler("getResourceResponse", Repository.GET_RESOURCE_CALLBACK) {
			@Override
			public void handle(String messageType, JSONObject message) {
				getResourceResponse(message);
			}
		};
		this.messagingConnector.addMessageHandler(getResourceResponseHandler);
		this.messageHandlers.add(getResourceResponseHandler);
		
		IMessageHandler getMetadataRequestHandler = new MessageHandler("getMetadataRequest") {
			@Override
			public void handle(String messageType, JSONObject message) {
				getMetadata(message);
			}
		};
		this.messagingConnector.addMessageHandler(getMetadataRequestHandler);
		this.messageHandlers.add(getMetadataRequestHandler);
	}
	
	public String getUsername() {
		return this.username;
	}

	public ConnectedProject getProject(IProject project) {
		return getProject(project.getName());
	}
	
	public ConnectedProject getProject(String projectName) {
		return this.syncedProjects.get(projectName);
	}

	public boolean isConnected(IProject project) {
		return isConnected(project.getName());
	}

	public boolean isConnected(String projectName) {
		Project project = this.repository.getProject(projectName);
		return this.repository.getSynchronizedProjects().contains(project);
	}

	public void addProject(IProject project) {
		this.repository.addProject(project.getName(), project.getLocationURI().getPath());
		notifyProjectConnected(project);
	}

	public void removeProject(IProject project) {
		this.repository.removeProject(project.getName());
		notifyProjectDisonnected(project);
	}

	protected void syncConnectedProjects(String projectName) {
		try {
			JSONObject message = new JSONObject();
			message.put("username", this.username);
			message.put("project", projectName);
			message.put("includeDeleted", true);
			message.put("callback_id", GET_PROJECT_CALLBACK);
			messagingConnector.send("getProjectRequest", message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ConnectedProject[] getConnectedProjects() {
		return syncedProjects.values().toArray(
				new ConnectedProject[syncedProjects.size()]);
	}

	public void getResource(JSONObject request) {
		try {
			final String username = request.getString("username");
			final int callbackID = request.getInt("callback_id");
			final String sender = request.getString("requestSenderID");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null && connectedProject.containsResource(resourcePath)) {
				IProject project = connectedProject.getProject();

				if (request.has("timestamp") && request.getLong("timestamp") != connectedProject.getTimestamp(resourcePath)) {
					return;
				}

				IResource resource = project.findMember(resourcePath);

				JSONObject message = new JSONObject();
				message.put("callback_id", callbackID);
				message.put("requestSenderID", sender);
				message.put("username", this.username);
				message.put("project", projectName);
				message.put("resource", resourcePath);
				message.put("timestamp", connectedProject.getTimestamp(resourcePath));
				message.put("hash", connectedProject.getHash(resourcePath));

				if (resource instanceof IFile) {
					if (request.has("hash") && !request.getString("hash").equals(connectedProject.getHash(resourcePath))) {
						return;
					}

					IFile file = (IFile) resource;

					ByteArrayOutputStream array = new ByteArrayOutputStream();
					if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
						file.refreshLocal(IResource.DEPTH_ZERO, null);
					}
					
					IOUtils.copy(file.getContents(), array);

					String content = new String(array.toByteArray(), file.getCharset());

					message.put("content", content);
					message.put("type", "file");
				} else if (resource instanceof IFolder) {
					message.put("type", "folder");
				}

				messagingConnector.send("getResourceResponse", message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getClasspathResource(JSONObject request) {
		try {
			final int callbackID = request.getInt("callback_id");
			final String sender = request.getString("requestSenderID");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");
			final String username = request.getString("username");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				String typeName = resourcePath.substring("classpath:/".length());
				if (typeName.endsWith(".class")) {
					typeName = typeName.substring(0, typeName.length() - ".class".length());
				}
				typeName = typeName.replace('/', '.');

				IJavaProject javaProject = JavaCore.create(connectedProject.getProject());
				if (javaProject != null) {
					IType type = javaProject.findType(typeName);
					IClassFile classFile = type.getClassFile();
					if (classFile != null && classFile.getSourceRange() != null) {

						JSONObject message = new JSONObject();
						message.put("callback_id", callbackID);
						message.put("requestSenderID", sender);
						message.put("username", this.username);
						message.put("project", projectName);
						message.put("resource", resourcePath);
						message.put("readonly", true);

						String content = classFile.getSource();

						message.put("content", content);
						message.put("type", "file");

						messagingConnector.send("getResourceResponse", message);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateResource(JSONObject request) {
		try {
			final String username = request.getString("username");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");
			final long updateTimestamp = request.getLong("timestamp");
			final String updateHash = request.optString("hash");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);

				if (resource != null && resource instanceof IFile) {
					String localHash = connectedProject.getHash(resourcePath);
					long localTimestamp = connectedProject.getTimestamp(resourcePath);

					if (localHash != null && !localHash.equals(updateHash) && localTimestamp < updateTimestamp) {
						JSONObject message = new JSONObject();
						message.put("callback_id", GET_RESOURCE_CALLBACK);
						message.put("username", this.username);
						message.put("project", projectName);
						message.put("resource", resourcePath);
						message.put("timestamp", updateTimestamp);
						message.put("hash", updateHash);

						messagingConnector.send("getResourceRequest", message);
						notifyResourceChanged(resource);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createResource(JSONObject request) {
		try {
			final String username = request.getString("username");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");
			final long updateTimestamp = request.getLong("timestamp");
			final String updateHash = request.optString("hash");
			final String type = request.optString("type");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);
				
				if (resource == null) {
					if ("folder".equals(type)) {
						IFolder newFolder = project.getFolder(resourcePath);
						
						connectedProject.setHash(resourcePath, updateHash);
						connectedProject.setTimestamp(resourcePath, updateTimestamp);

						newFolder.create(true, true, null);
						newFolder.setLocalTimeStamp(updateTimestamp);
						
						JSONObject message = new JSONObject();
						message.put("username", this.username);
						message.put("project", projectName);
						message.put("resource", resourcePath);
						message.put("timestamp", updateTimestamp);
						message.put("hash", updateHash);
						message.put("type", type);
						
						messagingConnector.send("resourceStored", message);
					}
					else if ("file".equals(type)) {
						JSONObject message = new JSONObject();
						message.put("callback_id", GET_RESOURCE_CALLBACK);
						message.put("username", this.username);
						message.put("project", projectName);
						message.put("resource", resourcePath);
						message.put("timestamp", updateTimestamp);
						message.put("hash", updateHash);
						message.put("type", type);				

						messagingConnector.send("getResourceRequest", message);
					}
				}
				else {
					// TODO
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteResource(JSONObject request) {
		try {
			final String username = request.getString("username");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");
			final long deletedTimestamp = request.getLong("timestamp");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);

				if (resource != null && resource.exists() && (resource instanceof IFile || resource instanceof IFolder)) {
					long localTimestamp = connectedProject.getTimestamp(resourcePath);

					if (localTimestamp < deletedTimestamp) {
						resource.delete(true, null);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getResourceResponse(JSONObject response) {
		try {
			final String username = response.getString("username");
			final String projectName = response.getString("project");
			final String resourcePath = response.getString("resource");
			final long updateTimestamp = response.getLong("timestamp");
			final String updateHash = response.getString("hash");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				boolean stored = false;
				
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);
				
				if (resource != null) {
					if (resource instanceof IFile) {
						String localHash = connectedProject.getHash(resourcePath);
						long localTimestamp = connectedProject.getTimestamp(resourcePath);

						if (localHash != null && !localHash.equals(updateHash) && localTimestamp < updateTimestamp) {
							IFile file = (IFile) resource;
							String newResourceContent = response.getString("content");

							connectedProject.setTimestamp(resourcePath, updateTimestamp);
							connectedProject.setHash(resourcePath, updateHash);

							file.setContents(new ByteArrayInputStream(newResourceContent.getBytes()), true, true, null);
							file.setLocalTimeStamp(updateTimestamp);
							stored = true;
						}
					}
				}
				else {
					IFile newFile = project.getFile(resourcePath);
					String newResourceContent = response.getString("content");

					connectedProject.setHash(resourcePath, updateHash);
					connectedProject.setTimestamp(resourcePath, updateTimestamp);

					newFile.create(new ByteArrayInputStream(newResourceContent.getBytes()), true, null);
					newFile.setLocalTimeStamp(updateTimestamp);
					stored = true;
				}
				
				if (stored) {
					JSONObject message = new JSONObject();
					message.put("username", this.username);
					message.put("project", connectedProject.getName());
					message.put("resource", resourcePath);
					message.put("timestamp", updateTimestamp);
					message.put("hash", updateHash);
					message.put("type", "file");
					messagingConnector.send("resourceStored", message);
					if (resource != null) {
						notifyResourceChanged(resource);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getMetadata(JSONObject request) {
		try {
			final String username = request.getString("username");
			final int callbackID = request.getInt("callback_id");
			final String sender = request.getString("requestSenderID");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);

				JSONObject message = new JSONObject();
				message.put("callback_id", callbackID);
				message.put("requestSenderID", sender);
				message.put("username", this.username);
				message.put("project", projectName);
				message.put("resource", resourcePath);
				message.put("type", "marker");

				IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
				String markerJSON = toJSON(markers);
				JSONArray content = new JSONArray(markerJSON);
				message.put("metadata", content);

				messagingConnector.send("getMetadataResponse", message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void resourceChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		if (project != null) {
			if (isConnected(project)) {
				reactToResourceChange(delta);
			}
		}
	}

	public void metadataChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
		if (project != null && isConnected(project) && markerDeltas != null && markerDeltas.length > 0) {
			sendMetadataUpdate(delta.getResource());
		}
	}

	public void reactToResourceChange(IResourceDelta delta) {
		IResource resource = delta.getResource();

		if (resource != null && resource.isDerived(IResource.CHECK_ANCESTORS)) {
			return;
		}

		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			reactOnResourceAdded(resource);
			break;
		case IResourceDelta.REMOVED:
			reactOnResourceRemoved(resource);
			break;
		case IResourceDelta.CHANGED:
			reactOnResourceChange(resource);
			break;
		}
	}

	protected void reactOnResourceAdded(IResource resource) {
		try {
			ConnectedProject connectedProject = this.syncedProjects.get(resource.getProject().getName());

			String resourcePath = resource.getProjectRelativePath().toString();
			long timestamp = resource.getLocalTimeStamp();
			String hash = "0";
			String type = null;

			connectedProject.setTimestamp(resourcePath, timestamp);

			if (resource instanceof IFile) {
				try {
					IFile file = (IFile) resource;
					hash = DigestUtils.shaHex(file.getContents());
					type = "file";
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (resource instanceof IFolder) {
				type = "folder";
			}

			connectedProject.setHash(resourcePath, hash);

			JSONObject createdMessage = new JSONObject();
			createdMessage.put("username", this.username);
			createdMessage.put("project", connectedProject.getName());
			createdMessage.put("resource", resourcePath);
			createdMessage.put("timestamp", timestamp);
			createdMessage.put("hash", hash);
			createdMessage.put("type", type);
			messagingConnector.send("resourceCreated", createdMessage);
			
			JSONObject storedMessage = new JSONObject();
			storedMessage.put("username", this.username);
			storedMessage.put("project", connectedProject.getName());
			storedMessage.put("resource", resourcePath);
			storedMessage.put("timestamp", timestamp);
			storedMessage.put("hash", hash);
			storedMessage.put("type", type);
			messagingConnector.send("resourceStored", storedMessage);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void reactOnResourceRemoved(IResource resource) {
		if (resource instanceof IProject) {
			this.removeProject((IProject) resource);
		}
		else if (!resource.isDerived() && (resource instanceof IFile || resource instanceof IFolder)) {
			ConnectedProject connectedProject = this.syncedProjects.get(resource.getProject().getName());
			String resourcePath = resource.getProjectRelativePath().toString();
			long deletedTimestamp = System.currentTimeMillis();
			
			try {
				JSONObject message = new JSONObject();
				message.put("username", this.username);
				message.put("project", connectedProject.getName());
				message.put("resource", resourcePath);
				message.put("timestamp", deletedTimestamp);
	
				messagingConnector.send("resourceDeleted", message);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void reactOnResourceChange(IResource resource) {
		if (resource != null && resource instanceof IFile) {
			IFile file = (IFile) resource;

			ConnectedProject connectedProject = this.syncedProjects.get(file.getProject().getName());
			String resourcePath = resource.getProjectRelativePath().toString();

			try {

				long changeTimestamp = file.getLocalTimeStamp();
				if (changeTimestamp > connectedProject.getTimestamp(resourcePath)) {
					String changeHash = DigestUtils.shaHex(file.getContents());
					if (!changeHash.equals(connectedProject.getHash(resourcePath))) {

						connectedProject.setTimestamp(resourcePath, changeTimestamp);
						connectedProject.setHash(resourcePath, changeHash);

						JSONObject changedMessage = new JSONObject();
						changedMessage.put("username", this.username);
						changedMessage.put("project", connectedProject.getName());
						changedMessage.put("resource", resourcePath);
						changedMessage.put("timestamp", changeTimestamp);
						changedMessage.put("hash", changeHash);
						messagingConnector.send("resourceChanged", changedMessage);
						
						JSONObject storedMessage = new JSONObject();
						storedMessage.put("username", this.username);
						storedMessage.put("project", connectedProject.getName());
						storedMessage.put("resource", resourcePath);
						storedMessage.put("timestamp", changeTimestamp);
						storedMessage.put("hash", changeHash);
						messagingConnector.send("resourceStored", storedMessage);

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMetadataUpdate(IResource resource) {
		try {
			String project = resource.getProject().getName();
			String resourcePath = resource.getProjectRelativePath().toString();

			JSONObject message = new JSONObject();
			message.put("username", this.username);
			message.put("project", project);
			message.put("resource", resourcePath);
			message.put("type", "marker");

			IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
			String markerJSON = toJSON(markers);
			JSONArray content = new JSONArray(markerJSON);
			message.put("metadata", content);

			messagingConnector.send("metadataChanged", message);
		} catch (Exception e) {

		}
	}

	public String toJSON(IMarker[] markers) {
		StringBuilder result = new StringBuilder();
		boolean flag = false;
		result.append("[");
		for (IMarker m : markers) {
			if (flag) {
				result.append(",");
			}

			result.append("{");
			result.append("\"description\":" + JSONObject.quote(m.getAttribute("message", "")));
			result.append(",\"line\":" + m.getAttribute("lineNumber", 0));
			result.append(",\"severity\":\"" + (m.getAttribute("severity", IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR ? "error" : "warning")
					+ "\"");
			result.append(",\"start\":" + m.getAttribute("charStart", 0));
			result.append(",\"end\":" + m.getAttribute("charEnd", 0));
			result.append("}");

			flag = true;
		}
		result.append("]");
		return result.toString();
	}
	
	public void addRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}
	
	public void removeRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}
	
	protected void notifyProjectConnected(IProject project) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.projectConnected(project);
		}
	}

	protected void notifyProjectDisonnected(IProject project) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.projectDisconnected(project);
		}
	}
	
	protected void notifyResourceChanged(IResource resource) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.resourceChanged(resource);
		}
	}
	
	public void dispose() {
		connected.set(false);
		for (IMessageHandler messageHandler : messageHandlers) {
			messagingConnector.removeMessageHandler(messageHandler);
		}
		syncedProjects.clear();
	}

}
