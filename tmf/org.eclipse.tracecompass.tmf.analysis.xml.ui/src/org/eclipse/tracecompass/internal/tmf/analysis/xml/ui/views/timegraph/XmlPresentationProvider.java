/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Florian Wininger - Initial API and implementation
 *   Geneviève Bastien - Review of the initial implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.views.timegraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.TmfXmlUiStrings;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.views.timegraph.XmlEntry.EntryDisplayType;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.w3c.dom.Element;

/**
 * Presentation provider for the XML view, based on the generic TMF presentation
 * provider.
 *
 * TODO: This should support colors/states defined for each entry element in the
 * XML element.
 *
 * @author Florian Wininger
 */
public class XmlPresentationProvider extends TimeGraphPresentationProvider {

    private static final long[] COLOR_SEED = { 0x0000ff, 0xff0000, 0x00ff00,
            0xff00ff, 0x00ffff, 0xffff00, 0x000000, 0xf07300
    };

    private static final int COLOR_MASK = 0xffffff;

    private List<StateItem> stateValues = new ArrayList<>();
    /*
     * Maps the value of an event with the corresponding index in the
     * stateValues list
     */
    private Map<Integer, Integer> stateIndex = new HashMap<>();

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            TimeEvent tcEvent = (TimeEvent) event;

            XmlEntry entry = (XmlEntry) event.getEntry();
            int value = tcEvent.getValue();

            if (entry.getType() == EntryDisplayType.DISPLAY) {
                // Draw state only if state is already known
                Integer index = stateIndex.get(value);
                if (index != null) {
                    return index;
                }
            }
        }
        return INVISIBLE;
    }

    @Override
    public StateItem[] getStateTable() {
        return stateValues.toArray(new StateItem[stateValues.size()]);
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            TimeEvent tcEvent = (TimeEvent) event;

            XmlEntry entry = (XmlEntry) event.getEntry();
            int value = tcEvent.getValue();

            if (entry.getType() == EntryDisplayType.DISPLAY) {
                Integer index = stateIndex.get(value);
                if (index != null) {
                    String rgb = stateValues.get(index.intValue()).getStateString();
                    return rgb;
                }
            }
            return null;
        }
        return Messages.XmlPresentationProvider_MultipleStates;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        /*
         * TODO: Add the XML elements to support adding extra information in the
         * tooltips and implement this
         */
        return Collections.EMPTY_MAP;
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        /*
         * TODO Add the XML elements to support texts in intervals and implement
         * this
         */
    }

    @Override
    public void postDrawEntry(ITimeGraphEntry entry, Rectangle bounds, GC gc) {
    }

    /**
     * Loads the states from a {@link TmfXmlUiStrings#TIME_GRAPH_VIEW} XML
     * element
     *
     * @param viewElement
     *            The XML view element
     */
    public synchronized void loadNewStates(@NonNull Element viewElement) {
        stateValues.clear();
        stateIndex.clear();
        List<Element> states = TmfXmlUtils.getChildElements(viewElement, TmfXmlStrings.DEFINED_VALUE);

        for (Element state : states) {
            int value = Integer.parseInt(state.getAttribute(TmfXmlStrings.VALUE));
            String name = state.getAttribute(TmfXmlStrings.NAME);
            String color = state.getAttribute(TmfXmlStrings.COLOR);

            addOrUpdateState(value, name, color);
        }
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                fireColorSettingsChanged();
            }
        });
    }

    /**
     * Add a new state in the time graph view. This allow to define at runtime
     * new states that cannot be known at the conception of this analysis.
     *
     * @param name
     *            The string associated with the state
     * @return the value for this state
     */
    public synchronized int addState(String name) {
        // Find a value for this name, start at 10000
        int value = 10000;
        while (stateIndex.get(value) != null) {
            value++;
        }
        addOrUpdateState(value, name, ""); //$NON-NLS-1$
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                fireColorSettingsChanged();
            }
        });
        return value;
    }

    private synchronized void addOrUpdateState(int value, String name, String color) {
        // FIXME Allow this case
        if (value < 0) {
            return;
        }

        final RGB colorRGB = (color.startsWith(TmfXmlStrings.COLOR_PREFIX)) ? parseColor(color) : calcColor(name);

        StateItem item = new StateItem(colorRGB, name);

        Integer index = stateIndex.get(value);
        if (index == null) {
            /* Add the new state value */
            stateIndex.put(value, stateValues.size());
            stateValues.add(item);
        } else {
            /* Override a previous state value */
            stateValues.set(index, item);
        }
    }

    private static RGB parseColor(String color) {
        RGB colorRGB;
        Integer hex = Integer.parseInt(color.substring(1), 16);
        int hex1 = hex.intValue() % 256;
        int hex2 = (hex.intValue() / 256) % 256;
        int hex3 = (hex.intValue() / (256 * 256)) % 256;
        colorRGB = new RGB(hex3, hex2, hex1);
        return colorRGB;
    }

    /*
     * This method will always return the same color for a same name, no matter
     * the value, so that different traces with the same XML analysis will
     * display identically states with the same name.
     */
    private static RGB calcColor(String name) {
        long hash = name.hashCode(); // hashcodes can be Integer.MIN_VALUE.
        long base = COLOR_SEED[(int) (Math.abs(hash) % COLOR_SEED.length)];
        int x = (int) ((hash & COLOR_MASK) ^ base);
        final int r = (x >> 16) & 0xff;
        final int g = (x >> 8) & 0xff;
        final int b = x & 0xff;
        return new RGB(r, g, b);
    }

}
