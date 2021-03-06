package dev.jcri.mdde.registry.benchmark;

import dev.jcri.mdde.registry.benchmark.cluster.IReadOnlyTupleLocator;
import dev.jcri.mdde.registry.benchmark.cluster.ITupleLocatorFactory;
import dev.jcri.mdde.registry.benchmark.counterfeit.CounterfeitBenchSettings;
import dev.jcri.mdde.registry.benchmark.counterfeit.CounterfeitRunner;
import dev.jcri.mdde.registry.benchmark.ycsb.EYCSBWorkloadCatalog;
import dev.jcri.mdde.registry.benchmark.ycsb.YCSBRunner;
import dev.jcri.mdde.registry.data.exceptions.KeyNotFoundException;
import dev.jcri.mdde.registry.exceptions.MddeRegistryException;
import dev.jcri.mdde.registry.shared.benchmark.commands.LocateTuple;
import dev.jcri.mdde.registry.shared.benchmark.enums.EBenchmarkRunStage;
import dev.jcri.mdde.registry.shared.benchmark.responses.TupleLocation;
import dev.jcri.mdde.registry.shared.commands.containers.result.benchmark.BenchmarkRunResult;
import dev.jcri.mdde.registry.shared.commands.containers.result.benchmark.BenchmarkStatus;
import dev.jcri.mdde.registry.shared.store.response.TupleCatalog;
import dev.jcri.mdde.registry.store.IReadCommandHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Optional.ofNullable;


public class BenchmarkRunner {
    private static final Logger logger = LogManager.getLogger(BenchmarkRunner.class);

    private final ITupleLocatorFactory _tupleLocatorFactory;
    private final IReadCommandHandler _storeReader;
    private final YCSBRunner _ycsbRunner;
    private final CounterfeitRunner _counterfeitRunner;

    // TODO: Refactor counterfeit benchmark parametrisation (interface + factory)
    private String _lastValidRunId = null;
    private BenchmarkRunResult _lastValidResult = null;

    /**
     * State of the executing benchmark, results of the executed benchmark
     */
    private final RunnerState _runnerState = new RunnerState();

    /**
     * Constructor
     * @param tupleLocatorFactory Factory of the tuple locators used for benchmark runs
     * @param storeReader Implementation of the Registry-store reader
     * @param ycsbRunner YCSB Benchmark runner initialized instance
     */
    public BenchmarkRunner(ITupleLocatorFactory tupleLocatorFactory,
                           IReadCommandHandler storeReader,
                           YCSBRunner ycsbRunner){
        Objects.requireNonNull(tupleLocatorFactory);
        Objects.requireNonNull(storeReader);
        Objects.requireNonNull(ycsbRunner);
        _tupleLocatorFactory = tupleLocatorFactory;
        _storeReader = storeReader;
        _ycsbRunner = ycsbRunner;

        _counterfeitRunner = new CounterfeitRunner(storeReader);
    }

    private IReadOnlyTupleLocator _tmpTupleLocator = null;

    /**
     * Make preparations for running a benchmark.
     * @throws MddeRegistryException Failure to switch the registry to "Benchmark-ready" state.
     */
    public synchronized void prepareBenchmarkEnvironment()
            throws MddeRegistryException {
        IReadOnlyTupleLocator newLocator = _tupleLocatorFactory.getNewTupleLocator();
        TupleCatalog currentTupleStoreSnapshot = _storeReader.getTupleCatalog();
        newLocator.initializeDataLocator(currentTupleStoreSnapshot);
        _tmpTupleLocator = newLocator;
    }

    /**
     * Call after running a benchmark. It disposes of all resources and memory objects that are needed only at a time
     * of executing the benchmark.
     * @throws IOException Failure to switching off the "Benchmark ready" state of the registry.
     */
    public synchronized void disposeBenchmarkEnvironment()
            throws IOException {
        try {
            if (_tmpTupleLocator != null) _tmpTupleLocator.close();
        } finally {
            _tmpTupleLocator = null;
        }
    }

    private void verifyState(){
        if(_tmpTupleLocator == null){
            throw new IllegalStateException("Benchmark runner has no initialized benchmark environment. " +
                    "You must call prepareBenchmarkEnvironment() prior executing the benchmark.");
        }
    }

