package it.unibas.lunatic;

import speedy.SpeedyConstants;

public class LunaticConfiguration {

//    private static boolean printSteps = true;
    private static boolean printSteps = false;
    private static boolean printResults = false;
    private boolean printTargetStats = false;
    private boolean debugMode = false;
    private boolean recreateDBOnStart = true;
    private boolean cleanSchemasOnStartForDEScenarios = true;
    private Integer iterationLimit = null;
    private boolean useLimit1ForEGDs = false;
    private boolean preventInsertDuplicateTuples = false;
    private String chaseMode;
    private boolean deScenario = false; //MCProxy for DE chase
    private boolean optimizeSTTGDs = true;
    private boolean forceOptimizeSTTGDs = false;
    private boolean rewriteSTTGDOverlaps = true;
    private boolean checkGroundSolutions = false;
    private boolean checkSolutions = false;
    private boolean checkSolutionsQuery = false;
    private boolean checkConsistencyOfDBs = false;
    private boolean removeDuplicates = true;
    private boolean checkAllNodesForEGDSatisfaction = false;
    private boolean forceMCChaserInTests = false;
    private boolean useSymmetricOptimization = true;
    private boolean checkDCDuringChase = false;
    private boolean chaseDEDGreedyExecuteAllScenarios = false;
    private boolean chaseDEDGreedyRandomScenarios = false;
    private boolean discardDuplicateTuples = false;
    private double numberCellsWeightForRanking = 0.7;
    private boolean useUnloggedWorkTables = true;
    private boolean useHashForSkolem = true;
    private boolean useDictionaryEncoding = false;
    private boolean useBatchInsert = true;
    private boolean useCompactAttributeName = false;
    private boolean exportSolutions = false;
    private String exportSolutionsPath;
    private String exportSolutionsType = SpeedyConstants.CSV;
    private boolean exportSolutionsWithHeader = false;
    private boolean exportQueryResults = false;
    private String exportQueryResultsPath;
    private String exportQueryResultsType = SpeedyConstants.CSV;
    private boolean exportQueryResultsWithHeader = false;
    private boolean exportChanges = false;
    private String exportChangesStrategy = SpeedyConstants.MULTIPLE_FILES;
    private String exportChangesPath;
    private boolean printStatsOnly = false;

    private boolean chaseRestricted = false;
    private String nullGenerationStrategy;

    private boolean autoSelectBestNumberOfThreads = true;
    private int maxNumberOfThreads = 1;
    private boolean useThreadsForQueries = false;

    private boolean stopOnNotWA = true;

//    private String deChaser = LunaticConstants.CLASSIC_DE_CHASER;
//    private String deChaser = LunaticConstants.PROXY_MC_CHASER;
    private String deChaser = LunaticConstants.DE_OPTIMIZED_CHASER;
//
    private String cacheTypeForMC = LunaticConstants.GREEDY_SINGLESTEP_JCS_CACHE;
//    private String cacheTypeForMC = LunaticConstants.GREEDY_MULTI_STEP_JCS;

    private String cacheTypeForDE = LunaticConstants.GREEDY_DE_JCS;
//    private String cacheTypeForDE = LunaticConstants.GREEDY_SINGLESTEP_JCS_CACHE;

    public LunaticConfiguration() {
        this(LunaticConstants.CHASE_PARALLEL_RESTRICTED_SKOLEM);
//        this(LunaticConstants.CHASE_UNRESTRICTED_SKOLEM); //Requires Postgres >= 9.5
//        this(LunaticConstants.CHASE_RESTRICTED_FRESH_NULL);
//        this(LunaticConstants.CHASE_RESTRICTED_SKOLEM);
    }

    public LunaticConfiguration(String chaseMode) {
        setChaseMode(chaseMode);
    }

