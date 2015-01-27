/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;


/**
 * Implementation of a Java completion proposal to be used for quick fix and quick assist proposals
 * that are based on a {@link Change}. The proposal offers additional proposal information (based on
 * the {@link Change}).
 * 
 * @since 3.8
 */
public class ChangeCorrectionProposal implements IJavaCompletionProposal, ICommandAccess, ICompletionProposalExtension5, ICompletionProposalExtension6 {

	private static final NullChange COMPUTING_CHANGE= new NullChange("ChangeCorrectionProposal computing..."); //$NON-NLS-1$
	
	private Change fChange;
	private String fName;
	private int fRelevance;
	private String fCommandId;

	/**
	 * Constructs a change correction proposal.
	 * 
	 * @param name the name that is displayed in the proposal selection dialog
	 * @param change the change that is executed when the proposal is applied or <code>null</code>
	 *            if the change will be created by implementors of {@link #createChange()}
	 * @param relevance the relevance of this proposal
	 * @param image the image that is displayed for this proposal or <code>null</code> if no image
	 *            is desired
	 */
	public ChangeCorrectionProposal(String name, Change change, int relevance) {
		if (name == null) {
			throw new IllegalArgumentException("Name must not be null"); //$NON-NLS-1$
		}
		fName= name;
		fChange= change;
		fRelevance= relevance;
		fCommandId= null;
	}


	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		Object info= getAdditionalProposalInfo(new NullProgressMonitor());
		return info == null ? null : info.toString();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension5#getAdditionalProposalInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		StringBuffer buf= new StringBuffer();
		buf.append("<p>"); //$NON-NLS-1$
		try {
			Change change= getChange();
			if (change != null) {
				String name= change.getName();
				if (name.length() == 0) {
					return null;
				}
				buf.append(name);
			} else {
				return null;
			}
		} catch (CoreException e) {
			buf.append("Unexpected error when accessing this proposal:<p><pre>"); //$NON-NLS-1$
			buf.append(e.getLocalizedMessage());
			buf.append("</pre>"); //$NON-NLS-1$
		}
		buf.append("</p>"); //$NON-NLS-1$
		return buf.toString();
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension6#getStyledDisplayString()
	 */
	public StyledString getStyledDisplayString() {
		StyledString str= new StyledString(getName());

		return str;
	}

	/**
	 * Returns the name of the proposal.
	 *
	 * @return the name of the proposal
	 */
	public String getName() {
		return fName;
	}


	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return null;
	}


	/**
	 * Returns the change that will be executed when the proposal is applied.
	 * This method calls {@link #createChange()} to compute the change.
	 * 
	 * @return the change for this proposal, can be <code>null</code> in rare cases if creation of
	 *         the change failed
	 * @throws CoreException when the change could not be created
	 */
	public final Change getChange() throws CoreException {
		if (Util.isGtk()) {
			// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=293995 :
			// [Widgets] Deadlock while UI thread displaying/computing a change proposal and non-UI thread creating image
			
			// Solution is to create the change outside a 'synchronized' block.
			// Synchronization is achieved by polling fChange, using "fChange == COMPUTING_CHANGE" as barrier.
			// Timeout of 10s for safety reasons (should not be reached).
			long end= System.currentTimeMillis() + 10000;
			do {
				boolean computing;
				synchronized (this) {
					computing= fChange == COMPUTING_CHANGE;
				}
				if (computing) {
					try {
						Display display= Display.getCurrent();
						if (display != null) {
							while (! display.isDisposed() && display.readAndDispatch()) {
								// empty the display loop
							}
							display.sleep();
						} else {
							Thread.sleep(100);
						}
					} catch (InterruptedException e) {
						//continue
					}
				} else {
					synchronized (this) {
						if (fChange == COMPUTING_CHANGE) {
							continue;
						} else if (fChange != null) {
							return fChange;
						} else {
							fChange= COMPUTING_CHANGE;
						}
					}
					Change change= createChange();
					synchronized (this) {
						fChange= change;
					}
					return change;
				}
			} while (System.currentTimeMillis() < end);
			
			synchronized (this) {
				if (fChange == COMPUTING_CHANGE) {
					return null; //failed
				}
			}
			
		} else {
			synchronized (this) {
				if (fChange == null) {
					fChange= createChange();
				}
			}
		}
		return fChange;
	}

	/**
	 * Creates the change for this proposal.
	 * This method is only called once and only when no change has been passed in
 	 * {@link #ChangeCorrectionProposal(String, Change, int, Image)}.
 	 *
 	 * Subclasses may override.
 	 * 
	 * @return the created change
	 * @throws CoreException if the creation of the change failed
	 */
	protected Change createChange() throws CoreException {
		return new NullChange();
	}

	/**
	 * Sets the display name.
	 *
	 * @param name the name to set
	 */
	public void setDisplayName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Name must not be null"); //$NON-NLS-1$
		}
		fName= name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return fRelevance;
	}

	/**
	 * Sets the relevance.
	 * 
	 * @param relevance the relevance to set
	 * 
	 * @see #getRelevance()
	 */
	public void setRelevance(int relevance) {
		fRelevance= relevance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.correction.ICommandAccess#getCommandId()


	 */
	public String getCommandId() {
		return fCommandId;
	}

	/**
	 * Set the proposal id to allow assigning a shortcut to the correction proposal.
	 *
	 * @param commandId The proposal id for this proposal or <code>null</code> if no command
	 * should be assigned to this proposal.
	 */
	public void setCommandId(String commandId) {
		fCommandId= commandId;
	}

}
