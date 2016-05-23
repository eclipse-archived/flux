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
package org.eclipse.flux.sync;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Lippert
 */
public class SyncSimpleDeleteTest {
	
	private SyncController sync;
	private SimpleStringSyncConnector connector;

	@Before
	public void setup() {
		connector = new SimpleStringSyncConnector("cat");
		sync = new SyncController("sync1", connector);
	}
	
	@Test
	public void testCreateDeleteOperation() {
		DeleteOperation operation = sync.createDeleteOperation(0, 2);
		assertEquals(0, operation.getPosition());
		assertEquals(2, operation.getLength());
		
		assertEquals("sync1-0", operation.getId());
		assertNull(operation.getPreviousOperation());
	}
	
	@Test
	public void testCreateMultipleDeleteOperations() {
		DeleteOperation operation1 = sync.createDeleteOperation(0, 1);
		DeleteOperation operation2 = sync.createDeleteOperation(1, 2);
		DeleteOperation operation3 = sync.createDeleteOperation(2, 1);
		
		assertEquals("sync1-0", operation1.getId());
		assertEquals("sync1-1", operation2.getId());
		assertEquals("sync1-2", operation3.getId());
		
		assertNull(operation2.getPreviousOperation());
		assertNull(operation3.getPreviousOperation());
	}
	
	@Test
	public void testSimpleCharacterDeletionAtTheBeginning() {
		sync.applyOperation(sync.createDeleteOperation(0, 1));
		assertEquals("at", connector.getText());
	}

	@Test
	public void testSimpleCharacterDeletionInTheMiddle() {
		sync.applyOperation(sync.createDeleteOperation(1, 1));
		assertEquals("ct", connector.getText());
	}

	@Test
	public void testSimpleCharacterDeletionAtTheEnd() {
		sync.applyOperation(sync.createDeleteOperation(2, 1));
		assertEquals("ca", connector.getText());
	}
	
	@Test
	public void testApplyMultipleDeleteOperations() {
		DeleteOperation operation1 = sync.createDeleteOperation(0, 1);
		sync.applyOperation(operation1);

		DeleteOperation operation2 = sync.createDeleteOperation(0, 1);
		sync.applyOperation(operation2);

		DeleteOperation operation3 = sync.createDeleteOperation(0, 1);
		sync.applyOperation(operation3);
		
		assertNull(operation1.getPreviousOperation());
		assertEquals("sync1-0", operation2.getPreviousOperation());
		assertEquals("sync1-1", operation3.getPreviousOperation());
		
		assertEquals("sync1-2", sync.getLastOperationID());
		
		assertEquals("", connector.getText());
	}
	
}
