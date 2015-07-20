package it.unibas.lunatic.model.chase.chasemc.partialorder;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.PartialOrderConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.ChaseException;
import it.unibas.lunatic.exceptions.PartialOrderException;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.CellGroupCell;
import it.unibas.lunatic.model.chase.chasemc.operators.CellGroupIDGenerator;
import it.unibas.lunatic.model.chase.chasemc.partialorder.valuecomparator.CellComparatorUsingAdditionalValue;
import it.unibas.lunatic.model.chase.chasemc.partialorder.valuecomparator.IValueComparator;
import it.unibas.lunatic.model.chase.commons.ChaseUtility;
import it.unibas.lunatic.model.database.AttributeRef;
import it.unibas.lunatic.model.database.IValue;
import it.unibas.lunatic.model.database.NullValue;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardPartialOrder implements IPartialOrder {

    private static Logger logger = LoggerFactory.getLogger(StandardPartialOrder.class);

    public boolean canHandleAttributes(List<AttributeRef> attributes) {
        return true;
    }

    public CellGroup findLUB(List<CellGroup> cellGroups, Scenario scenario) throws PartialOrderException {
        if (logger.isDebugEnabled()) logger.debug("Finding lub of cell groups\n" + LunaticUtility.printCollection(cellGroups));
        CellGroup lubCellGroup = new CellGroup(LunaticConstants.NULL_IVALUE, true);
        for (CellGroup cellGroup : cellGroups) {
            mergeCells(cellGroup, lubCellGroup);
        }
        if (logger.isDebugEnabled()) logger.debug("LubCellGroup before setting value: \n" + lubCellGroup.toStringWithAdditionalCells());
        setCellGroupValue(lubCellGroup, scenario);
        if (logger.isDebugEnabled()) logger.debug("LubCellGroup after setting value: \n" + lubCellGroup.toStringWithAdditionalCells());
        return lubCellGroup;
    }

    public void setCellGroupValue(CellGroup lubCellGroup, Scenario scenario) {
        IValue lubValue = findLubValue(lubCellGroup, scenario);
        if (logger.isDebugEnabled()) logger.debug("LubValue: " + lubValue);
        lubCellGroup.setValue(lubValue);
        correctCellGroupId(lubCellGroup);
    }

    public String toString() {
        return "Standard";
    }

    private void correctCellGroupId(CellGroup lubCellGroup) {
        IValue mostFrequentCellGroupId = findMostFrequentCellGroupId(lubCellGroup);
        if (mostFrequentCellGroupId != null) {
            if (logger.isDebugEnabled()) logger.debug("Using existingClusterId " + mostFrequentCellGroupId);
            lubCellGroup.setId(mostFrequentCellGroupId);
        }
        correctToSaveInCells(lubCellGroup.getOccurrences(), mostFrequentCellGroupId, lubCellGroup.getId());
        correctToSaveInCells(lubCellGroup.getJustifications(), mostFrequentCellGroupId, lubCellGroup.getId());
        correctToSaveInCells(lubCellGroup.getUserCells(), mostFrequentCellGroupId, lubCellGroup.getId());
        if (lubCellGroup.hasInvalidCell()) {
            correctToSaveInCell(lubCellGroup.getInvalidCell(), mostFrequentCellGroupId, lubCellGroup.getId());
        }
        for (AttributeRef attributeRef : lubCellGroup.getAdditionalCells().keySet()) {
            Set<CellGroupCell> additionalCells = lubCellGroup.getAdditionalCells().get(attributeRef);
            correctToSaveInCells(additionalCells, mostFrequentCellGroupId, lubCellGroup.getId());
        }
    }

    private void correctToSaveInCells(Set<CellGroupCell> cells, IValue mostFrequentCellGroupId, IValue lubCellGroupId) {
        for (CellGroupCell cell : cells) {
            correctToSaveInCell(cell, mostFrequentCellGroupId, lubCellGroupId);
        }
    }

    private void correctToSaveInCell(CellGroupCell cell, IValue mostFrequentCellGroupId, IValue lubCellGroupId) {
        if (mostFrequentCellGroupId != null) {
            if (cell.getOriginalCellGroupId() != null && cell.getOriginalCellGroupId().equals(mostFrequentCellGroupId)) {
                cell.setToSave(false);
            } else {
                cell.setToSave(true);
                cell.setOriginalCellGroupId(mostFrequentCellGroupId);
            }
        } else {
            cell.setToSave(true);
            cell.setOriginalCellGroupId(lubCellGroupId);
        }
    }

    private void mergeCells(CellGroup source, CellGroup dest) {
        dest.getOccurrences().addAll(source.getOccurrences());
        dest.getJustifications().addAll(source.getJustifications());
        dest.getUserCells().addAll(source.getUserCells());
        if (source.hasInvalidCell() && !dest.hasInvalidCell()) {
            dest.setInvalidCell(source.getInvalidCell());
        }
        dest.addAllAdditionalCells(source.getAdditionalCells());
    }

    private IValue findLubValue(CellGroup lubCellGroup, Scenario scenario) {
        //User Cells
        Set<CellGroupCell> userCells = lubCellGroup.getUserCells();
        if (!userCells.isEmpty()) {
            if (ChaseUtility.haveAllEqualValues(userCells)) {
                return userCells.iterator().next().getValue();
            }
            return CellGroupIDGenerator.getNextLLUNID();
        }
        // Authoritative Cells
        Set<CellGroupCell> authoritativeCells = lubCellGroup.getAuthoritativeJustifications();
        if (logger.isDebugEnabled()) logger.debug("Authoritative cells: " + authoritativeCells);
        if (!authoritativeCells.isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("Finding lubValue btw authoritative cells: " + authoritativeCells);
            if (ChaseUtility.haveAllEqualValues(authoritativeCells)) {
                return authoritativeCells.iterator().next().getValue();
            }
            return CellGroupIDGenerator.getNextLLUNID();
        }
        // Invalid Cell
        if (lubCellGroup.hasInvalidCell()) {
            return CellGroupIDGenerator.getNextLLUNID();
        }
        // Occurrences and nonAuthoritative Justifications
        Set<CellGroupCell> nonAuthoritativeConstantCells = extractNonAuthoritativeConstantCells(lubCellGroup);
        if (!nonAuthoritativeConstantCells.isEmpty()) {
            return generalizeNonAuthoritativeConstantCells(nonAuthoritativeConstantCells, lubCellGroup, scenario);
        }
        // Null Value
        return new NullValue(LunaticConstants.NULL_VALUE);
    }

    private Set<CellGroupCell> extractNonAuthoritativeConstantCells(CellGroup cellGroup) {
        Set<CellGroupCell> result = new HashSet<CellGroupCell>();
        for (CellGroupCell occurrence : cellGroup.getOccurrences()) {
            if (occurrence.getValue().getType().equals(PartialOrderConstants.CONST)) {
                result.add(occurrence);
            }
        }
        for (CellGroupCell justification : cellGroup.getNonAuthoritativeJustifications()) {
            if (justification.getValue().getType().equals(PartialOrderConstants.CONST)) {
                result.add(justification);
            }
        }
        return result;
    }

    public IValue generalizeNonAuthoritativeConstantCells(Set<CellGroupCell> nonAuthoritativeCells, CellGroup cellGroup, Scenario scenario) {
        Set<OrderingAttribute> orderingAttributes = findAllOrderingAttributes(nonAuthoritativeCells, scenario);
        if (orderingAttributes.isEmpty()) {
            // No PI
            if (ChaseUtility.haveAllEqualValues(nonAuthoritativeCells)) {
                return nonAuthoritativeCells.iterator().next().getValue();
            }
            return CellGroupIDGenerator.getNextLLUNID();
        }
        IValueComparator valueComparator = extractValueComparator(orderingAttributes);
        setAdditionalValues(nonAuthoritativeCells, cellGroup, scenario);
        List<CellGroupCell> cellList = new ArrayList<CellGroupCell>(nonAuthoritativeCells);
        Collections.sort(cellList, new CellComparatorUsingAdditionalValue(valueComparator));
        return cellList.get(cellList.size() - 1).getValue();
    }

    private IValueComparator extractValueComparator(Set<OrderingAttribute> orderingAttributes) {
        IValueComparator valueComparator = orderingAttributes.iterator().next().getValueComparator();
        for (OrderingAttribute orderingAttribute : orderingAttributes) {
            IValueComparator otherValueComparator = orderingAttribute.getValueComparator();
            if (otherValueComparator.equals(valueComparator)) {
                continue;
            }
            throw new PartialOrderException("Unable to compare attributes with different ordering attributes" + LunaticUtility.printCollection(orderingAttributes));
        }
        return valueComparator;
    }

    private Set<OrderingAttribute> findAllOrderingAttributes(Set<CellGroupCell> nonAuthoritativeCells, Scenario scenario) {
        Set<OrderingAttribute> result = new HashSet<OrderingAttribute>();
        for (CellGroupCell cellGroupCell : nonAuthoritativeCells) {
            OrderingAttribute orderingAttribute = getOrderingAttributeForAttributeRef(cellGroupCell.getAttributeRef(), scenario);
            if (orderingAttribute != null) {
                result.add(orderingAttribute);
            }
        }
        return result;
    }

    private OrderingAttribute getOrderingAttributeForAttributeRef(AttributeRef attributeRef, Scenario scenario) {
        for (OrderingAttribute orderingAttribute : scenario.getOrderingAttributes()) {
            if (orderingAttribute.getAttribute().equals(attributeRef)) {
                return orderingAttribute;
            }
        }
        return null;
    }

    private void setAdditionalValues(Set<CellGroupCell> nonAuthoritativeCells, CellGroup cellGroup, Scenario scenario) {
        for (CellGroupCell cell : nonAuthoritativeCells) {
//            List<IValue> additionalValues = findAdditionalValues(cell, cellGroup, scenario);
//            IValue maxValue = computeMaxValue(additionalValues, valueComparator);
            IValue maxValue = findAdditionalValue(cell, cellGroup, scenario);
            cell.setAdditionalValue(maxValue);
        }
    }

    private IValue findAdditionalValue(CellGroupCell cell, CellGroup cellGroup, Scenario scenario) {
        OrderingAttribute orderingAttribute = getOrderingAttributeForAttributeRef(cell.getAttributeRef(), scenario);
        if (orderingAttribute.getAssociatedAttribute().equals(cell.getAttributeRef())) {
            return cell.getOriginalValue();
        }
        Set<CellGroupCell> additionalCellsForAttribute = cellGroup.getAdditionalCells().get(orderingAttribute.getAssociatedAttribute());
        for (CellGroupCell additionalCell : additionalCellsForAttribute) {
            if (cell.getTupleOID().equals(additionalCell.getTupleOID())) {
                return additionalCell.getOriginalValue();
            }
        }
        throw new ChaseException("Unable to extract additional cell for cell " + cell + " in cell group \n\t" + cellGroup);
    }

//    private List<IValue> findAdditionalValues(CellGroupCell cell, CellGroup cellGroup, Scenario scenario) {
//        List<IValue> result = new ArrayList<IValue>();
//        Set<CellGroupCell> additionalCells = cellGroup.getAdditionalCells().get(cell.getAttributeRef());
//        if (additionalCells == null) {
//            result.add(cell.getOriginalValue());
//            return result;
//        }
//        for (CellGroupCell additionalCell : additionalCells) {
//            result.add(additionalCell.getOriginalValue());
//        }
//        return result;
//    }
    private IValue computeMaxValue(List<IValue> additionalValues, IValueComparator valueComparator) {
        Collections.sort(additionalValues, valueComparator);
        return additionalValues.get(additionalValues.size() - 1);
    }

    private IValue findMostFrequentCellGroupId(CellGroup lubCellGroup) {
        Map<IValue, Integer> variableOccurrenceHistogram = buildOccurrenceHistogramForCellGroupId(lubCellGroup, lubCellGroup.getId());
        if (variableOccurrenceHistogram.isEmpty()) {
            return null;
        }
        return ChaseUtility.findFirstOrderedValue(variableOccurrenceHistogram);
    }

    private Map<IValue, Integer> buildOccurrenceHistogramForCellGroupId(CellGroup lubCellGroup, IValue lubCellGroupId) {
        Map<IValue, Integer> result = new HashMap<IValue, Integer>();
        computeFrequencyOfCellGroupId(lubCellGroup.getOccurrences(), lubCellGroupId, result);
        computeFrequencyOfCellGroupId(lubCellGroup.getJustifications(), lubCellGroupId, result);
        computeFrequencyOfCellGroupId(lubCellGroup.getUserCells(), lubCellGroupId, result);
        for (AttributeRef attributeRef : lubCellGroup.getAdditionalCells().keySet()) {
            Set<CellGroupCell> additionalCells = lubCellGroup.getAdditionalCells().get(attributeRef);
            computeFrequencyOfCellGroupId(additionalCells, lubCellGroupId, result);
        }
        return result;
    }

    private void computeFrequencyOfCellGroupId(Set<CellGroupCell> cells, IValue lubCellGroupId, Map<IValue, Integer> result) {
        for (CellGroupCell cell : cells) {
            IValue originalCellGroupId = cell.getOriginalCellGroupId();
            if (originalCellGroupId == null || !isCompatible(originalCellGroupId, lubCellGroupId)) {
                continue;
            }
            Integer occurrences = result.get(originalCellGroupId);
            if (occurrences == null) {
                result.put(originalCellGroupId, 1);
            } else {
                result.put(originalCellGroupId, occurrences++);
            }
        }
    }

    private boolean isCompatible(IValue originalCellGroupId, IValue lubCellGroupId) {
        if (!originalCellGroupId.getType().equals(lubCellGroupId.getType())) {
            return false;
        }
        if (originalCellGroupId.getType().equals(PartialOrderConstants.LLUN)
                || originalCellGroupId.getType().equals(PartialOrderConstants.LLUN)) {
            return true;
        }
        IValue originalConstantValue = CellGroupIDGenerator.getCellGroupValueFromGroupID(originalCellGroupId);
        IValue lubConstantValue = CellGroupIDGenerator.getCellGroupValueFromGroupID(lubCellGroupId);
        return (originalConstantValue.equals(lubConstantValue));
    }

}
