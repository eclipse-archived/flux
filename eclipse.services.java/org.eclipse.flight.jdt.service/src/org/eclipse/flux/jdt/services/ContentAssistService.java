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
import org.eclipse.flux.core.AbstractMessageHandler;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
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

	public ContentAssistService(IMessagingConnector messagingConnector, LiveEditUnits liveEditUnits) {
		this.messagingConnector = messagingConnector;
		this.liveEditUnits = liveEditUnits;

		IMessageHandler contentAssistRequestHandler = new AbstractMessageHandler("contentassistrequest") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleContentAssistRequest(message);
			}
		};
		messagingConnector.addMessageHandler(contentAssistRequestHandler);
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

				String proposalsSource = computeContentAssist(username, liveEditID, offset, prefix);

				JSONObject responseMessage = new JSONObject();
				responseMessage.put("username", username);
				responseMessage.put("project", projectName);
				responseMessage.put("resource", resourcePath);
				responseMessage.put("callback_id", callbackID);
				responseMessage.put("requestSenderID", sender);

				JSONArray proposals = new JSONArray(proposalsSource);
				responseMessage.put("proposals", proposals);

				messagingConnector.send("contentassistresponse", responseMessage);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected String computeContentAssist(String username, String resourcePath, int offset, String prefix) {
		final List<CompletionProposal> proposals = new ArrayList<CompletionProposal>();

		try {
			ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(username, resourcePath);
			if (liveEditUnit != null) {
				liveEditUnit.codeComplete(offset, new CompletionRequestor() {
					@Override
					public void accept(CompletionProposal proposal) {
						proposals.add(proposal);
					}
				});
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		Collections.sort(proposals, new Comparator<CompletionProposal>() {
			@Override
			public int compare(CompletionProposal o1, CompletionProposal o2) {
				return o2.getRelevance() - o1.getRelevance();
			}
		});

		StringBuilder result = new StringBuilder();
		boolean flag = false;
		result.append("[");
		for (CompletionProposal proposal : proposals) {
			String description = getDescription(proposal);
			String completion = getCompletion(proposal, prefix);
			String positions = getPositions(proposal, prefix, offset);

			if (description != null) {
				if (flag) {
					result.append(",");
				}

				result.append("{");
				result.append("\"proposal\"");
				result.append(":");
				result.append("\"");
				result.append(completion);
				result.append("\",");
				result.append("\"description\"");
				result.append(":");
				result.append(description);
				result.append(",");

				if (positions != null) {
					result.append("\"positions\"");
					result.append(":");
					result.append(positions);
					result.append(",");
				}

				result.append("\"style\":\"attributedString\",");
				result.append("\"replace\"");
				result.append(":");
				result.append("true");
				result.append("}");

				flag = true;
			}
		}
		result.append("]");
		return result.toString();
	}

	private String getPositions(CompletionProposal proposal, String prefix, int globalOffset) {
		if (proposal.getKind() == CompletionProposal.METHOD_REF) {
			String completion = new String(proposal.getCompletion());
			if (completion.startsWith(prefix)) {
				completion = completion.substring(prefix.length());
			}

			StringBuilder positions = new StringBuilder();
			positions.append("[");

			char[][] parameterNames = proposal.findParameterNames(null);
			if (parameterNames != null && parameterNames.length > 0 && completion.endsWith(")")) {
				int offset = globalOffset;
				offset += completion.length() - 1;

				for (int i = 0; i < parameterNames.length; i++) {
					if (i > 0) {
						positions.append(",");
					}
					positions.append("{");
					positions.append("\"offset\"");
					positions.append(":");
					positions.append(offset);

					positions.append(",");
					positions.append("\"length\"");
					positions.append(":");
					positions.append(parameterNames[i].length);

					positions.append("}");

					offset += parameterNames[i].length;
					offset += ", ".length();
				}
			}

			positions.append("]");
			return positions.toString();
		} else {
			return null;
		}
	}

	private String getCompletion(CompletionProposal proposal, String prefix) {
		String completion = new String(proposal.getCompletion());
		if (completion.startsWith(prefix)) {
			completion = completion.substring(prefix.length());
		}

		if (proposal.getKind() == CompletionProposal.METHOD_REF) {
			char[][] parameterNames = proposal.findParameterNames(null);
			if (parameterNames != null && parameterNames.length > 0 && completion.endsWith(")")) {
				completion = completion.substring(0, completion.length() - 1);
				for (int i = 0; i < parameterNames.length; i++) {
					if (i > 0) {
						completion += ", ";
					}
					completion += new String(parameterNames[i]);
				}
				completion += ")";
			}
		}

		return completion;
	}

	protected String getDescription(CompletionProposal proposal) {
		StringBuilder description = new StringBuilder();
		description.append("{");

		if (proposal.getKind() == CompletionProposal.METHOD_REF) {
			description.append("\"icon\":{\"src\":\"../js/editor/textview/methpub_obj.gif\"},");
			description.append("\"segments\": ");
			description.append("[");

			char[][] parameterNames = proposal.findParameterNames(null);
			String[] parameters = new String[parameterNames.length];
			for (int i = 0; i < parameterNames.length; i++) {
				parameters[i] = new String(parameterNames[i]);
			}

			String sig = Signature.toString(new String(proposal.getSignature()), new String(proposal.getName()), parameters, false, false);

			description.append("{");
			String result = sig + " : " + Signature.getSimpleName(Signature.toString(Signature.getReturnType(new String(proposal.getSignature()))));
			description.append("\"value\":\"" + result + "\"");
			description.append("}");

			description.append(",");
			description.append("{");
			String appendix = " - " + Signature.getSignatureSimpleName(new String(proposal.getDeclarationSignature()));
			description.append("\"value\":\"" + appendix + "\",");
			description.append("\"style\":{");
			description.append("\"color\":\"#AAAAAA\"");
			description.append("}");
			description.append("}");

			description.append("]");

		} else if (proposal.getKind() == CompletionProposal.FIELD_REF) {
			description.append("\"icon\":{\"src\":\"../js/editor/textview/field_public_obj.gif\"},");
			description.append("\"segments\": ");
			description.append("[");

			description.append("{");
			String result = new String(proposal.getCompletion()) + " : " + (proposal.getSignature() != null ? Signature.getSignatureSimpleName(new String(proposal.getSignature())) : "<unknown>");
			description.append("\"value\":\"" + result + "\"");
			description.append("}");

			description.append(",");
			description.append("{");
			String appendix = " - " + (proposal.getDeclarationSignature() != null ? Signature.getSignatureSimpleName(new String(proposal.getDeclarationSignature())) : "<unknown>");
			description.append("\"value\":\"" + appendix + "\",");
			description.append("\"style\":{");
			description.append("\"color\":\"#AAAAAA\"");
			description.append("}");
			description.append("}");

			description.append("]");

		} else if (proposal.getKind() == CompletionProposal.TYPE_REF) {
			if (proposal.getAccessibility() == IAccessRule.K_NON_ACCESSIBLE) {
				return null;
			}

			description.append("\"icon\":{\"src\":\"../js/editor/textview/class_obj.gif\"},");
			description.append("\"segments\": ");
			description.append("[");

			description.append("{");
			String result = Signature.getSignatureSimpleName(new String(proposal.getSignature()));
			description.append("\"value\":\"" + result + "\"");
			description.append("}");

			description.append(",");
			description.append("{");
			String appendix = " - " + new String(proposal.getDeclarationSignature());
			description.append("\"value\":\"" + appendix + "\",");
			description.append("\"style\":{");
			description.append("\"color\":\"#AAAAAA\"");
			description.append("}");
			description.append("}");

			description.append("]");

		} else {
			return null;
		}

		description.append("}");
		return description.toString();
	}
}