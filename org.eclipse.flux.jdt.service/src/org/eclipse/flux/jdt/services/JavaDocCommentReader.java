/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.flux.jdt.services;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.formatter.IndentManipulation;


/**
 * Reads a java doc comment from a java doc comment. Skips star-character on begin of line.
 */
public class JavaDocCommentReader extends Reader {

	private IBuffer fBuffer;

	private int fCurrPos;

	private int fStartPos;

	private int fEndPos;

	private boolean fWasNewLine;

	public JavaDocCommentReader(IBuffer buf, int start, int end) {
		fBuffer= buf;
		fStartPos= start + 3;
		fEndPos= end - 2;

		reset();
	}

	/**
	 * @see java.io.Reader#read()
	 */
	@Override
	public int read() {
		if (fCurrPos < fEndPos) {
			char ch= fBuffer.getChar(fCurrPos++);
			if (fWasNewLine && !IndentManipulation.isLineDelimiterChar(ch)) {
				while (fCurrPos < fEndPos && Character.isWhitespace(ch)) {
					ch= fBuffer.getChar(fCurrPos++);
				}
				if (ch == '*') {
					if (fCurrPos < fEndPos) {
						do {
							ch= fBuffer.getChar(fCurrPos++);
						} while (ch == '*');
					} else {
						return -1;
					}
				}
			}
			fWasNewLine= IndentManipulation.isLineDelimiterChar(ch);

			return ch;
		}
		return -1;
	}

	/**
	 * @see java.io.Reader#close()
	 */
	@Override
	public void close() {
		fBuffer= null;
	}

	/**
	 * @see java.io.Reader#reset()
	 */
	@Override
	public void reset() {
		fCurrPos= fStartPos;
		fWasNewLine= true;
		// skip first line delimiter:
		if (fCurrPos < fEndPos && '\r' == fBuffer.getChar(fCurrPos)) {
			fCurrPos++;
		}
		if (fCurrPos < fEndPos && '\n' == fBuffer.getChar(fCurrPos)) {
			fCurrPos++;
		}
	}


	/**
	 * Returns the offset of the last read character in the passed buffer.
	 * 
	 * @return the offset
	 */
	public int getOffset() {
		return fCurrPos;
	}
	/**
	 * @see Reader#read(char[],int,int)
	 */
	public int read(char cbuf[], int off, int len) throws IOException {
		int end= off + len;
		for (int i= off; i < end; i++) {
			int ch= read();
			if (ch == -1) {
				if (i == off)
					return -1;
				return i - off;
			}
			cbuf[i]= (char)ch;
		}
		return len;
	}

	/**
	 * @see Reader#ready()
	 */
    public boolean ready() throws IOException {
		return true;
	}

	/**
	 * Returns the readable content as string.
	 * @return the readable content as string
	 * @exception IOException in case reading fails
	 */
	public String getString() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch;
		while ((ch= read()) != -1) {
			buf.append((char)ch);
		}
		return buf.toString();
	}

}
