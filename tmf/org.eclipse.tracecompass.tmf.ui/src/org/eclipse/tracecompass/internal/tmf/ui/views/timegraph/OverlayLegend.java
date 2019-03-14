/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.timegraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.internal.tmf.ui.util.TimeGraphStyleUtil;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.ITimeGraphStyleProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.TimeGraphLegend;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;

/**
 *
 * Legend widget for the overlay.
 *
 * @author Guillaume Champagne
 */
public class OverlayLegend extends TimeGraphLegend {

    private static final ImageDescriptor RESET_IMAGE = Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_RESET_BUTTON);
    private final ITimeGraphStyleProvider fProvider;

    /**
     * Open the time graph legend window
     *
     * @param parent
     *            The parent shell
     * @param provider
     *            The presentation provider
     */
    public static void open(Shell parent, ITimeGraphStyleProvider provider) {
        (new OverlayLegend(parent, provider)).open();
    }

    /**
     * Constructor
     *
     * @param parent
     *            the shell to draw on
     * @param provider
     *            the provider containing the states
     */
    public OverlayLegend(Shell parent, ITimeGraphStyleProvider provider) {
        super(parent, provider);
        fProvider = provider;
    }

    @Override
    protected void createStatesGroup(Composite composite) {
        Group gs = new Group(composite, SWT.NONE);
        String stateTypeName = fProvider.getStateTypeName();
        StringBuilder buffer = new StringBuilder();
        if (!stateTypeName.isEmpty()) {
            buffer.append(stateTypeName);
            buffer.append(" "); //$NON-NLS-1$
        }
        buffer.append(Messages.TmfTimeLegend_StateTypeName);
        gs.setText(buffer.toString());

        GridLayout layout = new GridLayout();
        layout.marginWidth = 20;
        layout.marginBottom = 10;
        gs.setLayout(layout);

        GridData gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        gs.setLayoutData(gridData);

        // Go through all the defined pairs of state color and state name and
        // display them.
        StateItem[] stateTable = fProvider.getStateTable();
        List<StateItem> stateItems = stateTable != null ? Arrays.asList(stateTable) : Collections.emptyList();
        stateItems.forEach(si -> {
                new OverlayLegendEntry(gs, si);
        });
    }

    /**
     * Entries in the overlay legend widget
     */
    protected class OverlayLegendEntry extends Composite {

        private final Swatch fBar;
        private final Scale fScale;
        private final Button fReset;
        private final Button fActivate;

        /**
         * Constructor
         *
         * @param parent
         *            parent composite
         * @param si
         *            the state item
         */
        public OverlayLegendEntry(Composite parent, StateItem si) {
            super(parent, SWT.NONE);
            String fillColorKey = TimeGraphStyleUtil.getPreferenceName(fProvider, si, ITimeEventStyleStrings.fillColor());
            String heightFactorKey = TimeGraphStyleUtil.getPreferenceName(fProvider, si, ITimeEventStyleStrings.heightFactor());
            String activateOverlayKey = TimeGraphStyleUtil.getPreferenceName(fProvider, si, "activate"); //$NON-NLS-1$
            IPreferenceStore store = TimeGraphStyleUtil.getStore();
            Boolean enableControls = store.getBoolean(activateOverlayKey);
            TimeGraphStyleUtil.loadValue(fProvider, si);
            String name = si.getStateString();
            setLayout(GridLayoutFactory.swtDefaults().numColumns(5).create());

            fActivate = new Button(this, SWT.CHECK);
            fActivate.setSelection(enableControls);
            fActivate.setLayoutData(GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).create());
            fActivate.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Button btn = (Button) e.getSource();
                    store.setValue(activateOverlayKey, btn.getSelection());
                    fProvider.refresh();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    // TODO Auto-generated method stub

                }

            });

            fBar = new Swatch(this, si.getStateColor());
            fBar.setEnabled(enableControls);
            fBar.setToolTipText(Messages.TimeGraphLegend_swatchClick);
            fBar.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseUp(MouseEvent e) {
                    Shell shell = new Shell();
                    ColorDialog cd = new ColorDialog(shell, SWT.NONE);
                    cd.setRGB(fBar.getColor().getRGB());
                    RGB color = cd.open();
                    if (color != null) {
                        store.setValue(fillColorKey, new RGBAColor(color.red, color.green, color.blue, 255).toString());
                        fBar.setColor(color);
                        si.setStateColor(color);
                        fProvider.refresh();
                        fReset.setEnabled(true);
                    }
                }
            });
            fBar.addMouseTrackListener(new MouseTrackListener() {

                @Override
                public void mouseHover(MouseEvent e) {
                    // Do nothing
                }

                @Override
                public void mouseExit(MouseEvent e) {
                    Shell shell = parent.getShell();
                    Cursor old = shell.getCursor();
                    shell.setCursor(new Cursor(e.display, SWT.CURSOR_ARROW));
                    if (old != null) {
                        old.dispose();
                    }
                }

                @Override
                public void mouseEnter(MouseEvent e) {
                    Shell shell = parent.getShell();
                    Cursor old = shell.getCursor();
                    shell.setCursor(new Cursor(e.display, SWT.CURSOR_HAND));
                    if (old != null) {
                        old.dispose();
                    }
                }
            });

            fBar.setLayoutData(GridDataFactory.swtDefaults().hint(30, 20).create());
            CLabel label = new CLabel(this, SWT.NONE) {
                @Override
                protected String shortenText(GC gc, String t, int w) {
                    String text = super.shortenText(gc, t, w);
                    setToolTipText(t.equals(text) ? null : t);
                    return text;
                }
            };
            label.setText(name);
            label.setLayoutData(GridDataFactory.fillDefaults().hint(160, SWT.DEFAULT).align(SWT.FILL, SWT.CENTER).grab(true, false).create());
            fScale = new Scale(this, SWT.NONE);
            fScale.setEnabled(enableControls);
            fScale.setMaximum(100);
            fScale.setMinimum(1);
            fScale.setSelection((int) (100 * si.getStateHeightFactor()));
            fScale.setToolTipText(Messages.TimeGraphLegend_widthTooltip);
            fScale.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    float newWidth = fScale.getSelection() * 0.01f;
                    store.setValue(heightFactorKey, newWidth);
                    si.getStyleMap().put(ITimeEventStyleStrings.heightFactor(), newWidth);
                    fProvider.refresh();
                    fReset.setEnabled(true);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    // do nothing
                }
            });
            fScale.setLayoutData(GridDataFactory.swtDefaults().hint(120, SWT.DEFAULT).create());
            fReset = new Button(this, SWT.FLAT);
            fReset.setEnabled(enableControls);
            fReset.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    si.reset();
                    store.setToDefault(heightFactorKey);
                    store.setToDefault(fillColorKey);
                    fBar.setColor(si.getStateColor());
                    fScale.setSelection((int) (100 * si.getStateHeightFactor()));
                    fProvider.refresh();
                    fReset.setEnabled(false);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    // do nothing
                }
            });
            fReset.setToolTipText(Messages.TimeGraphLegend_resetTooltip);
            fReset.setImage(RESET_IMAGE.createImage());
            fReset.setLayoutData(GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).create());
            if (store.getString(fillColorKey).equals(store.getDefaultString(fillColorKey)) &&
                    store.getFloat(heightFactorKey) == store.getDefaultFloat(heightFactorKey)) {
                fReset.setEnabled(false);
            }
        }

    }

}
