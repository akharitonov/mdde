package dev.jcri.mdde.registry.shared.commands;

/**
 * Constants used by the command container (JSON, XML, CLI argument, etc) field parsers in the registry.
 */
public final class Constants {
    /**
     *  Command tag
     */
    public static final String CommandFiled = "cmd";
    /**
     * If the used protocol has arguments as a separate container (JSON), serialize them in here
     */
    public static final String ArgumentsField = "args";


    public static final String ArgTupleIdField = "tid";
    public static final String ArgTupleIdsField = "tids";
    public static final String ArgNodeIdField = "nid";
    public static final String ArgNodeIdsField = "nids";
    public static final String ArgSecondNodeIdFiled = "nidb";
    public static final String ArgFragmentIdField = "fid";

    public static final String ArgWorkloadIdField = "workload";
    public static final String ArgWorkloadWorkersField = "workers";

    public static final String ArgFragmentMetaTag = "fmtag";
    public static final String ArgFragmentLocalMetaTags = "fmtagsloc";
    public static final String ArgFragmentGlobalMetaTags = "fmtagsglb";
    public static final String ArgFragmentMetaValue = "fmval";

    public static final String ArgSnapshotIdField = "snapid";
    public static final String ArgSnapshotDefaultField = "snapisdef";

    public static final String ResultPayload = "result";
    public static final String ResultError = "error";
    public static final String ResultErrorCode = "errcode";

    public static final String ArgBenchmarkCounterfeitMagnitudeStart = "bcmagst";
    public static final String ArgBenchmarkCounterfeitMagnitudeEnd = "bcmagen";
}
