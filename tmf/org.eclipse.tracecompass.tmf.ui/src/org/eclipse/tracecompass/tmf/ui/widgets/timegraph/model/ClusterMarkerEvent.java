/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

import org.eclipse.swt.graphics.RGBA;

/**
 * Time event for a cluster of marker events.
 *
 * @author Guillaume Champagne
 * @since 4.2
 */
public class ClusterMarkerEvent extends MarkerEvent {

    /**
     * Standard constructor
     *
     * @param entry
     *            The entry of the marker, or null
     * @param time
     *            The timestamp of this marker
     * @param duration
     *            The duration of the marker
     * @param category
     *            The category of the marker
     * @param color
     *            The marker color
     * @param label
     *            The label of the marker, or null
     * @param foreground
     *            true if the marker is drawn in foreground, and false otherwise
     */
    public ClusterMarkerEvent(ITimeGraphEntry entry, long time, long duration, String category, RGBA color, String label, boolean foreground) {
        super(entry, time, duration, category, color, label, foreground);
    }
}
