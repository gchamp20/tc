/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/


package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.tracecompass.internal.tmf.ui.util.TimeGraphStyleUtil;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.ITimeGraphStyleProvider;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.IYAppearance;
import org.eclipse.tracecompass.tmf.core.presentation.QualitativePaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

/**
 * Style provider for the overlays.
 *
 * @author Guillaume Champagne
 */
public class OverlayStyleProvider implements ITimeGraphStyleProvider {

    private @Nullable OverlayManager fOverlayManager;
    private List<OverlayStateItem> fOverlayStateItems;
    @Nullable private IPaletteProvider fPaletteProvider;

    /**
     * Default constructor
     *
     */
    public OverlayStyleProvider() {
        fOverlayManager = null;
    }

    /**
     * Set the available overlays.
     *
     * @param availableOverlays
     *          Names of the available overlays.
     */
    public void setAvailableOverlays(Set<String> availableOverlays) {
        /* Build the overlay state items and set their colors */
        QualitativePaletteProvider.Builder paletteBuilder = new QualitativePaletteProvider.Builder();
        paletteBuilder.setNbColors(availableOverlays.size());
        fPaletteProvider = paletteBuilder.build();

        /* This is rerun each time the overlays are refreshed, not ideal.. */
        Iterator<RGBAColor> colorIt = fPaletteProvider.get().iterator();
        fOverlayStateItems = availableOverlays.stream()
            .map(x -> new OverlayStateItem(x, colorIt.next()))
            .collect(Collectors.toList());

        /* Reloads the preferences (color, height) */
        TimeGraphStyleUtil.loadValues(this);

        /* Reload activation preference (color, height) */
        IPreferenceStore store = TimeGraphStyleUtil.getStore();
        fOverlayStateItems.stream()
        .forEach(si -> {
            String activateOverlayKey = TimeGraphStyleUtil.getPreferenceName(this, si, "activate"); //$NON-NLS-1$
            si.setActive(store.getBoolean(activateOverlayKey));
        });

        refresh();
    }

    /**
     * @param manager
     *          The manager for the overlays on which this class provides styles.
     */
    public void setOverlayManager(OverlayManager manager) {
        fOverlayManager = manager;
    }

    @Override
    public StateItem[] getStateTable() {
        return fOverlayStateItems.toArray(new StateItem[0]);
    }

    @Override
    public String getStateTypeName() {
        return null;
    }

    @Override
    public void refresh() {
        if (fOverlayManager != null) {
            fOverlayManager.setActiveOverlays(
                    fOverlayStateItems.stream()
                    .filter(x -> x.isActive())
                    .map(x -> x.getStateString())
                    .collect(Collectors.toSet()));
        }
    }

    @Override
    public Map<String, Object> getEventStyle(ITimeEvent event) {
        if (event instanceof MarkerEvent) {
            MarkerEvent ev = (MarkerEvent)event;
            Optional<OverlayStateItem> item = fOverlayStateItems.stream()
                    .filter(x -> x.getStateString().equals(ev.getCategory()))
                    .findFirst();
            if (item.isPresent()) {
                Map<String, Object> styleMap = item.get().getStyleMap();
                if (event instanceof ClusterMarkerEvent) {
                    styleMap.put(ITimeEventStyleStrings.symbolStyle(), IYAppearance.SymbolStyle.DIAMOND);
                }
                return styleMap;
            }
        }
        return Collections.EMPTY_MAP;
    }

}
