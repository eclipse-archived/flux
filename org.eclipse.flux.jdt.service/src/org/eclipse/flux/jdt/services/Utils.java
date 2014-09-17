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

import java.util.LinkedList;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditVisitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility methods
 * 
 * @author aboyko
 *
 */
public class Utils {
	
	public static JSONArray editsToJsonArray(TextEdit edit) {
		final LinkedList<JSONObject> list = new LinkedList<JSONObject>();
		
		edit.accept(new TextEditVisitor() {

			@Override
			public boolean visit(DeleteEdit delete) {
				try {
					JSONObject json = new JSONObject();
					json.put("offset", delete.getOffset());
					json.put("length", delete.getLength());
					json.put("text", "");
					list.addFirst(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return super.visit(delete);
			}

			@Override
			public boolean visit(InsertEdit insert) {
				try {
					JSONObject json = new JSONObject();
					json.put("offset", insert.getOffset());
					json.put("length", 0);
					json.put("text", insert.getText());
					list.addFirst(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return super.visit(insert);
			}

			@Override
			public boolean visit(ReplaceEdit replace) {
				try {
					JSONObject json = new JSONObject();
					json.put("offset", replace.getOffset());
					json.put("length", replace.getLength());
					json.put("text", replace.getText());
					list.addFirst(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return super.visit(replace);
			}
			
		});
		
		return new JSONArray(list);
	}
	
	public static int getOffsetAdjustment(TextEdit edit, final int offset) {
		final int[] holder = new int[] { 0 };
		edit.accept(new TextEditVisitor() {

			@Override
			public boolean visit(DeleteEdit edit) {
				if (offset >= edit.getOffset()) {
					holder[0] -= edit.getLength();
				}
				return super.visit(edit);
			}

			@Override
			public boolean visit(InsertEdit edit) {
				if (offset >= edit.getOffset()) {
					holder[0] += edit.getText().length();
				}
				return super.visit(edit);	
			}

			@Override
			public boolean visit(ReplaceEdit edit) {
				if (offset >= edit.getOffset()) {
					holder[0] += edit.getText().length() - edit.getLength();
				}
				return super.visit(edit);
			}
			
		});
		return holder[0];
	}
	
}
