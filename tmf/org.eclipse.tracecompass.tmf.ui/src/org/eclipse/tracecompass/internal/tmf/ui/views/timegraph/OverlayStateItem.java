/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/


package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;

/**
 * Holds the styling information and activation state
 * for an overlay.
 */
public class OverlayStateItem extends StateItem {
    private boolean fActive;

    /**
     * Default height for the overlay state items
     */
    public static final float DEFAULT_HEIGHT = 0.33f;

    /**
     * @param name
     *          Name of the group of overlay
     * @param color
     *          Default color
     */
    public OverlayStateItem(String name, RGBAColor color) {
        super(new RGB(color.getRed(), color.getGreen(), color.getBlue()), name);
        getStyleMap().put(ITimeEventStyleStrings.heightFactor(), DEFAULT_HEIGHT);
        fActive = false;
    }

    public boolean isActive() {
        return fActive;
    }

    public void setActive(boolean active) {
        fActive = active;
    }

    @Override
    public float getStateHeightFactor() {
        Map<String, Object> styleMap = getStyleMap();
        Object itemType = getStyleMap().get(ITimeEventStyleStrings.itemTypeProperty());
        float defaultStateWidth = ITimeEventStyleStrings.linkType().equals(itemType) ?
                TimeGraphControl.DEFAULT_LINK_WIDTH : DEFAULT_HEIGHT;
        return (float) styleMap.getOrDefault(ITimeEventStyleStrings.heightFactor(), defaultStateWidth);
    }
}
