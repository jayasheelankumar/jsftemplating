/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2011 Ken Paulsen
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.jsft.facelets;

import com.sun.jsft.event.Command;
import com.sun.jsft.event.ELCommand;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


/**
 *  <p>	This class is responsible for reading in all the commands for the
 *	given String.  The String typically is passed in from the body
 *	content of event.</p>
 *
 *  @author  Ken Paulsen (kenapaulsen@gmail.com)
 */
public class CommandReader {

    /**
     *	<p> Constructor.</p>
     */
    public CommandReader(String str) {
	this(new ByteArrayInputStream(("{" + str + "}").getBytes()));
    }

    /**
     *	<p> Constructor.</p>
     *
     *	@param	stream	The <code>InputStream</code> for the {@link Command}.
     */
    protected CommandReader(InputStream stream) {
	parser = new CommandParser(stream);
    }

    /**
     *	<p> The read method uses the {@link CommandParser} to parses the
     *	    template.  It populates a {@link LayoutDefinition} structure, which
     *	    is returned.</p>
     *
     *	@return	The {@link LayoutDefinition}
     *
     *	@throws	IOException
     */
    public List<Command> read() throws IOException {
	// Start...
	parser.open();

	try {
	    // Populate the LayoutDefinition from the Document
	    return readCommandList();
	} finally {
	    parser.close();
	}
    }

    /**
     *
     */
    private Command readCommand() throws IOException {
	// Skip White Space...
	parser.skipCommentsAndWhiteSpace(CommandParser.SIMPLE_WHITE_SPACE);

	// Read the next Command
	String commandLine = parser.readUntil(new int[] {';', '{', '}'}, true);

	// Read the children
	int ch = parser.nextChar();
	List<Command> commandChildren = null;
	if (ch == '{') {
	    // Read the Command Children 
	    commandChildren = readCommandList();
	} else if (ch == '}') {
	    parser.unread(ch);
	}

	// FIXME: Handle return values, for now assume they are not supported...

	// Create the Command
	Command command = null;
	if (commandLine.length() > 0) {
	    command = new ELCommand(
		    convertKeywords(commandLine), commandChildren);
	}
// FIXME: else if (commandChildren != null) <-- may happen if empty {} block is used

	// Return the LayoutElement
	return command;
    }

    /**
     *	<p> This method replaces keywords with the "real" syntax for
     *	    developer convenience.</p>
     */
    private String convertKeywords(String exp) {
	// Enable "if" keyword (beginning of expression only for now)
	if (exp.startsWith("if")) {
	    exp = "jsft._if" + exp.substring(2);
	} else if (exp.startsWith("foreach")) {
	    exp = "jsft.foreach" + exp.substring(7);
	}
	return exp;
    }

    /**
     *	<p> This method reads Commands until a closing '}' is encountered.</p>
     */
    private List<Command> readCommandList() throws IOException {
	int ch = parser.nextChar();
	List<Command> commands = new ArrayList<Command>();
	Command command = null;
	while (ch != '}') {
	    // Read a Command
	    command = readCommand();
	    if (command != null) {
		commands.add(command);
	    }

	    // Get the next char...
	    ch = parser.nextChar();
	    if (ch == -1)  {
		throw new IOException(
		    "Unexpected end of stream! Expected to find '}'.");
	    }
	}

	// Return the Commands
	return commands;
    }

    private CommandParser  parser    = null;
}
