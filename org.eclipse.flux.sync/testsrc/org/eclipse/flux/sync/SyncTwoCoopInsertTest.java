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
public class SyncTwoCoopInsertTest {
	
	private SyncController sync1;
	private SyncController sync2;
	private SimpleStringSyncConnector connector1;
	private SimpleStringSyncConnector connector2;

	@Before
	public void setup() {
		connector1 = new SimpleStringSyncConnector("cat");
		connector2 = new SimpleStringSyncConnector("cat");
		sync1 = new SyncController("sync1", connector1);
		sync2 = new SyncController("sync2", connector2);
	}
	
	@Test
	public void testInsertsAfterEachOther() {
		InsertOperation operation1 = sync1.createInsertOperation("s", 3);
		InsertOperation operation2 = sync2.createInsertOperation("old ", 0);
		
		sync1.applyOperation(operation1);
		sync2.applyOperation(operation1);
		
		assertEquals("sync1-0", sync1.getLastOperationID());
		assertEquals("sync1-0", sync2.getLastOperationID());
		
		assertEquals("cats", connector1.getText());
		assertEquals("cats", connector2.getText());

		sync2.applyOperation(operation2);
		sync1.applyOperation(operation2);
		
		assertEquals("sync2-0", sync1.getLastOperationID());
		assertEquals("sync2-0", sync2.getLastOperationID());
		
		assertEquals("old cats", connector1.getText());
		assertEquals("old cats", connector2.getText());
	}
	
	@Test
	public void testSingleParallelInserts() {
		InsertOperation operation1 = sync1.createInsertOperation("s", 3);
		InsertOperation operation2 = sync2.createInsertOperation("old ", 0);
		
		sync1.applyOperation(operation1);
		sync2.applyOperation(operation2);
		
		assertEquals("cats", connector1.getText());
		assertEquals("old cat", connector2.getText());
		
		sync1.applyOperation(operation2);
		sync2.applyOperation(operation1);
		
		assertEquals("old cats", connector1.getText());
		assertEquals("old cats", connector2.getText());
	}
	
}
