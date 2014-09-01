/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.flux.jdt.services;

import java.io.IOException;

import org.eclipse.flux.core.AbstractMessageHandler;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.json.JSONException;
import org.json.JSONObject;

public class JavaDocService {
	private IMessagingConnector messagingConnector;
	private LiveEditUnits liveEditUnits;
	private IMessageHandler javadocRequestHandler;

	public JavaDocService(IMessagingConnector messagingConnector, LiveEditUnits liveEditUnits) {
		this.messagingConnector = messagingConnector;
		this.liveEditUnits = liveEditUnits;
		this.javadocRequestHandler = new AbstractMessageHandler("javadocrequest") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleJavadocRequest(message);
			}
		};
		messagingConnector.addMessageHandler(this.javadocRequestHandler);
	}

	protected void handleJavadocRequest(JSONObject message) {
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
				JSONObject javadocResult = getJavadoc(username, liveEditID,
						offset, length);
				if (javadocResult != null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("username", username);
					responseMessage.put("project", projectName);
					responseMessage.put("resource", resourcePath);
					responseMessage.put("callback_id", callbackID);
					responseMessage.put("requestSenderID", sender);
					responseMessage.put("javadoc", javadocResult);
					messagingConnector.send("javadocresponse", responseMessage);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONObject getJavadoc(String username, String requestorResourcePath,
			int offset, int length) {
		try {
			ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(
					username, requestorResourcePath);
			if (liveEditUnit != null) {
				IJavaElement[] elements = liveEditUnit.codeSelect(offset,
						length);
				if (elements != null && elements.length > 0) {
					JSONObject result = new JSONObject();
					IJavaElement element = elements[0];
					String javadoc = null;
					if (element instanceof IMember
							&& !((IMember) element).isBinary()) {
						javadoc = getJavadocFromSourceElement((IMember) element);
					} else {
						javadoc = element.getAttachedJavadoc(null);
					}
					result.put("javadoc", javadoc);
					return result;
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getJavadocFromSourceElement(IMember member) {
		JavaDocCommentReader reader = null;
		try {
			IBuffer buffer = member.getOpenable().getBuffer();
			if (buffer == null) {
				return null;
			}
			ISourceRange javadocRange = member.getJavadocRange();
			if (javadocRange != null) {
				reader = new JavaDocCommentReader(buffer,
						javadocRange.getOffset(), javadocRange.getOffset()
								+ javadocRange.getLength() - 1);
				StringBuffer buf = new StringBuffer();
				char[] charBuffer = new char[1024];
				int count;
				try {
					while ((count = reader.read(charBuffer)) != -1)
						buf.append(charBuffer, 0, count);
				} catch (IOException e) {
					return null;
				}
				return buf.toString();
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				reader.close();
		}
		return null;
	}
	public void dispose() {
		this.messagingConnector.removeMessageHandler(javadocRequestHandler);
	}
}