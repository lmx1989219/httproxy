package com.lmx.httproxy;

import com.google.common.net.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;


/**
 * Created by limingxin on 2017/11/9.
 */
public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private Channel outboundChannel;

    public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
            "Connection established");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            final Channel inboundChannel = ctx.channel();
            if (outboundChannel == null) {
                Bootstrap b = new Bootstrap();
                b.group(new NioEventLoopGroup())
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer() {
                            protected void initChannel(Channel ch) throws Exception {
                                ch.pipeline().
                                        addLast(new LoggingHandler(LogLevel.DEBUG)).
                                        addLast(new ProxyBackendHandler(inboundChannel));
                            }
                        })
                        .option(ChannelOption.AUTO_READ, false);
                String host = req.headers().get("Host");
                try {
                    ChannelFuture f = b.connect(HostAndPort.fromString(host).getHostText(),
                            HostAndPort.fromString(host).hasPort() ? HostAndPort.fromString(host).getPort() : 80).sync();
                    outboundChannel = f.channel();
                    f.addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) {
                            if (future.isSuccess()) {
                                future.channel().read();
                            } else {
                                future.channel().close();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if ("CONNECT".equalsIgnoreCase(req.method().name())) {
                //删除http编解码，后续请求直接透传
                inboundChannel.pipeline().remove("codec");
                inboundChannel.pipeline().remove("aggregator");
                //直接读密文
                ctx.read();
            } else {
                outboundChannel.pipeline().
                        addFirst("agg", new HttpObjectAggregator(Integer.MAX_VALUE)).addFirst("cc", new HttpClientCodec());
                outboundChannel.writeAndFlush(msg);
            }
        } else {
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        //开始处理握手信息，直到透传任何数据请求
                        ctx.read();
                    } else {
                        future.channel().close();
                    }
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