    private void setChaseMode(String chaseMode) {
        if (chaseMode == null) {
            chaseMode = LunaticConstants.CHASE_PARALLEL_RESTRICTED_SKOLEM;
        }
        if (chaseMode.equals(LunaticConstants.CHASE_PARALLEL_RESTRICTED_SKOLEM)) {
            this.chaseRestricted = true;
            this.nullGenerationStrategy = LunaticConstants.SKOLEM_STRATEGY;
            this.preventInsertDuplicateTuples = false;
            this.optimizeSTTGDs = true;
        } else if (chaseMode.equals(LunaticConstants.CHASE_RESTRICTED_FRESH_NULL)) {
            this.chaseRestricted = true;
            this.nullGenerationStrategy = LunaticConstants.FRESH_NULL_STRATEGY;
            this.preventInsertDuplicateTuples = false;
            this.optimizeSTTGDs = false;
        } else if (chaseMode.equals(LunaticConstants.CHASE_RESTRICTED_SKOLEM)) {
            this.chaseRestricted = true;
            this.nullGenerationStrategy = LunaticConstants.SKOLEM_STRATEGY;
            this.preventInsertDuplicateTuples = false;
            this.optimizeSTTGDs = false;
        } else if (chaseMode.equals(LunaticConstants.CHASE_UNRESTRICTED_SKOLEM)) {
            this.chaseRestricted = false;
            this.nullGenerationStrategy = LunaticConstants.SKOLEM_STRATEGY;
            this.preventInsertDuplicateTuples = true;
            this.optimizeSTTGDs = false;
        } else {
            throw new IllegalArgumentException("Unknow chase mode " + chaseMode);
        }
        this.chaseMode = chaseMode;
    }

    public String getChaseMode() {
        return chaseMode;
    }

    public boolean isChaseRestricted() {
        return chaseRestricted;
    }

    public String getNullGenerationStrategy() {
        return nullGenerationStrategy;
    }

    public static boolean isPrintSteps() {
        return printSteps;
    }

    public static void setPrintSteps(boolean aPrintSteps) {
        printSteps = aPrintSteps;
    }

    public static boolean isPrintResults() {
        return printResults;
    }

    public static void setPrintResults(boolean aPrintResults) {
        printResults = aPrintResults;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isRecreateDBOnStart() {
        return recreateDBOnStart;
    }

    public void setRecreateDBOnStart(boolean recreateDBOnStart) {
        this.recreateDBOnStart = recreateDBOnStart;
    }

    public boolean isRemoveDuplicates() {
        return removeDuplicates;
    }

    public void setRemoveDuplicates(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
    }

    public Integer getIterationLimit() {
        return iterationLimit;
    }

    public void setIterationLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }

    public boolean isUseLimit1ForEGDs() {
        return useLimit1ForEGDs;
    }

    public void setUseLimit1ForEGDs(boolean useLimit1ForEGDs) {
        this.useLimit1ForEGDs = useLimit1ForEGDs;
    }

    public boolean isDeScenario() {
        return deScenario;
    }

    public void setDeScenario(boolean deScenario) {
        this.deScenario = deScenario;
    }

    public boolean isCheckGroundSolutions() {
        return checkGroundSolutions;
    }

    public void setCheckGroundSolutions(boolean checkGroundSolutions) {
        this.checkGroundSolutions = checkGroundSolutions;
    }

    public boolean isCheckAllNodesForEGDSatisfaction() {
        return checkAllNodesForEGDSatisfaction;
    }

    public void setCheckAllNodesForEGDSatisfaction(boolean checkAllNodesForEGDSatisfaction) {
        this.checkAllNodesForEGDSatisfaction = checkAllNodesForEGDSatisfaction;
    }

    public boolean isCheckSolutionsQuery() {
        return checkSolutionsQuery;
    }

    public void setCheckSolutionsQuery(boolean checkSolutionsQuery) {
        this.checkSolutionsQuery = checkSolutionsQuery;
    }

    public boolean isCheckSolutions() {
        return checkSolutions;
    }

    public void setCheckSolutions(boolean checkSolutions) {
        this.checkSolutions = checkSolutions;
    }

    public String getCacheTypeForMC() {
        return cacheTypeForMC;
    }

