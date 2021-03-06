/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.journalkeeper.core.client;

import io.journalkeeper.core.api.AdminClient;
import io.journalkeeper.core.api.ClusterConfiguration;
import io.journalkeeper.core.api.RaftJournal;
import io.journalkeeper.core.api.RaftServer;
import io.journalkeeper.core.api.ResponseConfig;
import io.journalkeeper.core.api.ServerStatus;
import io.journalkeeper.core.api.SnapshotsEntry;
import io.journalkeeper.core.api.UpdateRequest;
import io.journalkeeper.core.entry.internal.CreateSnapshotEntry;
import io.journalkeeper.core.entry.internal.InternalEntriesSerializeSupport;
import io.journalkeeper.core.entry.internal.RecoverSnapshotEntry;
import io.journalkeeper.core.entry.internal.ReservedPartition;
import io.journalkeeper.core.entry.internal.ScalePartitionsEntry;
import io.journalkeeper.core.entry.internal.SetPreferredLeaderEntry;
import io.journalkeeper.rpc.BaseResponse;
import io.journalkeeper.rpc.client.ClientServerRpc;
import io.journalkeeper.rpc.client.ConvertRollRequest;
import io.journalkeeper.rpc.client.GetServerStatusResponse;
import io.journalkeeper.rpc.client.GetServersResponse;
import io.journalkeeper.rpc.client.GetSnapshotsResponse;
import io.journalkeeper.rpc.client.UpdateVotersRequest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author LiYue
 * Date: 2019-09-09
 */
public class DefaultAdminClient extends AbstractClient implements AdminClient {


    public DefaultAdminClient(ClientRpc clientRpc, Properties properties) {
        super(clientRpc);
    }

    @Override
    public CompletableFuture<ClusterConfiguration> getClusterConfiguration() {
        return clientRpc.invokeClientLeaderRpc(ClientServerRpc::getServers)
                .thenApply(GetServersResponse::getClusterConfiguration);
    }

    @Override
    public CompletableFuture<ClusterConfiguration> getClusterConfiguration(URI uri) {
        return clientRpc.invokeClientServerRpc(uri, ClientServerRpc::getServers)
                .thenApply(GetServersResponse::getClusterConfiguration);
    }

    @Override
    public CompletableFuture<Boolean> updateVoters(List<URI> oldConfig, List<URI> newConfig) {
        return clientRpc.invokeClientLeaderRpc(leaderRpc -> leaderRpc.updateVoters(new UpdateVotersRequest(oldConfig, newConfig)))
                .thenApply(BaseResponse::success);
    }

    @Override
    public CompletableFuture<Void> convertRoll(URI uri, RaftServer.Roll roll) {
        return clientRpc.invokeClientServerRpc(uri, rpc -> rpc.convertRoll(new ConvertRollRequest(roll)))
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Void> scalePartitions(Set<Integer> partitions) {

        ReservedPartition.validatePartitions(partitions);

        return this.update(InternalEntriesSerializeSupport.serialize(new ScalePartitionsEntry(partitions)));
    }

    private CompletableFuture<Void> update(byte[] entry) {
        return update(Collections.singletonList(new UpdateRequest(entry, RaftJournal.INTERNAL_PARTITION, 1)), false,
                ResponseConfig.REPLICATION).thenApply(v -> null);
    }

    @Override
    public CompletableFuture<ServerStatus> getServerStatus(URI uri) {
        return clientRpc.invokeClientServerRpc(uri,
                ClientServerRpc::getServerStatus)
                .thenApply(GetServerStatusResponse::getServerStatus);
    }

    @Override
    public CompletableFuture<Void> setPreferredLeader(URI preferredLeader) {
        return this.update(InternalEntriesSerializeSupport.serialize(new SetPreferredLeaderEntry(preferredLeader)));
    }

    @Override
    public CompletableFuture<Void> takeSnapshot() {
        return this.update(InternalEntriesSerializeSupport.serialize(new CreateSnapshotEntry()));
    }

    @Override
    public CompletableFuture<Void> recoverSnapshot(long index) {
        return this.update(InternalEntriesSerializeSupport.serialize(new RecoverSnapshotEntry(index)));
    }

    @Override
    public CompletableFuture<SnapshotsEntry> getSnapshots() {
        return clientRpc.invokeClientLeaderRpc(ClientServerRpc::getSnapshots)
                .thenApply(GetSnapshotsResponse::getSnapshots);
    }
}
