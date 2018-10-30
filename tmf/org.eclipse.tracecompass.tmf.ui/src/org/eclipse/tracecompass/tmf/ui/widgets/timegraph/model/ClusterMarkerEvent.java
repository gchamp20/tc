/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.core.presentation.IYAppearance;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;

/**
 * Time event for a cluster of marker events.
 *
 * @author Guillaume Champagne
 * @since 4.2
 */
public class ClusterMarkerEvent extends MarkerEvent {

    private final Map<String, Object> fSpecificStyleMap;

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
        Integer opaqueColor = RGBAUtil.fromRGBA(color) | 0xFF;
        fSpecificStyleMap = new HashMap<>();
        fSpecificStyleMap.put(ITimeEventStyleStrings.symbolStyle(), IYAppearance.SymbolStyle.DIAMOND);
        fSpecificStyleMap.put(ITimeEventStyleStrings.fillColor(), opaqueColor);
        fSpecificStyleMap.put(ITimeEventStyleStrings.heightFactor(), 0.25f); // Should probably be user configurable
    }

    /**
     * @return An unmodifiable reference to the style map for this ClusterMarkerEvent.
     */
    public Map<String, Object> getSpecificEventStyle() {
        /*
         * I'm not sure this is the best place for this because every event holds pretty much the same
         * map object, but I am not sure where this would be more appropriate.
         */
        return Collections.unmodifiableMap(fSpecificStyleMap);
    }
}
