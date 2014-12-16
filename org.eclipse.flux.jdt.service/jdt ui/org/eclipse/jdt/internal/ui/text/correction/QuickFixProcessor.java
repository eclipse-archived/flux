/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [quick fix] Quick fix for missing synchronized modifier - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245250
 *     Stephan Herrmann - Contributions for
 *								[quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *								[quick fix] The fix change parameter type to @Nonnull generated a null change - https://bugs.eclipse.org/400668 
 *								[quick fix] don't propose null annotations when those are disabled - https://bugs.eclipse.org/405086
 *								[quickfix] Update null annotation quick fixes for bug 388281 - https://bugs.eclipse.org/395555
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *     Sandra Lions <sandra.lions-piron@oracle.com> - [quick fix] for qualified enum constants in switch-case labels - https://bugs.eclipse.org/bugs/90140
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

/**
  */
public class QuickFixProcessor implements IQuickFixProcessor {


	public boolean hasCorrections(ICompilationUnit cu, int problemId) {
		switch (problemId) {
			case IProblem.UnterminatedString:
			case IProblem.UnusedImport:
			case IProblem.DuplicateImport:
			case IProblem.CannotImportPackage:
			case IProblem.ConflictingImport:
			case IProblem.ImportNotFound:
			case IProblem.UndefinedMethod:
			case IProblem.UndefinedConstructor:
			case IProblem.ParameterMismatch:
			case IProblem.MethodButWithConstructorName:
			case IProblem.UndefinedField:
			case IProblem.UndefinedName:
			case IProblem.UnresolvedVariable:
			case IProblem.PublicClassMustMatchFileName:
			case IProblem.PackageIsNotExpectedPackage:
			case IProblem.UndefinedType:
			case IProblem.TypeMismatch:
			case IProblem.ReturnTypeMismatch:
			case IProblem.UnhandledException:
			case IProblem.UnhandledExceptionOnAutoClose:
			case IProblem.UnreachableCatch:
			case IProblem.InvalidCatchBlockSequence:
			case IProblem.InvalidUnionTypeReferenceSequence:
			case IProblem.VoidMethodReturnsValue:
			case IProblem.ShouldReturnValue:
			case IProblem.ShouldReturnValueHintMissingDefault:
			case IProblem.MissingReturnType:
			case IProblem.NonExternalizedStringLiteral:
			case IProblem.NonStaticAccessToStaticField:
			case IProblem.NonStaticAccessToStaticMethod:
			case IProblem.NonStaticOrAlienTypeReceiver:
			case IProblem.StaticMethodRequested:
			case IProblem.NonStaticFieldFromStaticInvocation:
			case IProblem.InstanceMethodDuringConstructorInvocation:
			case IProblem.InstanceFieldDuringConstructorInvocation:
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleType:
			case IProblem.NotVisibleField:
			case IProblem.BodyForAbstractMethod:
			case IProblem.AbstractMethodInAbstractClass:
			case IProblem.AbstractMethodMustBeImplemented:
			case IProblem.EnumAbstractMethodMustBeImplemented:
			case IProblem.AbstractMethodsInConcreteClass:
			case IProblem.AbstractMethodInEnum:
			case IProblem.EnumConstantMustImplementAbstractMethod:
			case IProblem.ShouldImplementHashcode:
			case IProblem.BodyForNativeMethod:
			case IProblem.OuterLocalMustBeFinal:
			case IProblem.UninitializedLocalVariable:
			case IProblem.UninitializedLocalVariableHintMissingDefault:
			case IProblem.UndefinedConstructorInDefaultConstructor:
			case IProblem.UnhandledExceptionInDefaultConstructor:
			case IProblem.NotVisibleConstructorInDefaultConstructor:
			case IProblem.AmbiguousType:
			case IProblem.UnusedPrivateMethod:
			case IProblem.UnusedPrivateConstructor:
			case IProblem.UnusedPrivateField:
			case IProblem.UnusedPrivateType:
			case IProblem.LocalVariableIsNeverUsed:
			case IProblem.ArgumentIsNeverUsed:
			case IProblem.MethodRequiresBody:
			case IProblem.NeedToEmulateFieldReadAccess:
			case IProblem.NeedToEmulateFieldWriteAccess:
			case IProblem.NeedToEmulateMethodAccess:
			case IProblem.NeedToEmulateConstructorAccess:
			case IProblem.SuperfluousSemicolon:
			case IProblem.UnnecessaryCast:
			case IProblem.UnnecessaryInstanceof:
			case IProblem.IndirectAccessToStaticField:
			case IProblem.IndirectAccessToStaticMethod:
			case IProblem.Task:
			case IProblem.UnusedMethodDeclaredThrownException:
			case IProblem.UnusedConstructorDeclaredThrownException:
			case IProblem.UnqualifiedFieldAccess:
			case IProblem.JavadocMissing:
			case IProblem.JavadocMissingParamTag:
			case IProblem.JavadocMissingReturnTag:
			case IProblem.JavadocMissingThrowsTag:
			case IProblem.JavadocUndefinedType:
			case IProblem.JavadocAmbiguousType:
			case IProblem.JavadocNotVisibleType:
			case IProblem.JavadocInvalidThrowsClassName:
			case IProblem.JavadocDuplicateThrowsClassName:
			case IProblem.JavadocDuplicateReturnTag:
			case IProblem.JavadocDuplicateParamName:
			case IProblem.JavadocInvalidParamName:
			case IProblem.JavadocUnexpectedTag:
			case IProblem.JavadocInvalidTag:
			case IProblem.NonBlankFinalLocalAssignment:
			case IProblem.DuplicateFinalLocalInitialization:
			case IProblem.FinalFieldAssignment:
			case IProblem.DuplicateBlankFinalFieldInitialization:
			case IProblem.AnonymousClassCannotExtendFinalClass:
			case IProblem.ClassExtendFinalClass:
			case IProblem.FinalMethodCannotBeOverridden:
			case IProblem.InheritedMethodReducesVisibility:
			case IProblem.MethodReducesVisibility:
			case IProblem.OverridingNonVisibleMethod:
			case IProblem.CannotOverrideAStaticMethodWithAnInstanceMethod:
			case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
			case IProblem.UnexpectedStaticModifierForMethod:
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
			case IProblem.DuplicateField:
			case IProblem.DuplicateMethod:
			case IProblem.DuplicateTypeVariable:
			case IProblem.DuplicateNestedType:
			case IProblem.IllegalQualifiedEnumConstantLabel:
			case IProblem.IllegalModifierForInterfaceMethod:
			case IProblem.IllegalModifierForInterfaceMethod18:
			case IProblem.IllegalModifierForInterface:
			case IProblem.IllegalModifierForClass:
			case IProblem.IllegalModifierForInterfaceField:
			case IProblem.IllegalModifierForMemberInterface:
			case IProblem.IllegalModifierForMemberClass:
			case IProblem.IllegalModifierForLocalClass:
			case IProblem.IllegalModifierForArgument:
			case IProblem.IllegalModifierForField:
			case IProblem.IllegalModifierForMethod:
			case IProblem.IllegalModifierForConstructor:
			case IProblem.IllegalModifierForVariable:
			case IProblem.IllegalModifierForEnum:
			case IProblem.IllegalModifierForEnumConstant:
			case IProblem.IllegalModifierForEnumConstructor:
			case IProblem.IllegalModifierForMemberEnum:
			case IProblem.UnexpectedStaticModifierForField:
			case IProblem.IllegalModifierCombinationFinalVolatileForField:
			case IProblem.IllegalVisibilityModifierForInterfaceMemberType:
			case IProblem.IncompatibleReturnType:
			case IProblem.IncompatibleExceptionInThrowsClause:
			case IProblem.NoMessageSendOnArrayType:
			case IProblem.InvalidOperator:
			case IProblem.MissingSerialVersion:
			case IProblem.UnnecessaryElse:
			case IProblem.SuperclassMustBeAClass:
			case IProblem.UseAssertAsAnIdentifier:
			case IProblem.UseEnumAsAnIdentifier:
			case IProblem.RedefinedLocal:
			case IProblem.RedefinedArgument:
			case IProblem.CodeCannotBeReached:
			case IProblem.DeadCode:
			case IProblem.InvalidUsageOfTypeParameters:
			case IProblem.InvalidUsageOfStaticImports:
			case IProblem.InvalidUsageOfForeachStatements:
			case IProblem.InvalidUsageOfTypeArguments:
			case IProblem.InvalidUsageOfEnumDeclarations:
			case IProblem.InvalidUsageOfVarargs:
			case IProblem.InvalidUsageOfAnnotations:
			case IProblem.InvalidUsageOfAnnotationDeclarations:
			case IProblem.FieldMissingDeprecatedAnnotation:
			case IProblem.OverridingDeprecatedMethod:
			case IProblem.MethodMissingDeprecatedAnnotation:
			case IProblem.TypeMissingDeprecatedAnnotation:
			case IProblem.MissingOverrideAnnotation:
			case IProblem.MissingOverrideAnnotationForInterfaceMethodImplementation:
			case IProblem.MethodMustOverride:
			case IProblem.MethodMustOverrideOrImplement:
			case IProblem.IsClassPathCorrect:
			case IProblem.MethodReturnsVoid:
			case IProblem.ForbiddenReference:
			case IProblem.DiscouragedReference:
			case IProblem.UnnecessaryNLSTag:
			case IProblem.AssignmentHasNoEffect:
			case IProblem.UnsafeTypeConversion:
			case IProblem.UnsafeElementTypeConversion:
			case IProblem.RawTypeReference:
			case IProblem.UnsafeRawMethodInvocation:
			case IProblem.RedundantSpecificationOfTypeArguments:
			case IProblem.UndefinedAnnotationMember:
			case IProblem.MissingValueForAnnotationMember:
			case IProblem.FallthroughCase:
			case IProblem.NonGenericType:
			case IProblem.UnhandledWarningToken:
			case IProblem.UnusedWarningToken:
			case IProblem.RedundantSuperinterface:
			case IProblem.JavadocInvalidMemberTypeQualification:
			case IProblem.IncompatibleTypesInForeach:
			case IProblem.MissingEnumConstantCase:
			case IProblem.MissingEnumDefaultCase:
			case IProblem.MissingDefaultCase:
			case IProblem.MissingEnumConstantCaseDespiteDefault:
			case IProblem.MissingSynchronizedModifierInInheritedMethod:
			case IProblem.UnusedObjectAllocation:
			case IProblem.MethodCanBeStatic:
			case IProblem.MethodCanBePotentiallyStatic:
			case IProblem.AutoManagedResourceNotBelow17:
			case IProblem.MultiCatchNotBelow17:
			case IProblem.PolymorphicMethodNotBelow17:
			case IProblem.BinaryLiteralNotBelow17:
			case IProblem.UnderscoresInLiteralsNotBelow17:
			case IProblem.SwitchOnStringsNotBelow17:
			case IProblem.DiamondNotBelow17:
			case IProblem.PotentialHeapPollutionFromVararg :
			case IProblem.UnsafeGenericArrayForVarargs:
			case IProblem.SafeVarargsOnFixedArityMethod :
			case IProblem.SafeVarargsOnNonFinalInstanceMethod:
			case IProblem.RequiredNonNullButProvidedNull:
			case IProblem.RequiredNonNullButProvidedPotentialNull:
			case IProblem.RequiredNonNullButProvidedSpecdNullable:
			case IProblem.RequiredNonNullButProvidedUnknown:
			case IProblem.IllegalReturnNullityRedefinition:
			case IProblem.IllegalRedefinitionToNonNullParameter:
			case IProblem.IllegalDefinitionToNonNullParameter:
			case IProblem.ParameterLackingNonNullAnnotation:
			case IProblem.ParameterLackingNullableAnnotation:
			case IProblem.SpecdNonNullLocalVariableComparisonYieldsFalse:
			case IProblem.RedundantNullCheckOnSpecdNonNullLocalVariable:
			case IProblem.RedundantNullAnnotation:
			case IProblem.UnusedTypeParameter:
			case IProblem.NullableFieldReference:
			case IProblem.ConflictingNullAnnotations:
			case IProblem.ConflictingInheritedNullAnnotations:
			case IProblem.ExplicitThisParameterNotBelow18:
			case IProblem.DefaultMethodNotBelow18:
			case IProblem.StaticInterfaceMethodNotBelow18:
			case IProblem.LambdaExpressionNotBelow18:
			case IProblem.MethodReferenceNotBelow18:
			case IProblem.ConstructorReferenceNotBelow18:
			case IProblem.IntersectionCastNotBelow18:
			case IProblem.InvalidUsageOfTypeAnnotations:
				return true;
			default:
				return false;//SuppressWarningsSubProcessor.hasSuppressWarningsProposal(cu.getJavaProject(), problemId);
		}
	}

	private static int moveBack(int offset, int start, String ignoreCharacters, ICompilationUnit cu) {
		try {
			IBuffer buf= cu.getBuffer();
			while (offset >= start) {
				if (ignoreCharacters.indexOf(buf.getChar(offset - 1)) == -1) {
					return offset;
				}
				offset--;
			}
		} catch(JavaModelException e) {
			// use start
		}
		return start;
	}


	/* (non-Javadoc)
	 * @see IAssistProcessor#getCorrections(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		if (locations == null || locations.length == 0) {
			return null;
		}

		HashSet<Integer> handledProblems= new HashSet<Integer>(locations.length);
		ArrayList<ICommandAccess> resultingCollections= new ArrayList<ICommandAccess>();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation curr= locations[i];
			Integer id= new Integer(curr.getProblemId());
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
	}

	private void process(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		int id= problem.getProblemId();
		if (id == 0) { // no proposals for none-problem locations
			return;
		}
		switch (id) {
			case IProblem.UnterminatedString:
				String quoteLabel= CorrectionMessages.JavaCorrectionProcessor_addquote_description;
				int pos= moveBack(problem.getOffset() + problem.getLength(), problem.getOffset(), "\n\r", context.getCompilationUnit()); //$NON-NLS-1$
				proposals.add(new ReplaceCorrectionProposal(quoteLabel, context.getCompilationUnit(), pos, 0, "\"", IProposalRelevance.ADD_QUOTE)); //$NON-NLS-1$
				break;
			case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
			case IProblem.IllegalModifierForInterfaceMethod:
			case IProblem.IllegalModifierForInterface:
			case IProblem.IllegalModifierForClass:
			case IProblem.IllegalModifierForInterfaceField:
			case IProblem.UnexpectedStaticModifierForField:
			case IProblem.IllegalModifierCombinationFinalVolatileForField:
			case IProblem.IllegalModifierForMemberInterface:
			case IProblem.IllegalModifierForMemberClass:
			case IProblem.IllegalModifierForLocalClass:
			case IProblem.IllegalModifierForArgument:
			case IProblem.IllegalModifierForField:
			case IProblem.IllegalModifierForMethod:
			case IProblem.IllegalModifierForConstructor:
			case IProblem.IllegalModifierForVariable:
			case IProblem.IllegalModifierForEnum:
			case IProblem.IllegalModifierForEnumConstant:
			case IProblem.IllegalModifierForEnumConstructor:
			case IProblem.IllegalModifierForMemberEnum:
			case IProblem.IllegalVisibilityModifierForInterfaceMemberType:
			case IProblem.UnexpectedStaticModifierForMethod:
			case IProblem.IllegalModifierForInterfaceMethod18:
				ModifierCorrectionSubProcessor.addRemoveInvalidModifiersProposal(context, problem, proposals, IProposalRelevance.REMOVE_INVALID_MODIFIERS);
				break;
			
			default:
		}
	}
}
