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

import org.eclipse.core.resources.IResource;
import org.eclipse.flux.core.AbstractMessageHandler;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implements "Jump to declaration" navigation for a location in a Java file.
 * @author Martin Lippert
 */
public class NavigationService {

	private LiveEditUnits liveEditUnits;
	private IMessagingConnector messagingConnector;
	private IMessageHandler navigationRequestHandler;

	public NavigationService(IMessagingConnector messagingConnector, LiveEditUnits liveEditUnits) {
		this.messagingConnector = messagingConnector;
		this.liveEditUnits = liveEditUnits;

		this.navigationRequestHandler = new AbstractMessageHandler("navigationrequest") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleNavigationRequest(message);
			}
		};
		messagingConnector.addMessageHandler(this.navigationRequestHandler);
	}

	protected void handleNavigationRequest(JSONObject message) {
		try {
			String username = message.getString("username");
			String projectName = message.getString("project");
			String resourcePath = message.getString("resource");
			int callbackID = message.getInt("callback_id");

			String liveEditID = projectName + "/" + resourcePath;
			if (liveEditUnits.isLiveEditResource(username, liveEditID)) {

				int offset = message.getInt("offset");
				int length = message.getInt("length");
				String sender = message.getString("requestSenderID");

				JSONObject navigationResult = computeNavigation(username, liveEditID, offset, length);

				if (navigationResult != null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("username", username);
					responseMessage.put("project", projectName);
					responseMessage.put("resource", resourcePath);
					responseMessage.put("callback_id", callbackID);
					responseMessage.put("requestSenderID", sender);
					responseMessage.put("navigation", navigationResult);

					messagingConnector.send("navigationresponse", responseMessage);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONObject computeNavigation(String username, String requestorResourcePath, int offset, int length) {
		try {
			ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(username, requestorResourcePath);
			if (liveEditUnit != null) {
				IJavaElement[] elements = liveEditUnit.codeSelect(offset, length);

				if (elements != null && elements.length > 0) {
					JSONObject result = new JSONObject();

					IJavaElement element = elements[0];
					IResource resource = element.getResource();

					//if the selected element corresponds to a resource in workspace, navigate to it
					if (resource != null && resource.getProject() != null) {
						String projectName = resource.getProject().getName();
						String resourcePath = resource.getProjectRelativePath().toString();

						result.put("project", projectName);
						result.put("resource", resourcePath);

						if (element instanceof ISourceReference) {
							ISourceRange nameRange = ((ISourceReference) element).getNameRange();
							result.put("offset", nameRange.getOffset());
							result.put("length", nameRange.getLength());
						}

						return result;
					}
					//walk up the java model until we reach a class file
					while (element != null && !(element instanceof IClassFile)) {
						element = element.getParent();
					}

					if (element instanceof IClassFile) {
						IClassFile classFile = (IClassFile) element;
						ISourceRange sourceRange = classFile.getSourceRange();
						if (sourceRange != null) {
							String projectName = element.getJavaProject().getProject().getName();
							String resourcePath = classFile.getParent().getElementName().replace('.', '/');
							resourcePath = "classpath:/" + resourcePath + "/" + classFile.getElementName();

							result.put("project", projectName);
							result.put("resource", resourcePath);

							return result;
						}
					}
					//we don't know how to navigate to this element
				}
			}

		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void dispose() {
		messagingConnector.removeMessageHandler(navigationRequestHandler);
	}

}
