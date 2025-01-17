package org.example.gqs.cypher.dsl;

import org.example.gqs.cypher.ast.IRet;
import org.example.gqs.cypher.ast.analyzer.IReturnAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IWithAnalyzer;
import org.example.gqs.cypher.schema.CypherSchema;

import java.util.List;
import java.util.Map;

public abstract class BasicAliasGenerator<S extends CypherSchema<?,?>> implements IAliasGenerator{

    protected final S schema;
    private final IIdentifierBuilder identifierBuilder;

    public BasicAliasGenerator(S schema, IIdentifierBuilder identifierBuilder){
        this.schema = schema;
        this.identifierBuilder = identifierBuilder;
    }

    @Override
    public void fillReturnAlias(IReturnAnalyzer returnClause) {
        returnClause.setReturnList(generateReturnAlias(returnClause, identifierBuilder, schema));
    }

    public void fillReturnAlias(IReturnAnalyzer returnClause, List<Map<String, Object>> namespace) {
        returnClause.setReturnList(generateReturnAlias(returnClause, identifierBuilder, schema, namespace));
    }

    @Override
    public void fillWithAlias(IWithAnalyzer withClause) {
        withClause.setReturnList(generateWithAlias(withClause, identifierBuilder, schema));
    }

    public void fillWithAlias(IWithAnalyzer withClause, List<Map<String, Object>> namespace) {
        withClause.setReturnList(generateWithAlias(withClause, identifierBuilder, schema, namespace));
    }

    public abstract List<IRet> generateReturnAlias(IReturnAnalyzer returnClause, IIdentifierBuilder identifierBuilder, S schema);
    public abstract List<IRet> generateReturnAlias(IReturnAnalyzer returnClause, IIdentifierBuilder identifierBuilder, S schema, List<Map<String, Object>>namespace);
    public abstract List<IRet> generateWithAlias(IWithAnalyzer withClause, IIdentifierBuilder identifierBuilder, S schema);
    public abstract List<IRet> generateWithAlias(IWithAnalyzer withClause, IIdentifierBuilder identifierBuilder, S schema, List<Map<String, Object>>namespace);
}