    public void setCacheTypeForMC(String cacheTypeForMC) {
        this.cacheTypeForMC = cacheTypeForMC;
    }

    public String getCacheTypeForDE() {
        return cacheTypeForDE;
    }

    public void setCacheTypeForDE(String cacheTypeForDE) {
        this.cacheTypeForDE = cacheTypeForDE;
    }

    public boolean isForceMCChaserInTests() {
        return forceMCChaserInTests;
    }

    public void setForceMCChaserInTests(boolean forceMCChaser) {
        this.forceMCChaserInTests = forceMCChaser;
    }

    public void changeParametersForScalabilityTests() {
        this.checkGroundSolutions = false;
        this.removeDuplicates = false;
    }

    public String getDeChaser() {
        return deChaser;
    }

    public void setDeChaser(String deChaser) {
        this.deChaser = deChaser;
    }

    public boolean isUseSymmetricOptimization() {
        return useSymmetricOptimization;
    }

    public void setUseSymmetricOptimization(boolean useSymmetricOptimization) {
        this.useSymmetricOptimization = useSymmetricOptimization;
    }

    public boolean isChaseDEDGreedyExecuteAllScenarios() {
        return chaseDEDGreedyExecuteAllScenarios;
    }

    public void setChaseDEDGreedyExecuteAllScenarios(boolean chaseDEDGreedyExecuteAllScenarios) {
        this.chaseDEDGreedyExecuteAllScenarios = chaseDEDGreedyExecuteAllScenarios;
    }

    public boolean isCheckDCDuringChase() {
        return checkDCDuringChase;
    }

    public void setCheckDCDuringChase(boolean checkDCDuringChase) {
        this.checkDCDuringChase = checkDCDuringChase;
    }

    public boolean isCheckConsistencyOfDBs() {
        return checkConsistencyOfDBs;
    }

    public void setCheckConsistencyOfDBs(boolean checkConsistencyOfDBs) {
        this.checkConsistencyOfDBs = checkConsistencyOfDBs;
    }

    public boolean isChaseDEDGreedyRandomScenarios() {
        return chaseDEDGreedyRandomScenarios;
    }

    public void setChaseDEDGreedyRandomScenarios(boolean chaseDEDGreedyRandomScenarios) {
        this.chaseDEDGreedyRandomScenarios = chaseDEDGreedyRandomScenarios;
    }

    public boolean isDiscardDuplicateTuples() {
        return discardDuplicateTuples;
    }

    public void setDiscardDuplicateTuples(boolean discardDuplicateTuples) {
        this.discardDuplicateTuples = discardDuplicateTuples;
    }

    public double getNumberCellsWeightForRanking() {
        return numberCellsWeightForRanking;
    }

    public void setNumberCellsWeightForRanking(double numberCellsWeightForRanking) {
        this.numberCellsWeightForRanking = numberCellsWeightForRanking;
    }

    public boolean isUseHashForSkolem() {
        return useHashForSkolem;
    }

    public void setUseHashForSkolem(boolean useHashForSkolem) {
        this.useHashForSkolem = useHashForSkolem;
    }

    public boolean isUseUnloggedWorkTables() {
        return useUnloggedWorkTables;
    }

    public void setUseUnloggedWorkTables(boolean useUnloggedWorkTables) {
        this.useUnloggedWorkTables = useUnloggedWorkTables;
    }

    public boolean isUseBatchInsert() {
        return useBatchInsert;
    }

    public void setUseBatchInsert(boolean useBatchInsert) {
        this.useBatchInsert = useBatchInsert;
    }

    public boolean isExportSolutions() {
        return exportSolutions;
    }

    public void setExportSolutions(boolean exportSolutions) {
        this.exportSolutions = exportSolutions;
    }

    public String getExportSolutionsPath() {
        return exportSolutionsPath;
    }

    public void setExportSolutionsPath(String exportSolutionsPath) {
        this.exportSolutionsPath = exportSolutionsPath;
    }

