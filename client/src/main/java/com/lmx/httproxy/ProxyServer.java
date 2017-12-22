package com.lmx.httproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by limingxin on 2017/11/9.
 */
public class ProxyServer {
    static final int LOCAL_PORT = Integer.parseInt(System.getProperty("port", "8888"));
    static final String REMOTE_HOST = System.getProperty("remote.host", "127.0.0.1");
    static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remote.port", "18888"));

    public static void main(String[] args) throws Exception {
        System.err.println("Client Proxying local Port on :" + LOCAL_PORT + " to remote host " + REMOTE_HOST + " port " + REMOTE_PORT);
        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(4);
        ServerBootstrap b_ = new ServerBootstrap();
        try {
            b_.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ProxyInitializer())
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(LOCAL_PORT).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
