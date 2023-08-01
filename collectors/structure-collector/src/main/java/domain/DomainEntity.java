package domain;

import domain.datatypes.DataType;
import domain.datatypes.PrimitiveDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a domain entity class in the source code.
 */
public class DomainEntity {

    private String name;
    private List<Field> fields;
    private DataType superclass;

    public DomainEntity(String name) {
        this.name = name;
        this.fields = new ArrayList<>();
        this.superclass = null;
    }

    public String getName() {
        return this.name;
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void addField(Field field) {
        this.fields.add(field);
    }

    public DataType getSuperclass() {
        return this.superclass;
    }

    public void setSuperclass(DataType superclass) {
        if (superclassDataTypeIsThisDataType(superclass) || superclassDataTypeIsPrimitive(superclass)) {
            return; // Source code error
        }
        this.superclass = superclass;
    }

    private boolean superclassDataTypeIsThisDataType(DataType superclass) {
        return this.name.equals(superclass.getName());
    }

    private boolean superclassDataTypeIsPrimitive(DataType superclass) {
        return superclass instanceof PrimitiveDataType;
    }
}
