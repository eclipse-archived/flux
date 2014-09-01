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
package org.eclipse.flux.jdt.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.flux.core.CallbackIDAwareMessageHandler;
import org.eclipse.flux.core.ILiveEditConnector;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.IRepositoryListener;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages the lifecycle of JDT working copies for files that are currently being edited remotely.
 * @author Martin Lippert
 */
public class LiveEditUnits {

	private static final String LIVE_EDIT_CONNECTOR_ID = "JDT-Service-Live-Edit-Connector";
	private static int GET_LIVE_RESOURCES_CALLBACK = "LiveEditUnits - getLiveResourcesCallback".hashCode();

	private ConcurrentMap<String, ICompilationUnit> liveEditUnits;
	private Repository repository;
	private IMessagingConnector messagingConnector;
	private LiveEditCoordinator liveEditCoordinator;
	
	private ILiveEditConnector liveEditConnector;
	private IRepositoryListener repositoryListener;
	private IResourceChangeListener metadataChangeListener;
	private IMessageHandler liveResourcesResponseHandler;

	public LiveEditUnits(IMessagingConnector messagingConnector, LiveEditCoordinator liveEditCoordinator, Repository repository) {
		this.messagingConnector = messagingConnector;
		this.liveEditCoordinator = liveEditCoordinator;
		this.repository = repository;

		this.liveEditUnits = new ConcurrentHashMap<String, ICompilationUnit>();

		this.liveEditConnector = new ILiveEditConnector() {
			@Override
			public String getConnectorID() {
				return LIVE_EDIT_CONNECTOR_ID;
			}

			@Override
			public void liveEditingEvent(String username, String resourcePath, int offset, int removeCount, String newText) {
				modelChanged(username, resourcePath, offset, removeCount, newText);
			}

			@Override
			public void liveEditingStarted(String requestSenderID, int callbackID, String username, String resourcePath, String hash, long timestamp) {
				startLiveUnit(requestSenderID, callbackID, username, resourcePath, hash, timestamp);
			}

			@Override
			public void liveEditingStartedResponse(String requestSenderID, int callbackID, String username, String projectName, String resourcePath, String savePointHash, long savePointTimestamp, String content) {
				updateLiveUnit(requestSenderID, callbackID, username, projectName, resourcePath, savePointHash, savePointTimestamp, content);
			}

			@Override
			public void liveEditors(String requestSenderID, int callbackID,
					String username, String projectRegEx, String resourceRefEx) {
				// Do nothing since JDT service is not a host for editors
			}
		};
		liveEditCoordinator.addLiveEditConnector(this.liveEditConnector);
		
		this.repositoryListener = new IRepositoryListener() {
			@Override
			public void projectConnected(IProject project) {
				startupConnectedProject(project);
			}

			@Override
			public void projectDisconnected(IProject project) {
			}

			@Override
			public void resourceChanged(IResource resource) {
				// nothing
			}
		};
		this.repository.addRepositoryListener(this.repositoryListener);

		startup();
		
		this.liveResourcesResponseHandler = new CallbackIDAwareMessageHandler("getLiveResourcesResponse", GET_LIVE_RESOURCES_CALLBACK) {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				startupLiveUnits(message);
			}
		};
		messagingConnector.addMessageHandler(this.liveResourcesResponseHandler);

