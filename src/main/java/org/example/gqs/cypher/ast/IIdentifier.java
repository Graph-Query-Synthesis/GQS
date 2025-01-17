package org.example.gqs.cypher.ast;

public interface IIdentifier extends ITextRepresentation, ICopyable{
    String getName();
    ICypherType getType();
    public boolean equals(IIdentifier i2);
    @Override
    IIdentifier getCopy();

}
