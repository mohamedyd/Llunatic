package it.unibas.lunatic.model.chase.chasemc.operators;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.model.chase.commons.operators.ChaseUtility;
import speedy.model.database.AttributeRef;
import speedy.model.database.CellRef;
import speedy.model.database.IDatabase;
import speedy.model.database.IValue;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import speedy.model.expressions.Expression;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.SpeedyConstants;
import speedy.model.algebra.GroupBy;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Join;
import speedy.model.algebra.Project;
import speedy.model.algebra.Scan;
import speedy.model.algebra.Select;
import speedy.model.algebra.aggregatefunctions.IAggregateFunction;
import speedy.model.algebra.aggregatefunctions.MaxAggregateFunction;
import speedy.model.algebra.aggregatefunctions.ValueAggregateFunction;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.TableAlias;
import speedy.model.database.Tuple;
import speedy.model.database.operators.IRunQuery;
import speedy.utility.SpeedyUtility;

public class ValueExtractor {

    private static Logger logger = LoggerFactory.getLogger(ValueExtractor.class);

    private IRunQuery queryRunner;

    public ValueExtractor(IRunQuery queryRunner) {
        this.queryRunner = queryRunner;
    }

    public IValue getStepValue(CellRef cellRef, IDatabase deltaDB, String stepId) {
        IAlgebraOperator query = buildQuery(cellRef, stepId);
        ITupleIterator it = queryRunner.run(query, null, deltaDB);
        if (!it.hasNext()) {
            //Throws exceptions?
            return null;
        }
        Tuple tuple = it.next();
        IValue value = LunaticUtility.getAttributevalueInTuple(tuple, cellRef.getAttributeRef().getName());
        it.close();
        return value;
    }

    public IValue getStepValue(CellRef cellRef, DeltaChaseStep chaseStep) {
        return getStepValue(cellRef, chaseStep.getDeltaDB(), chaseStep.getId());
    }

    public IValue getOriginalValue(CellRef cellRef, IDatabase deltaDB) {
        return getStepValue(cellRef, deltaDB, LunaticConstants.CHASE_STEP_ROOT);
    }

    public IValue getOriginalValue(CellRef cellRef, DeltaChaseStep chaseStep) {
        return getStepValue(cellRef, chaseStep.getDeltaDB(), LunaticConstants.CHASE_STEP_ROOT);
    }

    public IValue getPreviousValue(CellRef cellRef, DeltaChaseStep chaseStep) {
        if (chaseStep.isRoot()) {
            throw new IllegalArgumentException("Unable to extract previous value from root step");
        }
        return getStepValue(cellRef, chaseStep.getDeltaDB(), chaseStep.getFather().getId());
    }

    private IAlgebraOperator buildQuery(CellRef cellRef, String stepId) {
        AttributeRef attribute = cellRef.getAttributeRef();
        // select * from R_A where step
        TableAlias table = new TableAlias(ChaseUtility.getDeltaRelationName(attribute.getTableName(), attribute.getName()));
        Scan tableScan = new Scan(table);
        Expression tidExpression = new Expression(SpeedyConstants.TID + " == " + cellRef.getTupleOID().toString());
        tidExpression.changeVariableDescription(SpeedyConstants.TID, new AttributeRef(table, SpeedyConstants.TID));
        Expression stepExpression = new Expression("startswith(\"" + stepId + "\", " + SpeedyConstants.STEP + ")");
        stepExpression.changeVariableDescription(SpeedyConstants.STEP, new AttributeRef(table, SpeedyConstants.STEP));
        Select stepSelect = new Select(Arrays.asList(new Expression[]{tidExpression, stepExpression}));
        stepSelect.addChild(tableScan);
        // select max(step), oid from R_A group by oid
        AttributeRef oid = new AttributeRef(table, SpeedyConstants.TID);
        AttributeRef step = new AttributeRef(table, SpeedyConstants.STEP);
        List<AttributeRef> groupingAttributes = new ArrayList<AttributeRef>(Arrays.asList(new AttributeRef[]{oid}));
        IAggregateFunction max = new MaxAggregateFunction(step);
        IAggregateFunction oidValue = new ValueAggregateFunction(oid);
        List<IAggregateFunction> aggregateFunctions = new ArrayList<IAggregateFunction>(Arrays.asList(new IAggregateFunction[]{max, oidValue}));
        GroupBy groupBy = new GroupBy(groupingAttributes, aggregateFunctions);
        groupBy.addChild(stepSelect);
        // select * from R_A_1
        TableAlias alias = new TableAlias(table.getTableName(), "0");
        Scan aliasScan = new Scan(alias);
        // select * from (group-by) join R_A_1 on oid, step
        List<AttributeRef> leftAttributes = new ArrayList<AttributeRef>(Arrays.asList(new AttributeRef[]{oid, step}));
        AttributeRef oidInAlias = new AttributeRef(alias, SpeedyConstants.TID);
        AttributeRef stepInAlias = new AttributeRef(alias, SpeedyConstants.STEP);
        List<AttributeRef> rightAttributes = new ArrayList<AttributeRef>(Arrays.asList(new AttributeRef[]{oidInAlias, stepInAlias}));
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(groupBy);
        join.addChild(aliasScan);
        // select oid, A from (join)
        AttributeRef attributeInAlias = new AttributeRef(alias, attribute.getName());
        List<AttributeRef> projectionAttributes = new ArrayList<AttributeRef>(Arrays.asList(new AttributeRef[]{oid, attributeInAlias}));
        List<AttributeRef> newAttributes = new ArrayList<AttributeRef>(Arrays.asList(new AttributeRef[]{oid, attributeInAlias}));
        Project project = new Project(SpeedyUtility.createProjectionAttributes(projectionAttributes), newAttributes, false);
        project.addChild(join);
        if (logger.isDebugEnabled()) logger.debug("Algebra tree for attribute: " + attribute + "\n" + project);
        return project;
    }
}
