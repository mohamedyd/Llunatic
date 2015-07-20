package it.unibas.lunatic.model.chase.chasemc.costmanager;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.ChaseException;
import it.unibas.lunatic.model.chase.chasemc.BackwardAttribute;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.ChangeSet;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.chasemc.EquivalenceClass;
import it.unibas.lunatic.model.chase.chasemc.Repair;
import it.unibas.lunatic.model.chase.chasemc.TargetCellsToChange;
import it.unibas.lunatic.model.chase.chasemc.operators.CellGroupIDGenerator;
import it.unibas.lunatic.model.chase.chasemc.operators.OccurrenceHandlerMC;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.database.LLUNValue;
import it.unibas.lunatic.model.database.NullValue;
import it.unibas.lunatic.utility.LunaticUtility;
import it.unibas.lunatic.utility.combinatorial.GenericPowersetGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardCostManager extends AbstractCostManager {

    private static Logger logger = LoggerFactory.getLogger(StandardCostManager.class);

    @SuppressWarnings("unchecked")
    public List<Repair> chooseRepairStrategy(EquivalenceClass equivalenceClass, DeltaChaseStep chaseTreeRoot,
            List<Repair> repairsForDependency, Scenario scenario, String stepId,
            OccurrenceHandlerMC occurrenceHandler) {
        if (logger.isDebugEnabled()) logger.debug("########Current node: " + chaseTreeRoot.toStringWithSort());
        if (logger.isDebugEnabled()) logger.debug("########Choosing repair strategy for equivalence class: " + equivalenceClass);
        List<TargetCellsToChange> tupleGroupsWithSameConclusionValue = equivalenceClass.getTupleGroups();
        if (isNotViolation(tupleGroupsWithSameConclusionValue, scenario)) {
            return Collections.EMPTY_LIST;
        }
        List<Repair> result = new ArrayList<Repair>();
        // generate forward repair for all groups
        ChangeSet changesForForwardRepair = generateForwardRepair(equivalenceClass.getTupleGroups(), scenario, chaseTreeRoot.getDeltaDB(), stepId);
        Repair forwardRepair = new Repair();
        forwardRepair.addChanges(changesForForwardRepair);
        if (logger.isDebugEnabled()) logger.debug("########Forward repair: " + forwardRepair);
        result.add(forwardRepair);
        if (isDoBackward()) {
            // check if repairs with backward chasing are possible
            int chaseBranching = chaseTreeRoot.getNumberOfLeaves();
            int potentialSolutions = chaseTreeRoot.getPotentialSolutions();
//            int repairsForDependenciesSize = repairsForDependency.size();
//            int databaseSize = scenario.getTarget().getSize();
//            if (isTreeSizeBelowThreshold(chaseTreeSize, potentialSolutions, repairsForDependenciesSize)) {
            if (isTreeSizeBelowThreshold(chaseBranching, potentialSolutions)) {
                List<Repair> backwardRepairs = generateBackwardRepairs(equivalenceClass.getTupleGroups(), scenario, chaseTreeRoot.getDeltaDB(), stepId, equivalenceClass);
                for (Repair repair : backwardRepairs) {
                    LunaticUtility.addIfNotContained(result, repair);
                }
//                result.addAll(backwardRepairs);
                if (logger.isDebugEnabled()) logger.debug("########Backward repairs: " + backwardRepairs);
            }
        }
        return result;
    }

    private List<Repair> generateBackwardRepairs(List<TargetCellsToChange> tupleGroups, Scenario scenario, IDatabase deltaDB, String stepId, EquivalenceClass equivalenceClass) {
        if (tupleGroups.size() > 5) {
            throw new ChaseException("Tuple group of excessive size, it is not possible to chase this scenario: " + tupleGroups);
        }
        List<Repair> result = new ArrayList<Repair>();
        if (logger.isDebugEnabled()) logger.debug("Generating backward repairs for groups:\n" + LunaticUtility.printCollection(tupleGroups));
        GenericPowersetGenerator<TargetCellsToChange> powersetGenerator = new GenericPowersetGenerator<TargetCellsToChange>();
        List<List<TargetCellsToChange>> powerset = powersetGenerator.generatePowerSet(tupleGroups);
        for (List<TargetCellsToChange> subset : powerset) {
            if (subset.isEmpty()) {
                continue;
            }
            if (logger.isDebugEnabled()) logger.debug("Generating backward repairs for subset:\n" + LunaticUtility.printCollection(subset));
            for (BackwardAttribute backwardAttribute : equivalenceClass.getAttributesToChangeForBackwardChasing()) {
                if (!allGroupsCanBeBackwardChasedForAttribute(subset, backwardAttribute)) {
                    break;
                }
                List<TargetCellsToChange> forwardGroups = new ArrayList<TargetCellsToChange>(tupleGroups);
                List<TargetCellsToChange> backwardGroups = new ArrayList<TargetCellsToChange>();
                for (TargetCellsToChange tupleGroup : subset) {
                    CellGroup cellGroup = tupleGroup.getCellGroupsForBackwardRepairs().get(backwardAttribute);
                    if (backwardIsAllowed(cellGroup)) {
                        backwardGroups.add(tupleGroup);
                        forwardGroups.remove(tupleGroup);
                    }
                }
                if (backwardGroups.isEmpty() || forwardGroups.isEmpty()) {
                    continue;
                }
                if (logger.isDebugEnabled()) logger.debug("Generating repair for: \nForward groups: " + forwardGroups + "\nBackward groups: " + backwardGroups);
                Repair repair = generateRepairWithBackwards(equivalenceClass, forwardGroups, backwardGroups, backwardAttribute, scenario, deltaDB, stepId);
                result.add(repair);
            }
        }
        return result;
    }

    protected boolean allGroupsCanBeBackwardChasedForAttribute(List<TargetCellsToChange> subset, BackwardAttribute backwardAttribute) {
        for (TargetCellsToChange tupleGroup : subset) {
            if (tupleGroup.getCellGroupsForBackwardRepairs().get(backwardAttribute) == null) {
                return false;
            }
        }
        return true;
    }

    protected Repair generateRepairWithBackwards(EquivalenceClass equivalenceClass, List<TargetCellsToChange> forwardTupleGroups, List<TargetCellsToChange> backwardTupleGroups, BackwardAttribute backwardAttribute,
            Scenario scenario, IDatabase deltaDB, String stepId) {
        Repair repair = new Repair();
        if (forwardTupleGroups.size() > 1) {
            ChangeSet forwardChanges = generateForwardRepair(forwardTupleGroups, scenario, deltaDB, stepId);
            repair.addChanges(forwardChanges);
        }
        for (TargetCellsToChange backwardTupleGroup : backwardTupleGroups) {
            CellGroup backwardCellGroup = backwardTupleGroup.getCellGroupsForBackwardRepairs().get(backwardAttribute).clone();
            LLUNValue llunValue = CellGroupIDGenerator.getNextLLUNID();
            backwardCellGroup.setValue(llunValue);
            backwardCellGroup.setInvalidCell(CellGroupIDGenerator.getNextInvalidCell());
            ChangeSet backwardChangesForGroup = new ChangeSet(backwardCellGroup, LunaticConstants.CHASE_BACKWARD, buildWitnessCellGroups(backwardTupleGroups));
            repair.addChanges(backwardChangesForGroup);
            if (scenario.getConfiguration().isRemoveSuspiciousSolutions() && isSuspicious(backwardCellGroup, backwardAttribute, equivalenceClass)) {
                backwardTupleGroup.setSuspicious(true);
                repair.setSuspicious(true);
            }
        }
        return repair;
    }

    @Override
    public String toString() {
        return "Standard";
    }
}
