package it.unibas.lunatic.model.chase.chasemc.operators;

import it.unibas.lunatic.LunaticConfiguration;
import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.ChaseException;
import it.unibas.lunatic.model.algebra.IAlgebraOperator;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import it.unibas.lunatic.model.chase.commons.ChaseUtility;
import it.unibas.lunatic.model.chase.commons.control.IChaseState;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.dependency.Dependency;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaseDeltaExtTGDs implements IChaseDeltaExtTGDs {

    public static final int ITERATION_LIMIT = 10;
    private static Logger logger = LoggerFactory.getLogger(ChaseDeltaExtTGDs.class);

    private ChaseTGDEquivalenceClass dependencyChaser;
    private IBuildDatabaseForChaseStep databaseBuilder;
    private IOIDGenerator oidGenerator;

    public ChaseDeltaExtTGDs(IRunQuery queryRunner, IBuildDatabaseForChaseStep databaseBuilder,
            OccurrenceHandlerMC occurrenceHandler, IOIDGenerator oidGenerator, ChangeCell cellChanger) {
        this.databaseBuilder = databaseBuilder;
        this.oidGenerator = oidGenerator;
        this.dependencyChaser = new ChaseTGDEquivalenceClass(queryRunner, oidGenerator, occurrenceHandler, cellChanger);
    }

    @Override
    public boolean doChase(DeltaChaseStep treeRoot, Scenario scenario, IChaseState chaseState, Map<Dependency, IAlgebraOperator> tgdTreeMap, Map<Dependency, IAlgebraOperator> tgdQuerySatisfactionMap) {
        if (scenario.getExtTGDs().isEmpty()) {
            return false;
        }
        long start = new Date().getTime();
        int size = treeRoot.getNumberOfNodes();
        chaseTree(treeRoot, scenario, chaseState, tgdTreeMap, tgdQuerySatisfactionMap);
        int newSize = treeRoot.getNumberOfNodes();
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.TGD_TIME, end - start);
        return (size != newSize);
    }

    private void chaseTree(DeltaChaseStep treeRoot, Scenario scenario, IChaseState chaseState, Map<Dependency, IAlgebraOperator> tgdTreeMap, Map<Dependency, IAlgebraOperator> tgdQuerySatisfactionMap) {
        if (treeRoot.isInvalid()) {
            return;
        }
        if (treeRoot.isLeaf()) {
            chaseNode((DeltaChaseStep) treeRoot, scenario, chaseState, tgdTreeMap, tgdQuerySatisfactionMap);
        } else {
            for (DeltaChaseStep child : treeRoot.getChildren()) {
                chaseTree(child, scenario, chaseState, tgdTreeMap, tgdQuerySatisfactionMap);
            }
        }
    }

    private void chaseNode(DeltaChaseStep node, Scenario scenario, IChaseState chaseState, Map<Dependency, IAlgebraOperator> tgdTreeMap, Map<Dependency, IAlgebraOperator> tgdQuerySatisfactionMap) {
        if (node.isDuplicate() || node.isInvalid()) {
            return;
        }
        if (node.isEditedByUser()) {
            DeltaChaseStep newStep = new DeltaChaseStep(scenario, node, LunaticConstants.CHASE_USER, LunaticConstants.CHASE_USER);
            node.addChild(newStep);
            chaseNode(newStep, scenario, chaseState, tgdTreeMap, tgdQuerySatisfactionMap);
            return;
        }
        if (LunaticConfiguration.sout) System.out.println("******Chasing node for tgds: " + node.getId());
        if (logger.isDebugEnabled()) logger.debug("Chasing ext tgds:\n" + LunaticUtility.printCollection(scenario.getExtTGDs()) + "\non tree: " + node);
        int iterations = 0;
        while (true) {
            if (logger.isDebugEnabled()) logger.debug("======= Starting tgd chase cycle on step " + node.getId());
            boolean newNode = false;
            for (Dependency eTgd : scenario.getExtTGDs()) {
                if (chaseState.isCancelled()) {
                    ChaseUtility.stopChase(chaseState);
                }
                long startTgd = new Date().getTime();
                String localId = ChaseUtility.generateChaseStepIdForTGDs(eTgd);
                DeltaChaseStep newStep = new DeltaChaseStep(scenario, node, localId, LunaticConstants.CHASE_STEP_TGD);
                if (logger.isDebugEnabled()) logger.debug("---- Candidate new step: " + newStep.getId() + "- Chasing tgd: " + eTgd);
                IAlgebraOperator tgdQuery = tgdTreeMap.get(eTgd);
                if (logger.isTraceEnabled()) logger.debug("----TGD Query: " + tgdQuery);
//                IDatabase databaseForStep = databaseBuilder.extractDatabase(newStep.getId(), newStep.getDeltaDB(), newStep.getOriginalDB(), eTgd);
                IDatabase databaseForStep = databaseBuilder.extractDatabase(newStep.getId(), newStep.getDeltaDB(), newStep.getOriginalDB()); //TODO++ Optimize checking test deds workers
                if (logger.isTraceEnabled()) logger.trace("Database for step: " + databaseBuilder.extractDatabase(newStep.getId(), newStep.getDeltaDB(), newStep.getOriginalDB()).printInstances() + "\nDeltaDB: " + newStep.getDeltaDB().printInstances());
                long start = new Date().getTime();
                boolean insertedTuples = dependencyChaser.chaseDependency(newStep, eTgd, tgdQuery, scenario, chaseState, databaseForStep);
                long end = new Date().getTime();
                if (LunaticConfiguration.sout) System.out.println("Dependency chasing Execution time: " + (end - start) + " ms");
                ChaseStats.getInstance().addDepenendecyStat(eTgd, end - start);
                if (insertedTuples) {
                    if (logger.isDebugEnabled()) logger.debug("Tuples have been inserted, adding new step to tree...");
                    node.addChild(newStep);
                    node = newStep;
                    newNode = true;
                }
                long endTgd = new Date().getTime();
                ChaseStats.getInstance().addDepenendecyStat(eTgd, endTgd - startTgd);
            }
            if (!newNode) {
                if (logger.isDebugEnabled()) logger.debug("***** No new nodes, exit tgd chase...");
                return;
            } else {
                if (logger.isDebugEnabled()) logger.debug("***** There are new nodes, cycling the chase...");
                iterations++;
                if (iterations > ITERATION_LIMIT) {
                    throw new ChaseException("Reached iteration limit " + ITERATION_LIMIT + " with no solution...");
                }
            }
        }
    }

    @Override
    public void initializeOIDs(IDatabase targetDB) {
        this.oidGenerator.initializeOIDs(targetDB);
    }
}
