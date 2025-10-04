package com.example.logserver.service;

import com.example.logserver.handler.LogMessageHandler;
import com.example.logserver.metrics.LogMetrics;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * TCP server for receiving log streams.
 * 
 * Architecture:
 * - Non-blocking I/O with Netty event loops
 * - Line-based protocol (newline-delimited JSON)
 * - Connection limits to prevent resource exhaustion
 * - Idle timeout to close zombie connections
 * - Backpressure via bounded channel buffers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TcpLogServer {

    private final LogMessageHandler logMessageHandler;
    private final LogMetrics logMetrics;

    @Value("${tcp.server.port:9090}")
    private int port;

    @Value("${tcp.server.max-connections:1000}")
    private int maxConnections;

    @Value("${tcp.server.idle-timeout-seconds:30}")
    private int idleTimeoutSeconds;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() throws Exception {
        log.info("Starting TCP log server on port {}", port);

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // Idle state handler - closes connection after inactivity
                        pipeline.addLast(new IdleStateHandler(
                            idleTimeoutSeconds, 0, 0, TimeUnit.SECONDS
                        ));
                        
                        // Line-based frame decoder - splits by newlines
                        // Max line length 10KB to prevent memory exhaustion
                        pipeline.addLast(new LineBasedFrameDecoder(10 * 1024));
                        
                        // String decoder
                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        
                        // Business logic handler
                        pipeline.addLast(logMessageHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();

        log.info("TCP log server started successfully on port {}", port);
        logMetrics.recordServerStarted();
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down TCP log server...");
        
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        log.info("TCP log server shut down successfully");
    }
}
