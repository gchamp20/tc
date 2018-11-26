/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.realtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.tracecompass.analysis.graph.core.building.EventKey;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Functions to implement the algorithm from:
 *
 * Zhu, H., Wang, P., He, X., Li, Y., Wang, W., & Shi, B. (2010, December).
 * Efficient episode mining with minimal and non-overlapping occurrences.
 * In Data mining (ICDM), 2010 IEEE 10th international conference on (pp. 1211-1216). IEEE.
 *
 * @author Guillaume Champagne
 * @since 3.1
 */
public class MANEPI {

    private static class FEPT {
        /**
         * Label of this node.
         */
        private List<EventKey> fLabel;

        /**
         * Set of minimal occurences
         */
        private List<Pair<Long, Long>> fMinimalOccurences;

        /**
         * Set of minimal and non overlaping occurences
         */
        private List<Pair<Long, Long>> fMinimalNonOverlappingOccurences;

        /**
         * Childrens of this node.
         */
        private List<FEPT> fChildrens;

        /**
         *  The sequence on which pattern are discovered
         */
        private List<ITmfEvent> fSequence;

        /**
         * @param label
         *      Label of the node
         * @param sequence
         *      The initial sequence
         */
        public FEPT(EventKey label, List<ITmfEvent> sequence) {
            fLabel  = new ArrayList<>();
            fLabel.add(label);
            fMinimalOccurences = new ArrayList<>();
            fMinimalNonOverlappingOccurences = new ArrayList<>();

            fChildrens = new ArrayList<>();
            fSequence = sequence;
        }

        /**
         * @param label
         *      Label of the node
         * @param sequence
         *      The initial sequence
         */
        public FEPT(List<EventKey> label, List<ITmfEvent> sequence) {
            fLabel  = label;
            fMinimalOccurences = new ArrayList<>();
            fMinimalNonOverlappingOccurences = new ArrayList<>();

            fChildrens = new ArrayList<>();
            fSequence = sequence;
        }
    }

    private static Map<EventKey, List<Pair<Long, Long>>> fOneEpisodeOccurences = new HashMap<>();

    private static final int TRESH = 8;

    /**
     * Compute the MANEPI algorithm
     *
     * @param edges
     *          The ordered list of events
     * @return
     *       A list of pair<Label, minimal non overalapping occurences>
     */
    public static List<Pair<List<EventKey>, List<Pair<Long, Long>>>> compute(List<ITmfEvent> edges) {
        if (fOneEpisodeOccurences.size() > 0) {
            fOneEpisodeOccurences = new HashMap<>();
        }

        FEPT root = new FEPT(new ArrayList<EventKey>(), edges);
        List<EventKey> oneEpisodes = getOneEpisode(edges, TRESH);

        for (EventKey e : oneEpisodes) {
            FEPT node = new FEPT(e, edges);
            root.fChildrens.add(node);
            node.fMinimalOccurences = fOneEpisodeOccurences.getOrDefault(e, new ArrayList<>());
            node.fMinimalNonOverlappingOccurences = fOneEpisodeOccurences.getOrDefault(e, new ArrayList<>());
            mineGrow(node, oneEpisodes);
        }

        List<Pair<List<EventKey>, List<Pair<Long, Long>>>> results = new ArrayList<>();
        walk(root, results);

        //results.removeIf((s -> s.getFirst().size() != 2));

        return results;
    }

    private static void walk(FEPT node, List<Pair<List<EventKey>, List<Pair<Long, Long>>>> results) {
        if (node.fChildrens.size() == 0) {
            results.add(new Pair<>(
                        node.fLabel,
                        node.fMinimalNonOverlappingOccurences
                    ));
        } else {
            for (FEPT n : node.fChildrens) {
                walk(n, results);
            }
        }
    }

    private static void mineGrow(FEPT node, List<EventKey> oneEpisodes) {
        if (node.fLabel.size() > 4) {
            return;
        }

        for (EventKey e : oneEpisodes) {
            List<Pair<Long, Long>> minimalOcc = computeMO(node.fMinimalOccurences,
                    fOneEpisodeOccurences.getOrDefault(e, new ArrayList<>()));

            List<Pair<Long, Long>> minimalNonOverlapOcc = computeMANO(minimalOcc);

            if (minimalNonOverlapOcc.size() >= TRESH) {
                List<EventKey> label = new ArrayList<>(node.fLabel);
                label.add(e);

                FEPT newNode = new FEPT(label, node.fSequence);
                newNode.fMinimalOccurences = minimalOcc;
                newNode.fMinimalNonOverlappingOccurences = minimalNonOverlapOcc;
                node.fChildrens.add(newNode);
                mineGrow(newNode, oneEpisodes);
            }
        }
    }

    private static List<Pair<Long, Long>> computeMO(List<Pair<Long, Long>> moa, List<Pair<Long, Long>> moe) {
        List<Pair<Long, Long>> mob = new ArrayList<>();
        int i = -1;

        for (Pair<Long, Long> occ : moe) {
            Long ts = occ.getFirst();
            for (int j = i + 1; j < moa.size(); j++) {
                Long tjs = moa.get(j).getFirst();
                Long tje = moa.get(j).getSecond();

                if (j + 1 < moa.size()) {
                    Long tje1 = moa.get(j + 1).getSecond();
                    if (tje < ts && tje1 > ts) {
                        mob.add(new Pair<>(tjs, ts));
                        i = j;
                        break;
                    }
                } else {
                    if (tje < ts) {
                        mob.add(new Pair<>(tjs, ts));
                        i = j;
                        break;
                    }
                }
            }
        }
        return mob;
    }

    private static List<Pair<Long, Long>> computeMANO(List<Pair<Long, Long>> mob) {
        int i = 0;
        int j = i + 1;
        List<Pair<Long, Long>> manob = new ArrayList<>();
        if (mob.size() > 0) {
            manob.add(mob.get(i));
            while (j < mob.size()) {
                Long tie = mob.get(i).getSecond();
                int k = 0;
                for (k = j; k < mob.size(); k ++) {
                    Long tks = mob.get(k).getFirst();
                    if (tie < tks) {
                        manob.add(mob.get(k));
                        break;
                    }
                }
                i = k;
                j = i + 1;
            }
        }

        return manob;
    }

    /**
     * Find the frequent one episode in the provided
     * sequence.
     *
     * @param edges
     *          Edge sequence.
     * @param frequencyTreshold
     *          Minimal frequency of the one episodes
     * @return
     */
    private static List<EventKey> getOneEpisode(List<ITmfEvent> edges, int frequencyTreshold) {
        Map<EventKey, Long> oneEpisodeFrequencies = new HashMap<>();

        Long idx = 0L;
        for (ITmfEvent e : edges) {
            EventKey key = new EventKey(e);

            /* Update frequency */
            Long val = oneEpisodeFrequencies.getOrDefault(key, 0L);
            val += 1;
            oneEpisodeFrequencies.put(key, val);

            /* Add occurence */
            List<Pair<Long, Long>> occurences = fOneEpisodeOccurences.getOrDefault(key, new ArrayList<>());
            occurences.add(new Pair<>(idx, idx));
            fOneEpisodeOccurences.put(key, occurences);
            idx += 1;
        }

        List<EventKey> results = new ArrayList<>();
        for (Entry<EventKey, Long> e : oneEpisodeFrequencies.entrySet()) {
            if (e.getValue() >= frequencyTreshold) {
                results.add(e.getKey());
            }
        }
        return results;
    }
}
