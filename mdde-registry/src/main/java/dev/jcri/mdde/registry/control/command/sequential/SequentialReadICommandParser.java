package dev.jcri.mdde.registry.control.command.sequential;

import dev.jcri.mdde.registry.control.ICommandParser;
import dev.jcri.mdde.registry.control.ReadCommand;
import dev.jcri.mdde.registry.control.exceptions.IllegalCommandArgumentException;
import dev.jcri.mdde.registry.exceptions.MddeRegistryException;
import dev.jcri.mdde.registry.store.IReadCommandHandler;
import dev.jcri.mdde.registry.store.exceptions.ReadOperationException;
import dev.jcri.mdde.registry.store.exceptions.ResponseSerializationException;
import dev.jcri.mdde.registry.store.exceptions.UnknownRegistryCommandExceptions;
import dev.jcri.mdde.registry.control.serialization.IResponseSerializer;

import java.util.List;
import java.util.Objects;

import static dev.jcri.mdde.registry.control.ExpectedCommandArgument.*;

public class SequentialReadICommandParser<T> extends BaseSequentialCommandParser implements ICommandParser<T, ReadCommand, List<Object>> {
    private final IReadCommandHandler _readCommandHandler;
    private final IResponseSerializer<T> _serializer;

    public SequentialReadICommandParser(IReadCommandHandler readCommandHandler, IResponseSerializer<T> serializer){
        Objects.requireNonNull(readCommandHandler, "Read commands handlers can't be null");
        Objects.requireNonNull(serializer, "Serializer can't be null");
        _serializer = serializer;
        _readCommandHandler = readCommandHandler;
    }

    public T runCommand(ReadCommand readCommand, List<Object> arguments)
            throws UnknownRegistryCommandExceptions, MddeRegistryException {
        switch (readCommand)    {
            case GET_REGISTRY:
                return processGetFullRegistryCommand();
            case FIND_TUPLE:
                return processFindTupleCommand(arguments);
            case FIND_TUPLE_FRAGMENT:
                return processFindTupleFragmentCommand(arguments);
            case FIND_FRAGMENT:
                return processFindFragmentNodesCommand(arguments);
            case GET_FRAGMENT_TUPLES:
                return processGetFragmentTuplesCommand(arguments);
            case COUNT_FRAGMENT:
                return processCountFragmentsCommand(arguments);
            case COUNT_TUPLE:
                return processCountTuplesCommand(arguments);
            case GET_NODES:
                return processGetNodesCommand();
        }

        throw new UnknownRegistryCommandExceptions(readCommand.toString());
    }

    private T processGetFullRegistryCommand() throws ResponseSerializationException, ReadOperationException {
        return _serializer.serialize(_readCommandHandler.getFullRegistry());
    }

    private T processFindTupleCommand(List<Object> arguments)
            throws ResponseSerializationException, IllegalCommandArgumentException {
        final ReadCommand thisCommand = ReadCommand.FIND_TUPLE;
        validateNotNullArguments(arguments, thisCommand.toString());

        var tupleId = getPositionalArgumentAsString(arguments, thisCommand, ARG_TUPLE_ID);
        return _serializer.serialize(_readCommandHandler.getTupleNodes(tupleId));
    }

    private T processFindTupleFragmentCommand(List<Object> arguments)
            throws ResponseSerializationException, IllegalCommandArgumentException {
        final ReadCommand thisCommand = ReadCommand.FIND_TUPLE_FRAGMENT;
        validateNotNullArguments(arguments, thisCommand.toString());

        var tupleId = getPositionalArgumentAsString(arguments, thisCommand, ARG_TUPLE_ID);
        return _serializer.serialize(_readCommandHandler.getTupleFragment(tupleId));
    }

    private T processFindFragmentNodesCommand(List<Object> arguments)
            throws ResponseSerializationException, IllegalCommandArgumentException {
        final ReadCommand thisCommand = ReadCommand.FIND_FRAGMENT;
        validateNotNullArguments(arguments, thisCommand.toString());

        var fragmentId = getPositionalArgumentAsString(arguments, thisCommand, ARG_FRAGMENT_ID);
        return _serializer.serialize(_readCommandHandler.getFragmentNodes(fragmentId));
    }

    private T processGetFragmentTuplesCommand(List<Object> arguments)
            throws ResponseSerializationException, ReadOperationException, IllegalCommandArgumentException {
        final ReadCommand thisCommand = ReadCommand.GET_FRAGMENT_TUPLES;
        validateNotNullArguments(arguments, thisCommand.toString());

        var fragmentId = getPositionalArgumentAsString(arguments, thisCommand, ARG_FRAGMENT_ID);
        return _serializer.serialize(_readCommandHandler.getFragmentTuples(fragmentId));
    }

    private T processCountFragmentsCommand(List<Object> arguments)
            throws ResponseSerializationException, IllegalCommandArgumentException {
        final ReadCommand thisCommand = ReadCommand.COUNT_FRAGMENT;
        validateNotNullArguments(arguments, thisCommand.toString());

        var fragmentId = getPositionalArgumentAsString(arguments, thisCommand, ARG_FRAGMENT_ID);
        return _serializer.serialize(_readCommandHandler.getCountFragment(fragmentId));
    }

    private T processCountTuplesCommand(List<Object> arguments)
            throws ResponseSerializationException, IllegalCommandArgumentException {
        final ReadCommand thisCommand = ReadCommand.COUNT_TUPLE;
        validateNotNullArguments(arguments, thisCommand.toString());

        var tupleId = getPositionalArgumentAsString(arguments, thisCommand, ARG_TUPLE_ID);
        return _serializer.serialize(_readCommandHandler.getCountTuple(tupleId));
    }

    private T processGetNodesCommand()
            throws ResponseSerializationException {
        var nodesList = _readCommandHandler.getNodes();
        return _serializer.serialize(nodesList);
    }
}