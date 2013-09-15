/*
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.nfgraph.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.nfgraph.NFGraphModelHolder;
import com.netflix.nfgraph.spec.NFGraphSpec;

public class NFBuildGraphNodeCache {

	private final NFGraphSpec graphSpec;
	private final NFGraphModelHolder buildGraphModelHolder;
    private final Map<String, List<NFBuildGraphNode>> nodesByOrdinal;

    NFBuildGraphNodeCache(NFGraphSpec graphSpec, NFGraphModelHolder modelHolder) {
        this.nodesByOrdinal = new HashMap<String, List<NFBuildGraphNode>>();
        this.graphSpec = graphSpec;
        this.buildGraphModelHolder = modelHolder;
    }

    NFBuildGraphNode getNode(String nodeType, int ordinal) {
        List<NFBuildGraphNode> nodes = getNodes(nodeType);

        while (ordinal >= nodes.size()) {
            nodes.add(null);
        }

        NFBuildGraphNode node = nodes.get(ordinal);

        if (node == null) {
            node = new NFBuildGraphNode(graphSpec.getNodeSpec(nodeType), ordinal, buildGraphModelHolder.size());
            nodes.set(ordinal, node);
        }

        return node;
    }

    public int numNodes(String nodeType) {
        return getNodes(nodeType).size();
    }
    
    public List<NFBuildGraphNode> getNodes(String nodeType) {
        List<NFBuildGraphNode> nodes = nodesByOrdinal.get(nodeType);
        if (nodes == null) {
            nodes = new ArrayList<NFBuildGraphNode>();
            nodesByOrdinal.put(nodeType, nodes);
        }
        return nodes;
    }

}
