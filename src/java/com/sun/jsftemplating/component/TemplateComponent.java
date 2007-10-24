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
package com.sun.jsftemplating.component;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import com.sun.jsftemplating.layout.descriptors.LayoutDefinition;


/**
 *  <p>	This interface defines additional methods in addition to those defined
 *	by UIComponent that are needed to work with a TemplateRenderer.</p>
 *
 *  <p>	JSF did not define an interface for UIComponent, so I cannot extend an
 *	interface here.  This means that casting is needed to use UIComponent
 *	features from a TemplateComponent.</p>
 *
 *  <p>	If you need to have a <code>NamingContainer</code>, do not forget to
 *	implement that interface in addition to this interface.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public interface TemplateComponent extends ChildManager {

    /**
     *	This method will find the request child UIComponent by id.  If it is
     *	not found, it will attempt to create it if it can find a LayoutElement
     *	describing it.
     *
     *	@param	context	    The FacesContext
     *	@param	id	    The UIComponent id to search for
     *
     *	@return	The requested UIComponent
     */
    public UIComponent getChild(FacesContext context, String id);

    /**
     *	This method returns the LayoutDefinition associated with this component.
     *
     *	@param	context	The FacesContext
     *
     *	@return	LayoutDefinition associated with this component.
     */
    public LayoutDefinition getLayoutDefinition(FacesContext context);

    /**
     *	This method returns the LayoutDefinitionKey for this component.
     *
     *	@return	key	The key to use in the LayoutDefinitionManager
     */
    public String getLayoutDefinitionKey();

    /**
     *	This method sets the LayoutDefinition key for this component.
     *
     *	@param	key The key to use in the LayoutDefinitionManager
     */
    public void setLayoutDefinitionKey(String key);

    /**
     * This method returns the value for the current field.  It first checks
     * the value of the field passed in, returning that if it is set.  Next,
     * it checks to see if there is a ValueExpression matching the attributeName
     * and returns that value, if it exists.  If both of those tests fail, the
     * defaultValue is returned.
     * @param field
     * @param attributeName
     * @param defaultValue
     * @return
     */
    public <V> V getPropertyValue(V field, String attributeName, V defaultValue);
}
