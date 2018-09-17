/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.timegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.Multimap;

/**
 *
 *
 * @author Geneviève Bastien
 * @since 4.2
 */
@NonNullByDefault
public class OverlayManager {

    private static final RGB DEFAULT_RGB = new RGB(80, 120, 190);
    private static final RGBA DEFAULT_RGBA = new RGBA(80, 120, 190, 50);
    private static final List<ITimeGraphOverlayProvider> TG_OVERLAYS = new ArrayList<>();
    private static final ImageDescriptor IMG_OVERLAY = Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_OVERLAY);

    private static class AvailableOverlay {
        private final Collection<ITimeGraphOverlay> fOverlays;
        private boolean fActive;
        private RGB fRGB = DEFAULT_RGB;
        private RGBA fRGBA = DEFAULT_RGBA;

        public AvailableOverlay(Collection<ITimeGraphOverlay> overlays) {
            fOverlays = overlays;
            fActive = false;
        }

        public void addOverlays(Collection<ITimeGraphOverlay> overlays) {
            fOverlays.addAll(overlays);
        }

        public void setActive(boolean active) {
            fActive = active;
        }

        public boolean isActive() {
            return fActive;
        }

        public Collection<? extends ITimeGraphOverlay> getOverlays() {
            return fOverlays;
        }

        public RGB getRGB() {
            return fRGB;
        }

        public void setRGB(RGB color) {
            fRGB = color;
            fRGBA = new RGBA(color.red, color.green, color.blue, 50);
        }

        public RGBA getRGBA() {
            return fRGBA;
        }
    }

    private final AbstractTimeGraphView fView;
    private final Map<String, AvailableOverlay> fAvailableOverlays = new HashMap<>();
    private @Nullable Action fOverlayAction;

    /**
     * @param view
     */
    public OverlayManager(AbstractTimeGraphView view) {
        fView = view;
        IToolBarManager manager = view.getViewSite().getActionBars().getToolBarManager();
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getSelectOverlayMenu());

    }

    /**
     *
     */
    public void refresh() {
        ITmfTrace trace = fView.getTrace();
        if (trace == null) {
            return;
        }
        fAvailableOverlays.clear();
        for (ITimeGraphOverlayProvider provider : TG_OVERLAYS) {
            Multimap<String, ITimeGraphOverlay> overlays = provider.getOverlays(trace);
            for (String overlayName : overlays.keySet()) {
                AvailableOverlay thisOverlay = fAvailableOverlays.get(overlayName);
                if (thisOverlay == null) {
                    thisOverlay = new AvailableOverlay(overlays.get(overlayName));
                } else {
                    thisOverlay.addOverlays(overlays.get(overlayName));
                }
                fAvailableOverlays.put(overlayName, thisOverlay);
            }
        }
    }

    private Action getSelectOverlayMenu() {
        Action overlayAction = fOverlayAction;
        if (overlayAction == null) {
            overlayAction = new Action(Messages.TmfTimeGraphOverlay_MenuButton, IAction.AS_DROP_DOWN_MENU) {
                @Override
                public void runWithEvent(@Nullable Event event) {

                }
            };
            overlayAction.setToolTipText(Messages.TmfTimeGraphOverlay_MenuButtonTooltip);
            overlayAction.setImageDescriptor(IMG_OVERLAY);
            overlayAction.setMenuCreator(new IMenuCreator() {
                @Nullable
                Menu fMenu = null;

                @Override
                public void dispose() {
                    if (fMenu != null) {
                        fMenu.dispose();
                        fMenu = null;
                    }
                }

                @Override
                public Menu getMenu(@Nullable Control parent) {
                    if (fMenu != null) {
                        fMenu.dispose();
                    }
                    Menu menu = new Menu(parent);
                    for (Entry<String, AvailableOverlay> overlay : fAvailableOverlays.entrySet()) {
                        final Action action = new Action(overlay.getKey(), IAction.AS_CHECK_BOX) {
                            @Override
                            public void runWithEvent(@Nullable Event event) {
                                AvailableOverlay currentOverlay = overlay.getValue();
                                if (isChecked()) {
                                    currentOverlay.setActive(true);
                                    // Show the dialog for
                                    Shell shell = new Shell();
                                    ColorDialog cd = new ColorDialog(shell, SWT.NONE);
                                    cd.setRGB(currentOverlay.getRGB());
                                    RGB color = cd.open();
                                    if (color != null) {
                                        currentOverlay.setRGB(color);
                                    }
                                } else {
                                    currentOverlay.setActive(false);
                                }
                                fView.refresh();
                            }
                        };
                        action.setEnabled(true);
                        action.setChecked(overlay.getValue().isActive());
                        new ActionContributionItem(action).fill(menu, -1);
                    }
                    fMenu = menu;
                    return menu;
                }

                @Override
                public @Nullable Menu getMenu(@Nullable Menu parent) {
                    return null;
                }
            });
            fOverlayAction = overlayAction;
        }
        return overlayAction;
    }

    public void getOverlay() {
        fView.getTrace();
    }

    public Map<ITimeGraphOverlay, RGBA> getActiveOverlays() {
        Map<ITimeGraphOverlay, RGBA> active = new HashMap<>();
        for (AvailableOverlay overlay : fAvailableOverlays.values()) {
            if (overlay.isActive()) {
                for (ITimeGraphOverlay o : overlay.getOverlays()) {
                    active.put(o, overlay.getRGBA());
                }
            }
        }
        return active;
    }

    public static void addOverlayProvider(ITimeGraphOverlayProvider overlayProvider) {
        TG_OVERLAYS.add(overlayProvider);
    }

}
