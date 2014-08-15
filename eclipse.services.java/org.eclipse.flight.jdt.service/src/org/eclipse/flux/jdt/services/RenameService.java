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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.flux.core.AbstractMessageHandler;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Responds to requests to rename a member of a Java type and update all local references.
 * @author Martin Lippert
 */
public class RenameService {

	private LiveEditUnits liveEditUnits;
	private IMessagingConnector messagingConnector;
	private IMessageHandler contentAssistRequestHandler;

	public RenameService(IMessagingConnector messagingConnector, LiveEditUnits liveEditUnits) {
		this.messagingConnector = messagingConnector;
		this.liveEditUnits = liveEditUnits;

		this.contentAssistRequestHandler = new AbstractMessageHandler("renameinfilerequest") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleRenameInFileRequest(message);
			}
		};
		messagingConnector.addMessageHandler(this.contentAssistRequestHandler);
	}

	protected void handleRenameInFileRequest(JSONObject message) {
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

				JSONArray references = computeReferences(username, liveEditID, offset, length);

				if (references != null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("username", username);
					responseMessage.put("project", projectName);
					responseMessage.put("resource", resourcePath);
					responseMessage.put("callback_id", callbackID);
					responseMessage.put("requestSenderID", sender);
					responseMessage.put("references", references);

					messagingConnector.send("renameinfileresponse", responseMessage);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONArray computeReferences(String username, String resourcePath, int offset, int length) {
		try {
			ICompilationUnit unit = liveEditUnits.getLiveEditUnit(username, resourcePath);
			if (unit != null) {
				final ASTParser parser = ASTParser.newParser(AST.JLS4);

				// Parse the class as a compilation unit.
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource(unit);
				parser.setResolveBindings(true);

				// Return the compiled class as a compilation unit
				final ASTNode compilationUnit = parser.createAST(null);
				final ASTNode nameNode = NodeFinder.perform(compilationUnit, offset, length);

				final List<ASTNode> nodes = new ArrayList<ASTNode>();

				if (nameNode instanceof SimpleName) {
					compilationUnit.accept(new ASTVisitor() {
						@Override
						public boolean visit(SimpleName node) {
							if (node.getIdentifier().equals(((SimpleName) nameNode).getIdentifier())) {
								nodes.add(node);
							}
							return super.visit(node);
						}
					});
				}

				JSONArray references = new JSONArray();
				for (ASTNode astNode : nodes) {
					JSONObject nodeObject = new JSONObject();
					nodeObject.put("offset", astNode.getStartPosition());
					nodeObject.put("length", astNode.getLength());

					references.put(nodeObject);
				}

				return references;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void dispose() {
		messagingConnector.removeMessageHandler(contentAssistRequestHandler);
	}

}
