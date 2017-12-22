package com.lmx.httproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Created by limingxin on 2017/11/9.
 */
public class ProxyBackendHandler extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;


    public ProxyBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            //这里是https密文消息
            inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        //开始处理握手信息，直到透传任何数据请求
                        ctx.read();
                    } else {
                        future.channel().close();
                    }
                }
            });
        } else if (msg instanceof FullHttpResponse) {
            inboundChannel.writeAndFlush(msg);
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ProxyFrontendHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        cause.printStackTrace();
        ProxyFrontendHandler.closeOnFlush(ctx.channel());
    }
}
