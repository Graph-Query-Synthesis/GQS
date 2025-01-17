package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.List;

public abstract class BasicPatternGenerator<S extends CypherSchema<?,?>> implements IPatternGenerator{

    protected final S schema;
    private final IIdentifierBuilder identifierBuilder;

    public BasicPatternGenerator(S schema, IIdentifierBuilder identifierBuilder){
        this.schema = schema;
        this.identifierBuilder = identifierBuilder;
    }


    @Override
    public void fillMatchPattern(IMatchAnalyzer matchClause) {
        matchClause.setPatternTuple(generatePattern(matchClause, identifierBuilder, schema));
    }

    public abstract List<IPattern> generatePattern(IMatchAnalyzer matchClause, IIdentifierBuilder identifierBuilder, S schema);
}
