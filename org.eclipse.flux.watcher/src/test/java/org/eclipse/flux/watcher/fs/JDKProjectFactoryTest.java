/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.flux.watcher.fs;


import org.eclipse.flux.watcher.core.RepositoryEventBus;
import org.eclipse.flux.watcher.core.spi.Project;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.mock;

/**
 * {@link com.codenvy.flux.watcher.fs.JDKProjectFactory} tests.
 *
 * @author Kevin Pollet
 */
public final class JDKProjectFactoryTest extends AbstractTest {
    private JDKProjectFactory jdkProjectFactory;

    @Before
    public void beforeTest() {
        jdkProjectFactory = new JDKProjectFactory(fileSystem(), mock(RepositoryEventBus.class));
    }

    @Test(expected = NullPointerException.class)
    public void testNewJDKProjectFactoryWithNullFileSystem() {
        new JDKProjectFactory(null, mock(RepositoryEventBus.class));
    }

    @Test(expected = NullPointerException.class)
    public void testNewJDKProjectFactoryWithNullRepositoryEventBus() {
        new JDKProjectFactory(fileSystem(), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNewProjectWithNullProjectId() {
        jdkProjectFactory.newProject(null, PROJECT_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void testNewProjectWithNullProjectPath() {
        jdkProjectFactory.newProject(PROJECT_ID, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewProjectWithNonExistentProjectPath() {
        jdkProjectFactory.newProject(PROJECT_ID, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewProjectWithFileProjectPath() {
        jdkProjectFactory.newProject(PROJECT_ID, PROJECT_PATH + File.separator + RELATIVE_PROJECT_README_FILE_PATH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewProjectWithNonAbsoluteProjectPath() {
        jdkProjectFactory.newProject(PROJECT_ID, RELATIVE_PROJECT_README_FILE_PATH);
    }

    @Test
    public void testNewProject() {
        final Project project = jdkProjectFactory.newProject(PROJECT_ID, PROJECT_PATH);

        Assert.assertNotNull(project);
        Assert.assertEquals(PROJECT_ID, project.id());
        Assert.assertEquals(PROJECT_PATH, project.path());
    }
}
