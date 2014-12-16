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
package org.eclipse.jdt.ui;


import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;

/**
 * A {@link JavaElementImageDescriptor} consists of a base image and several adornments. The adornments
 * are computed according to the flags either passed during creation or set via the method
 *{@link #setAdornments(int)}.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class JavaElementImageDescriptor {

	/** Flag to render the abstract adornment. */
	public final static int ABSTRACT= 		0x001;

	/** Flag to render the final adornment. */
	public final static int FINAL=			0x002;

	/** Flag to render the synchronized adornment. */
	public final static int SYNCHRONIZED=	0x004;

	/** Flag to render the static adornment. */
	public final static int STATIC=			0x008;

	/** Flag to render the runnable adornment. */
	public final static int RUNNABLE= 		0x010;

	/** Flag to render the warning adornment. */
	public final static int WARNING=			0x020;

	/** Flag to render the error adornment. */
	public final static int ERROR=			0x040;

	/** Flag to render the 'override' adornment. */
	public final static int OVERRIDES= 		0x080;

	/** Flag to render the 'implements' adornment. */
	public final static int IMPLEMENTS= 		0x100;

	/** Flag to render the 'constructor' adornment. */
	public final static int CONSTRUCTOR= 	0x200;

	/**
	 * Flag to render the 'deprecated' adornment.
	 * @since 3.0
	 */
	public final static int DEPRECATED= 	0x400;

	/**
	 * Flag to render the 'volatile' adornment.
	 * @since 3.3
	 */
	public final static int VOLATILE= 	0x800;

	/**
	 * Flag to render the 'transient' adornment.
	 * @since 3.3
	 */
	public final static int TRANSIENT= 	0x1000;

	/**
	 * Flag to render the build path error adornment.
	 * @since 3.7
	 */
	public final static int BUILDPATH_ERROR= 0x2000;

	/**
	 * Flag to render the 'native' adornment.
	 * @since 3.7
	 */
	public final static int NATIVE= 	0x4000;

	/**
	 * Flag to render the 'ignore optional compile problems' adornment.
	 * @since 3.8
	 */
	public final static int IGNORE_OPTIONAL_PROBLEMS= 0x8000;

	/**
	 * Flag to render the 'default' method adornment.
	 * 
	 * @since 3.10
	 */
	public final static int DEFAULT_METHOD= 0x10000;

	/**
	 * Flag to render the 'default' annotation adornment.
	 * 
	 * @since 3.10
	 */
	public final static int ANNOTATION_DEFAULT= 0x20000;

	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Creates a new JavaElementImageDescriptor.
	 *
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered. See {@link #setAdornments(int)}
	 * 	for valid values.
	 * @param size the size of the resulting image
	 */
	public JavaElementImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
		fBaseImage= baseImage;
		Assert.isNotNull(fBaseImage);
		fFlags= flags;
		Assert.isTrue(fFlags >= 0);
		fSize= size;
		Assert.isNotNull(fSize);
	}

	/**
	 * Sets the descriptors adornments. Valid values are: {@link #ABSTRACT}, {@link #FINAL},
	 * {@link #SYNCHRONIZED}, {@link #STATIC}, {@link #RUNNABLE}, {@link #WARNING},
	 * {@link #ERROR}, {@link #OVERRIDES}, {@link #IMPLEMENTS}, {@link #CONSTRUCTOR},
	 * {@link #DEPRECATED}, {@link #VOLATILE}, {@link #TRANSIENT}, {@link #BUILDPATH_ERROR},
	 * {@link #NATIVE}, or any combination of those.
	 *
	 * @param adornments the image descriptors adornments
	 */
	public void setAdornments(int adornments) {
		Assert.isTrue(adornments >= 0);
		fFlags= adornments;
	}

	/**
	 * Returns the current adornments.
	 *
	 * @return the current adornments
	 */
	public int getAdronments() {
		return fFlags;
	}

	/**
	 * Sets the size of the image created by calling {@link #createImage()}.
	 *
	 * @param size the size of the image returned from calling {@link #createImage()}
	 */
	public void setImageSize(Point size) {
		Assert.isNotNull(size);
		Assert.isTrue(size.x >= 0 && size.y >= 0);
		fSize= size;
	}

	/**
	 * Returns the size of the image created by calling {@link #createImage()}.
	 *
	 * @return the size of the image created by calling {@link #createImage()}
	 */
	public Point getImageSize() {
		return new Point(fSize.x, fSize.y);
	}

	/* (non-Javadoc)
	 * Method declared on Object.
	 */
	@Override
	public boolean equals(Object object) {
		if (object == null || !JavaElementImageDescriptor.class.equals(object.getClass()))
			return false;

		JavaElementImageDescriptor other= (JavaElementImageDescriptor)object;
		return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
	}

	/* (non-Javadoc)
	 * Method declared on Object.
	 */
	@Override
	public int hashCode() {
		return fBaseImage.hashCode() | fFlags | fSize.hashCode();
	}
}
