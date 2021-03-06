/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.example.contentSources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

import com.sun.jsftemplating.util.fileStreamer.ContentSource;
import com.sun.jsftemplating.util.fileStreamer.Context;


/**
 *  <p>	This class implements <code>ContentSource</code>.  It generates
 *	content by acting like a proxy.  This is another example of
 *	<code>FileStreamer</code> functionality.</p>
 */
public class ProxyContentSource implements ContentSource {

    /**
     *	<p> This method returns a unique string used to identify this
     *	    <code>ContentSource</code>.  This string must be specified in
     *	    order to select the appropriate {@link ContentSource} when using
     *	    <code>FileStreamer</code>.</p>
     */
    public String getId() {
	return ID;
    }

    /**
     *  <p> This method is responsible for getting the content and returning
     *	    an <code>InputStream</code> for it.  It is also responsible for
     *	    setting any attribute values in the <code>Context</code>, such as
     *	    <code>Context#EXTENSION</code> or
     *	    <code>Context#CONTENT_TYPE</code>.</p>
     */
    public InputStream getInputStream(Context ctx) throws IOException {
	// See if we already have it.
	InputStream in = (InputStream) ctx.getAttribute("inputStream");
	if (in == null) {
	    // Get the path...
	    String path = (String) ctx.getAttribute(Context.FILE_PATH);
	    while (path.startsWith("/")) {
		path = path.substring(1);
	    }

	    // Get the URL...
	    URL url = new URL("http://" + path);

	    // Set the extension so it can be mapped to a MIME type
	    int index = path.lastIndexOf('.');
	    if (index > 0) {
		ctx.setAttribute(Context.EXTENSION, path.substring(index + 1));
	    }

	    // Open the InputStream
	    in = url.openStream();

	    // Save in case method is called multiple times
	    ctx.setAttribute("inputStream", in);
	}

	// Return an InputStream to the file
	return in;
    }

    /**
     *	<p> This method may be used to clean up any temporary resources.  It
     *	    will be invoked after the <code>InputStream</code> has been
     *	    completely read.</p>
     */
    public void cleanUp(Context ctx) {
	// Get the File information
	InputStream is = (InputStream) ctx.getAttribute("inputStream");

	// Close the InputStream
	if (is != null) {
	    try {
		is.close();
	    } catch (Exception ex) {
		// Ignore...
	    }
	}
	ctx.removeAttribute("inputStream");
    }

    /**
     *	<p> This method is responsible for returning the last modified date of
     *	    the content, or -1 if not applicable.  This information will be
     *	    used for caching.</p>
     *
     *	<p> This example will tell the browser to cache ALL content, regardless
     *	    of changes in the content.</p>
     */
    public long getLastModified(Context context) {
	// This will enable caching on all content
	return DEFAULT_MODIFIED_DATE;
    }

    /**
     *	This is the default "Last-Modified" Date.  Its value is the
     *	<code>Long</code> representing the initialization time of this class.
     */
    protected static final long DEFAULT_MODIFIED_DATE = (new Date()).getTime();

    /**
     *	<p>This is the ID for this {@link ContentSource}.</p>
     */
    public static final String ID = "proxy";
}
