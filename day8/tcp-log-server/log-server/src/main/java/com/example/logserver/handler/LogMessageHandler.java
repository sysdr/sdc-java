package com.example.logserver.handler;

import com.example.logserver.metrics.LogMetrics;
import com.example.logserver.service.LogBufferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.channel.ChannelHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles incoming log messages from TCP connections.
 * 
 * Responsibilities:
 * - Parse JSON log messages
 * - Validate log structure
 * - Forward to buffer service
 * - Handle connection lifecycle events
 * - Track metrics per connection
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class LogMessageHandler extends SimpleChannelInboundHandler<String> {

    private final LogBufferService bufferService;
    private final ObjectMapper objectMapper;
    private final LogMetrics logMetrics;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("New connection from {}", ctx.channel().remoteAddress());
        logMetrics.incrementActiveConnections();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Connection closed from {}", ctx.channel().remoteAddress());
        logMetrics.decrementActiveConnections();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            // Parse JSON log message
            @SuppressWarnings("unchecked")
            Map<String, Object> logData = objectMapper.readValue(msg, Map.class);
            
            // Validate required fields
            if (!logData.containsKey("timestamp") || 
                !logData.containsKey("level") || 
                !logData.containsKey("message")) {
                log.warn("Invalid log format from {}: {}", 
                    ctx.channel().remoteAddress(), msg);
                logMetrics.incrementInvalidMessages();
                return;
            }

            // Add to buffer
            boolean accepted = bufferService.addLog(logData);
            
            if (accepted) {
                logMetrics.incrementReceivedMessages();
            } else {
                logMetrics.incrementDroppedMessages();
                log.warn("Log dropped due to buffer full: {}", msg);
            }

        } catch (Exception e) {
            log.error("Error processing log message from {}: {}", 
                ctx.channel().remoteAddress(), e.getMessage());
            logMetrics.incrementProcessingErrors();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.info("Closing idle connection from {}", 
                    ctx.channel().remoteAddress());
                logMetrics.incrementIdleTimeouts();
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in channel from {}: {}", 
            ctx.channel().remoteAddress(), cause.getMessage());
        logMetrics.incrementConnectionErrors();
        ctx.close();
    }
}
