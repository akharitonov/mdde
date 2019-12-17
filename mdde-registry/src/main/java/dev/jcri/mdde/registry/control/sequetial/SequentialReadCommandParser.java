package dev.jcri.mdde.registry.control.sequetial;

import dev.jcri.mdde.registry.control.ReadCommands;
import dev.jcri.mdde.registry.store.IReadCommandHandler;
import dev.jcri.mdde.registry.store.exceptions.ReadOperationException;
import dev.jcri.mdde.registry.store.exceptions.ResponseSerializationException;
import dev.jcri.mdde.registry.store.exceptions.UnknownRegistryCommandExceptions;
import dev.jcri.mdde.registry.store.response.serialization.IResponseSerializer;

import java.util.List;
import java.util.Objects;

import static dev.jcri.mdde.registry.control.ExpectedCommandArgument.*;

public class SequentialReadCommandParser<T> extends BaseSequentialCommandParser {
    private final IReadCommandHandler _readCommandHandler;
    private final IResponseSerializer<T> _serializer;

    public SequentialReadCommandParser(IReadCommandHandler readCommandHandler, IResponseSerializer<T> serializer){
        Objects.requireNonNull(readCommandHandler, "Read commands handlers can't be null");
        Objects.requireNonNull(serializer, "Serializer can't be null");
        _serializer = serializer;
        _readCommandHandler = readCommandHandler;
    }

    public T runCommand(ReadCommands readCommand, List<Object> arguments)
            throws UnknownRegistryCommandExceptions, ResponseSerializationException, ReadOperationException {
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
            throws ResponseSerializationException {
        final ReadCommands thisCommand = ReadCommands.FIND_TUPLE;
        validateNotNullArguments(arguments, thisCommand.toString());

        var tupleId = (String) Objects.requireNonNull(arguments.get(0),
                getPositionalArgumentError(thisCommand.toString(), ARG_TUPLE_ID, 0));

        return _serializer.serialize(_readCommandHandler.getTupleNodes(tupleId));
    }

    private T processFindTupleFragmentCommand(List<Object> arguments)
            throws ResponseSerializationException {
        final ReadCommands thisCommand = ReadCommands.FIND_TUPLE_FRAGMENT;
        validateNotNullArguments(arguments, thisCommand.toString());

        var tupleId = (String) Objects.requireNonNull(arguments.get(0),
                getPositionalArgumentError(thisCommand.toString(), ARG_TUPLE_ID, 0));

        return _serializer.serialize(_readCommandHandler.getTupleFragment(tupleId));
    }

    private T processFindFragmentNodesCommand(List<Object> arguments)
            throws ResponseSerializationException {
        final ReadCommands thisCommand = ReadCommands.FIND_FRAGMENT;
        validateNotNullArguments(arguments, thisCommand.toString());

        var fragmentId = (String) Objects.requireNonNull(arguments.get(0),
                getPositionalArgumentError(thisCommand.toString(), ARG_FRAGMENT_ID, 0));

        return _serializer.serialize(_readCommandHandler.getFragmentNodes(fragmentId));
    }

    private T processGetFragmentTuplesCommand(List<Object> arguments)
            throws ResponseSerializationException, ReadOperationException {
        final ReadCommands thisCommand = ReadCommands.GET_FRAGMENT_TUPLES;
        validateNotNullArguments(arguments, thisCommand.toString());

        var fragmentId = (String) Objects.requireNonNull(arguments.get(0),
                getPositionalArgumentError(thisCommand.toString(), ARG_FRAGMENT_ID, 0));

        return _serializer.serialize(_readCommandHandler.getFragmentTuples(fragmentId));
    }

    private T processCountFragmentsCommand(List<Object> arguments)
            throws ResponseSerializationException {
        final ReadCommands thisCommand = ReadCommands.COUNT_FRAGMENT;
        validateNotNullArguments(arguments, thisCommand.toString());

        var fragmentId = (String) Objects.requireNonNull(arguments.get(0),
                getPositionalArgumentError(thisCommand.toString(), ARG_FRAGMENT_ID, 0));

        return _serializer.serialize(_readCommandHandler.getCountFragment(fragmentId));
    }

    private T processCountTuplesCommand(List<Object> arguments)
            throws ResponseSerializationException {
        final ReadCommands thisCommand = ReadCommands.COUNT_TUPLE;
        validateNotNullArguments(arguments, thisCommand.toString());

        var tupleId = (String) Objects.requireNonNull(arguments.get(0),
                getPositionalArgumentError(thisCommand.toString(), ARG_TUPLE_ID, 0));

        return _serializer.serialize(_readCommandHandler.getCountTuple(tupleId));
    }

    private T processGetNodesCommand()
            throws ResponseSerializationException {
        var nodesList = _readCommandHandler.getNodes();
        return _serializer.serialize(nodesList);
    }
}
