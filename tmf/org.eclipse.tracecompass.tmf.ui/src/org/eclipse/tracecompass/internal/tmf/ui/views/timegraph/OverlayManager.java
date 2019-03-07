/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.ITimeGraphOverlay;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.ITimeGraphOverlayProvider;
import org.eclipse.tracecompass.internal.tmf.ui.views.timegraph.ITimeGraphViewMetadataProvider;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.ITimeGraphStyleProvider;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.QualitativePaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.TimeGraphLegend;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.Multimap;

/**
 * Manages the overlays available for the time graph
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public abstract class OverlayManager implements ITimeGraphStyleProvider {
    private static final RGBAColor DEFAULT_RGB = new RGBAColor(80, 120, 190);
    @Nullable private IPaletteProvider fPaletteProvider;

    private static final List<ITimeGraphOverlayProvider> TG_OVERLAYS = new ArrayList<>();
    private static final ImageDescriptor IMG_OVERLAY = Objects.requireNonNull(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_OVERLAY));

    /**
     * Builder class for #OverlayStateItem
     */
    private static class OverlayStateItemBuilder {
        private final Collection<ITimeGraphOverlay> fOverlays;
        private RGBAColor fColor;
        private String fName;

        public OverlayStateItemBuilder(String name, Collection<ITimeGraphOverlay> overlays) {
            fOverlays = overlays;
            fColor = DEFAULT_RGB;
            fName = name;
        }

        public void addOverlays(Collection<ITimeGraphOverlay> overlays) {
            fOverlays.addAll(overlays);
        }

        public void setColor(RGBAColor color) {
            fColor = color;
        }

        public OverlayStateItem build() {
            return new OverlayStateItem(fName, fOverlays, new RGBA(fColor.getRed(), fColor.getGreen(), fColor.getBlue(), fColor.getAlpha()));
        }
    }

    /**
     * Holds the styling information and activation state
     * for an overlay.
     */
    private static class OverlayStateItem extends StateItem {
        private final Collection<ITimeGraphOverlay> fOverlays;
        private boolean fActive;
        private RGBA fColor;

        /**
         * @param name
         *          Name of the group of overlay
         * @param overlays
         *          List of overlays associated with this name
         * @param color
         *          Default color
         */
        public OverlayStateItem(String name, Collection<ITimeGraphOverlay> overlays, RGBA color) {
            super(color.rgb, name);
            fActive = false;
            fOverlays = overlays;
            fActive = false;
            fColor = color;
        }

        /* public void setActive(boolean active) {
            fActive = active;
        } */

        public boolean isActive() {
            return fActive;
        }

        public Collection<? extends ITimeGraphOverlay> getOverlays() {
            return fOverlays;
        }

        public RGBA getRGBA() {
            return fColor;
        }
    }

    private final AbstractTimeGraphView fView;
    private final ITimeGraphViewMetadataProvider fMetadataProvider;
    private final Set<String> fRejectedOverlays = new HashSet<>();
    private @Nullable Action fOverlayAction;
    private List<OverlayStateItem> fAvailableOverlays = new ArrayList<>();

    /**
     * Constructor
     *
     * @param view
     *            The view that this manager is for
     * @param metadataProvider
     *            Provider of metadata about the view
     */
    public OverlayManager(AbstractTimeGraphView view, ITimeGraphViewMetadataProvider metadataProvider) {
        fView = view;
        fMetadataProvider = metadataProvider;
        IToolBarManager manager = view.getViewSite().getActionBars().getToolBarManager();
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getSelectOverlayMenu());
    }

    public void refreshOverlayList() {
        ITmfTrace trace = fView.getTrace();
        if (trace == null) {
            return;
        }

        Integer nbColors = 0;
        QualitativePaletteProvider.Builder paletteBuilder = new QualitativePaletteProvider.Builder();
        Set<String> viewMetadata = fMetadataProvider.getEntriesMetadata();
        Map<String, OverlayStateItemBuilder> availableOverlays = new HashMap<>();

        for (ITimeGraphOverlayProvider provider : TG_OVERLAYS) {
            Multimap<String, ITimeGraphOverlay> overlays = provider.getOverlays(trace);
            for (String overlayName : overlays.keySet()) {

                /* Build a set containing all the metadata that can be provided by this overlay */
                Set<String> overlayMetadata = new HashSet<>();
                for (ITimeGraphOverlay overlay : overlays.get(overlayName)) {
                    overlayMetadata.addAll(overlay.getOverlayMetadata());
                }

                /* If there's no intersection with the view metadata, this overlay will doesn't match for this view */
                if (viewMetadata.isEmpty() || overlayMetadata.isEmpty() ||
                        !(viewMetadata.stream().anyMatch(a -> overlayMetadata.contains(a)))) {
                    fRejectedOverlays.add(overlayName);
                    continue;
                }

                /* This overlay has the potential to create match based on metadata, list it as available */
                paletteBuilder.setNbColors(++nbColors);
                OverlayStateItemBuilder thisOverlay = availableOverlays.get(overlayName);
                if (thisOverlay == null) {
                    thisOverlay = new OverlayStateItemBuilder(overlayName, overlays.get(overlayName));
                    availableOverlays.put(overlayName, thisOverlay);
                } else {
                    thisOverlay.addOverlays(overlays.get(overlayName));
                }
            }
        }

        /* Build the overlay state items and set their colors */
        fPaletteProvider = paletteBuilder.build();
        Iterator<RGBAColor> colorIt = fPaletteProvider.get().iterator();
        fAvailableOverlays = availableOverlays.entrySet().stream()
            .map(x -> x.getValue())
            .peek(x -> x.setColor(colorIt.next()))
            .map(x -> x.build())
            .collect(Collectors.toList());
    }

    private Action getSelectOverlayMenu() {
        ITimeGraphStyleProvider provider = this;
        Action overlayAction = fOverlayAction;
        if (overlayAction == null) {
            overlayAction = new Action() {
                @Override
                public void run() {
                    Control tgControl = fView.getParentComposite();
                    if (tgControl == null || tgControl.isDisposed()) {
                        return;
                    }

                    TimeGraphLegend.open(tgControl.getShell(), provider);
                }
            };
            overlayAction.setToolTipText(Messages.TmfTimeGraphOverlay_MenuButtonTooltip);
            overlayAction.setImageDescriptor(IMG_OVERLAY);
            fOverlayAction = overlayAction;
        }
        return overlayAction;
    }

    /**
     * Trigger a view refresh
     *
     * @param view The view to refresh
     */
    protected abstract void refreshView(AbstractTimeGraphView view);

    /**
     * Get the active overlays
     *
     * FIXME: This should probably be the style instead of RGBA
     *
     * @return The map of active overlays with the color it represents
     */
    public Map<ITimeGraphOverlay, RGBA> getActiveOverlays() {
        Map<ITimeGraphOverlay, RGBA> active = new HashMap<>();
        for (OverlayStateItem overlay : fAvailableOverlays) {
            if (overlay.isActive()) {
                for (ITimeGraphOverlay o : overlay.getOverlays()) {
                    active.put(o, overlay.getRGBA());
                }
            }
        }
        return active;
    }

    /**
     * Add an overlay provider.
     *
     * @param overlayProvider
     *            The overlay provider
     */
    public static void addOverlayProvider(ITimeGraphOverlayProvider overlayProvider) {
        TG_OVERLAYS.add(overlayProvider);
    }

    @Override
    public StateItem[] getStateTable() {
        return fAvailableOverlays.toArray(new StateItem[0]);
    }

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getStateTypeName() {
        return "LOOOOOOOOOOOOOOOL";
    }
}
