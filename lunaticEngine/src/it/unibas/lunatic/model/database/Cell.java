package it.unibas.lunatic.model.database;

import it.unibas.lunatic.LunaticConstants;
import java.io.Serializable;

public class Cell implements Serializable, Cloneable {

    private TupleOID tupleOid;
    private AttributeRef attributeRef;
    private IValue value;

    public Cell(TupleOID tupleOid, AttributeRef attributeRef, IValue value) {
        assert (value == null || attributeRef.getTableName() == null || 
                attributeRef.getTableName().equalsIgnoreCase(LunaticConstants.CELLGROUP_TABLE) ||  
                attributeRef.getName().equalsIgnoreCase(LunaticConstants.GROUP_ID) || 
                !value.toString().contains(LunaticConstants.VALUE_LABEL)) : "Trying to build a cell group with id in place of value: " + value + " on table " + attributeRef;
        this.tupleOid = tupleOid;
        this.attributeRef = attributeRef;
        this.value = value;
    }

    public Cell(CellRef cellRef, IValue value) {
        this(cellRef.getTupleOID(), cellRef.getAttributeRef(), value);
    }

    public Cell(Cell originalCell, Tuple newTuple) {
        this.tupleOid = newTuple.getOid();
        this.attributeRef = originalCell.attributeRef;
        this.value = originalCell.value;
    }

    public boolean isOID() {
        return attributeRef.getName().equals(LunaticConstants.OID);
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public void setAttributeRef(AttributeRef attributeRef) {
        this.attributeRef = attributeRef;
    }

    public String getAttribute() {
        return attributeRef.getName();
    }

    public IValue getValue() {
        return value;
    }

    public TupleOID getTupleOID() {
        return tupleOid;
    }

    public void setTupleOid(TupleOID tupleOid) {
        this.tupleOid = tupleOid;
    }

    public boolean isSource() {
        return this.attributeRef.isSource();
    }

    public boolean isTarget() {
        return !isSource();
    }

    public boolean isAuthoritative() {
        return this.attributeRef.isAuthoritative();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return this.toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public Cell clone() {
        try {
            return (Cell) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return tupleOid + ":" + attributeRef + "-" + value + (isAuthoritative() ? " (Auth)" : "");
    }

    public String toShortString() {
        return attributeRef.getName() + ":" + value;
    }

    public String toStringWithAlias() {
        return attributeRef + ":" + value;
    }

    public String toStringWithOIDAndAlias() {
        return tupleOid + ":" + attributeRef + ":" + value;
    }
}
