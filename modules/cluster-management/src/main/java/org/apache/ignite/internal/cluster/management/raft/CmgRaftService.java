/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.cluster.management.raft;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.ignite.internal.cluster.management.ClusterManagementGroupManager;
import org.apache.ignite.internal.cluster.management.ClusterState;
import org.apache.ignite.internal.cluster.management.ClusterTag;
import org.apache.ignite.internal.cluster.management.network.messages.CmgMessagesFactory;
import org.apache.ignite.internal.cluster.management.raft.commands.ClusterNodeMessage;
import org.apache.ignite.internal.cluster.management.raft.commands.JoinReadyCommand;
import org.apache.ignite.internal.cluster.management.raft.commands.JoinRequestCommand;
import org.apache.ignite.internal.cluster.management.raft.commands.NodesLeaveCommand;
import org.apache.ignite.internal.cluster.management.raft.responses.LogicalTopologyResponse;
import org.apache.ignite.internal.cluster.management.raft.responses.ValidationErrorResponse;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.properties.IgniteProductVersion;
import org.apache.ignite.lang.IgniteInternalException;
import org.apache.ignite.network.ClusterNode;
import org.apache.ignite.network.ClusterService;
import org.apache.ignite.raft.client.Peer;
import org.apache.ignite.raft.client.service.RaftGroupService;

/**
 * A wrapper around a {@link RaftGroupService} providing helpful methods for working with the CMG.
 */
public class CmgRaftService {
    private static final IgniteLogger LOG = Loggers.forClass(ClusterManagementGroupManager.class);

    private final CmgMessagesFactory msgFactory = new CmgMessagesFactory();

    private final RaftGroupService raftService;

    private final ClusterService clusterService;

    public CmgRaftService(RaftGroupService raftService, ClusterService clusterService) {
        this.raftService = raftService;
        this.clusterService = clusterService;
    }

    /**
     * Returns {@code true} if the current node is the CMG leader.
     *
     * @return {@code true} if the current node is the CMG leader.
     */
    public CompletableFuture<Boolean> isCurrentNodeLeader() {
        Peer leader = raftService.leader();

        if (leader == null) {
            return raftService.refreshLeader().thenCompose(v -> isCurrentNodeLeader());
        } else {
            String nodeName = clusterService.topologyService().localMember().name();

            return CompletableFuture.completedFuture(leader.consistentId().equals(nodeName));
        }
    }

    /**
     * Retrieves the cluster state.
     *
     * @return Future that resolves into the current cluster state or {@code null} if it does not exist.
     */
    public CompletableFuture<ClusterState> readClusterState() {
        return raftService.run(msgFactory.readStateCommand().build())
                .thenApply(ClusterState.class::cast);
    }

    /**
     * Saves the given {@link ClusterState}.
     *
     * @param clusterState Cluster state.
     * @return Future that resolves to the current CMG state.
     */
    public CompletableFuture<ClusterState> initClusterState(ClusterState clusterState) {
        ClusterNodeMessage localNodeMessage = nodeMessage(clusterService.topologyService().localMember());

        return raftService.run(msgFactory.initCmgStateCommand().node(localNodeMessage).clusterState(clusterState).build())
                .thenApply(response -> {
                    if (response instanceof ValidationErrorResponse) {
                        throw new IllegalInitArgumentException("Init CMG request denied, reason: "
                                + ((ValidationErrorResponse) response).reason());
                    } else if (response instanceof ClusterState) {
                        return (ClusterState) response;
                    } else {
                        throw new IgniteInternalException("Unexpected response: " + response);
                    }
                });
    }

    /**
     * Sends a {@link JoinRequestCommand}, starting the validation procedure.
     *
     * @param clusterTag Cluster tag of the joining node.
     * @return Future that either resolves into a join token in case of successful validation or into an {@link IgniteInternalException}
     *         otherwise.
     * @see ValidationManager
     */
    public CompletableFuture<Void> startJoinCluster(ClusterTag clusterTag) {
        ClusterNodeMessage localNodeMessage = nodeMessage(clusterService.topologyService().localMember());

        JoinRequestCommand command = msgFactory.joinRequestCommand()
                .node(localNodeMessage)
                .version(IgniteProductVersion.CURRENT_VERSION.toString())
                .clusterTag(clusterTag)
                .build();

        return raftService.run(command)
                .thenAccept(response -> {
                    if (response instanceof ValidationErrorResponse) {
                        throw new JoinDeniedException("Join request denied, reason: " + ((ValidationErrorResponse) response).reason());
                    } else if (response != null) {
                        throw new IgniteInternalException("Unexpected response: " + response);
                    }  else {
                        LOG.info("JoinRequest command executed successfully");
                    }
                });
    }

    /**
     * Sends a {@link JoinReadyCommand} thus adding the current node to the local topology.
     *
     * @return Future that represents the state of the operation.
     */
    public CompletableFuture<Void> completeJoinCluster() {
        LOG.info("Node is ready to join the logical topology");

        ClusterNodeMessage localNodeMessage = nodeMessage(clusterService.topologyService().localMember());

        return raftService.run(msgFactory.joinReadyCommand().node(localNodeMessage).build())
                .thenAccept(response -> {
                    if (response instanceof ValidationErrorResponse) {
                        throw new JoinDeniedException("JoinReady request denied, reason: "
                                + ((ValidationErrorResponse) response).reason());
                    } else if (response != null) {
                        throw new IgniteInternalException("Unexpected response: " + response);
                    } else {
                        LOG.info("JoinReady command executed successfully");
                    }
                });
    }

    /**
     * Removes given nodes from the local topology. Should be called by the CMG leader.
     *
     * @return Future that represents the state of the operation.
     */
    public CompletableFuture<Void> removeFromCluster(Set<ClusterNode> nodes) {
        NodesLeaveCommand command = msgFactory.nodesLeaveCommand()
                .nodes(nodes.stream().map(this::nodeMessage).collect(Collectors.toSet()))
                .build();

        return raftService.run(command);
    }

    /**
     * Retrieves the logical topology.
     *
     * @return Logical topology.
     */
    public CompletableFuture<Collection<ClusterNode>> logicalTopology() {
        return raftService.run(msgFactory.readLogicalTopologyCommand().build())
                .thenApply(LogicalTopologyResponse.class::cast)
                .thenApply(LogicalTopologyResponse::logicalTopology);
    }

    /**
     * Returns a list of consistent IDs of the voting nodes of the CMG.
     *
     * @return List of consistent IDs of the voting nodes of the CMG.
     */
    public Set<String> nodeNames() {
        List<Peer> peers = raftService.peers();

        assert peers != null;

        return peers.stream()
                .map(Peer::consistentId)
                .collect(Collectors.toSet());
    }

    private ClusterNodeMessage nodeMessage(ClusterNode node) {
        return msgFactory.clusterNodeMessage()
                .id(node.id())
                .name(node.name())
                .host(node.address().host())
                .port(node.address().port())
                .build();
    }
}
