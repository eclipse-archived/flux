/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;

import org.eclipse.jdt.core.ICompilationUnit;


public class JavaMarkerAnnotation implements IJavaAnnotation {

	public static final String ERROR_ANNOTATION_TYPE= "org.eclipse.jdt.ui.error"; //$NON-NLS-1$
	public static final String WARNING_ANNOTATION_TYPE= "org.eclipse.jdt.ui.warning"; //$NON-NLS-1$
	public static final String INFO_ANNOTATION_TYPE= "org.eclipse.jdt.ui.info"; //$NON-NLS-1$
	public static final String TASK_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.task"; //$NON-NLS-1$
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean isPersistent() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isMarkedDeleted() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean hasOverlay() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public IJavaAnnotation getOverlay() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Iterator<IJavaAnnotation> getOverlaidIterator() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void addOverlaid(IJavaAnnotation annotation) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void removeOverlaid(IJavaAnnotation annotation) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isProblem() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public ICompilationUnit getCompilationUnit() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String[] getArguments() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public String getMarkerType() {
		// TODO Auto-generated method stub
		return null;
	}

}
