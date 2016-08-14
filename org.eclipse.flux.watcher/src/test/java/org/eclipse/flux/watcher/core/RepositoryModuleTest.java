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

import org.eclipse.flux.watcher.core.spi.ProjectFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests Google Guice bootstrap.
 *
 * @author Kevin Pollet
 */
public final class RepositoryModuleTest {
    @Test
    public void testBootstrap() {
        final Injector injector = Guice.createInjector(new RepositoryModule(), new TestModule());

        Assert.assertNotNull(injector.getInstance(Repository.class));
    }

    public static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ProjectFactory.class).toInstance(mock(ProjectFactory.class));
        }
    }
}
