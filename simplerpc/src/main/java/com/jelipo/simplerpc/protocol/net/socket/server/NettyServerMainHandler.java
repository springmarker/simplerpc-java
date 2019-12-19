package com.jelipo.simplerpc.protocol.net.socket.server;

import com.jelipo.simplerpc.pojo.ExceptionType;
import com.jelipo.simplerpc.pojo.ExchangeRequest;
import com.jelipo.simplerpc.pojo.ProtocolMeta;
import com.jelipo.simplerpc.pojo.RpcRequest;
import com.jelipo.simplerpc.protocol.net.CommonMetaUtils;
import com.jelipo.simplerpc.protocol.serialization.DataSerialization;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

/**
 * 主要用于处理Netty的Handler
 *
 * @author Jelipo
 * @date 2019/6/13 12:01
 */
@ChannelHandler.Sharable
public class NettyServerMainHandler extends ChannelInboundHandlerAdapter {


    private final List<NettyWorker> workerList;
    private final NettyExceptionWorker exceptionWorker;

    private final DataSerialization dataSerialization;

    private final NettyHeartBeatWorker nettyHeartBeatWorker;

    public NettyServerMainHandler(List<NettyWorker> workerList,
                                  NettyExceptionWorker exceptionWorker,
                                  NettyHeartBeatWorker nettyHeartBeatWorker,
                                  DataSerialization dataSerialization) {
        this.nettyHeartBeatWorker = nettyHeartBeatWorker;
        this.workerList = workerList;
        this.exceptionWorker = exceptionWorker;
        this.dataSerialization = dataSerialization;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        for (NettyWorker nettyHandlerWorker : workerList) {
            nettyHandlerWorker.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBufInputStream byteBufInputStream = new ByteBufInputStream((ByteBuf) msg);
            short headerLength = byteBufInputStream.readShort();
            byte[] bytes = new byte[headerLength];
            int read = byteBufInputStream.read(bytes);
            ProtocolMeta protocolMeta = CommonMetaUtils.deserialize(bytes);
            if (protocolMeta.isHreatBeat()) {
                nettyHeartBeatWorker.handle(ctx, protocolMeta, null);
                return;
            }
            RpcRequest rpcRequest = dataSerialization.deserializeRequest(byteBufInputStream);
            for (NettyWorker nettyHandlerWorker : workerList) {
                if (nettyHandlerWorker.handle(ctx, protocolMeta, rpcRequest)) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exceptionWorker.exception(ctx, ExceptionType.RPC_INNER_EXCEPTION);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 传输出现异常时，返回rpcResponse。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        exceptionWorker.exception(ctx, ExceptionType.RPC_INNER_EXCEPTION);
    }

}
