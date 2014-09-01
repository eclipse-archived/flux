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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.flux.core.AbstractMessageHandler;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles Java content assist requests coming from message bus by invoking JDT content assist engine.
 * @author Martin Lippert
 */
public class ContentAssistService {

	private LiveEditUnits liveEditUnits;
	private IMessagingConnector messagingConnector;
	private IMessageHandler contentAssistRequestHandler;

	public ContentAssistService(IMessagingConnector messagingConnector, LiveEditUnits liveEditUnits) {
		this.messagingConnector = messagingConnector;
		this.liveEditUnits = liveEditUnits;

		this.contentAssistRequestHandler = new AbstractMessageHandler("contentassistrequest") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleContentAssistRequest(message);
			}
		};
		messagingConnector.addMessageHandler(this.contentAssistRequestHandler);
	}

	protected void handleContentAssistRequest(JSONObject message) {
		try {
			String username = message.getString("username");
			String projectName = message.getString("project");
			String resourcePath = message.getString("resource");
			int callbackID = message.getInt("callback_id");

			String liveEditID = projectName + "/" + resourcePath;
			if (liveEditUnits.isLiveEditResource(username, liveEditID)) {

				int offset = message.getInt("offset");
				String prefix = message.optString("prefix");
				String sender = message.getString("requestSenderID");

				JSONObject responseMessage = new JSONObject();
				responseMessage.put("username", username);
				responseMessage.put("project", projectName);
				responseMessage.put("resource", resourcePath);
				responseMessage.put("callback_id", callbackID);
				responseMessage.put("requestSenderID", sender);

				responseMessage.put("proposals", computeContentAssist(username, liveEditID, offset, prefix));

				messagingConnector.send("contentassistresponse", responseMessage);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected JSONArray computeContentAssist(String username, String resourcePath, int offset, String prefix) throws JSONException {
		final List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
		final CompletionContext[] completionContextParam = new CompletionContext[] { null };

		ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(username, resourcePath);
		try {
			if (liveEditUnit != null) {
				CompletionRequestor collector = new CompletionRequestor() {
					@Override
					public void accept(CompletionProposal proposal) {
						proposals.add(proposal);
					}

					@Override
					public void acceptContext(CompletionContext context) {
						super.acceptContext(context);
						completionContextParam[0] = context;
					}
					
				};
				
				// Allow completions for unresolved types - since 3.3
				collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF, true);
				collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_IMPORT, true);
				collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.FIELD_IMPORT, true);

				collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_REF, true);
				collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT, true);
				collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT, true);

				collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

				collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);
				collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, CompletionProposal.TYPE_REF, true);

				collector.setAllowsRequiredProposals(CompletionProposal.TYPE_REF, CompletionProposal.TYPE_REF, true);
				
				liveEditUnit.codeComplete(offset, collector, new NullProgressMonitor());
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		List<JSONObject> jsonProposals = new ArrayList<JSONObject>(proposals.size());
		CompletionContext completionContext = completionContextParam[0];
		for (CompletionProposal proposal : proposals) {
			JSONObject jsonDescription = getDescription(proposal, completionContext);
			List<Integer> positionsList = new ArrayList<Integer>();
			StringBuilder jsonCompletion = new CompletionProposalReplacementProvider(liveEditUnit, proposal, completionContext, offset, prefix).createReplacement(positionsList);
			
			JSONObject jsonProposal = new JSONObject();
			jsonProposal.put("description", jsonDescription);
			jsonProposal.put("proposal", jsonCompletion);
			if (positionsList != null && !positionsList.isEmpty()) {
				jsonProposal.put("positions", getPositions(positionsList));
			}
			jsonProposal.put("style", "attributedString");
			jsonProposal.put("replace", true);
			jsonProposal.put("relevance", proposal.getRelevance());
			
			jsonProposals.add(jsonProposal);
		}
		
		Collections.sort(jsonProposals, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				try {
					int diff = o2.getInt("relevance") - o1.getInt("relevance");
					if (diff == 0) {
						JSONArray nameDescription1 = o1.getJSONObject("description").getJSONArray("segments");
						JSONArray nameDescription2 = o2.getJSONObject("description").getJSONArray("segments");
						StringBuilder nameBuffer1 = new StringBuilder();
						for (int i = 0; i < nameDescription1.length(); i++) {
							nameBuffer1.append(nameDescription1.getJSONObject(i).getString("value"));
						}
						StringBuilder nameBuffer2 = new StringBuilder();
						for (int i = 0; i < nameDescription2.length(); i++) {
							nameBuffer2.append(nameDescription2.getJSONObject(i).getString("value"));
						}
						return nameBuffer1.toString().compareTo(nameBuffer2.toString());
					} else {
						return diff;
					}
				} catch (JSONException e) {
					return -1;
				}
			}
		});

		return new JSONArray(jsonProposals);
	}
	
	private JSONArray getPositions(List<Integer> positionsList) throws JSONException {
		if (positionsList != null && positionsList.size() % 2 == 0) {
			JSONArray jsonPositions = new JSONArray();
			for (int i = 0; i < positionsList.size(); i += 2) {
				JSONObject position = new JSONObject();
				position.put("offset", positionsList.get(i));
				position.put("length", positionsList.get(i + 1));
				jsonPositions.put(position);
			}
			return jsonPositions;
		} else {
			return null;
		}
	}
	
	protected JSONObject getDescription(CompletionProposal proposal, CompletionContext context) throws JSONException {
		CompletionProposalDescriptionProvider provider = new CompletionProposalDescriptionProvider(context);
		JSONObject description = new JSONObject();
		/*
		 * Add icon field for now. Possibly needs to be moved to a client side
		 */
		if (proposal.getKind() == CompletionProposal.METHOD_REF) {
			JSONObject src = new JSONObject();
			src.put("src", "../js/editor/textview/methpub_obj.gif");
			description.put("icon", src);
		} else if (proposal.getKind() == CompletionProposal.FIELD_REF) {
			JSONObject src = new JSONObject();
			src.put("src", "../js/editor/textview/field_public_obj.gif");
			description.put("icon", src);
		} else if (proposal.getKind() == CompletionProposal.TYPE_REF) {
			JSONObject src = new JSONObject();
			src.put("src", "../js/editor/textview/class_obj.gif");
			description.put("icon", src);
		}
		
		description.put("segments", new JSONArray(provider.createDescription(proposal).toString()));
		description.put("metadata", new JSONObject(provider.createMetadata(proposal)));
		return description;
	}
	
	public void dispose() {
		messagingConnector.removeMessageHandler(contentAssistRequestHandler);
	}
}