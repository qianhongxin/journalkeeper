package com.jd.journalkeeper.rpc.client;

import com.jd.journalkeeper.rpc.LeaderResponse;

/**
 * @author liyue25
 * Date: 2019-03-14
 */
public class QueryClusterStateResponse  extends LeaderResponse {
    public QueryClusterStateResponse(Throwable exception) {
        super(exception);
    }
}
