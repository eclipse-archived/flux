/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Provides a shared AST for clients. The shared AST is
 * the AST of the active Java editor's input element.
 *
 * @since 3.0
 */
public final class ASTProvider {

	public static final int SHARED_AST_LEVEL= AST.JLS8;
	public static final boolean SHARED_AST_STATEMENT_RECOVERY= true;
	public static final boolean SHARED_BINDING_RECOVERY= true;
	public CompilationUnit getAST(final ITypeRoot input, IProgressMonitor progressMonitor) {


		CompilationUnit ast= null;
		ast= createAST(input, progressMonitor);
		if (progressMonitor != null && progressMonitor.isCanceled()) {
			ast= null;
		}
		return ast;
	}
	private static CompilationUnit createAST(final ITypeRoot input, final IProgressMonitor progressMonitor) {
		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		final ASTParser parser = ASTParser.newParser(SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(SHARED_AST_STATEMENT_RECOVERY);
		parser.setBindingsRecovery(SHARED_BINDING_RECOVERY);
		parser.setSource(input);

		if (progressMonitor != null && progressMonitor.isCanceled())
			return null;

		final CompilationUnit root[]= new CompilationUnit[1];

		SafeRunner.run(new ISafeRunnable() {
			public void run() {
				try {
					if (progressMonitor != null && progressMonitor.isCanceled())
						return;
					root[0]= (CompilationUnit)parser.createAST(progressMonitor);

					//mark as unmodifiable
					ASTNodes.setFlagsToAST(root[0], ASTNode.PROTECT);
				} catch (OperationCanceledException ex) {
					return;
				}
			}
			public void handleException(Throwable ex) {
				IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Error in JDT Core during AST creation", ex);  //$NON-NLS-1$
				JavaPlugin.getDefault().getLog().log(status);
			}
		});
		return root[0];
	}
}

