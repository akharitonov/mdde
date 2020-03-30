package dev.jcri.mdde.registry.server.tcp.handler;

import dev.jcri.mdde.registry.clinet.tcp.benchmark.commands.CommandArgsConverter;
import dev.jcri.mdde.registry.server.tcp.BenchmarkRunnerSingleton;
import dev.jcri.mdde.registry.server.tcp.protocol.BenchmarkContainerIn;
import dev.jcri.mdde.registry.server.tcp.protocol.BenchmarkContainerOut;
import dev.jcri.mdde.registry.server.tcp.protocol.BenchmarkResultCodes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MddeBenchmarkHandler extends ChannelInboundHandlerAdapter {
    protected static final Logger logger = LogManager.getLogger(MddeBenchmarkHandler.class);

    private BenchmarkContainerIn _lastReceivedMessage = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            _lastReceivedMessage = (BenchmarkContainerIn) msg;
        }
        finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        try {
            if(_lastReceivedMessage != null) {
                logger.trace(_lastReceivedMessage);
                ctx.write(processCommand(_lastReceivedMessage));
            }
            ctx.flush();
        }
        catch (Exception ex){
            logger.error("channelReadComplete error", ex);
        }
        finally {
            _lastReceivedMessage = null;
            super.channelReadComplete(ctx);
        }
    }

    protected BenchmarkContainerOut processCommand(BenchmarkContainerIn command){
        var commandTag = command.getOperation();
        try {
            var runner = BenchmarkRunnerSingleton.getDefaultInstance().getRunner();
            switch (commandTag) {
                case LOCATE_TUPLE:
                    var runnerArg = CommandArgsConverter.unmarshalLocateTuple(command);
                    var result = runner.getTupleLocation(runnerArg);
                    return CommandArgsConverter.marshalTupleLocatorResponse(BenchmarkResultCodes.OK, result);
                case RELEASE_CAPACITY:
                    var nodeIdArg = CommandArgsConverter.unmarshalString(command);
                    runner.notifyNodeAccessFinished(nodeIdArg);
                    return CommandArgsConverter.marshalStringResponse(BenchmarkResultCodes.OK, "ok");
                default:
                    throw new IllegalArgumentException(
                            String.format("Unhandled benchmark command command '%s'",
                                    command.getOperation().toString()));
            }

        }
        catch (Exception e){
            logger.error("processCommand error", e);
            return new BenchmarkContainerOut(BenchmarkResultCodes.ERROR, null);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause);
        ctx.close();
    }
}
