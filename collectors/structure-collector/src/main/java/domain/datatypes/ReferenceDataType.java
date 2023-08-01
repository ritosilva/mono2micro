package domain.datatypes;

public class ReferenceDataType extends DataType {

    public ReferenceDataType(String name) {
        super(name);
    }

    @Override
    public String toStringKey() {
        return getName();
    }
}