    public String getExportSolutionsType() {
        return exportSolutionsType;
    }

    public void setExportSolutionsType(String exportSolutionsType) {
        this.exportSolutionsType = exportSolutionsType;
    }

    public boolean isExportSolutionsWithHeader() {
        return exportSolutionsWithHeader;
    }

    public void setExportSolutionsWithHeader(boolean exportSolutionsWithHeader) {
        this.exportSolutionsWithHeader = exportSolutionsWithHeader;
    }

    public boolean isExportQueryResults() {
        return exportQueryResults;
    }

    public void setExportQueryResults(boolean exportQueryResults) {
        this.exportQueryResults = exportQueryResults;
    }

    public String getExportQueryResultsPath() {
        return exportQueryResultsPath;
    }

    public void setExportQueryResultsPath(String exportQueryResultsPath) {
        this.exportQueryResultsPath = exportQueryResultsPath;
    }

    public String getExportQueryResultsType() {
        return exportQueryResultsType;
    }

    public void setExportQueryResultsType(String exportQueryResultsType) {
        this.exportQueryResultsType = exportQueryResultsType;
    }

    public boolean isExportQueryResultsWithHeader() {
        return exportQueryResultsWithHeader;
    }

    public void setExportQueryResultsWithHeader(boolean exportQueryResultsWithHeader) {
        this.exportQueryResultsWithHeader = exportQueryResultsWithHeader;
    }

    public boolean isExportChanges() {
        return exportChanges;
    }

    public void setExportChanges(boolean exportChanges) {
        this.exportChanges = exportChanges;
    }

    public String getExportChangesPath() {
        return exportChangesPath;
    }

    public void setExportChangesPath(String exportChangesPath) {
        this.exportChangesPath = exportChangesPath;
    }

    public String getExportChangesStrategy() {
        return exportChangesStrategy;
    }

    public void setExportChangesStrategy(String exportChangesStrategy) {
        this.exportChangesStrategy = exportChangesStrategy;
    }

    public boolean isCleanSchemasOnStartForDEScenarios() {
        return cleanSchemasOnStartForDEScenarios;
    }

    public void setCleanSchemasOnStartForDEScenarios(boolean cleanSchemasOnStartForDEScenarios) {
        this.cleanSchemasOnStartForDEScenarios = cleanSchemasOnStartForDEScenarios;
    }

    public boolean isAutoSelectBestNumberOfThreads() {
        return autoSelectBestNumberOfThreads;
    }

    public void setAutoSelectBestNumberOfThreads(boolean autoSelectBestNumberOfThreads) {
        this.autoSelectBestNumberOfThreads = autoSelectBestNumberOfThreads;
    }

    public int getMaxNumberOfThreads() {
        return maxNumberOfThreads;
    }

    public void setMaxNumberOfThreads(int maxNumberOfThreads) {
        this.maxNumberOfThreads = maxNumberOfThreads;
    }

    public boolean isUseDictionaryEncoding() {
        return useDictionaryEncoding;
    }

    public void setUseDictionaryEncoding(boolean useDictionaryEncoding) {
        this.useDictionaryEncoding = useDictionaryEncoding;
    }

    public boolean isOptimizeSTTGDs() {
        return optimizeSTTGDs;
    }

    public void setOptimizeSTTGDs(boolean optimizeSTTGDs) {
        this.optimizeSTTGDs = optimizeSTTGDs;
    }

    public boolean isForceOptimizeSTTGDs() {
        return forceOptimizeSTTGDs;
    }

    public void setForceOptimizeSTTGDs(boolean forceOptimizeSTTGDs) {
        this.forceOptimizeSTTGDs = forceOptimizeSTTGDs;
    }

    public boolean isRewriteSTTGDOverlaps() {
        return rewriteSTTGDOverlaps;
    }

    public void setRewriteSTTGDOverlaps(boolean rewriteSTTGDOverlaps) {
        this.rewriteSTTGDOverlaps = rewriteSTTGDOverlaps;
    }