    private final ReentrantLock _benchmarkRunnerLock = new ReentrantLock();
    private EBenchmarkLoadStage _currentLoadState = EBenchmarkLoadStage.EMPTY;

    /**
     * Generate data in the nodes.
     * @param workload YCSB workload name from the catalog.
     * @return Success flag.
     */
    public boolean generateData(String workload) {
        var knownWorkload = EYCSBWorkloadCatalog.getWorkloadByTag(workload);
        return generateData(knownWorkload);
    }

    /**
     * LOAD initial data into the data nodes
     * @param workload Selected workload.
     * @return True - data was loaded successfully.
     */
    public boolean generateData(EYCSBWorkloadCatalog workload){
        _benchmarkRunnerLock.lock();
        try {
            if (_currentLoadState != EBenchmarkLoadStage.EMPTY) {
                throw new IllegalStateException(String.format("Benchmark data load is in incorrect state: %s",
                        _currentLoadState.toString()));
            }
            if (isBenchmarkInAMiddleOfARun()) {
                throw new IllegalStateException("Benchmark is already being executed");
            }
            _currentLoadState = EBenchmarkLoadStage.LOADING;
        }
        finally {
            _benchmarkRunnerLock.unlock();
        }
        try{
            var ycsbRunOutput = this._ycsbRunner.loadWorkload(workload);
            logger.trace(ycsbRunOutput);
        } catch (IOException e) {
            logger.error("Error loading workload", e);
            return false;
        } finally {
            _currentLoadState = EBenchmarkLoadStage.READY;
        }
        return true;
    }

    /**
     * Check if the benchmark workload is currently in the process of execution.
     * @return True - workload is running, False otherwise.
     */
    private boolean isBenchmarkInAMiddleOfARun(){
        return _runnerState.getState() != EBenchmarkRunStage.READY
                && _runnerState.getState() != EBenchmarkRunStage.DONE;
    }

    /**
     * Flush all of the data from data nodes.
     * @return Success flag.
     */
    public boolean flushData(){
        _benchmarkRunnerLock.lock();
        try {
            if (_currentLoadState != EBenchmarkLoadStage.READY
                    && _currentLoadState != EBenchmarkLoadStage.EMPTY) {
                throw new IllegalStateException(String.format("Benchmark data load is in incorrect state: %s",
                        _currentLoadState.toString()));
            }
            if (isBenchmarkInAMiddleOfARun()) {
                throw new IllegalStateException("Benchmark is already being executed");
            }
            _currentLoadState = EBenchmarkLoadStage.EMPTY;
        }
        finally {
            _benchmarkRunnerLock.unlock();
        }
        return true;
    }

    /**
     * Execute workload RUN.
     * Should always LOAD in data before running the benchmark for the first time
     * @param workload Selected workload. Should be the same one each load at least until the test data is reloaded
     *                 into the activate database.
     * @return Relevant statistics gathered during the benchmark run
     */
    public String executeBenchmark(String workload, Integer workers) {
        logger.trace("Execute benchmark was called with the workload ID: '{}'", ofNullable(workload).orElse(""));
        var knownWorkload = EYCSBWorkloadCatalog.getWorkloadByTag(workload);
        return executeBenchmark(knownWorkload, workers);
    }
    /**
     * Execute workload RUN.
     * Should always LOAD in data before running the benchmark for the first time
     * @param workload Selected workload. Should be the same one each load at least until the test data is reloaded
     *                 into the activate database.
     * @return ID of the new Run
     */
    public String executeBenchmark(EYCSBWorkloadCatalog workload, Integer workers){
        _benchmarkRunnerLock.lock();
        try {
            if (_currentLoadState != EBenchmarkLoadStage.READY) {
                var err = String.format("Benchmark data load is in incorrect state: %s", _currentLoadState.toString());
                logger.trace(err);
                throw new IllegalStateException(err);
            }
            if (isBenchmarkInAMiddleOfARun()) {
                throw new IllegalStateException("Benchmark is already being executed");
            }
            // Reset the state
            _runnerState.setState(EBenchmarkRunStage.STARTING);
            _runnerState.setCompeted(false);
            _runnerState.setResult(null);
        }
        finally {
            _benchmarkRunnerLock.unlock();
        }
        _runnerState.setRunId(UUID.randomUUID().toString());
        var benchRunnable = new BenchmarkThread(_runnerState, _ycsbRunner, workload, workers);
        Thread bench_runner_t = new Thread(benchRunnable);
        bench_runner_t.start();
        return _runnerState.getRunId();
    }

