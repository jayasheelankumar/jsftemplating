/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://jsftemplating.dev.java.net/cddl1.html or
 * jsftemplating/cddl1.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at jsftemplating/cddl1.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.sun.jsftemplating.layout;

import com.sun.jsftemplating.annotation.FormatDefinitionAPFactory;
import com.sun.jsftemplating.annotation.HandlerAPFactory;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.UIComponentFactoryAPFactory;
import com.sun.jsftemplating.component.factory.basic.GenericFactory;
import com.sun.jsftemplating.layout.descriptors.ComponentType;
import com.sun.jsftemplating.layout.descriptors.LayoutComponent;
import com.sun.jsftemplating.layout.descriptors.LayoutDefinition;
import com.sun.jsftemplating.layout.descriptors.LayoutElement;
import com.sun.jsftemplating.layout.descriptors.Resource;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerDefinition;
import com.sun.jsftemplating.layout.descriptors.handler.IODescriptor;
import com.sun.jsftemplating.layout.facelets.DbFactory;
import com.sun.jsftemplating.layout.facelets.NSContext;
import com.sun.jsftemplating.util.LogUtil;
import com.sun.jsftemplating.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.faces.context.FacesContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *  <p>	This abstract class provides the base functionality for all
 *	<code>LayoutDefinitionManager</code> implementations.  It provides a
 *	static method used to obtain an instance of a concrete
 *	<code>LayoutDefinitionManager</code>:
 *	{@link #getLayoutDefinitionManager(FacesContext,String)}.  However, in
 *	most cases is makes the most sense to call the static method:
 *	{@link #getLayoutDefinition(FacesContext,String)}.  This method
 *	ensures that the cache is checked first before going through the effort
 *	of finding a <code>LayoutDefinitionManager</code> instance.</p>
 *
 *  <p>	This class also provides access to global {@link HandlerDefinition}s,
 *	{@link Resource}s, and {@link ComponentType}s.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public abstract class LayoutDefinitionManager {


    /**
     *	<p> Constructor.</p>
     */
    protected LayoutDefinitionManager() {
        super();
    }


    /**
     *	<p> This method is responsible for finding/creating the requested
     *	    {@link LayoutDefinition}.</p>
     *
     *	@param	key The key used to identify the requested
     *		    {@link LayoutDefinition}.
     */
    public abstract LayoutDefinition getLayoutDefinition(String key) throws LayoutDefinitionException;

    /**
     *	<p> This method is used to determine if this
     *	    <code>LayoutDefinitionManager</code> should process the given key.
     *	    It does not necessarily mean that the
     *	    <code>LayoutDefinitionManager</code> <em>can</em> process it.
     *	    Parser errors do not necessarily mean that it should not process
     *	    the file.  In order to provide meaningful error messages, this
     *	    method should return true if the format of the template matches the
     *	    type that this <code>LayoutDefinitionManager</code> processes.  It
     *	    is understood that at times it may not be recognizable; in the case
     *	    where no <code>LayoutDefinitionManager</code>s return
     *	    <code>true</code> from this method, the parent
     *	    <code>ViewHandler</code> will be used, which likely means that it
     *	    will look for a .jsp and give error messages accordingly.  Also,
     *	    the existance of a file should not be used as a meassure of success
     *	    as other <code>LayoutDefinitionManager</code>s may be more
     *	    appropriate.</p>
     */
    public abstract boolean accepts(String key);

    /**
     *	<p> This method should be used to obtain a {@link LayoutDefinition}.
     *	    It first checks to see if a cached {@link LayoutDefinition}
     *	    already exists, if so it returns it.  If one does not already
     *	    exist, it will obtain the appropriate
     *	    <code>LayoutDefinitionManager</code> instance and call
     *	    {@link #getLayoutDefinition} and return the result.</p>
     */
    public static LayoutDefinition getLayoutDefinition(FacesContext ctx, String key) throws LayoutDefinitionException {
        // Remove leading '/' characters
        while (key.startsWith("/")) {
            key = key.substring(1);
        }

        // Check to see if we already have it.
        LayoutDefinition def = getCachedLayoutDefinition(key);
        if (def == null) {
            // Obtain the correct LDM, and get the LD
            def = getLayoutDefinitionManager(ctx, key).getLayoutDefinition(key);
        } else {
            // In the case where we found a cached version,
            // ensure we invoke "initPage" handlers
            def.dispatchInitPageHandlers(ctx, def);
        }
// FIXME: Flag a page as *not found* for performance reasons when JSP is used (or other view technologies)

        // Return the LD
        return def;
    }

    /**
     *	<p> This method finds the (closest) requested
     *	    <code>LayoutComponent</code> for the given <code>clientId</code>.
     *	    If the <code>viewId</code> is not supplied, the current
     *	    <code>UIViewRoot</code> will be used (NOTE: it must be a
     *	    {@link LayoutViewRoot}).  If an exact match is not found, it will
     *	    return the last {@link LayoutComponent} found while walking the
     *	    tree -- this represents the last {@link LayoutComponent} in the
     *	    hierarchy of the specified component.  If nothing matches the
     *	    given <code>clientId</code>, <code>null</code> will be returned.</p>
     *
     *	<p> This is not an easy process since JSF components may not all be
     *	    <code>NamingContainer</code>s, so the clientId is not sufficient to
     *	    find it.  This is unfortunate, but we we have to deal with it.</p>
     *
     *	@param	ctx	    The <code>FacesContext</code>.
     *	@param	ldKey	    The {@link LayoutDefinition} key to identify the
     *			    {@link LayoutDefinition} tree to be searched.
     *	@param	clientId    The component <code>clientId</code> for which to
     *			    obtain a {@link LayoutComponent}.
     */
    public static LayoutComponent getLayoutComponent(FacesContext ctx, String ldKey, String clientId) throws LayoutDefinitionException {
        // Find the page first...
        LayoutElement layElt = null;
        if (ldKey != null) {
            layElt = getLayoutDefinition(ctx, ldKey);
            if (layElt == null) {
                throw new LayoutDefinitionException(
                        "Unable to find LayoutDefinition ('" + ldKey + "')");
            }
        } else {
            layElt = ((LayoutViewRoot) ctx.getViewRoot()).getLayoutDefinition(ctx);
        }

        // Create a StringTokenizer over the clientId
        StringTokenizer tok = new StringTokenizer(clientId, ":");

        // Walk the LD looking for the individual id's specified in the
        // clientId.
        String id = null;
        LayoutElement match = null;
        while (tok.hasMoreTokens()) {
            // I don't want to create a bunch of objects to check for
            // instanceof NamingContainer.  I can't check the class file b/c
            // there is no way for me to know what class gets created before
            // actually creating the UIComponent.  This is because either the
            // ComponentFactory can decide how to create the UIComponent, which
            // it often uses the Application.  The Application is driven off
            // the faces-config.xml file(s).
            //
            // I will instead do a brute force search for a match.  This has
            // the potential to fail if non-naming containers have the same
            // id's as naming containers.  It may also fail for components with
            // dynamic id's.
            id = tok.nextToken();
            match = findById(ctx, layElt, id);
            if (match == null) {
                // Can't go any further! We're as close as we're going to get.
                break;
            }
            layElt = match;
        }

        // Make sure we're not still at the LayoutDefinition, if so do NOT
        // accept this as a match.
        if (layElt instanceof LayoutDefinition) {
            layElt = null;
        }

        // Return the closest match (or null if nothing found)
        return (LayoutComponent) layElt;
    }

    /**
     *	<p> This method performs a breadth-first search for a child
     *	    {@link LayoutComponent} with the given <code>id</code> of the given
     *	    {@link LayoutElement} (<code>elt</code>).  It will return null if
     *	    none of the children (or children's children, etc.) equal the given
     *	    <code>id</code>.</p>
     */
    private static LayoutComponent findById(FacesContext ctx, LayoutElement elt, String id) {
        // First search the direct child LayoutElement
        for (LayoutElement child : elt.getChildLayoutElements()) {
            // I am *NOT* providing the parent UIComponent as it may not be
            // available, this function is *not* guaranteed to work for
            // dynamic ids
            if (child.getId(ctx, null).equals(id)
                    && (child instanceof LayoutComponent)) {
                // Found it!
                return (LayoutComponent) child;
            }
        }

        // Not found directly under it, search children...
        // NOTE: Must do a breadth first search, so 2 loops are necessary
        LayoutComponent comp = null;
        for (LayoutElement child : elt.getChildLayoutElements()) {
            comp = findById(ctx, child, id);
            if (comp != null) {
                // Found it!
                break;
            }
        }

        // Return the result, or null if not found
        return comp;
    }

    /**
     *	<p> This method obtains the <code>LayoutDefinitionManager</code> that
     *	    is able to process the given <code>key</code>.</p>
     *
     *	<p> This implementation uses the <code>ExternalContext</code>'s
     *	    initParams to look for the <code>LayoutDefinitionManager</code>
     *	    class.  If it exists, the specified concrete
     *	    <code>LayoutDefinitionManager</code> class will be used as the
     *	    "default" (i.e. the first <code>LayoutDefinitionManager</code>
     *	    checked).  Otherwise,
     *	    {@link #DEFAULT_LAYOUT_DEFINITION_MANAGER_IMPL} will be used.
     *	    "{@link #LAYOUT_DEFINITION_MANAGER_KEY}" is the initParam key.</p>
     *
     *	<p> The <code>key</code> is used to test if desired
     *	    <code>LayoutDefinitionManager</code> is able to read the requested
     *	    {@link LayoutDefinition}.</p>
     *
     *	@param	ctx	The <code>FacesContext</code>.
     *	@param	key	The desired {@link LayoutDefinition}.
     *	@see #LAYOUT_DEFINITION_MANAGER_KEY
     */
    public static LayoutDefinitionManager getLayoutDefinitionManager(FacesContext ctx, String key) throws LayoutDefinitionException {
        List<String> ldms = getLayoutDefinitionManagers(ctx);
        LayoutDefinitionManager mgr = null;
        for (String className : ldms) {
            mgr = getLayoutDefinitionManager(className);
            if (mgr.accepts(key)) {
                return mgr;
            }
        }
        throw new LayoutDefinitionException("No LayoutDefinitionManager "
                + "available for '" + key + "'.  This may mean the file cannot "
                + "be found, or is unrecognizable.");
    }

    /**
     *	<p> This method is responsible for returning a <code>List</code> of
     *	    known <code>LayoutDefinitionManager</code> instances.  Each value
     *	    of the list is a <code>String</code> representing the classname of
     *	    a <code>LayoutDefinitionManager</code> implementation.</p>
     */
    public static List<String> getLayoutDefinitionManagers(FacesContext ctx) {
        if (_ldmKeys == null) {
            List<String> keys = new ArrayList<String>();
            String def = "";

            // Check to see what the default should be...
            if (ctx != null) {
                Map initParams = ctx.getExternalContext().getInitParameterMap();
                if (initParams.containsKey(LAYOUT_DEFINITION_MANAGER_KEY)) {
                    def = ((String)
                            initParams.get(LAYOUT_DEFINITION_MANAGER_KEY)).trim();
                    keys.add(def);
                }
            }

            try {
                // Get all the files that define them
                BufferedReader rdr = null;
                InputStream is = null;
                String line = null;
                Enumeration<URL> urls = Util.getClassLoader(ctx).
                        getResources(FormatDefinitionAPFactory.FACTORY_FILE);
                while (urls.hasMoreElements()) {
                    // Add all lines in each file to the list of LDMs
                    try {
                        is = urls.nextElement().openStream();
                        rdr = new BufferedReader(new InputStreamReader(is));
                        line = rdr.readLine();
                        while (line != null) {
                            line = line.trim();

                            if (line.equals("") || line.startsWith("#")) {
                                // Skip comments
                                continue;
                            }
                            if (line.equals(def)) {
                                // Skip the default one so that it doesn't get
                                // added again (we want it first)
                                continue;
                            }

                            // Add it!
                            keys.add(line);

                            // Get the next line
                            line = rdr.readLine();
                        }
                    } finally {
                        Util.closeStream(is);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            _ldmKeys = keys;
        }
        return _ldmKeys;
    }

    /**
     *	<p> This method is a singleton factory method for obtaining an instance
     *	    of a <code>LayoutDefintionManager</code>.  It is possible that
     *	    multiple different implementations of
     *	    <code>LayoutDefinitionManager</code>s will be used within the same
     *	    JVM.  This is OK, the purpose of the
     *	    <code>LayoutDefinitionManager</code> is primarily performance.
     *	    Someone may provide a different <code>LayoutDefinitionManager</code>
     *	    to locate {@link LayoutDefinition}'s in a different way (XML,
     *	    database, file, java code, etc.).</p>
     */
    public static LayoutDefinitionManager getLayoutDefinitionManager(String className) {
        LayoutDefinitionManager ldm =
                (LayoutDefinitionManager) _instances.get(className);
        if (ldm == null) {
            try {
                ldm = (LayoutDefinitionManager)
		    Util.loadClass(className, className).
                        getMethod("getInstance", (Class[]) null).
                        invoke((Object) null, (Object[]) null);
            } catch (ClassNotFoundException ex) {
                throw new LayoutDefinitionException(
                        "Unable to find LDM: '" + className + "'.", ex);
            } catch (NoSuchMethodException ex) {
                throw new LayoutDefinitionException("LDM '" + className
                        + "' does not have a 'getInstance()' method!", ex);
            } catch (IllegalAccessException ex) {
                throw new LayoutDefinitionException("Unable to access LDM: '"
                        + className + "'!", ex);
            } catch (InvocationTargetException ex) {
                throw new LayoutDefinitionException("Error while attempting "
                        + "to get LDM: '" + className + "'!", ex);
            } catch (ClassCastException ex) {
                throw new LayoutDefinitionException("LDM '" + className
                        + "' must extend from '"
                        + LayoutDefinitionManager.class.getName() + " and must "
                        + "be loaded from the same ClassLoader!", ex);
            } catch (NullPointerException ex) {
                throw new LayoutDefinitionException(ex);
            }
            _instances.put(className, ldm);
        }
        return ldm;
    }

    /**
     *	<p> This method may be used to obtain a cached
     *	    {@link LayoutDefinition}.  If it has not been cached, this method
     *	    returns <code>null</code>.</p>
     *
     *	@param	key	Key for the cached {@link LayoutDefinition} to obtain.
     *
     *	@return The {@link LayoutDefinition} or <code>null</code>.
     */
    public static LayoutDefinition getCachedLayoutDefinition(String key) {
        if (isDebug()) {
            FacesContext ctx = FacesContext.getCurrentInstance();
	    if (ctx != null) {
		// Make sure we cache during the life of the request, even
		// in Debug mode
		return (LayoutDefinition) ctx.getExternalContext().
		    getRequestMap().get(CACHE_PREFIX + key);
	    }

            // Disable caching for debug mode
            return null;
        }

        return _layoutDefinitions.get(key);
    }

    /**
     *	<p> In general, this method should be used by sub-classes to store a
     *	    cached {@link LayoutDefinition}.  It may also be used, however, to
     *	    define {@link LayoutDefinition}s on the fly (not recommended unless
     *	    you know what you're doing. ;)</p>
     *
     *	@param	key	The {@link LayoutDefinition} key to cache.
     *	@param	value	The {@link LayoutDefinition} to cache.
     */
    public static void putCachedLayoutDefinition(String key, LayoutDefinition value) {
	if (isDebug()) {
	    FacesContext ctx = FacesContext.getCurrentInstance();
	    if (ctx != null) {
		// Make sure we cache during the life of the request, even
		// in Debug mode
		ctx.getExternalContext().getRequestMap().
		    put(CACHE_PREFIX + key, value);
	    }
	} else {
	    synchronized (_layoutDefinitions) {
		_layoutDefinitions.put(key, value);
	    }
	}
    }

    /**
     *	<p> Retrieve an attribute by key.</p>
     *
     *	@param	key	The key used to retrieve the attribute.
     *
     *	@return The requested attribute or null
     */
    public Object getAttribute(String key) {
        return _attributes.get(key);
    }


    /**
     *	<p> Associate the given key with the given Object as an attribute.</p>
     *
     *	@param	key	The key associated with the given object (if this key
     *			is already in use, it will replace the previously set
     *			attribute object).
     *	@param	value	The Object to store.
     */
    public void setAttribute(String key, Object value) {
        _attributes.put(key, value);
    }

    /**
     *	<p> This method returns the <code>Map</code> of global
     *	    {@link ComponentType}s (the {@link ComponentType}s available across
     *	    the application).</p>
     *
     *	<p> It is recommended that this method not be used directly.  The map
     *	    returned by this method is shared across the application and is not
     *	    thread safe.  Instead access this Map via
     *	    {@link LayoutDefinitionManager#getGlobalComponentType(String)}.</p>
     *
     *	<p> This method will initialize the global {@link ComponentType}s if
     *	    they are not initialized.  It does this by finding all files in the
     *	    classpath named:
     *	    {@link UIComponentFactoryAPFactory#FACTORY_FILE}.  It then reads
     *	    each of these files (which must be <code>Properties</code> files)
     *	    and stores each identifier / fully qualified classname as an entry
     *	    in the <code>Map&lt;String, {@link ComponentType}&gt;</code>.</p>
     */
    public static Map<String, ComponentType> getGlobalComponentTypes() {
        if (_globalComponentTypes == null) {
            // We haven't initialized the global ComponentTypes yet
            Map types = new HashMap<String, ComponentType>();

            try {
                Properties props = null;
                URL url = null;
                String id = null;
                // Get all the properties files that define them
                Enumeration<URL> urls =
                        Util.getClassLoader(types).
                                getResources(UIComponentFactoryAPFactory.FACTORY_FILE);
                while (urls.hasMoreElements()) {
                    url = urls.nextElement();
                    props = new Properties();
                    // Load each Properties file
                    InputStream is = null;
                    try {
                        is = url.openStream();
                        props.load(is);
                        for (Map.Entry<Object, Object> entry : props.entrySet()) {
                            // Add each property entry (key, ComponentType)
                            id = (String) entry.getKey();
                            types.put(id, new ComponentType(id, (String) entry.getValue()));
                        }
                    } finally {
                        Util.closeStream(is);
                    }
                }
                readComponentsFromTaglibXml(types);
                _globalComponentTypes = types;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return _globalComponentTypes;
    }
    
    private static void readComponentsFromTaglibXml(Map<String, ComponentType> types) throws IOException {
	Enumeration<URL> urls = Util.getClassLoader(types).getResources("META-INF/");
	Set<URL> files = new HashSet<URL>();
	while (urls.hasMoreElements()) {
	    URL url = urls.nextElement();
	    URLConnection conn = url.openConnection();
	    conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            
	    if (conn instanceof JarURLConnection) {
		JarURLConnection jarConn = (JarURLConnection) conn;
		JarFile jarFile = jarConn.getJarFile();
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
		    JarEntry entry = entries.nextElement();
		    String entryName = entry.getName();
		    if (entryName.startsWith("META-INF") && entryName.endsWith("taglib.xml")) {
			Enumeration<URL> e = Util.getClassLoader(types).getResources(entryName);
			while (e.hasMoreElements()) {
			    files.add(e.nextElement());
			}
		    }
		}
	    } else {
		// TODO:  I have yet to hit this branch
//		File dir = new File(url.getFile());
//		String fileList[] = dir.list(new FilenameFilter() {
//		    public boolean accept(File file, String fileName) {
//			return fileName.endsWith("taglib.xml");
//		    } });
	    }
	    
	}
	if(files.size() >0) {
	    for (URL url : files) {
		processTaglibXml(url, types);
	    }
	}
    }
    
    private static void processTaglibXml(URL url, Map<String, ComponentType> types) {
	InputStream is = null;
	try {
	    is = url.openStream();
	    DocumentBuilder builder = DbFactory.getInstance();
	    Document document = builder.parse(is);
	    XPath xpath = XPathFactory.newInstance().newXPath();
	    NSContext ns = new NSContext();
	    ns.addNamespace("f", "http://java.sun.com/JSF/Facelet");
	    // Although the following should work, XPath doesn't attempt to
	    // get the namespace when no namespace is supplied.
	    //ns.setDefaultNSURI("http://java.sun.com/JSF/Facelet");
	    xpath.setNamespaceContext(ns);
	    
	    String nameSpace = xpath.evaluate("/f:facelet-taglib/f:namespace", document);

	    // Process <tag> elements
	    NodeList nl = (NodeList) xpath.evaluate("/f:facelet-taglib/f:tag", document, XPathConstants.NODESET);
	    for (int i = 0; i < nl.getLength(); i++) {
		Node node = nl.item(i);
		String tagName = xpath.evaluate("f:tag-name", node);
		String componentType = xpath.evaluate("f:component/f:component-type", node);
		String id = nameSpace + ":" + tagName;
		types.put(id, 
			new ComponentType(id, GenericFactory.class.getName(), componentType));
	    }
	} catch (Exception e) {
	    if (LogUtil.severeEnabled()) {
		LogUtil.severe(e.getMessage());
	    }
	    throw new RuntimeException(e);
	} finally {
	    Util.closeStream(is);
	}
    }

    /**
     *	<p> This method retrieves a globally defined {@link ComponentType} (a
     *	    {@link ComponentType} available across the application).</p>
     */
    public static ComponentType getGlobalComponentType(String typeID) {
        return getGlobalComponentTypes().get(typeID);
    }

    /**
     *	<p> This method allows a global {@link ComponentType} to be added.
     *	    This way of adding a global {@link ComponentType} is discouraged.
     *	    Instead, you should use a <code>UIComponentFactory</code>
     *	    annotation in each <code>ComponentFactory</code> and compile
     *	    using "<code>apt</code>".</p>
     */
    public static void addGlobalComponentType(ComponentType type) {
        synchronized (LayoutDefinitionManager.class) {
            getGlobalComponentTypes().put(type.getId(), type);
        }
    }

    /**
     *	<p> This method clears the cached global {@link ComponentType}s.</p>
     */
    public static void clearGlobalComponentTypes() {
        _globalComponentTypes = null;
    }

    /**
     *	<p> This method returns the <code>Map</code> of global
     *	    {@link HandlerDefinition}s (the {@link HandlerDefinition}s
     *	    available across the application).</p>
     *
     *	<p> It is recommended that this method not be used.  The map returned
     *	    by this method is shared across the application and is not thread
     *	    safe.  Instead get values from the Map via:
     *	    {@link LayoutDefinitionManager#getGlobalHandlerDefinition(String)}.
     *	    </p>
     *
     *	<p> This method will initialize the global {@link HandlerDefinition}s if
     *	    they are not initialized.  It does this by finding all files in the
     *	    classpath named: {@link HandlerAPFactory#HANDLER_FILE}.  It then
     *	    reads each file (which must be a valid <code>Properties</code>
     *	    file) and stores the information for later retrieval.</p>
     */
    public static Map<String, HandlerDefinition> getGlobalHandlerDefinitions() {
        if (_globalHandlerDefs != null) {
            // We've already done this, return the answer
            return _globalHandlerDefs;
        }

        // Create a new Map to hold the defs
        _globalHandlerDefs = new HashMap<String, HandlerDefinition>();
        Properties props = null;
        URL url = null;
        try {
            // Get all the properties files that define them
            Enumeration<URL> urls =
                    Util.getClassLoader(_globalHandlerDefs).
                            getResources(HandlerAPFactory.HANDLER_FILE);
            InputStream is = null;
            while (urls.hasMoreElements()) {
                try {
                    url = urls.nextElement();
                    props = new Properties();
                    // Load each Properties file
                    is = url.openStream();
                    props.load(is);
                    for (Map.Entry<Object, Object> entry : props.entrySet()) {
                        if (((String) entry.getKey()).endsWith(".class")) {
                            // We will only process .class entries. Check with Ken to change this into HashMap
                            readGlobalHandlerDefinition((Map) props, entry);
                        }
                    }
                } finally {
                    Util.closeStream(is);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return _globalHandlerDefs;
    }

    /**
     *	<p> This method processes a single {@link HandlerDefinition}'s
     *	    meta-data.</p>
     */
    private static void readGlobalHandlerDefinition(Map<String, String> map, Map.Entry<Object, Object> entry) {
        // Get the key.class value...
        String key = (String) entry.getKey();
        // Strip off .class
        key = key.substring(0, key.lastIndexOf('.'));

        // Create a new HandlerDefinition
        HandlerDefinition def = new HandlerDefinition(key);

        // Set the class / method
        String value = map.get(key + '.' + "method");
        def.setHandlerMethod((String) entry.getValue(), value);

        // Read the input defs
        def.setInputDefs(readIODefs(map, key, true));

        // Read the output defs
        def.setOutputDefs(readIODefs(map, key, false));

        // Add the Handler...
        _globalHandlerDefs.put(key, def);
    }

    /**
     *	<p> This method reads and creates IODescriptors for the given key.</p>
     */
    private static Map<String, IODescriptor> readIODefs(Map<String, String> map, String key, boolean input) {
        String type;
        String inOrOut = input ? "input" : "output";
        int count = 0;
        IODescriptor desc = null;
        Map<String, IODescriptor> defs = new HashMap<String, IODescriptor>(5);
        String value = map.get(key + "." + inOrOut + "[" + count + "].name");
        while (value != null) {
            // Get the type
            type = map.get(key + "." + inOrOut + "[" + count + "].type");
            if (type == null) {
                type = DEFAULT_TYPE;
            }

            // Create an IODescriptor
            desc = new IODescriptor(value, type);
            defs.put(value, desc);

            // If this is an output, we're done... for input we need to do more
            if (input) {
                // required?
                value = map.get(key + "." + inOrOut + "[" + count + "].required");
                if ((value != null) && Boolean.valueOf(value).booleanValue()) {
                    desc.setRequired(true);
                }

                // default?
                value = map.get(key + "." + inOrOut + "[" + count + "].defaultValue");
                if ((value != null) && !value.equals(HandlerInput.DEFAULT_DEFAULT_VALUE)) {
                    desc.setDefault(value);
                }
            }

            // Look for next IO declaration
            value = map.get(key + "." + inOrOut + "[" + (++count) + "].name");
        }

        return defs;
    }

    /**
     *	<p> This method retrieves a globally defined {@link HandlerDefinition} (a
     *	    {@link HandlerDefinition} available across the application).</p>
     */
    public static HandlerDefinition getGlobalHandlerDefinition(String id) {
        return getGlobalHandlerDefinitions().get(id);
    }

    /**
     *	<p> This method allows a global {@link HandlerDefinition} to be added.
     *	    This way of adding a global {@link HandlerDefinition} is
     *	    discouraged.  It should be done implicitly through annotations,
     *	    placement of a properties file in the correct location, or
     *	    explicitly by declaring it the page (some template formats may not
     *	    support this).</p>
     *
     *	@see LayoutDefinitionManager#getGlobalHandlerDefinitions()
     */
    public static void addGlobalHandlerDefinition(HandlerDefinition def) {
        synchronized (LayoutDefinitionManager.class) {
            getGlobalHandlerDefinitions().put(def.getId(), def);
        }
    }

    /**
     *	<p> This method clears cached global {@link HandlerDefinition}s.</p>
     */
    public static void clearGlobalHandlerDefinitions() {
        _globalHandlerDefs = null;
    }

    /**
     *	<p> This method provides a means to add an additional global
     *	    {@link Resource} (a {@link Resource} that is available across the
     *	    application).  It is recommended that this not be done using this
     *	    method, but instead by registering the global {@link Resource}.
     *	    This can be done by... FIXME: TBD...</p>
     */
    public static void addGlobalResource(Resource res) {
        synchronized (LayoutDefinitionManager.class) {
            getGlobalResources().add(res);
        }
    }

    /**
     *	<p> This method returns a <code>List</code> of global
     *	    {@link Resource}s.  The <code>List</code> returned should not be
     *	    changed, it is the actual internal <code>List</code> that is shared
     *	    across the application and it is not thread safe.</p>
     *
     *	<p> This method will find global resources by... FIXME: TBD...</p>
     */
    public static List<Resource> getGlobalResources() {
// FIXME: TBD...
        if (_globalResources == null) {
            _globalResources = new ArrayList<Resource>();
// FIXME: Find / Initialize resources...
        }
        return _globalResources;
    }

    /**
     *	<p> This method clears the cached global {@link Resource}s.</p>
     */
    public static void clearGlobalResources() {
        _globalResources = null;
    }

    /**
     *	<p> Getter for the debug flag.</p>
     */
    public static boolean isDebug() {
        if (_debug != null) {
            return _debug.booleanValue();
        }
        boolean flag = Boolean.getBoolean(DEBUG_FLAG);
        if (!flag) {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null) {
                String initParam =
                        ctx.getExternalContext().getInitParameter(DEBUG_FLAG);
                if (initParam != null) {
                    flag = Boolean.parseBoolean(initParam);
                }
            }
        }
        _debug = Boolean.valueOf(flag);
        return flag;
    }

    /**
     *	<p> Setter for the debug flag.</p>
     */
    public static void setDebug(boolean flag) {
        _debug = Boolean.valueOf(flag);
    }

    /**
     *	<p> This map contains sub-class specific attributes that may be needed
     *	    by specific implementations of
     *	    <code>LayoutDefinitionManager</code>s.  For example, setting an
     *	    <code>EntityResolver</code> on a
     *	    <code>LayoutDefinitionManager</code> that creates
     *	    <code>LayoutDefinitions</code> from XML files.</p>
     */
    private Map<String, Object> _attributes = new HashMap<String, Object>();

    /**
     *	<p> Static <code>Map</code> of <code>LayoutDefinitionManager</code s.
     *	    Normally this will only contain the default
     *	    {@link LayoutDefinitionManager}.</p>
     */
    private static Map<String, LayoutDefinitionManager> _instances =
            new HashMap<String, LayoutDefinitionManager>(4);

    /**
     *	<p> Static <code>Map</code> of cached {@link LayoutDefinition}s.</p>
     */
    private static Map<String, LayoutDefinition> _layoutDefinitions =
            new HashMap<String, LayoutDefinition>();

    /**
     *	<p> This <code>Map</code> holds global {@link ComponentType}s so they
     *	    can be defined once and shared across the application.</p>
     */
    private static Map<String, ComponentType> _globalComponentTypes = null;

    /**
     *	<p> This <code>Map</code> holds global {@link HandlerDefinition}s so
     *	    they can be defined once and shared across the application.</p>
     */
    private static Map<String, HandlerDefinition> _globalHandlerDefs = null;

    /**
     *	<p> This <code>List</code> holds global {@link Resource}s so
     *	    they can be defined once and shared across the application.</p>
     */
    private static List<Resource> _globalResources = null;

    /**
     *	<p> This <code>List</code> contains the classnames of known
     *	    {@link LayoutDefinitionManager} instances.</p>
     */
    private static List<String> _ldmKeys = null;

    /**
     *	<p> This is the default input and output type.</p>
     */
    public static final String DEFAULT_TYPE = "Object";

    /**
     *	<p> This constant defines the <code>LayoutDefinitionManager</code>
     *	    implementation key for initParams.
     *	    ("LayoutDefinitionManagerImpl")</p>
     */
    public static final String LAYOUT_DEFINITION_MANAGER_KEY =
            "LayoutDefinitionManagerImpl";

    /**
     *	<p> This is the name of the initParameter or JVM variable used to set
     *	    the DEBUG flag.</p>
     */
    public static final String DEBUG_FLAG = "com.sun.jsftemplating.DEBUG";

    /**
     *	<p> DEBUG flag.  Not final so that it can be switch at runtime.</p>
     */
    private static Boolean _debug = null;

    /**
     *	<p> DEBUG flag.</p>
     *
     *	@deprecated Use isDebug() instead.  This value will not reflect
     *	            runtime changes in this flag.
     */
    public static final boolean DEBUG = isDebug();

    /**
     *	<p> This is the prefix of a request-scoped variable that caches
     *	    {@link LayoutDefintion}s.</p>
     */
    public static final String CACHE_PREFIX = "_LDCache";
}
