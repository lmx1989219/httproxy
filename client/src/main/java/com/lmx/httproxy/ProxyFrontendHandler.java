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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;


/**
 * Created by limingxin on 2017/11/9.
 */
public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private Channel outboundChannel;

    public final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
            "Connection established");

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws SSLException {
        final SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
                                addLast("https", sslContext.newHandler(ch.alloc())).
                                addLast("codec", new HttpClientCodec()).
                                addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE)).
                                addLast(new LoggingHandler(LogLevel.DEBUG)).
                                addLast(new ProxyBackendHandler(ctx.channel()));
                    }
                })
                .option(ChannelOption.AUTO_READ, false);
        ChannelFuture f = b.connect(ProxyServer.REMOTE_HOST, ProxyServer.REMOTE_PORT)/*.sync()*/;
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    ctx.read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            final Channel inboundChannel = ctx.channel();
            if ("CONNECT".equalsIgnoreCase(req.method().name())) {
                //远程代理服务建立
                outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            //远程代理服务建立成功后伪造一个ssl通道建立请求返回给浏览器
                            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, SUCCESS);
                            inboundChannel.writeAndFlush(response).addListener(new ChannelFutureListener() {
                                public void operationComplete(ChannelFuture future) {
                                    if (future.isSuccess()) {
                                        //删除http编解码，后续请求直接透传
                                        inboundChannel.pipeline().remove("codec");
                                        inboundChannel.pipeline().remove("aggregator");
                                        //建立连接后开始准备读ssl的密文
                                        ctx.read();
                                    } else {
                                        future.channel().close();
                                    }
                                }
                            });

                            outboundChannel.pipeline().remove("https");
                            outboundChannel.pipeline().remove("codec");
                            outboundChannel.pipeline().remove("aggregator");
                        } else {
                            future.channel().close();
                        }
                    }
                });
            } else {
                outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            ctx.read();
                        } else {
                            future.channel().close();
                        }
                    }
                });
            }
        } else {
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        //client开始处理握手信息，直到透传任何数据请求
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
        cause.printStackTrace();
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
