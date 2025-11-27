package com.example.syslog.server;

import com.example.syslog.model.SyslogMessage;
import com.example.syslog.parser.SyslogParser;
import com.example.syslog.service.KafkaProducerService;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SyslogServer {
    
    private static final Logger logger = LoggerFactory.getLogger(SyslogServer.class);

    @Value("${syslog.udp.port:514}")
    private int udpPort;

    @Value("${syslog.tcp.port:601}")
    private int tcpPort;

    private final SyslogParser parser;
    private final KafkaProducerService kafkaProducer;
    
    private EventLoopGroup udpGroup;
    private EventLoopGroup tcpBossGroup;
    private EventLoopGroup tcpWorkerGroup;

    public SyslogServer(SyslogParser parser, KafkaProducerService kafkaProducer) {
        this.parser = parser;
        this.kafkaProducer = kafkaProducer;
    }

    @PostConstruct
    public void start() {
        startUdpServer();
        startTcpServer();
    }

    private void startUdpServer() {
        try {
            udpGroup = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            
            bootstrap.group(udpGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                            String message = packet.content().toString(CharsetUtil.UTF_8);
                            String sourceIp = packet.sender().getAddress().getHostAddress();
                            processSyslogMessage(message, sourceIp);
                        }
                    });

            ChannelFuture future = bootstrap.bind(udpPort);
            future.addListener((ChannelFuture f) -> {
                if (f.isSuccess()) {
                    logger.info("Syslog UDP server started on port {}", udpPort);
                } else {
                    logger.warn("Failed to start UDP server on port {}: {}. Service will continue without UDP syslog support.", udpPort, f.cause() != null ? f.cause().getMessage() : "Unknown error");
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to initialize UDP server on port {}: {}. Service will continue without UDP syslog support.", udpPort, e.getMessage());
        }
    }

    private void startTcpServer() {
        try {
            tcpBossGroup = new NioEventLoopGroup(1);
            tcpWorkerGroup = new NioEventLoopGroup();
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            
            bootstrap.group(tcpBossGroup, tcpWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                new LineBasedFrameDecoder(8192),
                                new StringDecoder(CharsetUtil.UTF_8),
                                new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String message) {
                                        String sourceIp = ctx.channel().remoteAddress().toString();
                                        processSyslogMessage(message, sourceIp);
                                    }
                                }
                            );
                        }
                    });

            ChannelFuture future = bootstrap.bind(tcpPort);
            future.addListener((ChannelFuture f) -> {
                if (f.isSuccess()) {
                    logger.info("Syslog TCP server started on port {}", tcpPort);
                } else {
                    logger.warn("Failed to start TCP server on port {}: {}. Service will continue without TCP syslog support.", tcpPort, f.cause() != null ? f.cause().getMessage() : "Unknown error");
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to initialize TCP server on port {}: {}. Service will continue without TCP syslog support.", tcpPort, e.getMessage());
        }
    }

    private void processSyslogMessage(String rawMessage, String sourceIp) {
        try {
            SyslogMessage message = parser.parse(rawMessage, sourceIp);
            kafkaProducer.sendSyslogMessage(message);
        } catch (Exception e) {
            logger.error("Error processing syslog message: {}", rawMessage, e);
        }
    }

    @PreDestroy
    public void stop() {
        if (udpGroup != null) {
            udpGroup.shutdownGracefully();
        }
        if (tcpBossGroup != null) {
            tcpBossGroup.shutdownGracefully();
        }
        if (tcpWorkerGroup != null) {
            tcpWorkerGroup.shutdownGracefully();
        }
        logger.info("Syslog servers stopped");
    }
}
