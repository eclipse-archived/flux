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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.flux.core.ConnectionStatus;
import org.eclipse.flux.core.util.Listener;
import org.eclipse.flux.core.util.Observable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.progress.UIJob;

/**
 * Connection listener attached to flux message bus on startup to report
 * problems creating the connection because of authorization failure.
 * 
 * @author Kris De Volder
 */
public class AuthFailureReporter implements Listener<ConnectionStatus> {

	private Observable<ConnectionStatus> state;

	public AuthFailureReporter(Observable<ConnectionStatus> state) {
		this.state = state;
		this.state.addListener(this);
	}

	@Override
	public void newValue(Observable<ConnectionStatus> o, ConnectionStatus v) {
		if (v.isAuthFailure()) {
			UIJob job = new UIJob("Flux Error Reporter") {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					MessageDialog.openError(null, "Flux Connection Failed", 
							"Flux Web Socket handshake failed. Most likely this means " + 
							"your Flux credentials are invalid."
					);
					return Status.OK_STATUS;
				}
			};
			job.schedule();
			//We only report this error once. 
			// since this error is all we handle... there's no point to continue listening  
		} else if (v.isConnected()) {
			//This means credentials must be valid and this listener now useless.
			dispose();
		}
	}

	void dispose() {
		Observable<ConnectionStatus> s = state;
		if (s!=null) {
			state = null;
			s.removeListener(this);
		}
	}
	
}
