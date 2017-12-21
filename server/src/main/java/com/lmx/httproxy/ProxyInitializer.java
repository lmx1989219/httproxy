package com.lmx.httproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * Created by limingxin on 2017/11/9.
 */
public class ProxyInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .build();
        ch.pipeline().
                addLast("https", sslCtx.newHandler(ch.alloc())).
                addLast("codec", new HttpServerCodec()).
                addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE)).
                addLast("log", new LoggingHandler(LogLevel.DEBUG)).
                addLast("handler", new ProxyFrontendHandler());
    }
}
