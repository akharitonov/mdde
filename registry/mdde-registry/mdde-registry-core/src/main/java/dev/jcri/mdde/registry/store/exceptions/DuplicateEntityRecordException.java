package dev.jcri.mdde.registry.store.exceptions;

import dev.jcri.mdde.registry.exceptions.EErrorCode;
import dev.jcri.mdde.registry.exceptions.MddeRegistryException;

/**
 * Error thrown when a duplicate entity is encountered in the registry where it shouldn't
 */
public class DuplicateEntityRecordException extends MddeRegistryException {
    private final static EErrorCode _exCode = EErrorCode.DUPLICATE_ENTITY;

    /**
     * Custom message constructor
     * @param message Meaningful message
     */
    public DuplicateEntityRecordException(String message){
        super(_exCode, message);
    }

    /**
     * Constructor with predefined message about attempting to a duplicate entity id to the registry
     * @param triedToAddDuplicate Type of the duplicate entity
     * @param duplicateId Entity unique id
     */
    public DuplicateEntityRecordException(RegistryEntityType triedToAddDuplicate, String duplicateId){
        this(String.format("Attempted to add a duplicate %s with id '%s'", triedToAddDuplicate.name(), duplicateId));
    }
}
