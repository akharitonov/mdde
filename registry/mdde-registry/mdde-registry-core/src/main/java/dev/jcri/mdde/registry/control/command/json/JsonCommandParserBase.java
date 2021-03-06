package dev.jcri.mdde.registry.control.command.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jcri.mdde.registry.control.exceptions.MalformedCommandStatementException;
import dev.jcri.mdde.registry.shared.commands.ExpectedCommandArgument;
import dev.jcri.mdde.registry.shared.commands.ICommand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class JsonCommandParserBase {

    /**
     * Parse arguments JSON object in accordance to the expected
     * @param command Exact command that's being processed
     * @param arguments Serialized JSON object containing appropriate arguments
     * @return List of object in types and order corresponding to the ones defined in arguments property of ICommand
     * @throws MalformedCommandStatementException Command statement is unknown or malformed.
     */
    public List<Object> parseArguments(ICommand command, String arguments)
            throws MalformedCommandStatementException {
        var result = new ArrayList<Object>();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode parent= null;
        try {
            parent = mapper.readTree(arguments);
            for(var arg: command.getExpectedArguments()){
                if(arg.getArgumentType() == ExpectedCommandArgument.ArgumentType.STRING){
                    // Simple string value
                    result.add(parent.get(arg.getTag()).asText());
                }
                else if(arg.getArgumentType() == ExpectedCommandArgument.ArgumentType.SET_STRINGS){
                    // Set of strings
                    var jsonNode = parent.get(arg.getTag());
                    var content = mapper.convertValue(jsonNode, new TypeReference<HashSet<String>>() { });
                    result.add(content);
                }
                else if(arg.getArgumentType() == ExpectedCommandArgument.ArgumentType.BOOLEAN){
                    // Boolean value
                    var jsonNode = parent.get(arg.getTag());
                    var content = mapper.convertValue(jsonNode, Boolean.class);
                    result.add(content);
                }
                else if(arg.getArgumentType() == ExpectedCommandArgument.ArgumentType.INTEGER){
                    // Integer value
                    var jsonNode = parent.get(arg.getTag());
                    var content = mapper.convertValue(jsonNode, Integer.class);
                    result.add(content);
                }
                else if(arg.getArgumentType() == ExpectedCommandArgument.ArgumentType.DOUBLE){
                    // Double value
                    var jsonNode = parent.get(arg.getTag());
                    var content = mapper.convertValue(jsonNode, Double.class);
                    result.add(content);
                }
            }
        } catch (JsonProcessingException e) {
            throw new MalformedCommandStatementException("Failed to parse arguments", e);
        }

        return result;
    }
}
