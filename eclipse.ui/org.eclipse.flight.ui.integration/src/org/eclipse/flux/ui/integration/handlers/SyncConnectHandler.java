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
package org.eclipse.flux.ui.integration.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.flux.core.Repository;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @author Martin Lippert
 */
public class SyncConnectHandler extends AbstractHandler {

	public static final String ID = "org.springsource.ide.eclipse.ui.cloudsync.connect";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IProject[] selectedProjects = getSelectedProjects(selection);
		
		Repository repository = org.eclipse.flux.core.Activator.getDefault().getRepository();

		for (IProject project : selectedProjects) {
			if (!repository.isConnected(project)) {
				repository.addProject(project);
			}
		}
		
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			IEvaluationContext evalContext = (IEvaluationContext) evaluationContext;
			Object selection = evalContext.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ISelection) {
				IProject[] selectedProjects = getSelectedProjects((ISelection) selection);
				
				Repository repository = org.eclipse.flux.core.Activator.getDefault().getRepository();
				for (IProject project : selectedProjects) {
					if (!repository.isConnected(project)) {
						setBaseEnabled(true);
						return;
					}
				}
			}
		}
		
		setBaseEnabled(false);
	}
	
	protected IProject[] getSelectedProjects(ISelection selection) {
		List<IProject>selectedProjects = new ArrayList<IProject>();
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object[] selectedObjects = structuredSelection.toArray();
			for (int i = 0; i < selectedObjects.length; i++) {
				if (selectedObjects[i] instanceof IAdaptable) {
					IProject project = (IProject) ((IAdaptable)selectedObjects[i]).getAdapter(IProject.class);
					if (project != null) {
						selectedProjects.add(project);
					}
				}
			}
		}
		
		return (IProject[]) selectedProjects.toArray(new IProject[selectedProjects.size()]);
	}
	
}
