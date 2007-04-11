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
package com.sun.jsftemplating.layout.descriptors;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;


/**
 *  <p>	This class defines the descriptor for LayoutFacet.  A LayoutFacet
 *	descriptor provides information needed to attempt to obtain a Facet
 *	from the UIComponent.  If the Facet doesn't exist, it also has the
 *	opportunity to provide a "default" in place of the facet.</p>
 *
 *  @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class LayoutFacet extends LayoutElementBase implements LayoutElement {
    private static final long serialVersionUID = 1L;

    /**
     *	Constructor
     */
    public LayoutFacet(LayoutElement parent, String id) {
	super(parent, id);
    }

    /**
     *	<p> Returns whether this LayoutFacet should be rendered.  When this
     *	    component is used to specify an actual facet (i.e. specifies a
     *	    <code>UIComponent</code>), it should not be rendred.  When it
     *	    defines a place holder for a facet, then it should be rendered.</p>
     *
     *	<p> This property is normally set when the LayoutElement tree is
     *	    created (i.e. <code>XMLLayoutDefinitionReader</code>).  One way to
     *	    know what to do is to see if the is <code>LayoutFacet</code> is
     *	    used inside a <code>LayoutComponent</code> or not.  If it is, it
     *	    can be assumed that it represents an actual facet, not a place
     *	    holder.</p>
     *
     *	@return	true if {@link #encodeThis(FacesContext, UIComponent)} should
     *		execute
     */
    public boolean isRendered() {
	return _rendered;
    }

    /**
     *
     */
    public void setRendered(boolean render) {
	_rendered = render;
    }

    /**
     *	<p>This method looks for the facet on the component.  If found, it
     *	renders it and returns false (so children will not be rendered).  If
     *	not found, it returns true so that children will be rendered.
     *	Children of a LayoutFacet represent the default value for the
     *	Facet.</p>
     *
     *	@param	context	    The <code>FacesContext</code>.
     *	@param	component  The parent <code>UIComponent</code>.
     *
     *	@return	true if children are to be rendered, false otherwise.
     */
    protected boolean encodeThis(FacesContext context, UIComponent component) throws IOException {
	// Make sure we are supposed to render facets
	if (!isRendered()) {
	    return false;
	}

	// Look for Facet
	component = (UIComponent) component.getFacets().
	    get(getId(context, component));
	if (component != null) {
	    // Found Facet, Display UIComponent
	    encodeChild(context, component);

	    // Return false so the default won't be rendered
	    return false;
	}

	// Return true so that the default will be rendered
	return true;
    }

    private boolean _rendered = true;
}
