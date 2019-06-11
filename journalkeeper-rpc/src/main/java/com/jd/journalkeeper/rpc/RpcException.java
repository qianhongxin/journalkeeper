package com.jd.journalkeeper.rpc;

/**
 * @author liyue25
 * Date: 2019-04-03
 */
public class RpcException extends RuntimeException {
    public RpcException(BaseResponse response) {
        super(response.getError());
    }
    public RpcException(Throwable t) {
        super(t);
    }
    public RpcException(String message) {
        super(message);
    }
}
