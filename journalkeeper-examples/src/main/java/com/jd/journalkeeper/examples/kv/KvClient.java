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
package com.jd.journalkeeper.examples.kv;

import com.jd.journalkeeper.core.api.ClusterConfiguration;
import com.jd.journalkeeper.core.api.RaftClient;
import com.jd.journalkeeper.utils.event.EventType;
import com.jd.journalkeeper.utils.event.EventWatcher;
import com.jd.journalkeeper.utils.format.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author liyue25
 * Date: 2019-04-03
 */
public class KvClient {
    private static final Logger logger = LoggerFactory.getLogger(KvClient.class);
    private final RaftClient<KvEntry, KvQuery, KvResult> client;

    public KvClient(RaftClient<KvEntry, KvQuery, KvResult> client) {
        this.client = client;
    }

    public void set(String key, String value) {
        long t0 = System.nanoTime();
        try {
            client.update(new KvEntry(KvEntry.CMD_SET, key, value)).get();
        } catch (CompletionException e) {
            throw new RuntimeException(e.getCause());
        }catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.info("SET {} {}, {} ns.", key, value, Format.formatWithComma(System.nanoTime() - t0));
        }
    }

    public String get(String key) {
        long t0 = System.nanoTime();
        String value = null;
        try {
            return value = client
                    .query(new KvQuery(KvQuery.CMD_GET, key))
                    .get()
                    .getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.info("GET {} , return {}, {} ns.", key, value, Format.formatWithComma(System.nanoTime() - t0));
        }
    }

    public void del(String key) {
        long t0 = System.nanoTime();
        try {
            client.update(new KvEntry(KvEntry.CMD_DEL, key, null)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.info("DEL {} , {} ns.", key,  Format.formatWithComma(System.nanoTime() - t0));
        }

    }


    public List<String> listKeys() {
        long t0 = System.nanoTime();
        try {
            return client
                    .query(new KvQuery(KvQuery.CMD_LIST_KEYS, null))
                    .get()
                    .getKeys();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.info("LIST_KEYS, {} ns.", Format.formatWithComma(System.nanoTime() - t0));
        }
    }

    public ClusterConfiguration getClusterConfiguration() {
        try {
            return client.getServers().get();
        } catch (Throwable e) {
            return null;
        }
    }


    public void waitForLeader(long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        EventWatcher watcher = event -> {if(event.getEventType() == EventType.ON_LEADER_CHANGE) latch.countDown();} ;
        client.watch(watcher);
        latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        client.unWatch(watcher);
    }

}
