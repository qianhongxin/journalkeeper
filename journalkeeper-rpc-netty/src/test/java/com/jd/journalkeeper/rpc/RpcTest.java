/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.journalkeeper.rpc;

import com.jd.journalkeeper.core.api.ClusterConfiguration;
import com.jd.journalkeeper.core.api.ResponseConfig;
import com.jd.journalkeeper.exceptions.IndexOverflowException;
import com.jd.journalkeeper.exceptions.IndexUnderflowException;
import com.jd.journalkeeper.exceptions.NotLeaderException;
import com.jd.journalkeeper.rpc.client.*;
import com.jd.journalkeeper.rpc.server.*;
import com.jd.journalkeeper.utils.event.Event;
import com.jd.journalkeeper.utils.event.EventWatcher;
import com.jd.journalkeeper.utils.event.PullEvent;
import com.jd.journalkeeper.utils.net.NetworkingUtils;
import com.jd.journalkeeper.utils.state.StateServer;
import com.jd.journalkeeper.utils.test.ByteUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

/**
 * @author liyue25
 * Date: 2019-04-01
 */
public class RpcTest {
    private static final Logger logger = LoggerFactory.getLogger(RpcTest.class);
    private ServerRpc serverRpcMock = mock(ServerRpc.class);
    private ClientServerRpcAccessPoint clientServerRpcAccessPoint;
    private ServerRpcAccessPoint serverRpcAccessPoint;
    private StateServer server;
    @Before
    public void before() throws IOException, URISyntaxException {
        int port = NetworkingUtils.findRandomOpenPortOnAllLocalInterfaces();
        when(serverRpcMock.serverUri()).thenReturn(new URI("jk://localhost:" + port));
        logger.info("Server URI: {}", serverRpcMock.serverUri());
        RpcAccessPointFactory rpcAccessPointFactory = new JournalKeeperRpcAccessPointFactory();

        server = rpcAccessPointFactory.bindServerService(serverRpcMock);
        server.start();

        clientServerRpcAccessPoint = rpcAccessPointFactory.createClientServerRpcAccessPoint(Collections.singletonList(serverRpcMock.serverUri()),new Properties());
        serverRpcAccessPoint = rpcAccessPointFactory.createServerRpcAccessPoint(new Properties());
    }

