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

package com.sun.jsftemplating.layout.descriptors;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import com.sun.jsftemplating.el.PermissionChecker;
import com.sun.jsftemplating.layout.LayoutDefinitionManager;


/**
 *  <p>	This class defines a LayoutIf {@link LayoutElement}.  The LayoutIf
 *	provides the functionality necessary to conditionally display a portion
 *	of the layout tree.  The condition is a boolean equation and may use
 *	"$...{...}" type expressions to substitute in values.</p>
 *
 *  <p>	Depending on its environment, this {@link LayoutElement} can represent
 *	an {@link com.sun.jsftemplating.component.If} <code>UIComponent</code>
 *	or simply exist as a {@link LayoutElement}.  When its {@link #encode}
 *	method is called, the if functionality will act as a
 *	{@link LayoutElement}.  When the
 *	{@link LayoutComponent#getChild(FacesContext, UIComponent)} method is
 *	called, it will create an {@link com.sun.jsftemplating.component.If}
 *	<code>UIComponent</code>.</p>
 *
 *  @see com.sun.jsftemplating.el.VariableResolver
 *  @see com.sun.jsftemplating.el.PermissionChecker
 *
 *  @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class LayoutIf extends LayoutComponent {
    private static final long serialVersionUID = 1L;

    /**
     *	Constructor.
     */
    public LayoutIf(LayoutElement parent, String condition) {
	this(parent, condition,
	    LayoutDefinitionManager.getGlobalComponentType(null, "if"));
    }

    /**
     *	<p> This constructor may be used by subclasses which wish to provide
     *	    an alternate {@link ComponentType}.  The {@link ComponentType} is
     *	    used to instantiate an {@link com.sun.jsftemplating.component.If}
     *	    <code>UIComponent</code> (or whatever the given
     *	    {@link ComponentType} specifies).  This occurs when this
     *	    {@link LayoutElement} is nested inside a {@link LayoutComponent}.
     *	    It must create a <code>UIComponent</code> in order to ensure it
     *	    is executed because during rendering there is no other way to get
     *	    control to perform the functionality provided by this
     *	    {@link LayoutElement}.</p>
     */
    protected LayoutIf(LayoutElement parent, String condition, ComponentType type) {
	super(parent, (String) null, type);
	addOption("condition", condition);
	if (condition.equals("$property{condition}")) {
	    _doubleEval = true;
	}
    }
// FIXME: getHandlers() may need to be overriden to prevent beforeEncode/afterEncode from being called multiple times in some cases.  I may also need to explicitly invoke these Handlers in some cases (in the Component??); See LayoutForEach for example of what may need to be done...

    /**
     *	This method returns true if the condition of this LayoutIf is met,
     *	false otherwise.  This provides the functionality for conditionally
     *	displaying a portion of the layout tree.
     *
     *	@param	ctx    The FacesContext
     *	@param	comp   The UIComponent
     *
     *	@return	true if children are to be rendered, false otherwise.
     */
    public boolean encodeThis(FacesContext ctx, UIComponent comp) {
	PermissionChecker checker = new PermissionChecker(
	    this, comp, (_doubleEval) ?
		((String) getEvaluatedOption(ctx, "condition", comp)) :
		((String) getOption("condition")));
	return checker.hasPermission();
    }

    /**
     *	<p> This flag is set to true when the condition equals
     *	    "$property{condition}".  This is a special case where the value to
     *	    be evaluated is not $property{condition}, but rather the value of
     *	    this expression.  This requires double evaluation to correct
     *	    interpret the expression.  For now this is a hack for this case
     *	    only.  In the future we may want to support an $eval{} or something
     *	    more general syntax for doing this declaratively.</p>
     *
     *	See LayoutForEach also.
     */
    private boolean _doubleEval = false;
}
