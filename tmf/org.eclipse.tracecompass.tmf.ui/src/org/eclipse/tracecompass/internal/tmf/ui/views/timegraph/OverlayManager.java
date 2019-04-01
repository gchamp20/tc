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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Manages the overlays available for the time graph
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public abstract class OverlayManager {
    private static final List<ITimeGraphOverlayProvider> TG_OVERLAYS = new ArrayList<>();
    private static final ImageDescriptor IMG_OVERLAY = Objects.requireNonNull(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_OVERLAY));

    private final AbstractTimeGraphView fView;
    private final ITimeGraphViewMetadataProvider fMetadataProvider;
    private final Set<String> fRejectedOverlays = new HashSet<>();
    private @Nullable Action fOverlayAction;
    private final Multimap<String, ITimeGraphOverlay> fAvailableOverlays = HashMultimap.create();
    private final Set<String> fActiveOverlays = new HashSet<>();
    private final OverlayStyleProvider fStyleProvider;

    /**
     * Constructor
     *
     * @param view
     *            The view that this manager is for
     * @param metadataProvider
     *            Provider of metadata about the view
     * @param styleProvider
     *            The style provider for the overlays
     */
    public OverlayManager(AbstractTimeGraphView view, ITimeGraphViewMetadataProvider metadataProvider, OverlayStyleProvider styleProvider) {
        fView = view;
        fMetadataProvider = metadataProvider;
        fStyleProvider = styleProvider;
        fStyleProvider.setOverlayManager(this);
        IToolBarManager manager = view.getViewSite().getActionBars().getToolBarManager();
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getSelectOverlayMenu());
    }

    public void refreshOverlayList() {
        ITmfTrace trace = fView.getTrace();
        if (trace == null) {
            return;
        }

        Set<String> viewMetadata = fMetadataProvider.getEntriesMetadata();

        for (ITimeGraphOverlayProvider provider : TG_OVERLAYS) {
            Multimap<String, ITimeGraphOverlay> overlays = provider.getOverlays(trace);
            Multimap<String, ITimeGraphOverlay> availableOverlays = HashMultimap.create();
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
                } else {
                    availableOverlays.putAll(overlayName, overlays.get(overlayName));
                }
            }
            fAvailableOverlays.putAll(availableOverlays);
        }

        fStyleProvider.setAvailableOverlays(fAvailableOverlays.keySet());
    }

    private Action getSelectOverlayMenu() {
        Action overlayAction = fOverlayAction;
        if (overlayAction == null) {
            overlayAction = new Action() {
                @Override
                public void run() {
                    Control tgControl = fView.getParentComposite();
                    if (tgControl == null || tgControl.isDisposed()) {
                        return;
                    }

                    OverlayLegend.open(tgControl.getShell(), fStyleProvider);
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
     * Assign the set of active overlays
     * @param keys
     *          Keys of the active overlays
     */
    public void setActiveOverlays(Set<String> keys) {
        fActiveOverlays.addAll(keys);
        refreshView(fView);
    }

    /**
     * Get the active overlays
     *
     * FIXME: This should probably be the style instead of RGBA
     *
     * @return The map of active overlays with the color it represents
     */
    public Map<ITimeGraphOverlay, RGBA> getActiveOverlays() {
        Map<ITimeGraphOverlay, RGBA> active = new HashMap<>();
        for (String overlay : fActiveOverlays) {
            Collection<ITimeGraphOverlay> overlays = fAvailableOverlays.get(overlay);
            StateItem[] stateItems = fStyleProvider.getStateTable();

            @SuppressWarnings("null")
            Optional<StateItem> item = Arrays.asList(stateItems).stream()
            .filter(x -> x.getStateString().equals(overlay))
            .findFirst();

            RGBA color;
            if (item.isPresent()) {
                RGB stateColor = item.get().getStateColor();
                color = new RGBA(stateColor.red, stateColor.green, stateColor.blue, 80);
            } else {
                color = new RGBA(0, 0, 0, 80);
            }

            for (ITimeGraphOverlay ov : overlays) {
                active.put(ov, color);
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
}