    /**
     * Retrieve the status of the benchmark run.
     * @return Info about the stage of the running benchmark or the latest generated benchmark result values.
     */
    public BenchmarkStatus getBenchmarkStatus(){
        var result = this._runnerState.getResult();
        var failed = this._runnerState.isFailed();
        var stage = this._runnerState.getState().toString();
        var runId = this._runnerState.getRunId();
        var completed = this._runnerState.isCompeted; // read this flag last

        if(completed){
            _lastValidRunId = runId;
            _lastValidResult = result;
        }

        return new BenchmarkStatus(completed, failed, stage, result, runId);
    }

    /**
     * Retrieves node where the requested tuple is located and records the retrieval statistic
     * @param tupleParams Tuple location request argument
     * @return Tuple location result container
     */
    public TupleLocation getTupleLocation(LocateTuple tupleParams){
        verifyState();
        var result = _tmpTupleLocator.getNodeForRead(tupleParams.getTupleId());
        return new TupleLocation(result);
    }

    /**
     * Notify the read operation on a specific node was finished (for capacity counters).
     * @param nodeId Node ID where the operation was performed.
     * @throws KeyNotFoundException Thrown if the specified Node ID is not found.
     */
    public void notifyNodeAccessFinished(String nodeId) throws KeyNotFoundException {
        verifyState();
        _tmpTupleLocator.notifyReadFinished(nodeId);
    }