    @Test
    public  void testException() throws ExecutionException, InterruptedException {
        // Test exception response
        LastAppliedResponse response;
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();

        String errorMsg = "原谅他们是上帝的事，我们的任务是负责送他们见上帝。 --普京";
        Throwable t = new RuntimeException(errorMsg);
        when(serverRpcMock.lastApplied()).thenThrow(t);
        response = clientServerRpc.lastApplied().get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.EXCEPTION, response.getStatusCode());
        Assert.assertTrue(response.getError().contains(errorMsg));
    }

    @Test
    public  void testResponseException() throws ExecutionException, InterruptedException {
        // Test exception response
        LastAppliedResponse response;
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();

        String errorMsg = "原谅他们是上帝的事，我们的任务是负责送他们见上帝。 --普京";
        Throwable t = new RuntimeException(errorMsg);
        when(serverRpcMock.lastApplied())
                .thenReturn(CompletableFuture.supplyAsync(() -> new LastAppliedResponse(t)));
        response = clientServerRpc.lastApplied().get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.EXCEPTION, response.getStatusCode());
        Assert.assertTrue(response.getError().contains(errorMsg));
    }
    @Test
    public void testNotLeader() throws ExecutionException, InterruptedException {
        LastAppliedResponse response;
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();

        String leaderUriStr = "jk://leader.host:8888";
        when(serverRpcMock.lastApplied())
                .thenThrow(new NotLeaderException(URI.create(leaderUriStr)));
        response = clientServerRpc.lastApplied().get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.NOT_LEADER, response.getStatusCode());
        Assert.assertEquals(leaderUriStr, response.getLeader().toString());
    }

    @Test
    public void testUpdateClusterState() throws ExecutionException, InterruptedException {
        int entrySize = 128;

        byte [] entry = new byte[entrySize];
        for (int i = 0; i < entrySize; i++) {
            entry[i] = (byte) i;
        }
        UpdateClusterStateRequest request = new UpdateClusterStateRequest(entry, 0, 1, ResponseConfig.RECEIVE);
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        UpdateClusterStateResponse response;
        // Test success response
        when(serverRpcMock.updateClusterState(any(UpdateClusterStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(UpdateClusterStateResponse::new));
        response = clientServerRpc.updateClusterState(request).get();
        Assert.assertTrue(response.success());
        verify(serverRpcMock).updateClusterState(argThat((UpdateClusterStateRequest r) ->
                Arrays.equals(entry, r.getEntry()) &&
                ResponseConfig.RECEIVE == r.getResponseConfig()));

    }

    @Test
    public void testQueryClusterState() throws ExecutionException, InterruptedException {
        int querySize = 128;
        int resultSize = 55;
        byte [] query = new byte[querySize];
        for (int i = 0; i < querySize; i++) {
            query[i] = (byte) i;
        }
        byte [] result = new byte[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = (byte) i;
        }
        QueryStateRequest request = new QueryStateRequest(query);
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        QueryStateResponse response;
        // Test success response
        when(serverRpcMock.queryClusterState(any(QueryStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new QueryStateResponse(result)));
        response = clientServerRpc.queryClusterState(request).get();
        Assert.assertTrue(response.success());
        Assert.assertArrayEquals(result, response.getResult());

        verify(serverRpcMock).queryClusterState(
                argThat((QueryStateRequest r) -> Arrays.equals(query, r.getQuery())));

    }


    @Test
    public void testQueryServerState() throws ExecutionException, InterruptedException {
        int querySize = 128;
        int resultSize = 55;
        byte [] query = new byte[querySize];
        for (int i = 0; i < querySize; i++) {
            query[i] = (byte) i;
        }
        byte [] result = new byte[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = (byte) i;
        }
        long lastApplied = -993L;
        QueryStateRequest request = new QueryStateRequest(query);
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        QueryStateResponse response;
        // Test success response
        when(serverRpcMock.queryServerState(any(QueryStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new QueryStateResponse(result, lastApplied)));
        response = clientServerRpc.queryServerState(request).get();
        Assert.assertTrue(response.success());
        Assert.assertEquals(lastApplied, response.getLastApplied());
        Assert.assertArrayEquals(result, response.getResult());

        verify(serverRpcMock).queryServerState(
                argThat((QueryStateRequest r) -> Arrays.equals(query, r.getQuery())));

    }

    @Test
    public void testQuerySnapshotState() throws ExecutionException, InterruptedException {
        int querySize = 128;
        int resultSize = 55;
        byte [] query = new byte[querySize];
        for (int i = 0; i < querySize; i++) {
            query[i] = (byte) i;
        }
        byte [] result = new byte[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = (byte) i;
        }
        long index = 23339L;
        QueryStateRequest request = new QueryStateRequest(query, index);
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        QueryStateResponse response;
        // Test success response
        when(serverRpcMock.queryServerState(any(QueryStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new QueryStateResponse(result)));
        response = clientServerRpc.queryServerState(request).get();
        Assert.assertTrue(response.success());
        Assert.assertArrayEquals(result, response.getResult());

        verify(serverRpcMock).queryServerState(
                argThat((QueryStateRequest r) -> Arrays.equals(query, r.getQuery()) && r.getIndex() == index));


        // Test index overflow
        when(serverRpcMock.queryServerState(any(QueryStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new QueryStateResponse(new IndexOverflowException())));
        response = clientServerRpc.queryServerState(request).get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.INDEX_OVERFLOW, response.getStatusCode());

        // Test index underflow
        when(serverRpcMock.queryServerState(any(QueryStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new QueryStateResponse(new IndexUnderflowException())));
        response = clientServerRpc.queryServerState(request).get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.INDEX_UNDERFLOW, response.getStatusCode());


    }

    @Test
    public void testGerServers() throws ExecutionException, InterruptedException {
        URI leader = URI.create("jk://leader_host:8888");
        List<URI> observers = null;
        List<URI> voters = Arrays.asList(
                URI.create("jk://voter1_host:8888"),
                URI.create("jk://leader_host:8888"),
                URI.create("jk://192.168.8.8:8888"));
        ClusterConfiguration clusterConfiguration = new ClusterConfiguration(leader, voters, observers);
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        GetServersResponse response;

        when(serverRpcMock.getServers())
                .thenReturn(CompletableFuture.supplyAsync(() -> new GetServersResponse(clusterConfiguration)));
        response = clientServerRpc.getServers().get();
        Assert.assertTrue(response.success());

        Assert.assertEquals(leader, response.getClusterConfiguration().getLeader());
        Assert.assertEquals(voters, response.getClusterConfiguration().getVoters());
        Assert.assertTrue(response.getClusterConfiguration().getObservers().isEmpty());
    }


    @Test
    public void testAddPullWatch() throws ExecutionException, InterruptedException {

        long pullWatchId = 666L;
        long pullIntervalMs =  10000L;
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        AddPullWatchResponse response;

        when(serverRpcMock.addPullWatch())
                .thenReturn(CompletableFuture.supplyAsync(() -> new AddPullWatchResponse(pullWatchId, pullIntervalMs)));
        response = clientServerRpc.addPullWatch().get();
        Assert.assertTrue(response.success());

        Assert.assertEquals(pullWatchId, response.getPullWatchId());
        Assert.assertEquals(pullIntervalMs, response.getPullIntervalMs());
    }


    @Test
    public void testRemovePullWatch() throws ExecutionException, InterruptedException {

        long pullWatchId = 666L;
        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        RemovePullWatchResponse response;

        when(serverRpcMock.removePullWatch(any(RemovePullWatchRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(RemovePullWatchResponse::new));
        response = clientServerRpc.removePullWatch(new RemovePullWatchRequest(pullWatchId)).get();
        Assert.assertTrue(response.success());
        verify(serverRpcMock).removePullWatch(argThat((RemovePullWatchRequest r) -> r.getPullWatchId() == pullWatchId));
    }

    @Test
    public void testPullEvents() throws ExecutionException, InterruptedException {

        long pullWatchId = 666L;
        long ackSequence = 888888L;
        Map<String, String> eventData = new HashMap<>();
        eventData.put("key1", "value1");
        eventData.put("key2", "value2");
        List<PullEvent> pullEvents = Collections.singletonList(new PullEvent(23, 83999L, eventData));


        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();
        PullEventsResponse response;

        when(serverRpcMock.pullEvents(any(PullEventsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new PullEventsResponse(pullEvents)));
        response = clientServerRpc.pullEvents(new PullEventsRequest(pullWatchId, ackSequence)).get();
        Assert.assertTrue(response.success());

        Assert.assertEquals(pullEvents.size(),response.getPullEvents().size());
        Assert.assertEquals(pullEvents.get(0).getSequence(), response.getPullEvents().get(0).getSequence());
        Assert.assertEquals(pullEvents.get(0).getEventData(), response.getPullEvents().get(0).getEventData());

        verify(serverRpcMock).pullEvents(argThat((PullEventsRequest r) ->
                r.getPullWatchId() == pullWatchId &&
                r.getAckSequence() == ackSequence));
    }

    @Test
    public void testWatch() throws Exception {
        long pullWatchId = 666L;
        long pullIntervalMs = 100L;
        Map<String, String> eventData = new HashMap<>();
        eventData.put("key1", "value1");
        eventData.put("key2", "value2");
        List<PullEvent> pullEvents = Collections.singletonList(new PullEvent(23, 83999L, eventData));


        ClientServerRpc clientServerRpc = clientServerRpcAccessPoint.defaultClientServerRpc();

        when(serverRpcMock.pullEvents(any(PullEventsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new PullEventsResponse(pullEvents)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new PullEventsResponse(Collections.emptyList())));
        when(serverRpcMock.addPullWatch())
                .thenReturn(CompletableFuture.supplyAsync(() -> new AddPullWatchResponse(pullWatchId, pullIntervalMs)));
        when(serverRpcMock.removePullWatch(any(RemovePullWatchRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(RemovePullWatchResponse::new));

        List<Event> eventList = new ArrayList<>();
        EventWatcher eventWatcher = eventList::add;

        clientServerRpc.watch(eventWatcher);
        Thread.sleep(3 * pullIntervalMs);
        clientServerRpc.unWatch(eventWatcher);

        Assert.assertEquals(pullEvents.size(), eventList.size());
        Assert.assertEquals(pullEvents.get(0).getEventData(), eventList.get(0).getEventData());
    }



    @Test
    public void testAsyncAppendEntries() throws ExecutionException, InterruptedException {

        AsyncAppendEntriesRequest request = new AsyncAppendEntriesRequest(
                88,
                URI.create("jk://leader.host:8888"),
                838472234228L,
                87,
                ByteUtils.createRandomSizeByteList(1024, 1000),
                6666666L
        );
        ServerRpc serverRpc = serverRpcAccessPoint.getServerRpcAgent(serverRpcMock.serverUri());
        AsyncAppendEntriesResponse response, serverResponse;
        serverResponse = new AsyncAppendEntriesResponse(false, 8837222L, 74,request.getEntries().size());
        // Test success response
        when(serverRpcMock.asyncAppendEntries(any(AsyncAppendEntriesRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> serverResponse));
        response = serverRpc.asyncAppendEntries(request).get();
        Assert.assertTrue(response.success());
        Assert.assertEquals(serverResponse.getJournalIndex(), response.getJournalIndex());
        Assert.assertEquals(serverResponse.isSuccess(), response.isSuccess());
        Assert.assertEquals(serverResponse.getTerm(), response.getTerm());
        Assert.assertEquals(serverResponse.getEntryCount(), response.getEntryCount());

        verify(serverRpcMock).asyncAppendEntries(
                argThat((AsyncAppendEntriesRequest r) ->
                                r.getTerm() == request.getTerm() &&
                                r.getLeader().equals(request.getLeader()) &&
                                r.getPrevLogIndex() == request.getPrevLogIndex() &&
                                r.getPrevLogTerm() == request.getPrevLogTerm() &&
                                r.getLeaderCommit() == request.getLeaderCommit() &&
                                testListOfBytesEquals(r.getEntries(), request.getEntries())
                        ));

    }

    @Test
    public void testRequestVote() throws ExecutionException, InterruptedException {

        RequestVoteRequest request = new RequestVoteRequest(
                88,
                URI.create("jk://candidate.host:8888"),
                6666666L,
                87
                );
        ServerRpc serverRpc = serverRpcAccessPoint.getServerRpcAgent(serverRpcMock.serverUri());
        RequestVoteResponse response, serverResponse;
        serverResponse = new RequestVoteResponse(88, false);
        // Test success response
        when(serverRpcMock.requestVote(any(RequestVoteRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> serverResponse));
        response = serverRpc.requestVote(request).get();
        Assert.assertTrue(response.success());
        Assert.assertEquals(serverResponse.isVoteGranted(), response.isVoteGranted());
        Assert.assertEquals(serverResponse.getTerm(), response.getTerm());

        verify(serverRpcMock).requestVote(
                argThat((RequestVoteRequest r) ->
                                r.getTerm() == request.getTerm() &&
                                r.getCandidate().equals(request.getCandidate()) &&
                                r.getLastLogIndex() == request.getLastLogIndex() &&
                                r.getLastLogTerm() == request.getLastLogTerm()
                        ));

    }


    @Test
    public void testGetServerEntries() throws ExecutionException, InterruptedException {

        GetServerEntriesRequest request = new GetServerEntriesRequest(
                6666666L,
                87
                );
        ServerRpc serverRpc = serverRpcAccessPoint.getServerRpcAgent(serverRpcMock.serverUri());
        GetServerEntriesResponse response, serverResponse;
        serverResponse = new GetServerEntriesResponse(
                ByteUtils.createRandomSizeByteList(2048, 1024),
                87783L
                , 9384884L);
        // Test success response
        when(serverRpcMock.getServerEntries(any(GetServerEntriesRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> serverResponse));
        response = serverRpc.getServerEntries(request).get();
        Assert.assertTrue(response.success());
        Assert.assertTrue(testListOfBytesEquals(serverResponse.getEntries(), response.getEntries()));
        Assert.assertEquals(serverResponse.getMinIndex(), response.getMinIndex());
        Assert.assertEquals(serverResponse.getLastApplied(), response.getLastApplied());

        verify(serverRpcMock).getServerEntries(
                argThat((GetServerEntriesRequest r) ->
                                r.getIndex() == request.getIndex() &&
                                r.getMaxSize() == request.getMaxSize()
                ));

        when(serverRpcMock.getServerEntries(any(GetServerEntriesRequest.class)))
                .thenThrow(new IndexUnderflowException());
        response = serverRpc.getServerEntries(request).get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.INDEX_UNDERFLOW, response.getStatusCode());

        when(serverRpcMock.getServerEntries(any(GetServerEntriesRequest.class)))
                .thenThrow(new IndexOverflowException());
        response = serverRpc.getServerEntries(request).get();
        Assert.assertFalse(response.success());
        Assert.assertEquals(StatusCode.INDEX_OVERFLOW, response.getStatusCode());


    }

    @Test
    public void testGetServerState() throws ExecutionException, InterruptedException {

        GetServerStateRequest request = new GetServerStateRequest(
                6666666L,
                87444L
                );
        ServerRpc serverRpc = serverRpcAccessPoint.getServerRpcAgent(serverRpcMock.serverUri());
        GetServerStateResponse response, serverResponse;
        serverResponse = new GetServerStateResponse(
                2342345L,
                883,
                899334545L,
                ByteUtils.createRandomSizeBytes(1024 * 1024 * 10),
                false);
        // Test success response
        when(serverRpcMock.getServerState(any(GetServerStateRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> serverResponse));
        response = serverRpc.getServerState(request).get();
        Assert.assertTrue(response.success());
        Assert.assertEquals(serverResponse.getLastIncludedIndex(), response.getLastIncludedIndex());
        Assert.assertEquals(serverResponse.getLastIncludedTerm(), response.getLastIncludedTerm());
        Assert.assertEquals(serverResponse.getOffset(), response.getOffset());
        Assert.assertArrayEquals(serverResponse.getData(), response.getData());
        Assert.assertEquals(serverResponse.isDone(), response.isDone());

        verify(serverRpcMock).getServerState(
                argThat((GetServerStateRequest r) ->
                                r.getLastIncludedIndex() == request.getLastIncludedIndex() &&
                                r.getOffset() == request.getOffset()
                ));

    }

    private static boolean testListOfBytesEquals(List<byte[]> entries, List<byte[]> entries1) {
        if(entries.size() == entries1.size()) {
            for (int i = 0; i < entries.size(); i++) {
                if(!Arrays.equals(entries.get(i), entries1.get(i))){
                    return false;
                }
            }
            return  true;
        }
        return false;
    }


    @After
    public void after() {
        if(null != server) server.stop();
    }





}
