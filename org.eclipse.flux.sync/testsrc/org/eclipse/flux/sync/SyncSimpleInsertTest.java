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
public class SyncSimpleInsertTest {
	
	private SyncController sync;
	private SimpleStringSyncConnector connector;

	@Before
	public void setup() {
		connector = new SimpleStringSyncConnector("cat");
		sync = new SyncController("sync1", connector);
	}
	
	@Test
	public void testCreateInsertOperation() {
		InsertOperation operation = sync.createInsertOperation("text", 0);
		assertEquals("text", operation.getCharacters());
		assertEquals(0, operation.getPosition());
		
		assertEquals("sync1-0", operation.getId());
		assertNull(operation.getPreviousOperation());
	}
	
	@Test
	public void testCreateMultipleInsertOperations() {
		InsertOperation operation1 = sync.createInsertOperation("g", 0);
		InsertOperation operation2 = sync.createInsertOperation("o", 0);
		InsertOperation operation3 = sync.createInsertOperation("d", 0);
		
		assertEquals("sync1-0", operation1.getId());
		assertEquals("sync1-1", operation2.getId());
		assertEquals("sync1-2", operation3.getId());
		
		assertNull(operation2.getPreviousOperation());
		assertNull(operation3.getPreviousOperation());
	}
	
	@Test
	public void testSimpleCharacterAdditionAtTheBeginning() {
		sync.applyOperation(sync.createInsertOperation("one ", 0));
		assertEquals("one cat", connector.getText());
	}

	@Test
	public void testSimpleCharacterAdditionInTheMiddle() {
		sync.applyOperation(sync.createInsertOperation("aaa", 1));
		assertEquals("caaaat", connector.getText());
	}

	@Test
	public void testSimpleCharacterAdditionAtTheEnd() {
		sync.applyOperation(sync.createInsertOperation("s", 3));
		assertEquals("cats", connector.getText());
	}
	
	@Test
	public void testApplyMultipleInsertOperations() {
		InsertOperation operation1 = sync.createInsertOperation("g", 0);
		sync.applyOperation(operation1);

		InsertOperation operation2 = sync.createInsertOperation("o", 0);
		sync.applyOperation(operation2);

		InsertOperation operation3 = sync.createInsertOperation("d", 0);
		sync.applyOperation(operation3);
		
		assertNull(operation1.getPreviousOperation());
		assertEquals("sync1-0", operation2.getPreviousOperation());
		assertEquals("sync1-1", operation3.getPreviousOperation());
		
		assertEquals("sync1-2", sync.getLastOperationID());

		assertEquals("dogcat", connector.getText());
	}
	
	@Test
	public void testApplyTheSameOperationMultipleTimes() {
		InsertOperation operation1 = sync.createInsertOperation("one ", 0);
		sync.applyOperation(operation1);
		sync.applyOperation(operation1);
		sync.applyOperation(operation1);
		sync.applyOperation(operation1);

		assertNull(operation1.getPreviousOperation());
		assertEquals("sync1-0", sync.getLastOperationID());

		assertEquals("one cat", connector.getText());
	}
	
}
