/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph;

import java.util.Map;

import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;

/**
 * Interface for query style information
 *
 * FIXME: This interface should replace the equivalent methods
 *  in the presentation providers once this interface is API.
 *  By splitting the two APIs, we can create style widget for things
 *  that are not views, like the overlays.
 *
 * @author Guillaume Champagne
 */
public interface ITimeGraphStyleProvider {

    /**
     *
     * @param event
     *          The time event
     * @return
     *          The style map
     */
    Map<String, Object> getEventStyle(ITimeEvent event);

    /**
     * Returns table of states with state name to state color relationship.
     *
     * @return table of states with color and name
     *
     */
    StateItem[] getStateTable();

    /**
     * Returns the name of state types.
     *
     * @return the name of state types
     */
    String getStateTypeName();

    /**
     * Get the name of the link type
     *
     * @return The name of the link type
     */
    default String getLinkTypeName() {
        return Messages.TimeGraphLegend_Arrows;
    }

    /**
     * Get the id of the state provider
     *
     * @return The preference key, if there are many instances of a class, this
     *         method needs to be overridden
     */
    default String getPreferenceKey() {
        return getClass().getName();
    }

    /**
     * Signal the provider that its color settings have changed
     */
    default void refresh(){
        // do nothing
    }
}
