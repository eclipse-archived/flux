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
package org.eclipse.flux.watcher.core;

import org.eclipse.flux.watcher.core.spi.Project;
import org.eclipse.flux.watcher.core.spi.ProjectFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link com.codenvy.flux.watcher.core.Repository} tests.
 *
 * @author Kevin Pollet
 */
public final class RepositoryTest {
    private final static String PROJECT_ID   = "project-id";
    private final static String PROJECT_PATH = "/project-id";

    private Repository     repository;

    @Before
    public void beforeTest() {
        final Project projectMock = mock(Project.class);
        when(projectMock.id()).thenReturn(PROJECT_ID);
        when(projectMock.path()).thenReturn(PROJECT_PATH);

        final ProjectFactory projectFactoryMock = mock(ProjectFactory.class);
        when(projectFactoryMock.newProject(anyString(), anyString())).thenReturn(projectMock);
    }

    @Test(expected = NullPointerException.class)
    public void testAddProjectWithNullProjectId() {
        repository.addProject(null, PROJECT_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void testAddProjectWithNullProjectPath() {
        repository.addProject(PROJECT_ID, null);
    }

    @Test
    public void testRemoveProjectWithNonExistentProjectId() {
        final Project project = repository.removeProject(PROJECT_ID);

        Assert.assertNull(project);
    }

    @Test
    public void testRemoveProject() {
        final Project project = repository.addProject(PROJECT_ID, PROJECT_PATH);
        final Project removedProject = repository.removeProject(PROJECT_ID);

        Assert.assertNotNull(project);
        Assert.assertNotNull(removedProject);
        Assert.assertSame(project, removedProject);
    }

    @Test
    public void testAddProjectWithAlreadyAddedProject() {
        final Project project = repository.addProject(PROJECT_ID, PROJECT_PATH);

        Assert.assertNotNull(project);

        final Project newProject = repository.addProject(PROJECT_ID, PROJECT_PATH);

        Assert.assertNotNull(newProject);
        Assert.assertSame(project, newProject);
    }

    @Test
    public void testGetProjectWithNullProjectId() {
        final Project project = repository.getProject(null);

        Assert.assertNull(project);
    }

    @Test
    public void testGetProjectWithNonExistentProjectId() {
        final Project project = repository.getProject("foo");

        Assert.assertNull(project);
    }

    @Test
    public void testGetProject() {
        final Project project = repository.addProject(PROJECT_ID, PROJECT_PATH);

        Assert.assertNotNull(project);

        final Project currentProject = repository.getProject(PROJECT_ID);

        Assert.assertNotNull(currentProject);
        Assert.assertSame(project, currentProject);
        Assert.assertEquals(PROJECT_ID, currentProject.id());
        Assert.assertEquals(PROJECT_PATH, currentProject.path());
    }
}
