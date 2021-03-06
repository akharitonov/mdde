package dev.jcri.mdde.registry.control.serialization;

import dev.jcri.mdde.registry.exceptions.EErrorCode;
import dev.jcri.mdde.registry.shared.commands.containers.result.benchmark.BenchmarkRunResult;
import dev.jcri.mdde.registry.shared.commands.containers.result.benchmark.BenchmarkStatus;
import dev.jcri.mdde.registry.shared.store.response.FragmentCatalog;
import dev.jcri.mdde.registry.store.exceptions.ResponseSerializationException;
import dev.jcri.mdde.registry.shared.store.response.FullRegistryAllocation;

import java.util.List;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * Not a serializer really, just returns the output as a Object type.
 * Intended for unit testing and debugging, hardly makes sense in production.
 */
public class ResponseSerializerPassThrough extends ResponseSerializerBase<Object> {
    @Override
    public Object serialize(String value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(List<String> value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(Set<String> value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(int value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(boolean value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(FullRegistryAllocation value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(BenchmarkRunResult value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(BenchmarkStatus value) throws ResponseSerializationException {
        return value;
    }

    @Override
    public Object serialize(FragmentCatalog value) throws ResponseSerializationException {
        return value;
    }

    @Override
    protected Object serializeErrorWithCode(EErrorCode errorCode, String message) {
        return String.format("%s | %s",
                errorCode.getErrorCodeBase16(),
                ofNullable(message).orElse("null"));
    }
}
