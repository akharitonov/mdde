package dev.jcri.mdde.registry.server.tcp;

import dev.jcri.mdde.registry.configuration.RegistryConfig;
import dev.jcri.mdde.registry.configuration.reader.ConfigReaderYamlAllRedis;
import dev.jcri.mdde.registry.configuration.redis.DataNodeConfigRedis;
import dev.jcri.mdde.registry.configuration.redis.RegistryStoreConfigRedis;
import dev.jcri.mdde.registry.control.ICommandParser;
import dev.jcri.mdde.registry.control.ICommandPreProcessor;
import dev.jcri.mdde.registry.control.command.json.JsonCommandPreProcessor;
import dev.jcri.mdde.registry.control.command.json.JsonReadCommandParser;
import dev.jcri.mdde.registry.control.command.json.JsonWriteCommandParser;
import dev.jcri.mdde.registry.control.serialization.IResponseSerializer;
import dev.jcri.mdde.registry.control.serialization.ResponseSerializerJson;
import dev.jcri.mdde.registry.server.CommandProcessor;
import dev.jcri.mdde.registry.shared.commands.EReadCommand;
import dev.jcri.mdde.registry.shared.commands.EWriteCommand;
import dev.jcri.mdde.registry.store.IReadCommandHandler;
import dev.jcri.mdde.registry.store.IWriteCommandHandler;
import dev.jcri.mdde.registry.store.impl.redis.ReadCommandHandlerRedis;
import dev.jcri.mdde.registry.store.impl.redis.RedisConnectionHelper;
import dev.jcri.mdde.registry.store.impl.redis.WriteCommandHandlerRedis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static Listener _listener;

    /**
     * Main entry point
     * @param args expects:
     *             -p port on which this TCP server should be listening
     *             -c path to the appropriate MDDE configuration YAML
     */
    public static void main(String[] args){
        AppParams parsedArgs = null;
        try {
            parsedArgs = parseArgs(args);
        }catch (Exception e){
            logger.error(e);
            System.err.println(e.getMessage());
            System.exit(1);
        }
        // Read the config file
        ConfigReaderYamlAllRedis mddeAllRedisConfigReader = new ConfigReaderYamlAllRedis();
        RegistryConfig<RegistryStoreConfigRedis, DataNodeConfigRedis> mddeConfig = null;
        try {
            mddeConfig = mddeAllRedisConfigReader.readConfig(parsedArgs.getPathToConfigFile());
        } catch (Exception e) {
            logger.error(e);
            System.err.println(e.getMessage());
            System.exit(1);
        }
        // Configure CommandProcessorSingleton
        configureCommandProcessing(mddeConfig.getRegistryStore());

        // Hook attempting to properly shut down the TCP listener on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(_listener != null){
                    _listener.stop();
                    logger.info("Stopped the listener.");
                }
            } catch (Exception ex) {
                logger.error(ex);
            }
        }));

        // Start the TCP listener
        _listener = new Listener();
        try {
            _listener.start(parsedArgs.getTcpPort(), parsedArgs.getTcpBenchmarkPort());
        }
        catch (Exception ex){
            logger.error(ex);
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Configure CommandProcessorSingleton for working with redis registry store
     * @param mddeStoreConfig Redis configuration for the registry records storage
     */
    private static void configureCommandProcessing(RegistryStoreConfigRedis mddeStoreConfig){
        // Configure redis registry store
        var redisConnection = new RedisConnectionHelper(mddeStoreConfig);
        // Handle commands
        IReadCommandHandler readCommandHandler = new ReadCommandHandlerRedis(redisConnection);
        IWriteCommandHandler writeCommandHandler = new WriteCommandHandlerRedis(redisConnection, readCommandHandler);
        // Parse commands
        IResponseSerializer<String> responseSerializer = new ResponseSerializerJson();
        ICommandParser<String, EReadCommand, String> readCommandParser = new JsonReadCommandParser<>(readCommandHandler, responseSerializer);
        ICommandParser<String, EWriteCommand, String> writeCommandParser = new JsonWriteCommandParser<>(writeCommandHandler, responseSerializer);
        ICommandPreProcessor<String, String> commandPreProcessor = new JsonCommandPreProcessor();
        // Incoming statements processor
        var commandProcessor = new CommandProcessor<String, String, String>(commandPreProcessor, readCommandParser, writeCommandParser, responseSerializer);
        CommandProcessorSingleton.getDefaultInstance().initializeCommandProcessor(commandProcessor);
    }

    /**
     * Simple CLI arguments parser and verifier
     * @param args Main(args[]) contents
     * @return Parsed arguments object or Exception thrown
     */
    private static AppParams parseArgs(String[] args){
        final String portTag = "-p";
        final String configPathTag = "-c";
        final String portBenchmarkTag = "-pb";

        if(args.length < 6){
            throw new IllegalArgumentException(
                    MessageFormat.format("Required parameters: control port {}, " +
                                    "benchmark port {} " +
                                    "and path to config {}.",
                                    portTag, portBenchmarkTag, configPathTag)
            );
        }
        int port = -1;
        int portBenchmark = -1;
        Path configFilePath = null;
        Map<String, String> argsMap = new HashMap<>();
        for(int i = 0; i < args.length; i = i+2){
            var tag = args[i];
            if(i+1 >= args.length){
                throw new IllegalArgumentException(MessageFormat.format("Parameter {} passed without a value", tag));
            }
            var val = args[i+1];
            argsMap.put(tag, val);
        }
        // Get command port
        var portStr = getArgParam(argsMap, portTag);
        port = Integer.parseInt(portStr);
        // Get benchmark port
        var portBenchmarkStr = getArgParam(argsMap, portBenchmarkTag);
        portBenchmark = Integer.parseInt(portBenchmarkStr);
        // Get path to the config
        var configPathString =getArgParam(argsMap, configPathTag);

        return new AppParams(configPathString, port, portBenchmark);
    }

    private static String getArgParam(Map<String, String> argsMap, String tag){
        var value = argsMap.get(tag);
        if(value == null || value.isBlank()){
            throw new IllegalArgumentException(MessageFormat.format("Parameter {} passed without a value", tag));
        }
        return value;
    }

    /**
     * Parsed CLI arguments container
     */
    private static final class AppParams{
        private final String _pathToConfigFile;
        private final int _tcpPort;
        private final int tcpBenchmarkPort;

        private AppParams(String pathToConfigFile, int tcpPort, int tcpBenchmarkPort) {
            Objects.requireNonNull(pathToConfigFile, "Path to MDDE Registry config can't be null");
            if(tcpBenchmarkPort < 1){
                throw new IllegalArgumentException(String.format("Illegal benchmark handler TCP port: %d", tcpBenchmarkPort));
            }
            if(tcpPort < 1){
                throw new IllegalArgumentException(String.format("Illegal control handler TCP port: %d", tcpPort));
            }
            if(tcpBenchmarkPort == tcpPort){
                throw new IllegalArgumentException("Benchmark and command handlers can't run on the same port");
            }

            this._pathToConfigFile = pathToConfigFile;
            this._tcpPort = tcpPort;
            this.tcpBenchmarkPort = tcpBenchmarkPort;
        }

        /**
         * Path to the MDDE config YAML
         * @return
         */
        public String getPathToConfigFile() {
            return _pathToConfigFile;
        }

        /**
         * TCP port of this server
         * @return Port number
         */
        public int getTcpPort() {
            return _tcpPort;
        }

        /**
         * TCP port of the benchmark endpoint
         * @return Port number
         */
        public int getTcpBenchmarkPort() {
            return tcpBenchmarkPort;
        }
    }
}
