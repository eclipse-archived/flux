/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;


public class AssistContext extends TextInvocationContext implements IInvocationContext {

	private final ICompilationUnit fCompilationUnit;

	private CompilationUnit fASTRoot;
	/**
	 * The cached node finder, can be null.
	 * @since 3.6
	 */
	private NodeFinder fNodeFinder;


	/*
	 * @since 3.5
	 */
	private AssistContext(ICompilationUnit cu, ISourceViewer sourceViewer, int offset, int length) {
		super(sourceViewer, offset, length);
		Assert.isLegal(cu != null);
		fCompilationUnit= cu;
	}
	
	/*
	 * Constructor for CorrectionContext.
	 */
	public AssistContext(ICompilationUnit cu, int offset, int length) {
		this(cu, null, offset, length);
	}

	/**
	 * Returns the compilation unit.
	 * @return an <code>ICompilationUnit</code>
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	

	/**
	 * Returns the length.
	 * @return int
	 */
	public int getSelectionLength() {
		return Math.max(getLength(), 0);
	}

	/**
	 * Returns the offset.
	 * @return int
	 */
	public int getSelectionOffset() {
		return getOffset();
	}

	public CompilationUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= SharedASTProvider.getAST(fCompilationUnit, null);
			if (fASTRoot == null) {
				// see bug 63554
				fASTRoot= ASTResolving.createQuickFixAST(fCompilationUnit, null);
			}
		}
		return fASTRoot;
	}


	/**
	 * @param root The ASTRoot to set.
	 */
	public void setASTRoot(CompilationUnit root) {
		fASTRoot= root;
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveringNode()
	 */
	public ASTNode getCoveringNode() {
		if (fNodeFinder == null) {
			fNodeFinder= new NodeFinder(getASTRoot(), getOffset(), getLength());
		}
		return fNodeFinder.getCoveringNode();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveredNode()
	 */
	public ASTNode getCoveredNode() {
		if (fNodeFinder == null) {
			fNodeFinder= new NodeFinder(getASTRoot(), getOffset(), getLength());
		}
		return fNodeFinder.getCoveredNode();
	}

}