    /**
     * Initialize the naive benchmark estimator based on the latest YCSB run statistics.
     * @return Success flag.
     */
    public boolean initCounterfeitBenchmark(){
        // Fill out Counterfeit benchmark
        if(this._lastValidResult != null){
            this._counterfeitRunner.setCurrentSettings(
                    new CounterfeitBenchSettings(this._lastValidResult, this._lastValidRunId));
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Get estimation of the benchmark instead of running it over the real data distribution.
     * @return Estimated benchmark result based on the previous real benchmark run.
     */
    public BenchmarkStatus getCounterfeitStatus(double adjustRangeStart, double adjustRangeEnd){
        BenchmarkRunResult result = null;
        boolean failed = false;
        String stage = EBenchmarkRunStage.DONE.toString();
        String runId = null;
        var completed = this._counterfeitRunner.isReady();
        if(completed){
            runId = this._counterfeitRunner.getCurrentSettings().getRunId();
            result = this._counterfeitRunner.estimateBenchmarkRun(adjustRangeStart, adjustRangeEnd);
        }
        else{
            stage = EBenchmarkRunStage.READY.toString();
            failed = true;
        }

        return new BenchmarkStatus(completed, failed, stage, result, runId);
    }

    /**
     * Stages of running the benchmark
     */
    private enum EBenchmarkLoadStage {
        /**
         * No data was generated.
         */
        EMPTY("Empty"),
        /**
         * Data is generating.
         */
        LOADING("Loading"),
        /**
         * Data generation is finished.
         */
        READY("Ready");

        /**
         * Textual value of the current data generation execution stage.
         */
        private String _stage;

        /**
         * Constructor.
         * @param stage Textual value of the current data generation stage.
         */
        EBenchmarkLoadStage(String stage){
            _stage = stage;
        }

        @Override
        public String toString() {
            return _stage;
        }
    }

    /**
     * State holder for the current benchmark execution
     */
    public static final class RunnerState{
        /**
         * When benchmark execution is invoked a new ID is generated
         */
        private String _runId;
        /**
         * After every run, the latest result object stored in this variable
         */
        private BenchmarkRunResult _result;
        /**
         * Current state of the runner thread
         */
        private EBenchmarkRunStage _state = EBenchmarkRunStage.READY;
        /**
         * Benchmark run has failed. Cause will be in the _result
         */
        private boolean isFailed = false;
        /**
         * Benchmark run has completed it's run. Meaning the execution finished after the last time the thread was
         * called. icCompleted = true doesn't mean there was no errors during the execution.
         */
        private boolean isCompeted = false;

        /**
         * Get a unique ID for the current benchmark run
         * @return Benchmark run unique ID string
         */
        public String getRunId() {
            return _runId;
        }
        /**
         * Set a unique ID for the current benchmark run
         * @param runId Benchmark run unique ID string
         */
        public void setRunId(String runId) {
            this._runId = runId;
        }

        /**
         * Ger the result of the benchmark run
         * @return Result of the current benchmark run or null if the benchmark is still running
         */
        public BenchmarkRunResult getResult() {
            return _result;
        }
        /**
         * Set the result of the benchmark
         * @param result Result of the current benchmark run or null if the benchmark is still running
         */
        public void setResult(BenchmarkRunResult result) {
            this._result = result;
        }

        /**
         * Get the current state of the benchmark run
         * @return EBenchmarkRunStage
         */
        public EBenchmarkRunStage getState() {
            return _state;
        }
        /**
         * Set the current state of the benchmark
         * @param state EBenchmarkRunStage
         */
        public void setState(EBenchmarkRunStage state) {
            this._state = state;
        }

        /**
         * Failed state.
         * @return True - the benchmark run has failed.
         */
        public boolean isFailed() {
            return isFailed;
        }

        /**
         * Set failed state.
         * @param failed True - the benchmark run has failed.
         */
        public void setFailed(boolean failed) {
            isFailed = failed;
        }

        /**
         * Completion flag
         * @return True the latest run was finished.
         */
        public boolean isCompeted() {
            return isCompeted;
        }

        /**
         * Set the completion flag value of benchmark.
         * @param competed True if the benchmark was executed till completion or reached a failed state.
         */
        public void setCompeted(boolean competed) {
            isCompeted = competed;
        }
    }

    /**
     * Benchmark runner thread that starts the Benchmark runner and polls it till completion or timeout.
     */
    private static final class BenchmarkThread implements Runnable{
        /**
         * Logger
         */
        private final Logger logger = LogManager.getLogger(BenchmarkThread.class);

        /**
         * State of the current benchmark run.
         */
        final RunnerState _state;
        /**
         * Instance of YCSB managing class.
         */
        final YCSBRunner _ycsbRunner;
        /**
         * Selected YCSB workload.
         */
        final EYCSBWorkloadCatalog _workload;
        /**
         * Number of YCSB workers.
         */
        final Integer _workers;

        /**
         * Constructor
         * @param stateObj Object containing state of the benchmark that can be read by the command handler.
         * @param runner Configured instance of the YCSB runner.
         * @param workload Workload configuration.
         * @param workers Number of workers executing the run workload. If null, the default value of the YCSB config is
         *                used instead.
         */
        private BenchmarkThread(RunnerState stateObj,
                                YCSBRunner runner,
                                EYCSBWorkloadCatalog workload,
                                Integer workers) {
            _state = stateObj;
            _ycsbRunner = runner;
            _workload = workload;
            _workers = workers;
        }

        @Override
        public void run() {
            try {
                this._state.setCompeted(false);
                this._state.setState(EBenchmarkRunStage.RUNNING);
                var ycsbRunOutput = this._ycsbRunner.runWorkload(this._workload, _workers);
                this._state.setState(EBenchmarkRunStage.FINALIZING);
                var result = new BenchmarkRunResult();
                result.setError(null);

                // Await for node statistics
                int awaitCycles = 1000; // Timout limit
                if(this._ycsbRunner.statsResultsReady() == false){
                    while(awaitCycles > 0){
                        awaitCycles--;
                        TimeUnit.SECONDS.sleep(3);
                        if(this._ycsbRunner.statsResultsReady()){
                            break;
                        }
                    }
                }
                else if(this._ycsbRunner.statsResultsReady() != null){
                    // Managed to get statistics
                    result.setNodes(this._ycsbRunner.getStats());
                }
                else{
                    // Timed out
                    logger.warn("Benchmark timed out.");
                    result.setNodes(null);
                }

                result.setThroughput(ycsbRunOutput.getThroughput());
                _state.setResult(result);
            }
            catch (Exception ex){
                this._state.setFailed(true);
                _state.setResult(new BenchmarkRunResult() {{setError(ex.getMessage());}});
            }
            finally {
                this._state.setCompeted(true);
                this._state.setState(EBenchmarkRunStage.DONE);
            }
        }
    }
}