		this.metadataChangeListener = new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						@Override
						public boolean visit(IResourceDelta delta) throws CoreException {
							checkForLiveUnitsInvolved(delta);
							return true;
						}
					});
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this.metadataChangeListener, IResourceChangeEvent.POST_BUILD);
	}

	protected void startup() {
		try {
			JSONObject message = new JSONObject();
			message.put("username", repository.getUsername());
			message.put("callback_id", GET_LIVE_RESOURCES_CALLBACK);
			message.put("resourceRegEx", ".*\\.java|.*\\.class");
			messagingConnector.send("getLiveResourcesRequest", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void startupConnectedProject(IProject project) {
		try {
			JSONObject message = new JSONObject();
			message.put("username", repository.getUsername());
			message.put("projectRegEx", project.getName());
			message.put("callback_id", GET_LIVE_RESOURCES_CALLBACK);
			messagingConnector.send("getLiveResourcesRequest", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void disconnect() {
	}

	public boolean isLiveEditResource(String username, String resourcePath) {
		return repository.getUsername().equals(username) && liveEditUnits.containsKey(resourcePath);
	}

	public ICompilationUnit getLiveEditUnit(String username, String resourcePath) {
		if (repository.getUsername().equals(username)) {
			return liveEditUnits.get(resourcePath);
		} else {
			return null;
		}
	}

	protected void startupLiveUnits(JSONObject message) {
		try {
			String username = message.getString("username");			
			JSONObject liveUnits = message.getJSONObject("liveEditUnits");
			for (String projectName : JSONObject.getNames(liveUnits)) {
				JSONArray resources = liveUnits.getJSONArray(projectName);
				
				for (int i = 0; i < resources.length(); i++) {
					JSONObject liveUnit = resources.getJSONObject(i);
					
					String resource = liveUnit.getString("resource");
					long timestamp = liveUnit.getLong("savePointTimestamp");
					String hash = liveUnit.getString("savePointHash");
					
					String resourcePath = projectName + "/" + resource;
					if (repository.getUsername().equals(username) && !liveEditUnits.containsKey(resourcePath)) {
						startLiveUnit(null, 0, username, resourcePath, hash, timestamp);
					}

					this.liveEditCoordinator.sendLiveEditStartedMessage(LIVE_EDIT_CONNECTOR_ID, username, projectName, resource, hash, timestamp);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void startLiveUnit(String requestSenderID, int callbackID, String username, String resourcePath, String hash, long timestamp) {
		String projectName = resourcePath.substring(0, resourcePath.indexOf('/'));
		String relativeResourcePath = resourcePath.substring(projectName.length() + 1);
		
		if (repository.getUsername().equals(username) && resourcePath.endsWith(".java") && repository.isConnected(projectName)) {
			ICompilationUnit liveUnit = liveEditUnits.get(resourcePath);
			if (liveUnit != null) {
				try {
					String liveContent = liveUnit.getBuffer().getContents();
					String liveUnitHash = DigestUtils.shaHex(liveContent);
					if (!liveUnitHash.equals(hash)) {
						liveEditCoordinator.sendLiveEditStartedResponse(LIVE_EDIT_CONNECTOR_ID, requestSenderID, callbackID, username, projectName, relativeResourcePath, hash, timestamp, liveContent);
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			} else {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (project != null && repository.isConnected(project)) {
					IFile file = project.getFile(relativeResourcePath);
					if (file != null) {
						try {
							final LiveEditProblemRequestor liveEditProblemRequestor = new LiveEditProblemRequestor(messagingConnector, username, projectName, relativeResourcePath);
							liveUnit = ((ICompilationUnit) JavaCore.create(file)).getWorkingCopy(new WorkingCopyOwner() {
								@Override
								public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
									return liveEditProblemRequestor;
								}
							}, new NullProgressMonitor());
							liveEditUnits.put(resourcePath, liveUnit);
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
				}
			}

			if (liveUnit != null) {
				try {
					liveUnit.reconcile(ICompilationUnit.NO_AST, true, null, null);
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void updateLiveUnit(String requestSenderID, int callbackID, String username, String projectName, String resource, String savePointHash, long savePointTimestamp, String remoteContent) {
		if (repository.getUsername().equals(username) && resource.endsWith(".java") && repository.isConnected(projectName)) {
			String resourcePath = projectName + "/" + resource;

			ICompilationUnit liveUnit = liveEditUnits.get(resourcePath);
			if (liveUnit != null) {
				try {
					String liveContent = liveUnit.getBuffer().getContents();
					String liveUnitHash = DigestUtils.shaHex(liveContent);

					String remoteContentHash = DigestUtils.shaHex(remoteContent);
					if (!liveUnitHash.equals(remoteContentHash)) {
						liveUnit.getBuffer().setContents(remoteContent);
						liveUnit.reconcile(ICompilationUnit.NO_AST, true, null, null);
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void modelChanged(String username, String resourcePath, int offset, int removedCharacterCount, String newText) {
		if (repository.getUsername().equals(username) && liveEditUnits.containsKey(resourcePath)) {
			System.out.println("live edit compilation unit found");
			ICompilationUnit unit = liveEditUnits.get(resourcePath);
			try {
				IBuffer buffer = unit.getBuffer();
				buffer.replace(offset, removedCharacterCount, newText);

				if (removedCharacterCount > 0 || newText.length() > 0) {
					unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
				}

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	protected void checkForLiveUnitsInvolved(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
		if (project != null && repository.isConnected(project) && markerDeltas != null && markerDeltas.length > 0) {
			IResource resource = delta.getResource();
			String resourcePath = project.getName() + "/" + resource.getProjectRelativePath().toString();

			ICompilationUnit unit = getLiveEditUnit(repository.getUsername(), resourcePath);
			if (unit != null) {
				try {
					unit.reconcile(ICompilationUnit.NO_AST, true, null, null);
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void dispose() {
		messagingConnector.removeMessageHandler(liveResourcesResponseHandler);
		liveEditCoordinator.removeLiveEditConnector(liveEditConnector);
		repository.removeRepositoryListener(repositoryListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.metadataChangeListener);
		liveEditUnits.clear();
	}

}
