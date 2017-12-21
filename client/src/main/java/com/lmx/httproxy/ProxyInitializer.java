package com.lmx.httproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by limingxin on 2017/11/9.
 */
public class ProxyInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) {
        ch.pipeline().
                addLast("codec", new HttpServerCodec()).
                addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE)).
                addLast("log", new LoggingHandler(LogLevel.DEBUG)).
                addLast("handler", new ProxyFrontendHandler());
    }
}
