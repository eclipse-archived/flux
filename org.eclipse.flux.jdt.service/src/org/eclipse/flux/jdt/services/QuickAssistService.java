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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.core.DocumentAdapter;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.json.JSONException;
import org.json.JSONObject;

public class QuickAssistService {
	private MessageConnector MessageConnector;
	private LiveEditUnits liveEditUnits;
	private IMessageHandler quickfixRequestHandler;
	
	DocumentListener documentListener;
	
	class DocumentListener implements IDocumentListener {

		DocumentEvent event = null;
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			this.event = event;
		}
		
		public DocumentEvent getDocumentEvent() {
			return this.event;
		}
		
	}

	public QuickAssistService(MessageConnector MessageConnector, LiveEditUnits liveEditUnits) {
		this.MessageConnector = MessageConnector;
		this.liveEditUnits = liveEditUnits;
		this.quickfixRequestHandler = new MessageHandler("quickfixrequest") {

			@Override
			public void handle(String type, JSONObject message) {
				 handleQuickAssist(message);
				
			}
		};
		MessageConnector.addMessageHandler(this.quickfixRequestHandler);
		this.documentListener = new DocumentListener();
	}

	protected void handleQuickAssist(JSONObject message) {
		try {
			String username = message.getString("username");
			String projectName = message.getString("project");
			String resourcePath = message.getString("resource");
			boolean applyFix = false;
			try {
				applyFix = Boolean.parseBoolean(message.getString("apply-fix"));
			} catch(Exception e) {}
			int callbackID = message.getInt("callback_id");
			String liveEditID = projectName + "/" + resourcePath;
			if (liveEditUnits.isLiveEditResource(username, liveEditID)) {
				int offset = message.getInt("offset");
				int length = message.getInt("length");
				int problemID = message.getInt("id");
				String sender = message.getString("requestSenderID");
				JSONObject assistResult = getAssistProposals(username, liveEditID, problemID, offset, length, applyFix);
				if (applyFix) {
					if (this.documentListener.event != null) {
						JSONObject responseMessage = new JSONObject();
						responseMessage.put("username", username);
						responseMessage.put("project", projectName);
						responseMessage.put("resource", resourcePath);
						responseMessage.put("offset", this.documentListener.event.getOffset());
						responseMessage.put("removedCharCount", this.documentListener.event.getDocument().getLength());
						responseMessage.put("addedCharacters", this.documentListener.event.getText());
						System.out.println(responseMessage);
						this.MessageConnector.send("liveResourceChanged", responseMessage);
					} else {
						// TODO
					}
				} else {
					if (assistResult != null) {
						JSONObject responseMessage = new JSONObject();
						responseMessage.put("username", username);
						responseMessage.put("project", projectName);
						responseMessage.put("resource", resourcePath);
						responseMessage.put("callback_id", callbackID);
						responseMessage.put("requestSenderID", sender);
						responseMessage.put("quickfix", assistResult);
						MessageConnector.send("quickfixresponse", responseMessage);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	protected static IProblemLocation filterOutProblems(IProblem[] problems, int id) {
		for (int i= 0; i < problems.length; i++) {
			System.out.println(problems[i].getID());
			if (problems[i].getID() == id) return new ProblemLocation(problems[i]);
		}
		return null;
	}
	
	protected static IProblemLocation[] convertProblems(IProblem[] problems) {
		IProblemLocation[] result= new IProblemLocation[problems.length];

		for (int i= 0; i < problems.length; i++) {
			result[i]= new ProblemLocation(problems[i]);
		}

		return result;
	}

	public JSONObject getAssistProposals(String username, String requestorResourcePath, int problemID, int offset, int length, boolean applyFix) {
		try {
			ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(username, requestorResourcePath);
			if (liveEditUnit != null) {
				IProblemLocation location = getProblemLocations(liveEditUnit, problemID, offset, length);
				return applyProposals(offset, length, applyFix, liveEditUnit, location);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
		}
		return null;
	}
	
	public IProblemLocation getProblemLocations(ICompilationUnit liveEditUnit, int problemID, int offset, int length) {
		final ASTParser parser = ASTParser.newParser(AST.JLS4);

		// Parse the class as a compilation unit.
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(liveEditUnit);
		parser.setResolveBindings(true);

		// Return the compiled class as a compilation unit
		final CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		IProblem[] problems = unit.getProblems();
		if (problemID > 0) {
			return filterOutProblems(problems, problemID);
		} else {
			IProblemLocation[] locations = convertProblems(unit.getProblems());
			for (IProblemLocation iProblemLocation : locations) {
				if (offset >= iProblemLocation.getOffset() && offset <= (iProblemLocation.getOffset() + iProblemLocation.getLength()) ) {
					return iProblemLocation;
				}
			}
		}
		return null;
	}
	
	private JSONObject applyProposals(int offset, int length, boolean applyFix,	ICompilationUnit liveEditUnit, IProblemLocation problem)
			throws CoreException, JavaModelException, JSONException {
		IInvocationContext context = new AssistContext(liveEditUnit, offset, length);
		QuickFixProcessor processor = new QuickFixProcessor();
		IJavaCompletionProposal[] proposals = processor.getCorrections(context, new IProblemLocation[]{problem});
		
		if (proposals == null || proposals.length == 0) {
			return null;
		}
		
		if (applyFix) {	
			IBuffer buffer = liveEditUnit.getBuffer();
			if (buffer != null) {
				IDocument document = buffer instanceof IDocument ? (IDocument) buffer : new DocumentAdapter(buffer);
				
				if (proposals[0] instanceof CUCorrectionProposal) {
					CUCorrectionProposal proposal = (CUCorrectionProposal) proposals[0];
					String preview = proposal.getPreviewContent();
					System.out.println(document.getLength());
					System.out.println(preview.length());
					try {
						document.addDocumentListener(this.documentListener);
						document.replace(0, preview.length(), preview);
						//proposal.apply(document);
					liveEditUnit.getBuffer().setContents(proposal.getPreviewContent());
					liveEditUnit.reconcile(ICompilationUnit.NO_AST, true, null, null);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
			return null;

		} else {
			List<JSONObject> jsonProposals = new ArrayList<JSONObject>(proposals.length);
			for (IJavaCompletionProposal proposal : proposals) {
				JSONObject jsonDescription = getDescription(proposal);
				JSONObject jsonProposal = new JSONObject();
				jsonProposal.put("description", jsonDescription);
				jsonProposal.put("relevance", proposal.getRelevance());
				jsonProposals.add(jsonProposal);
			}

			JSONObject result = new JSONObject();
			result.put("quickfix", jsonProposals);
			return result;
		}
	}
	
	protected JSONObject getDescription(IJavaCompletionProposal proposal) throws JSONException {
		JSONObject description = new JSONObject();
		description.put("display", proposal.getDisplayString());
		return description;
	}

	public void dispose() {
		this.MessageConnector.removeMessageHandler(quickfixRequestHandler);
	}
}
