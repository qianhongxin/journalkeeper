package com.jd.journalkeeper.rpc.transport.support;


import com.jd.journalkeeper.rpc.concurrent.EventBus;
import com.jd.journalkeeper.rpc.event.TransportEvent;
import com.jd.journalkeeper.rpc.event.TransportEventHandler;
import com.jd.journalkeeper.rpc.transport.RequestBarrier;
import com.jd.journalkeeper.rpc.transport.TransportServerSupport;
import com.jd.journalkeeper.rpc.transport.codec.Codec;
import com.jd.journalkeeper.rpc.transport.codec.support.NettyDecoder;
import com.jd.journalkeeper.rpc.transport.codec.support.NettyEncoder;
import com.jd.journalkeeper.rpc.transport.command.CommandDispatcher;
import com.jd.journalkeeper.rpc.transport.command.support.DefaultCommandDispatcher;
import com.jd.journalkeeper.rpc.transport.command.support.RequestHandler;
import com.jd.journalkeeper.rpc.transport.command.support.ResponseHandler;
import com.jd.journalkeeper.rpc.transport.config.ServerConfig;
import com.jd.journalkeeper.rpc.transport.handler.CommandInvocation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;

/**
 * 默认通信服务
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2018/8/22
 */
public class DefaultTransportServer extends TransportServerSupport {

    private Codec codec;
    private RequestBarrier requestBarrier;
    private RequestHandler requestHandler;
    private ResponseHandler responseHandler;
    private EventBus<TransportEvent> transportEventBus;

    public DefaultTransportServer(ServerConfig serverConfig, String host, int port, Codec codec, RequestBarrier requestBarrier, RequestHandler requestHandler, ResponseHandler responseHandler, EventBus<TransportEvent> transportEventBus) {
        super(serverConfig, host, port);
        this.codec = codec;
        this.requestBarrier = requestBarrier;
        this.requestHandler = requestHandler;
        this.responseHandler = responseHandler;
        this.transportEventBus = transportEventBus;
    }

    @Override
    protected ChannelHandler newChannelHandlerPipeline() {
        final CommandDispatcher commandDispatcher = new DefaultCommandDispatcher(requestBarrier, requestHandler, responseHandler);
        final TransportEventHandler transportEventHandler = new TransportEventHandler(requestBarrier, transportEventBus);
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline()
                        .addLast(new NettyDecoder(codec))
                        .addLast(new NettyEncoder(codec))
                        .addLast(transportEventHandler)
                        .addLast(new CommandInvocation(commandDispatcher));
            }
        };
    }
}