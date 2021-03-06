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

package com.sun.jsftemplating.layout.event;

import javax.faces.component.UIComponent;


/**
 *  <p>	This event is typically invoked when a factory not only creates a
 *	component, but creates children under that component.  This event may
 *	be invoked to allow a page author to have greater control over what
 *	happens during the child creation.  See individual factory JavaDocs to
 *	see which factories support this and what may be done during this
 *	event.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public class CreateChildEvent extends EventObjectBase implements UIComponentHolder {
    private static final long serialVersionUID = 1L;

    /**
     *	<p> Constructor.</p>
     *
     *	@param	component   The <code>UIComponent</code> associated with this
     *			    <code>EventObject</code>.
     */
    public CreateChildEvent(UIComponent component) {
	super(component);
    }

    /**
     *	<p> Constructor.</p>
     *
     *	@param	component   The <code>UIComponent</code> associated with this
     *			    <code>EventObject</code>.
     */
    public CreateChildEvent(UIComponent component, Object data) {
	super(component);
	setData(data);
    }

    /**
     *	<p> This method provides access to extra data that is set by the
     *	    creator of this Event.  See documentation of the code that fires
     *	    this event to learn what (if anything) is stored here.</p>
     */
    public Object getData() {
        return _data;
    }

    /**
     *	<p> This setter allows extending classes to set this value via this
     *	    setter.  Normally this value is passed into the constructor.<p>
     */
    protected void setData(Object data) {
        _data = data;
    }

    /**
     *	<p> The "createChild" event type. ("createChild")</p>
     */
    public static final String EVENT_TYPE   =	"createChild";

    /**
     *	<p> This value provides extra information that can be associated with
     *	    this event.  See code that uses this event to learn more about
     *	    what (if anything) this information is used for.</p>
     */
    private Object _data = null;
}
