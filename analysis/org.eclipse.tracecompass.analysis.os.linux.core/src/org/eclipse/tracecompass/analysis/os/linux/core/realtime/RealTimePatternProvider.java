/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.realtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * Finds the pattern per thread on a given trace
 *
 * @author Guillaume Champagne
 * @since 3.1
 */
public class RealTimePatternProvider {

    private static class EntryPPollState implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("syscall_entry_ppoll")) {
                return false;
            }

            // Resolve the KernelTidAspect to get the current TID for this event.
            Object tidObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), KernelTidAspect.class, event);
            Integer tid = (tidObj != null && tidObj instanceof Integer) ? (Integer)tidObj : null;
            if (tid == null) {
                return false;
            }

            return tid == 2792;
        }
    }

    private static class HRTimerStart implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("timer_hrtimer_start")) {
                return false;
            }

            // Resolve the KernelTidAspect to get the current TID for this event.
            Object tidObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), KernelTidAspect.class, event);
            Integer tid = (tidObj != null && tidObj instanceof Integer) ? (Integer)tidObj : null;
            if (tid == null) {
                return false;
            }

            return tid == 2792;
        }
    }

    private static class HRTimerExpireStateEntry implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("timer_hrtimer_expire_entry")) {
                return false;
            }
            return true;
        }
    }

    private static class HRTimerExpireStateExit implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("timer_hrtimer_expire_exit")) {
                return false;
            }
            return true;
        }
    }

    private static class SchedSwitchFrom implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("sched_switch")) {
                return false;
            }

            // Resolve the KernelTidAspect to get the current TID for this event.
            Object tidObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), KernelTidAspect.class, event);
            Integer tid = (tidObj != null && tidObj instanceof Integer) ? (Integer)tidObj : null;
            if (tid == null) {
                return false;
            }

            return tid == 2792;
        }
    }

    private static class SchedSwitchTo implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("sched_switch")) {
                return false;
            }

            ITmfEventField content = event.getContent();
            Object objWrapper = content.getField("next_tid").getValue();
            if (objWrapper == null || !(objWrapper instanceof Long)) {
                return false;
            }

            Long tid = (Long)objWrapper;

            return tid == 2792L;
        }
    }

    private static class SchedWakeupState implements IState {
        @Override
        public boolean checkIncomingCondition(ITmfEvent event) {
            if (!event.getName().equals("sched_wakeup")) {
                return false;
            }

            ITmfEventField content = event.getContent();
            Object objWrapper = content.getField("tid").getValue();
            if (objWrapper == null || !(objWrapper instanceof Long)) {
                return false;
            }

            Long tid = (Long)objWrapper;

            return tid == 2792L;
        }
    }

    private static class Scenario {
        public Scenario(Long fstart) {
            State = -1;
            Start = fstart;
            Completed = false;
        }

        public Long Start;
        public int State;
        public boolean Completed;
    }

    private ITmfTrace fTrace;

    private Set<Integer> fSelectedThreadsID;

    List<Pair<Long, Long>> fOccurences;

    private List<Scenario> fOngoingScenarios;

    private List<Scenario> fCompletedScenarios;

    private static List<IState> fStates;

    static {
        ImmutableList.Builder<IState> builder = new ImmutableList.Builder<>();

        builder.add(new HRTimerExpireStateEntry());
        builder.add(new SchedWakeupState());
        builder.add(new HRTimerExpireStateExit());
        builder.add(new SchedSwitchTo());
        builder.add(new EntryPPollState());
        builder.add(new HRTimerStart());
        builder.add(new SchedSwitchFrom());

        /*
         * DO NOT MODIFY AFTER
         */
        fStates = builder.build();
    }

    /**
     * @param trace
     *      The trace object
     */
    public RealTimePatternProvider(ITmfTrace trace) {
        fTrace = trace;
        fOngoingScenarios = new ArrayList<>();
        fCompletedScenarios = new ArrayList<>();
        fSelectedThreadsID = new HashSet<>();
        fSelectedThreadsID.add(2792);
    }

    /**
     * @param occurences
     *          The list in which the result are stored.
     */
    public void assignTargetMap(List<Pair<Long, Long>> occurences) {
        fOccurences = occurences;
    }

    /**
     * Process each event in the sequence.
     * @param event
     *          Event to process
     */
    public void processEvent(ITmfEvent event) {
        boolean isEntryNewScenario = fStates.get(0).checkIncomingCondition(event);
        if (isEntryNewScenario) {
            fOngoingScenarios.add(new Scenario(event.getTimestamp().toNanos()));
        }

        fCompletedScenarios.clear();
        for (int i = 0; i < fOngoingScenarios.size(); i++) {
            Scenario s = fOngoingScenarios.get(i);
            IState nextState = fStates.get(s.State + 1);
            if (nextState.checkIncomingCondition(event)) {

                s.State += 1;

                if (s.State == fStates.size() - 1) {
                    s.Completed = true;
                    fCompletedScenarios.add(s);
                }
            }
        }

        if (fCompletedScenarios.size() > 0) {
            Scenario s = fCompletedScenarios.get(fCompletedScenarios.size() - 1);
            fOccurences.add(new Pair<>(s.Start, event.getTimestamp().toNanos()));
        }

        fOngoingScenarios.removeIf(s -> s.Completed);
    }

    /**
     * Call when all the events have been processed.
     */
    public void done() {
        // Extends occureces to match the whole job.

        List<Pair<Long, Long>> extendedScenarios = new ArrayList<>();
        for (int i = 0; i < fOccurences.size() - 1; i++) {
            Long start = fOccurences.get(i).getFirst();
            Long newEnd = fOccurences.get(i + 1).getFirst() - 1;
            extendedScenarios.add(new Pair<>(start, newEnd));
        }

        fOccurences.clear();
        fOccurences.addAll(extendedScenarios);
    }

    ITmfTrace getTrace() {
        return fTrace;
    }
}
