/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jsftemplating.resource;

import com.sun.jsftemplating.util.Util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;


/**
 *  <p>	This class caches <code>ResourceBundle</code> objects per locale.</p>
 *
 *  @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class ResourceBundleManager {

    /**
     *	<p> Use {@link #getInstance()} to obtain an instance.</p>
     */
    protected ResourceBundleManager() {
    }


    /**
     *	<p> Use this method to get the instance of this class.</p>
     *
     *	@deprecated Use ResourceBundleManager#getInstance(FacesContext).
     */
    public static ResourceBundleManager getInstance() {
	return getInstance(null);
    }

    /**
     *	<p> Use this method to get the instance of this class.</p>
     */
    public static ResourceBundleManager getInstance(FacesContext ctx) {
	if (ctx == null) {
	    ctx = FacesContext.getCurrentInstance();
	}
	ResourceBundleManager mgr = null;
	if (ctx != null) {
	    // Look in application scope for it...
	    mgr = (ResourceBundleManager) ctx.getExternalContext().
		    getApplicationMap().get(RB_MGR);
	}
	if (mgr == null) {
	    // 1st time... create / initialize it
	    mgr = new ResourceBundleManager();
	    if (ctx != null) {
		ctx.getExternalContext().getApplicationMap().put(RB_MGR, mgr);
	    }
	}

	// Return the map...
	return mgr;
    }

    /**
     *	<p> This method checks the cache for the requested
     *	    <code>ResourceBundle</code>.</p>
     *
     *	@param	baseName    Name of the bundle.
     *	@param	locale	    The <code>Locale</code>.
     *
     *	@return	The requested <code>ResourceBundle</code> in the most
     *		appropriate <code>Locale</code>.
     */
    protected ResourceBundle getCachedBundle(String baseName, Locale locale) {
	return (ResourceBundle) _cache.get(getCacheKey(baseName, locale));
    }

    /**
     *	<p> This method generates a unique key for setting / getting
     *	    <code>ResourceBundle</code>s from the cache.  It is important to
     *	    have different keys per locale (obviously).</p>
     */
    protected String getCacheKey(String baseName, Locale locale) {
	return baseName + "__" + locale.toString();
    }

    /**
     *	<p> This method adds a <code>ResourceBundle</code> to the cache.</p>
     */
    protected void addCachedBundle(String baseName, Locale locale, ResourceBundle bundle) {
	// Copy the old Map to prevent changing a Map while someone is
	// accessing it.
	Map<String, ResourceBundle> map = new HashMap<String, ResourceBundle>(_cache);

	// Add the new bundle
	map.put(getCacheKey(baseName, locale), bundle);

	// Set this new Map as the shared cache Map
	_cache = map;
    }

    /**
     *	<p> This method obtains the requested <code>ResourceBundle</code> as
     *	    specified by the given <code>basename</code> and
     *	    <code>locale</code>.</p>
     *
     *	@param	baseName    The base name for the <code>ResourceBundle</code>.
     *	@param	locale	    The desired <code>Locale</code>.
     */
    public ResourceBundle getBundle(String baseName, Locale locale) {
	ResourceBundle bundle = getCachedBundle(baseName, locale);
	if (bundle == null) {
	    try {
		bundle = ResourceBundle.getBundle(baseName, locale,
		    Util.getClassLoader(baseName));
	    } catch (MissingResourceException ex) {
		// Use System.out.println b/c we don't want infinite loop and
		// we're too lazy to do more...
		System.out.println("Can't find bundle: " + baseName);
		ex.printStackTrace();
	    }
	    if (bundle != null) {
		addCachedBundle(baseName, locale, bundle);
	    }
	}
	return bundle;
    }

    /**
     *	<p> This method obtains the requested <code>ResourceBundle</ocde> as
     *	    specified by the given basename, locale, and classloader.</p>
     *
     *	@param	baseName    The base name for the <code>ResourceBundle</code>.
     *	@param	locale	    The desired <code>Locale</code>.
     *	@param	loader	    The <code>ClassLoader</code> that should be used.
     */
    public ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
	ResourceBundle bundle = getCachedBundle(baseName, locale);
	if (bundle == null) {
	    bundle = ResourceBundle.getBundle(baseName, locale, loader);
	    if (bundle != null) {
		addCachedBundle(baseName, locale, bundle);
	    }
	}
	return bundle;
    }


    /**
     *	<p> Application scope key which stores the
     *	    <code>ResourceBundleManager</code> instance for this
     *	    application.</p>
     */
    private static final String RB_MGR	=   "__jsft_ResourceBundleMgr";

    /**
     *	<p> The cache of <code>ResourceBundle</code>s.</p>
     */
    private Map<String, ResourceBundle>	_cache = new HashMap<String, ResourceBundle>();
}
