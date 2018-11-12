/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.realtime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.realtime.MANEPI.EventKey;
import org.eclipse.tracecompass.internal.analysis.graph.core.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Analysis for pattern finding in real time tasks.
 *
 * @author Guillaume Champagne
 */
public class KernelRealTimeAnalysis extends TmfAbstractAnalysisModule {

    private @Nullable KernelRealTimePatternRequest fRequest;

    private Map<Integer, List<Pair<List<EventKey>, List<Pair<Long, Long>>>>> fResults;

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        Set<IAnalysisModule> modules = new HashSet<>();

        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException("Analysis requires a trace"); //$NON-NLS-1$
        }
        /*
         * This analysis depends on the LTTng kernel analysis, so it's added to
         * dependent modules.
         */
        Iterable<KernelAnalysisModule> kernelModules = TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysisModule.class);
        for (KernelAnalysisModule kernelModule : kernelModules) {
            /* Only add the first one we find, if there is one */
            modules.add(kernelModule);
            break;
        }
        return modules;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {

        /* Cancel any previous request */
        KernelRealTimePatternRequest request = fRequest;
        if ((request != null) && (!request.isCompleted())) {
            request.cancel();
        }

        try {
            request = new KernelRealTimePatternRequest();
            fRequest = request;
            request.waitForCompletion();
            fResults = request.getResult();

            if (fResults == null) {
                return false;
            }

        } catch (InterruptedException e) {
            Activator.getInstance().logError("Request interrupted", e); //$NON-NLS-1$
        }

        return !monitor.isCanceled();
    }

    @Override
    protected void canceling() {

    }



}