    public boolean isPrintStatsOnly() {
        return printStatsOnly;
    }

    public void setPrintStatsOnly(boolean printStatsOnly) {
        this.printStatsOnly = printStatsOnly;
    }

    public boolean isUseCompactAttributeName() {
        return useCompactAttributeName;
    }

    public void setUseCompactAttributeName(boolean useCompactAttributeName) {
        this.useCompactAttributeName = useCompactAttributeName;
    }

    public boolean isUseThreadsForQueries() {
        return useThreadsForQueries;
    }

    public void setUseThreadsForQueries(boolean useThreadsForQueries) {
        this.useThreadsForQueries = useThreadsForQueries;
    }

    public boolean isPrintTargetStats() {
        return printTargetStats;
    }

    public void setPrintTargetStats(boolean printTargetStats) {
        this.printTargetStats = printTargetStats;
    }

    public boolean isPreventInsertDuplicateTuples() {
        return preventInsertDuplicateTuples;
    }

    public void setPreventInsertDuplicateTuples(boolean preventInsertDuplicateTuples) {
        this.preventInsertDuplicateTuples = preventInsertDuplicateTuples;
    }

    public boolean isStopOnNotWA() {
        return stopOnNotWA;
    }

    public void setStopOnNotWA(boolean stopOnNotWA) {
        this.stopOnNotWA = stopOnNotWA;
    }

    @Override
    public String toString() {
        return "\tDebugMode: " + debugMode
                + "\n\t PrintSteps: " + printSteps
                + "\n\t IterationLimit: " + iterationLimit
                + "\n\t RecreateDBOnStart: " + recreateDBOnStart
                + "\n\t UseLimit1: " + useLimit1ForEGDs
                + "\n\t DEProxyMode: " + deScenario
                + "\n\t RewriteTGDs: " + optimizeSTTGDs
                + "\n\t RewriteTGDOverlaps: " + rewriteSTTGDOverlaps
                + "\n\t CheckSolutions: " + checkSolutions
                + "\n\t CheckSolutionsQuery: " + checkSolutionsQuery
                + "\n\t CheckGroundSolutions: " + checkGroundSolutions
                + "\n\t CheckConsistencyOfDBs: " + checkConsistencyOfDBs
                + "\n\t RemoveDuplicates: " + removeDuplicates
                + "\n\t CheckAllNodesForEGDSatisfaction: " + checkAllNodesForEGDSatisfaction
                + "\n\t UseSymmetricOptimization: " + useSymmetricOptimization
                + "\n\t NumberCellsWeightForRanking: " + numberCellsWeightForRanking
                + "\n\t DiscardDuplicateTuples: " + discardDuplicateTuples
                + "\n\t AutoSelectBestNumberOfThreads: " + autoSelectBestNumberOfThreads
                + "\n\t MaxNumberOfThreads: " + maxNumberOfThreads
                + "\n\t UseThreadsForQueries: " + useThreadsForQueries
                + "\n\t UseBatchInsert: " + useBatchInsert
                + "\n\t UseUnloggedWorkTables: " + useUnloggedWorkTables
                + "\n\t UseHashForSkolem: " + useHashForSkolem
                + "\n\t UseDictionaryEncoding: " + useDictionaryEncoding
                + "\n\t ExportSolutions: " + exportSolutions
                + "\n\t ExportSolutionsPath: " + exportSolutionsPath
                + "\n\t ExportSolutionsType: " + exportSolutionsType
                + "\n\t ExportChanges: " + exportChanges
                + "\n\t ExportChangesPath: " + exportChangesPath
                + "\n\t StopOnNotWA: " + stopOnNotWA
                + "\n\t UseCompactAttributeName: " + useCompactAttributeName
                + "\n\t Cache type for MC: " + cacheTypeForMC
                + "\n\t Cache type for DE: " + cacheTypeForDE
                + "\n\t DeChaser: " + deChaser
                + "\n\t PrintStatsOnly: " + printStatsOnly;
    }
}
