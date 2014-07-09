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
package org.eclipse.flux.jdt.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Method implementations extracted from JDT UI. Mostly from
 * <code>org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal</code>
 * 
 * @author aboyko
 *
 */
public class TypeProposalUtils {

	static void createName(ITypeBinding type, boolean includePackage,
			List<String> list) {
		ITypeBinding baseType = type;
		if (type.isArray()) {
			baseType = type.getElementType();
		}
		if (!baseType.isPrimitive() && !baseType.isNullType()) {
			ITypeBinding declaringType = baseType.getDeclaringClass();
			if (declaringType != null) {
				createName(declaringType, includePackage, list);
			} else if (includePackage && !baseType.getPackage().isUnnamed()) {
				String[] components = baseType.getPackage().getNameComponents();
				for (int i = 0; i < components.length; i++) {
					list.add(components[i]);
				}
			}
		}
		if (!baseType.isAnonymous()) {
			list.add(type.getName());
		} else {
			list.add("$local$"); //$NON-NLS-1$
		}
	}

	static String getTypeQualifiedName(ITypeBinding type) {
		List<String> result= new ArrayList<String>(5);
		createName(type, false, result);
	
		StringBuffer buffer= new StringBuffer();
		for (int i= 0; i < result.size(); i++) {
			if (i > 0) {
				buffer.append('.');
			}
			buffer.append(result.get(i));
		}
		return buffer.toString();
	}

	static String[] getSuperTypeSignatures(IType subType, IType superType) throws JavaModelException {
		if (superType.isInterface())
			return subType.getSuperInterfaceTypeSignatures();
		else
			return new String[] {subType.getSuperclassTypeSignature()};
	}

	static String findMatchingSuperTypeSignature(IType subType, IType superType) throws JavaModelException {
			String[] signatures= getSuperTypeSignatures(subType, superType);
			for (int i= 0; i < signatures.length; i++) {
				String signature= signatures[i];
				String qualified= SignatureUtil.qualifySignature(signature, subType);
				String subFQN= SignatureUtil.stripSignatureToFQN(qualified);
	
				String superFQN= superType.getFullyQualifiedName();
				if (subFQN.equals(superFQN)) {
					return signature;
				}
	
				// TODO handle local types
			}
	
			return null;
	//		throw new JavaModelException(new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "Illegal hierarchy", null))); //$NON-NLS-1$
		}

	static int findMatchingTypeArgumentIndex(String signature, String argument) {
		String[] typeArguments= Signature.getTypeArguments(signature);
		for (int i= 0; i < typeArguments.length; i++) {
			if (Signature.getSignatureSimpleName(typeArguments[i]).equals(argument))
				return i;
		}
		return -1;
	}

	static int mapTypeParameterIndex(IType[] path, int pathIndex, int paramIndex) throws JavaModelException, ArrayIndexOutOfBoundsException {
		if (pathIndex == 0)
			// break condition: we've reached the top of the hierarchy
			return paramIndex;
	
		IType subType= path[pathIndex];
		IType superType= path[pathIndex - 1];
	
		String superSignature= findMatchingSuperTypeSignature(subType, superType);
		ITypeParameter param= subType.getTypeParameters()[paramIndex];
		int index= findMatchingTypeArgumentIndex(superSignature, param.getElementName());
		if (index == -1) {
			// not mapped through
			return -1;
		}
	
		return mapTypeParameterIndex(path, pathIndex - 1, index);
	}

	static IType[] computeInheritancePath(IType subType, IType superType) throws JavaModelException {
		if (superType == null)
			return null;
	
		// optimization: avoid building the type hierarchy for the identity case
		if (superType.equals(subType))
			return new IType[] { subType };
	
		ITypeHierarchy hierarchy= subType.newSupertypeHierarchy(new NullProgressMonitor());
		if (!hierarchy.contains(superType))
			return null; // no path
	
		List<IType> path= new LinkedList<IType>();
		path.add(superType);
		do {
			// any sub type must be on a hierarchy chain from superType to subType
			superType= hierarchy.getSubtypes(superType)[0];
			path.add(superType);
		} while (!superType.equals(subType)); // since the equality case is handled above, we can spare one check
	
		return path.toArray(new IType[path.size()]);
	}
	
}
