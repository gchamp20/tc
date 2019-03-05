/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alvaro Sanchez-Leon - Initial API and implementation
 *   Patrick Tasse - Refactoring
 *   Geneviève Bastien - Add drawing helper methods
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.ClusterMarkerEvent;
import org.eclipse.tracecompass.tmf.core.presentation.IYAppearance;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITmfTimeGraphDrawingHelper;

/**
 * Provider class for the time graph provider
 *
 * @author Patrick Tasse
 * @since 3.4
 *
 */
public class TimeGraphPresentationProvider implements ITimeGraphPresentationProvider2 {

    private static final StateItem[] EMPTY_STATE_TABLE = new StateItem[0];

    private ITmfTimeGraphDrawingHelper fDrawingHelper;
    private final String fStateTypeName;

    // The list of listeners for graph color changes
    private final List<ITimeGraphColorListener> fListeners = new ArrayList<>();

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    private static final int DEFAULT_ITEM_HEIGHT = 19;

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param stateTypeName  The state type name
     */
    public TimeGraphPresentationProvider(String stateTypeName) {
        fStateTypeName = stateTypeName;
    }

    /**
     * Constructor
     */
    public TimeGraphPresentationProvider() {
        this(Messages.TmfTimeLegend_TRACE_STATES);
    }

    @Override
    public String getStateTypeName() {
        return fStateTypeName;
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        return null;
    }

    @Override
    public StateItem[] getStateTable() {
        return EMPTY_STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        return 0;
    }

    @Override
    public ITmfTimeGraphDrawingHelper getDrawingHelper() {
        return fDrawingHelper;
    }

    @Override
    public void setDrawingHelper(ITmfTimeGraphDrawingHelper helper) {
        fDrawingHelper = helper;
    }

    @Override
    public void postDrawControl(Rectangle bounds, GC gc) {
        // Override to add own drawing code
    }

    @Override
    public void postDrawEntry(ITimeGraphEntry entry, Rectangle bounds, GC gc) {
        // Override to add own drawing code
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        // Override to add own drawing code
    }

    @Override
    public int getItemHeight(ITimeGraphEntry entry) {
        return DEFAULT_ITEM_HEIGHT;
    }

    @Override
    public Image getItemImage(ITimeGraphEntry entry) {
        return null;
    }

    @Override
    public String getEventName(ITimeEvent event) {
        return null;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        return null;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        return getEventHoverToolTipInfo(event);
    }

    @Override
    public boolean displayTimesInTooltip() {
        return true;
    }

    @Override
    public void addColorListener(ITimeGraphColorListener listener) {
        if (!fListeners.contains(listener)) {
            fListeners.add(listener);
        }
    }

    @Override
    public void removeColorListener(ITimeGraphColorListener listener) {
        fListeners.remove(listener);
    }

    /**
     * Notifies listeners of the state table change
     */
    protected void fireColorSettingsChanged(){
        refresh();
    }

    /**
     * Notifies listeners of the state table change
     * @since 3.2
     */
    @Override
    public void refresh() {
        for (ITimeGraphColorListener listener : fListeners) {
            listener.colorSettingsChanged(getStateTable());
        }
    }

    @Override
    public Map<String, Object> getSpecificEventStyle(ITimeEvent event) {
        Map<String, Object> styleMap = new HashMap<>(ITimeGraphPresentationProvider2.super.getSpecificEventStyle(event));
        if (event instanceof ClusterMarkerEvent) {
            ClusterMarkerEvent clusterEvent = (ClusterMarkerEvent)event;
            styleMap.put(ITimeEventStyleStrings.symbolStyle(), IYAppearance.SymbolStyle.DIAMOND);
            styleMap.put(ITimeEventStyleStrings.fillColor(), RGBAUtil.fromRGBA(clusterEvent.getColor()) | 0xFF);
            styleMap.put(ITimeEventStyleStrings.heightFactor(), 0.25f); // Should probably be user configurable
        }
        return styleMap;
    }

}
