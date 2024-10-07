package org.example.gqs.cypher.ast;

import java.util.List;

public interface IRelationIdentifier extends IPatternElement{
    List<IProperty> getProperties();
    List<IType> getTypes();
    Direction getDirection();
    void setDirection(Direction direction);
    IRelationIdentifier createRef();
    long getLengthLowerBound();
    long getLengthUpperBound();
    void setProperties(List<IProperty> properties);
}
