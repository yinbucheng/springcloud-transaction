package cn.intellif.transaction.intelliftransaction.core.netty.handler;

import cn.intellif.transaction.intelliftransaction.aware.ApplicationContextUtils;
import cn.intellif.transaction.intelliftransaction.core.TransactionConnUtils;
import cn.intellif.transaction.intelliftransaction.core.netty.NettyClient;
import cn.intellif.transaction.intelliftransaction.core.netty.entity.NettyEntity;
import cn.intellif.transaction.intelliftransaction.core.netty.protocol.ProtocolUtils;
import cn.intellif.transaction.intelliftransaction.utils.SocketManager;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class IntellifTransactionHandler extends ChannelInboundHandlerAdapter{

    private Logger logger = LoggerFactory.getLogger(IntellifTransactionHandler.class);

    private Executor threadPool = Executors.newFixedThreadPool(30);

    private NettyClient nettyClient = ApplicationContextUtils.getBean(NettyClient.class);


    public IntellifTransactionHandler() {
    }


    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        logger.debug("TxManager-response->" + msg);
        final NettyEntity entity = JSON.parseObject((String)msg,NettyEntity.class);
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                handleMsg(entity);
            }
        });
    }

    /**
     * 处理接收到的消息
     * @param nettyEntity
     */
    private void handleMsg(final  NettyEntity nettyEntity){
        String key = nettyEntity.getKey();
        Integer state = nettyEntity.getStatus();
        if(state==NettyEntity.PONG){
            SocketManager.getInstance().setNetState(true);
        }
        if(state==NettyEntity.COMMIT){
            TransactionConnUtils.commit();
        }
        if(state==NettyEntity.ROLLBACK){
            TransactionConnUtils.rollback();
        }
        if(state==NettyEntity.CLOSE){
            TransactionConnUtils.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.info("disconnection  -->" + ctx);
        SocketManager.getInstance().setNetState(false);
        //链接断开,重新连接
        nettyClient.restart();
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        SocketManager.getInstance().setContext(ctx);
        logger.info("connection -->" + ctx);
        //通道激活后进行心跳检查
        SocketManager.getInstance().sendMsg(ProtocolUtils.ping());
    }


    /**
     * 当客户端的所有ChannelHandler中4s内没有write事件，则会触发userEventTriggered方法
     *
     * @param ctx  管道
     * @param evt  状态
     * @throws Exception 异常数据
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //心跳配置
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                //表示已经多久没有收到数据了
                //ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                //表示已经多久没有发送数据了
                SocketManager.getInstance().sendMsg(ProtocolUtils.ping());
            } else if (event.state() == IdleState.ALL_IDLE) {
                //表示已经多久既没有收到也没有发送数据了
            }
        }
    }

}