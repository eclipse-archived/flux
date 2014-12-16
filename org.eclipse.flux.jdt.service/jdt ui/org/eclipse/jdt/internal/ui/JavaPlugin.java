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
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.internal.corext.dom.MembersOrderPreferenceCache;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;


/**
 * Represents the java plug-in. It provides a series of convenience methods such as
 * access to the workbench, keeps track of elements shared by all editors and viewers
 * of the plug-in such as document providers and find-replace-dialogs.
 */
public class JavaPlugin extends Plugin {

	public static String getPluginId() {
		return "org.eclipse.flux.jdt.service";
	}

	static JavaPlugin fDefault = new JavaPlugin();
	public static void log(Exception e) {
	}

	public static JavaPlugin getDefault() {
		return fDefault;
	}

	private ASTProvider fASTProvider;
	private ContextTypeRegistry fCodeTemplateContextTypeRegistry;

	public MembersOrderPreferenceCache getMemberOrderPreferenceCache() {
		return null;
	}

	public ASTProvider getASTProvider() {
		if (fASTProvider == null)
			fASTProvider= new ASTProvider();

		return fASTProvider;
	}

	public ContextTypeRegistry getCodeTemplateContextRegistry() {
		if (fCodeTemplateContextTypeRegistry == null) {
			fCodeTemplateContextTypeRegistry= new ContributionContextTypeRegistry();

			CodeTemplateContextType.registerContextTypes(fCodeTemplateContextTypeRegistry);
		}

		return fCodeTemplateContextTypeRegistry;
	}

	public TemplateStore getCodeTemplateStore() {
		// TODO Auto-generated method stub
		return null;
	}
}
