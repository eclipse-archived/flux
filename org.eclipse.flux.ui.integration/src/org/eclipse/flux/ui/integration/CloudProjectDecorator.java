/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.ui.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.ui.IDecoratorManager;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class CloudProjectDecorator extends LabelProvider implements ILightweightLabelDecorator {

	public static final String ID = "org.eclipse.flux.ui.integration.projectdecorator";

	public static CloudProjectDecorator getInstance() {
		IDecoratorManager decoratorManager = FluxUiPlugin.getDefault().getWorkbench().getDecoratorManager();
		if (decoratorManager.getEnabled(ID)) {
			return (CloudProjectDecorator) decoratorManager.getBaseLabelProvider(ID);
		}
		return null;
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IProject
				&& org.eclipse.flux.core.Activator.getDefault()
						.getMessagingConnector().getChannel() != null
				&& org.eclipse.flux.core.Activator.getDefault().getRepository() != null
				&& org.eclipse.flux.core.Activator.getDefault().getRepository()
						.isConnected((IProject) element)) {
			decoration.addSuffix(" [connected to flux]");
		}
	}

	@Override
	public void fireLabelProviderChanged(LabelProviderChangedEvent event) {
		super.fireLabelProviderChanged(event);
	}

}
