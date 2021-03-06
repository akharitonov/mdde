package dev.jcri.mdde.registry.control;

import dev.jcri.mdde.registry.control.exceptions.CommandException;
import dev.jcri.mdde.registry.control.serialization.ResponseSerializerBase;
import dev.jcri.mdde.registry.exceptions.MddeRegistryException;
import dev.jcri.mdde.registry.shared.commands.EStateControlCommand;
import dev.jcri.mdde.registry.shared.commands.containers.result.benchmark.BenchmarkStatus;
import dev.jcri.mdde.registry.store.exceptions.UnknownRegistryCommandExceptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Basic abstract router for the CONTROL commands.
 * @param <TOut> Expected result type.
 * @param <TArgs> Arguments container type.
 */
public abstract class CommandParserControlBase<TOut, TArgs>
        implements ICommandParser<TOut, EStateControlCommand, TArgs> {

    private static final Logger logger = LogManager.getLogger(CommandParserControlBase.class);
    protected final ResponseSerializerBase<TOut> _serializer;

    protected CommandParserControlBase(ResponseSerializerBase<TOut> serializer){
        Objects.requireNonNull(serializer, "Serializer can't be null");
        this._serializer = serializer;
    }

    public final TOut runCommand(EStateControlCommand command, TArgs arguments){
        try {
            switch (command) {
                case SET_BENCHMARK:
                    return _serializer.serialize(processSetBenchmarkState());
                case RUN_BENCHMARK:
                    return _serializer.serialize(processExecuteBenchmarkCommand(arguments));
                case LOAD_DATA:
                    return _serializer.serialize(processLoadDataCommand(arguments));
                case SET_SHUFFLE:
                    return _serializer.serialize(processSetShuffleState());
                case RUN_SHUFFLE:
                    return _serializer.serialize(syncDataFromRegistryToNodes());
                case GET_REGISTRY_MODE:
                    return _serializer.serialize(processGetRegistryMode());
                case INIT_NODES:
                    return _serializer.serialize(processInitDefaultNodes());
                case RESET:
                    return _serializer.serialize(processReset());
                case FLUSHALL:
                    return _serializer.serialize(processFlushAll());
                case SNAPSHOT_CREATE:
                    return _serializer.serialize(processCreateSnapshot(arguments));
                case SNAPSHOT_LOAD:
                    return _serializer.serialize(processLoadSnapshot(arguments));
                case GET_BENCHMARK:
                    return _serializer.serialize(processGetBenchmarkState());
                case COUNTERFEIT_INITIALIZE:
                    return _serializer.serialize(processInitCounterfeitBenchmark());
                case COUNTERFEIT_BENCHMARK:
                    return _serializer.serialize(processGetCounterfeitBenchmark(arguments));
                default:
                    throw new UnknownRegistryCommandExceptions(command.toString());
            }
        }
        catch (Exception ex){
            logger.error(ex.getMessage(), ex);
            return _serializer.serializeException(ex);
        }
    }

    protected abstract Set<String> processInitDefaultNodes() throws MddeRegistryException;

    protected abstract Boolean processSetBenchmarkState() throws MddeRegistryException;

    protected abstract Boolean processSetShuffleState() throws IOException, MddeRegistryException;

    protected abstract String processGetRegistryMode() throws MddeRegistryException;

    protected abstract Boolean processReset() throws IOException, MddeRegistryException;

    protected abstract Boolean syncDataFromRegistryToNodes() throws IOException;

    protected abstract String processExecuteBenchmarkCommand(TArgs arguments) throws MddeRegistryException;

    protected abstract boolean processLoadDataCommand(TArgs arguments) throws MddeRegistryException;

    protected abstract boolean processFlushAll() throws MddeRegistryException, IOException;

    protected abstract String processCreateSnapshot(TArgs arguments) throws IOException, CommandException;

    protected abstract boolean processLoadSnapshot(TArgs arguments) throws CommandException, IOException;

    protected abstract BenchmarkStatus processGetBenchmarkState();

    protected abstract boolean processInitCounterfeitBenchmark();

    protected abstract BenchmarkStatus processGetCounterfeitBenchmark(TArgs arguments) throws CommandException;
}
