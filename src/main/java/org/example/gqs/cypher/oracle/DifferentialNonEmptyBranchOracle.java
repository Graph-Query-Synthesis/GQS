package org.example.gqs.cypher.oracle;

import org.example.gqs.MainOptions;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.expr.*;
import org.example.gqs.exceptions.ResultMismatchException;
import org.example.gqs.cypher.standard_ast.*;
import org.neo4j.driver.exceptions.ClientException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class DifferentialNonEmptyBranchOracle <G extends CypherGlobalState<?,S>, S extends CypherSchema<G,?>> implements TestOracle {

    private final G globalState;
    private IQueryGenerator<S, G> queryGenerator;

    public static final int BRANCH_PAIR_SIZE = 65536;
    public static final  int BRANCH_SIZE = 1000000;

    public static final int PORT = 9009;
    public static final byte CLEAR = 1, PRINT_MEM = 2;
    public static String lastTime = "";
    public static List<GQSResultSet> lastCorrectResult = new ArrayList<>();
    public static String dbStatus = "";

    public DifferentialNonEmptyBranchOracle(G globalState, IQueryGenerator<S,G> generator) {
        this.globalState = globalState;
        this.queryGenerator = generator;
    }

    void tryExecute(List<ICypherClause> tryRemoveCopy, List<GQSResultSet> wrongResult) throws Exception {
        IClauseSequence sequence = new ClauseSequence(tryRemoveCopy);
        StringBuilder trysb = new StringBuilder();
        sequence.toTextRepresentation(trysb);
        System.out.println("Trying to minimize with: "+trysb);
        List<GQSResultSet> results;
        results = globalState.executeStatementAndGet(new CypherQueryAdapter(trysb.toString()));
        if(results.size() != wrongResult.size()){
            throw new ExecutionException(new Exception("The result size is not equal!"));
        }
        for(int j = 0; j < results.size(); j++) {
            if (!results.get(j).compareWithOutOrder(wrongResult.get(j))) {
                throw new ExecutionException(new Exception("Result is not the same, the original is: "+wrongResult.get(j).resultToStringList()+"\n the new one is: "+results.get(j).resultToStringList()));
            }
        }
    }

    List<ICypherClause> tryRemove(List<ICypherClause> clauses, Set<IIdentifier> removedIdentifer, int startIndex, List<GQSResultSet> wrongResult) throws Exception {
        List<ICypherClause> backup = new ArrayList<>();
        for (ICypherClause clause : clauses) {
            backup.add(clause.getCopy());
        }
        Set<IIdentifier> allProvide = new HashSet<>();
        Set<IIdentifier> allRequire = new HashSet<>();
        for (int i = 0; i < startIndex; i++) {
            CypherClause temp = (CypherClause) (clauses.get(i));
            temp.updateProvideAndRequire();
            if (temp instanceof With) {
                Set<IIdentifier> tempProvide = new HashSet<>();
                for (IIdentifier identifier : temp.provide) {
                    if (!(identifier instanceof Alias) && !allProvide.contains(identifier)) {
                        continue;
                    } else {
                        tempProvide.add(identifier);
                    }
                }
                allProvide = tempProvide;
                temp.provide = tempProvide;
                allRequire = new HashSet<>();
            } else {
                allProvide.addAll(temp.provide);
                allRequire.addAll(temp.require);
            }
        }

        for (int i = startIndex; i < clauses.size() - 1; i++) {
            CypherClause temp = (CypherClause) (clauses.get(i));
            temp.updateProvideAndRequire();
            Set<IIdentifier> identifierToFix = new HashSet<>();
            if (temp instanceof With) {
                for (IIdentifier identifer : temp.require) {
                    if (removedIdentifer.contains(identifer) && !allProvide.contains(identifer)) {
                        identifierToFix.add(identifer);
                    }
                }
            } else {
                for (IIdentifier identifer : temp.require) {
                    if (removedIdentifer.contains(identifer) && !allProvide.contains(identifer) && !temp.provide.contains(identifer)) {
                        identifierToFix.add(identifer);
                    }
                }
            }
            if (!identifierToFix.isEmpty()) {
                if (temp instanceof IMatch) {
                    IExpression condition = ((Match) temp).condition;
                    if (condition != null)
                        ((CypherExpression) condition).removeElement(identifierToFix);
                } else if (temp instanceof IWith) {
                    Set<IIdentifier> additionalFix = new HashSet<>();
                    for (IIdentifier identifier : identifierToFix) {
                        List<IRet> rets = ((With) temp).getReturnList();
                        for (int j = 0; j < rets.size(); j++) {
                            IRet ret = rets.get(j);
                            if (ret.getIdentifier() instanceof Alias && ret.getExpression() instanceof GetPropertyExpression) {
                                if (((GetPropertyExpression) ret.getExpression()).reliedContent().contains(identifier)) {
                                    ConstExpression tmp = new ConstExpression(((GetPropertyExpression) ret.getExpression()).getValue());
                                    ((Alias) (((Ret) ret).getIdentifier())).setExpression(tmp);
                                    ((Ret) ret).setExpression(tmp);
                                }
                            } else if (ret.getExpression() == null && ret.getIdentifier() != null) {
                                if (ret.getIdentifier().equals(identifier)) {
                                    if (ret.getIdentifier() instanceof Alias) {
                                        ((Alias) ret.getIdentifier()).setExpression(new ConstExpression(999));
                                        additionalFix.add(ret.getIdentifier());
                                    } else
                                        rets.set(j, new Ret(new ConstExpression((999)), "replacement" + identifier.getName()));
                                }
                            } else if (ret.getExpression() instanceof IdentifierExpression && ret.getIdentifier() instanceof Alias) {
                                if (((IdentifierExpression) ret.getExpression()).getIdentifier().equals(identifier)) {
                                    ConstExpression tmp = new ConstExpression(999);
                                    ((Alias) (((Ret) ret).getIdentifier())).setExpression(tmp);
                                    ((Ret) ret).setExpression(tmp);
                                    additionalFix.add(ret.getIdentifier());
                                }
                            } else if (ret.getExpression() instanceof CallExpression && ret.getIdentifier() instanceof Alias) {
                                if (((CallExpression) ret.getExpression()).reliedContent().contains(identifier)) {
                                    ConstExpression tmp = new ConstExpression(((CallExpression) ret.getExpression()).getValue());
                                    ((Alias) (((Ret) ret).getIdentifier())).setExpression(tmp);
                                    ((Ret) ret).setExpression(tmp);
                                    additionalFix.add(ret.getIdentifier());
                                }
                            }
                        }
                    }
                    identifierToFix.addAll(additionalFix);
                    IExpression condition = ((With) temp).getCondition();
                    if (condition != null) {
                        if (condition instanceof GetPropertyExpression) {
                            if (((GetPropertyExpression) condition).reliedContent().containsAll(identifierToFix)) {
                                ((With) temp).setCondition(new ConstExpression(((GetPropertyExpression) condition).getValue()));
                            }
                        } else {
                            ((CypherExpression) condition).removeElement(identifierToFix);
                        }
                    }
                    List<IExpression> orderBy = ((With) temp).getOrderBy();
                    if (orderBy != null) {
                        List<IExpression> newOrderBy = new ArrayList<>();
                        for (IExpression expression : orderBy) {
                            Set<IIdentifier> relied = ((CypherExpression) expression).reliedContent();
                            boolean flag = true;
                            for (IIdentifier identifier : relied) {
                                if (identifierToFix.contains(identifier)) {
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag)
                                newOrderBy.add(expression);
                        }
                        ((With) temp).setOrderBy(newOrderBy, ((With) temp).isOrderByDesc());

                    }
                } else if (temp instanceof IUnwind) {
                    IRet ret = ((Unwind) temp).getListAsAliasRet();
                    ((CypherExpression) (((Ret) ret).expression)).removeElement(identifierToFix);
                }
            }
        }
        try {
            tryExecute(clauses, wrongResult);
        } catch (ExecutionException e) {
            System.out.println("Wrong answer was produced: " + e.getMessage());
            throw new RuntimeException("Wrong answer was produced: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("This minimization will cause exeception, skip it!");
            throw new SecurityException("This minimization will cause exeception, skip it!");
        }

        return clauses;
    }

    IExpression dfsPrune(IExpression condition) {
        IExpression backup = condition.getCopy();
        if (condition instanceof BinaryLogicalExpression) {
            if (((BinaryLogicalExpression) condition).getLeftExpression() instanceof BinaryLogicalExpression)
                ((BinaryLogicalExpression) condition).setLeftExpression(dfsPrune(((BinaryLogicalExpression) condition).getLeftExpression()));
            if (((BinaryLogicalExpression) condition).getRightExpression() instanceof BinaryLogicalExpression)
                ((BinaryLogicalExpression) condition).setRightExpression(dfsPrune(((BinaryLogicalExpression) condition).getRightExpression()));
            if (((BinaryLogicalExpression) condition).getLeftExpression() instanceof ConstExpression && ((BinaryLogicalExpression) condition).getRightExpression() instanceof ConstExpression) {
                return new ConstExpression(((BinaryLogicalExpression) condition).getValue());
            } else if (((BinaryLogicalExpression) condition).getRightExpression() instanceof ConstExpression && ((BinaryLogicalExpression) condition).getOperation() == BinaryLogicalExpression.BinaryLogicalOperation.AND && ((ConstExpression) ((BinaryLogicalExpression) condition).getRightExpression()).getValue().equals(true)) {
                return ((BinaryLogicalExpression) condition).getLeftExpression();
            } else if (((BinaryLogicalExpression) condition).getLeftExpression() instanceof ConstExpression && ((BinaryLogicalExpression) condition).getOperation() == BinaryLogicalExpression.BinaryLogicalOperation.AND && ((ConstExpression) ((BinaryLogicalExpression) condition).getLeftExpression()).getValue().equals(true)) {
                return ((BinaryLogicalExpression) condition).getRightExpression();
            } else if (((BinaryLogicalExpression) condition).getRightExpression() instanceof ConstExpression && ((BinaryLogicalExpression) condition).getOperation() == BinaryLogicalExpression.BinaryLogicalOperation.OR) {
                return ((BinaryLogicalExpression) condition).getLeftExpression();
            } else if (((BinaryLogicalExpression) condition).getLeftExpression() instanceof ConstExpression && ((BinaryLogicalExpression) condition).getOperation() == BinaryLogicalExpression.BinaryLogicalOperation.OR) {
                return ((BinaryLogicalExpression) condition).getRightExpression();
            }

        } else if (condition instanceof BinaryComparisonExpression) {
            IExpression left = ((BinaryComparisonExpression) condition).getLeftExpression();
            IExpression right = ((BinaryComparisonExpression) condition).getRightExpression();
            if (left instanceof ConstExpression && right instanceof ConstExpression) {
                return new ConstExpression(((BinaryComparisonExpression) condition).getValue());
            }
        } else if (condition instanceof StringMatchingExpression) {
            IExpression left = ((StringMatchingExpression) condition).getSource();
            IExpression right = ((StringMatchingExpression) condition).getPattern();
            if (left instanceof ConstExpression && right instanceof ConstExpression) {
                return new ConstExpression(((StringMatchingExpression) condition).getValue());
            }
        }
        return condition;
    }


    IClauseSequence minimizeTestCase(List<ICypherClause> clauses, List<GQSResultSet> wrongResult, List<String> formerQueries) {
        StringBuilder sb = new StringBuilder();
        Set<IIdentifier> necessaryIdentifier = new HashSet<>();
        IReturn returnClause = (Return) (clauses.get(clauses.size() - 1));
        ((Return) returnClause).updateProvideAndRequire();
        necessaryIdentifier.addAll(((Return) returnClause).require);
        List<ICypherClause> tryRemoveCopy = new ArrayList<>();
        for (ICypherClause clause : clauses) {
            ((CypherClause) clause).updateProvideAndRequire();
            tryRemoveCopy.add(clause.getCopy());
        }
        for (int i = 0; i < tryRemoveCopy.size() - 1; i++) {
            boolean canRemove = true;
            CypherClause clause = (CypherClause) (tryRemoveCopy.get(i));
            if (clause instanceof Unwind)
                continue;
            for (IIdentifier identifier : necessaryIdentifier) {
                if (clause.require.contains(identifier)) {
                    canRemove = false;
                    break;
                }
            }
            if (canRemove) {
                Set<IIdentifier> removedIdentifer = new HashSet<>();
                removedIdentifer.addAll(clause.provide);
                long originalSize = tryRemoveCopy.size();

                List<ICypherClause> removedClause = new ArrayList<>();
                removedClause.add(clause);
                IClauseSequence sequence = new ClauseSequence(removedClause);
                StringBuilder trysb = new StringBuilder();
                sequence.toTextRepresentation(trysb);
                tryRemoveCopy.remove(i);
                try {
                    globalState.getState().logStatement("// Trying to remove clause: "+trysb.toString());
                    if (formerQueries.size() != 0)
                        ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                    tryRemoveCopy = tryRemove(tryRemoveCopy, removedIdentifer, i, wrongResult);
                } catch (Exception e) {
                    globalState.getState().logStatement("// Removing failed, restore the backup");
                    tryRemoveCopy = new ArrayList<>();
                    for (int j = 0; j < clauses.size(); j++) {
                        tryRemoveCopy.add(clauses.get(j).getCopy());
                    }
                }
                if (tryRemoveCopy.size() != originalSize) {
                    globalState.getState().logStatement("// Removing succeeded");
                    i--;
                    clauses = new ArrayList<>();
                    for (int j = 0; j < tryRemoveCopy.size(); j++) {
                        ((CypherClause) tryRemoveCopy.get(j)).updateProvideAndRequire();
                        clauses.add(tryRemoveCopy.get(j).getCopy());
                    }
                }
            }
        }
        List<ICypherClause> backupClauses = new ArrayList<>();
        for (ICypherClause clause : clauses) {
            backupClauses.add(clause.getCopy());
        }
        for (int i = 0; i < tryRemoveCopy.size() - 1; i++) {
            ICypherClause clause = tryRemoveCopy.get(i);
            if (clause instanceof IMatch) {
                Match matchClause = (Match) clause;
                List<IPattern> patterns = matchClause.getPatternTuple();
                for (int j = 0; j < patterns.size() && patterns.size() != 1; j++) {
                    if (patterns.size() <= 1)
                        continue;
                    IPattern pattern = patterns.get(j);
                    boolean canRemove = true;
                    Set<IIdentifier> toBeRemoved = new HashSet<>();
                    List<IPatternElement> patternElements = pattern.getPatternElements();
                    for (IPatternElement patternElement : patternElements) {
                        if (necessaryIdentifier.contains((IIdentifier) patternElement)) {
                            canRemove = false;
                            break;
                        } else {
                            toBeRemoved.add((IIdentifier) patternElement);
                        }
                    }
                    if (canRemove) {
                        long originalSize = patterns.size();
                        StringBuilder trysb = new StringBuilder();
                        pattern.toTextRepresentation(trysb);
                        patterns.remove(pattern);
                        matchClause.setPatternTuple(patterns);
                        try {
                            globalState.getState().logStatement("// Trying to remove pattern: "+trysb);
                            if (formerQueries.size() != 0)
                                ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                            tryRemoveCopy = tryRemove(tryRemoveCopy, toBeRemoved, i, wrongResult);
                        } catch (Exception e) {

                            globalState.getState().logStatement("// Removed failed, restore the backup");
                            for (int k = 0; k < tryRemoveCopy.size(); k++) {
                                tryRemoveCopy.set(k, backupClauses.get(k).getCopy());
                            }
                            clause = tryRemoveCopy.get(i);
                            matchClause = (Match) clause;
                            patterns = matchClause.getPatternTuple();
                        }
                        if (patterns.size() != originalSize) {
                            globalState.getState().logStatement("// Removed succeeded");
                            for (int k = 0; k < tryRemoveCopy.size(); k++) {
                                backupClauses.set(k, tryRemoveCopy.get(k).getCopy());
                            }
                            j--;
                        }
                    }
                }


                IExpression condition = matchClause.getCondition();
                if (condition instanceof BinaryLogicalExpression) {

                    Queue<IExpression> queue = new LinkedList<>();
                    queue.add(condition);
                    while (!queue.isEmpty()) {

                        IExpression expression = queue.poll();

                        if (expression instanceof BinaryLogicalExpression) {

                            IExpression left = ((BinaryLogicalExpression) expression).getLeftExpression();
                            IExpression right = ((BinaryLogicalExpression) expression).getRightExpression();
                            if (left instanceof BinaryLogicalExpression)
                                queue.add(left);
                            if (right instanceof BinaryLogicalExpression)
                                queue.add(right);

                            if (!(left instanceof BinaryLogicalExpression)) {
                                IExpression backup = left.getCopy();
                                StringBuilder trysb = new StringBuilder();
                                backup.toTextRepresentation(trysb);
                                ((BinaryLogicalExpression) expression).setLeftExpression(new ConstExpression(true));
                                try {
                                    globalState.getState().logStatement("// Trying to remove condition: "+trysb);
                                    if (formerQueries.size() != 0)
                                        ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                    tryExecute(tryRemoveCopy, wrongResult);
                                    globalState.getState().logStatement("// Removed succeeded");
                                } catch (ExecutionException e) {
                                    globalState.getState().logStatement("// Removed failed, restore the backup");
                                    System.out.println("Wrong answer was produced: " + e.getMessage());
                                    ((BinaryLogicalExpression) expression).setLeftExpression(backup);
                                } catch (Exception e) {
                                    globalState.getState().logStatement("// Removed failed, restore the backup");
                                    System.out.println("This minimization will cause exeception, skip it!");
                                    ((BinaryLogicalExpression) expression).setLeftExpression(backup);
                                }
                            }

                            if (!(right instanceof BinaryLogicalExpression)) {
                                IExpression backup = right.getCopy();
                                StringBuilder trysb = new StringBuilder();
                                backup.toTextRepresentation(trysb);
                                ((BinaryLogicalExpression) expression).setRightExpression(new ConstExpression(true));
                                try {
                                    globalState.getState().logStatement("// Trying to remove condition: "+trysb);
                                    if (formerQueries.size() != 0)
                                        ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                    tryExecute(tryRemoveCopy, wrongResult);
                                    globalState.getState().logStatement("// Removed succeeded");
                                } catch (ExecutionException e) {
                                    globalState.getState().logStatement("// Removed failed, restore the backup");
                                    System.out.println("Wrong answer was produced: " + e.getMessage());
                                    ((BinaryLogicalExpression) expression).setRightExpression(backup);
                                } catch (Exception e) {
                                    globalState.getState().logStatement("// Removed failed, restore the backup");
                                    System.out.println("This minimization will cause exeception, skip it!");
                                    ((BinaryLogicalExpression) expression).setRightExpression(backup);
                                }
                            }
                        }
                    }


                    ICypherClause backup = matchClause.getCopy();
                    matchClause.setCondition(dfsPrune(condition));
                    StringBuilder trysb = new StringBuilder();
                    matchClause.toTextRepresentation(trysb);
                    try {
                        globalState.getState().logStatement("// Trying to prune condition to: "+trysb);
                        if (formerQueries.size() != 0)
                            ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                        tryExecute(tryRemoveCopy, wrongResult);
                        globalState.getState().logStatement("// Removed succeeded");
                    } catch (ExecutionException e) {
                        globalState.getState().logStatement("// Removed failed, restore the backup");
                        System.out.println("Wrong answer was produced: " + e.getMessage());
                        matchClause.setCondition(((Match) backup).getCondition());
                    } catch (Exception e) {
                        globalState.getState().logStatement("// Removed failed, restore the backup");
                        System.out.println("This minimization will cause exeception, skip it!");
                        matchClause.setCondition(((Match) backup).getCondition());
                    }

                    for (int j = 0; j < backupClauses.size(); j++) {
                        backupClauses.set(j, tryRemoveCopy.get(j).getCopy());
                    }


                    patterns = matchClause.getPatternTuple();
                    for (int j = 0; j < patterns.size(); j++) {
                        IPattern pattern = patterns.get(j);

                        List<IPatternElement> patternElements = ((Pattern) pattern).patternElements;


                        for (int z = 0; z < patternElements.size(); z++) {
                            IPatternElement patternElement = patternElements.get(z);
                            ICypherType type = patternElement.getType();

                            if (type == CypherType.NODE) {

                                List<ILabel> labels = ((NodeIdentifier) patternElement).labels;
                                for (int k = 0; k < labels.size(); k++) {
                                    ILabel label = labels.get(k);

                                    long originalSize = labels.size();
                                    StringBuilder trysb2 = new StringBuilder();
                                    labels.remove(label);
                                    patterns.set(j, pattern);
                                    matchClause.setPatternTuple(patterns);
                                    patternElement.toTextRepresentation(trysb2);

                                    try {
                                        globalState.getState().logStatement("// Trying to remove label: "+trysb2);
                                        if (formerQueries.size() != 0)
                                            ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                        tryExecute(tryRemoveCopy, wrongResult);
                                    } catch (Exception e) {
                                        globalState.getState().logStatement("// Removed failed, restore the backup");

                                        labels.add(label);
                                        patternElements.set(z, patternElement);
                                        patterns.set(j, pattern);
                                        matchClause.setPatternTuple(patterns);
                                        tryRemoveCopy.set(i, backupClauses.get(i).getCopy());
                                        clause = tryRemoveCopy.get(i);
                                        matchClause = (Match) clause;
                                        patterns = matchClause.getPatternTuple();
                                    }
                                    if (labels.size() != originalSize) {
                                        globalState.getState().logStatement("// Removed succeeded");
                                        k--;
                                        backupClauses.set(i, tryRemoveCopy.get(i).getCopy());
                                    }
                                }
                            } else if (type == CypherType.RELATION) {

                                IType labels = new RelationType(((RelationIdentifier) patternElement).relationType.getName());

                                StringBuilder trysb2 = new StringBuilder();
                                ((RelationIdentifier) patternElement).relationType = null;
                                patterns.set(j, pattern);
                                patternElement.toTextRepresentation(trysb2);
                                matchClause.setPatternTuple(patterns);

                                try {
                                    globalState.getState().logStatement("// Trying to remove label: "+trysb2);
                                    if (formerQueries.size() != 0)
                                        ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                    tryExecute(tryRemoveCopy, wrongResult);
                                } catch (Exception e) {
                                    globalState.getState().logStatement("// Removed failed, restore the backup");

                                    ((RelationIdentifier) patternElement).relationType = labels;
                                    patternElements.set(z, patternElement);
                                    patterns.set(j, pattern);
                                    matchClause.setPatternTuple(patterns);
                                    tryRemoveCopy.set(i, backupClauses.get(i).getCopy());
                                    clause = tryRemoveCopy.get(i);
                                    matchClause = (Match) clause;
                                    patterns = matchClause.getPatternTuple();
                                }
                                if (((RelationIdentifier) patternElement).relationType != labels) {
                                    globalState.getState().logStatement("// Removed succeeded");
                                    backupClauses.set(i, tryRemoveCopy.get(i).getCopy());
                                }
                            }
                        }
                    }





















































                }
            }


            else if (clause instanceof IWith) {

                With withClause = (With) clause;
                With backup = (With) withClause.getCopy();

                List<IRet> rets = withClause.getReturnList();
                if (rets.size() <= 1)
                    continue;

                for (int j = 0; j < rets.size(); j++) {
                    IRet ret = rets.get(j);

                    if (ret.getIdentifier() instanceof Alias)
                    {

                        boolean canRemove = true;
                        if (necessaryIdentifier.contains(ret.getIdentifier()))
                            continue;

                        IExpression expression = ret.getExpression();

                        if (expression instanceof GetPropertyExpression) {

                            Set<IIdentifier> reliedContent = ((GetPropertyExpression) expression).reliedContent();

                            for (IIdentifier identifier : reliedContent) {
                                if (necessaryIdentifier.contains(identifier)) {
                                    canRemove = false;
                                    break;
                                }
                            }
                        } else if (expression instanceof IdentifierExpression) {

                            IIdentifier identifier = ((IdentifierExpression) expression).getIdentifier();

                            if (necessaryIdentifier.contains(identifier)) {
                                canRemove = false;
                            }
                        } else if (expression instanceof ConstExpression) {

                        } else {
                            System.out.println("Unknown expression type: " + expression.getClass().getName());
                        }
                        if (canRemove) {

                            long originalSize = rets.size();
                            rets.remove(ret);
                            StringBuilder trysb = new StringBuilder();
                            ret.toTextRepresentation(trysb);

                            Set<IIdentifier> toBeRemoved = new HashSet<>();
                            toBeRemoved.add(ret.getIdentifier());
                            try {
                                globalState.getState().logStatement("// Trying to remove with: "+ trysb);
                                if (formerQueries.size() != 0)
                                    ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                tryRemoveCopy = tryRemove(tryRemoveCopy, toBeRemoved, i, wrongResult);
                            } catch (Exception e) {
                                globalState.getState().logStatement("// Removed failed, restore the backup");

                                for (int k = 0; k < tryRemoveCopy.size(); k++) {
                                    tryRemoveCopy.set(k, backupClauses.get(k).getCopy());
                                }
                                clause = tryRemoveCopy.get(i);
                                withClause = (With) clause;
                                rets = withClause.getReturnList();
                            }
                            if (rets.size() != originalSize) {
                                globalState.getState().logStatement("// Removed succeeded");
                                j--;
                                for (int k = 0; k < tryRemoveCopy.size(); k++) {
                                    backupClauses.set(k, tryRemoveCopy.get(k).getCopy());
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < tryRemoveCopy.size() - 1; i++) {
            ICypherClause clause = tryRemoveCopy.get(i);
            if (clause instanceof IMatch) {
                Match matchClause = (Match) clause;
                List<IPattern> patterns = matchClause.getPatternTuple();

                patterns = matchClause.getPatternTuple();
                for (int j = 0; j < patterns.size(); j++) {
                    IPattern pattern = patterns.get(j);

                    List<IPatternElement> patternElements = ((Pattern) pattern).patternElements;


                    for (int z = 0; z < patternElements.size(); z++) {
                        IPatternElement patternElement = patternElements.get(z);
                        ICypherType type = patternElement.getType();

                        if (type == CypherType.NODE) {


                            List<IProperty> properties = ((NodeIdentifier) patternElement).getProperties();
                            for (int k = 0; k < properties.size(); k++) {
                                IProperty prop = properties.get(k);

                                long originalSize = properties.size();
                                StringBuilder trysb2 = new StringBuilder();
                                properties.remove(prop);
                                patternElement.toTextRepresentation(trysb2);
                                matchClause.setPatternTuple(patterns);

                                try {
                                    globalState.getState().logStatement("// Trying to remove property: "+trysb2);
                                    if (formerQueries.size() != 0)
                                        ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                    tryExecute(tryRemoveCopy, wrongResult);
                                } catch (Exception e) {
                                    globalState.getState().logStatement("// Removed failed, restore the backup");

                                    properties.add(prop);
                                    patternElements.set(z, patternElement);
                                    patterns.set(j, pattern);
                                    matchClause.setPatternTuple(patterns);
                                    tryRemoveCopy.set(i, backupClauses.get(i).getCopy());
                                    clause = tryRemoveCopy.get(i);
                                    matchClause = (Match) clause;
                                    patterns = matchClause.getPatternTuple();
                                }
                                if (properties.size() != originalSize) {
                                    globalState.getState().logStatement("// Removed succeeded");
                                    k--;
                                    backupClauses.set(i, tryRemoveCopy.get(i).getCopy());
                                }
                            }
                        }
                    }
                }
            }

        }
        clauses = new ArrayList<>();
        for (int i = 0; i < tryRemoveCopy.size(); i++) {
            ((CypherClause) tryRemoveCopy.get(i)).updateProvideAndRequire();
            clauses.add(tryRemoveCopy.get(i).getCopy());
        }
        IClauseSequence sequence = new ClauseSequence(clauses);
        return sequence;
    }

    IClauseSequence changeReturnColumn(IClauseSequence original, List<GQSResultSet> oldResult)
    {
        IClauseSequence sequence = original.getCopy();
        List<ICypherClause> clauses = sequence.getClauseList();
        IReturn returnClause = (Return)(clauses.get(clauses.size()-1));
        List<IRet> replaced = new ArrayList<>();
        replaced.add(returnClause.getReturnList().get(0));
        String oldname = ((Alias)(replaced.get(0).getIdentifier())).getName();

        for(int i = 0; i < oldResult.get(0).result.size(); i++)
        {
            Map<String, Object> result = oldResult.get(0).result.get(i);
            if(result.containsKey(oldname))
            {
                Map<String, Object> newResult = new HashMap<>();
                newResult.put("a0", result.get(oldname));
                oldResult.set(0, new GQSResultSet(newResult));
            }
        }
        ((Alias)(replaced.get(0).getIdentifier())).setName("a0");
        returnClause.setReturnList(replaced);

        List<IExpression> orderBy = returnClause.getOrderByExpressions();
        List<IExpression> newOrderBy = new ArrayList<>();
        for(IExpression curO : orderBy)
        {
            if(curO instanceof Alias)
            {
                if(((Alias)curO).getName().equals(oldname))
                {
                    ((Alias)curO).setName("a0");
                    newOrderBy.add(curO);
                }
            }
        }
        if(newOrderBy.size()!=0)
            returnClause.setOrderBy(newOrderBy, returnClause.isOrderByDesc());
        else
            returnClause.setOrderBy(null, false);
        return sequence;
    }


    @Override
    public void check() throws Exception {

        IClauseSequence sequence = queryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);
        System.out.println(sb);
        List<GQSResultSet> results;
        long resultLength = 0;
        String thisTime = sb.toString();
        if(dbStatus == "")
        {
            dbStatus = globalState.getDatabaseName();
        }
        else if (!dbStatus.equals(globalState.getDatabaseName()))
        {
            dbStatus = globalState.getDatabaseName();
            lastTime = "";
            lastCorrectResult.clear();
        }

        byte[] branchCoverage = new byte[BRANCH_SIZE];
        byte[] branchPairCoverage = new byte[BRANCH_PAIR_SIZE];

        try {












            if(MainOptions.debug != -1 && !MainOptions.assistSkipMatch && MainOptions.skipMatch!= -1)
                return;

            List<GQSResultSet> correctResult = null;
            List<ICypherClause> clauses = sequence.getClauseList();
            try{
                 Return returnClause = (Return)(clauses.get(clauses.size()-1));
                 correctResult = returnClause.getActualResult();
            }
            catch (Exception e){
                System.out.println("！");
                System.out.println(e.getMessage());
            }
            try {
                results = globalState.executeStatementAndGet(new CypherQueryAdapter(thisTime));
            } catch (TimeoutException e)
            {
                results = new ArrayList<>();
                String msg = "";
                msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                msg = msg + "TimeoutException: " + e.getMessage() + "\n";
                throw new ResultMismatchException(msg);
            }
            catch(ClientException e)
            {
                String msg = "";
                msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                msg = msg + "DatabaseCrashed: " + e.getMessage() + "\n";

                throw new ResultMismatchException(msg);
            }


            if(globalState.getOptions().getCoverage_port() != 0) {
                try (Socket clientSocket = new Socket("127.0.0.1", globalState.getOptions().getCoverage_port())){
                    OutputStream os = clientSocket.getOutputStream();
                    InputStream is = clientSocket.getInputStream();
                    os.write(PRINT_MEM);
                    os.flush();


                    byte[] allBytes = is.readAllBytes();
                    for(int j = 0; j < BRANCH_PAIR_SIZE; j++){
                        branchPairCoverage[j] = allBytes[j];
                    }

                    long coveredBranch = 0;
                    for(int j = 0; j < BRANCH_SIZE; j++){
                        branchCoverage[j] = allBytes[j+BRANCH_PAIR_SIZE];
                        if(allBytes[j+BRANCH_PAIR_SIZE] != 0){
                            coveredBranch++;
                        }
                    }
                    System.out.println(""+coveredBranch+"\n");
                    clientSocket.shutdownInput();
                    clientSocket.shutdownOutput();
                }
            }

            boolean found = false;
            StringBuilder msgSb = new StringBuilder();
            if(correctResult == null)
            {
                throw new Exception("！");
            }
            if(results.size()!=0) {
                for (int i = 0; results != null && i < results.size(); i++) {

                    if (!results.get(i).compareWithOutOrder(correctResult.get(i))) {
                    if(true){
                        IClauseSequence minimized = null;
                        if (!found) {

                            List<String> formerQueries = new ArrayList<>();
                            List<GQSResultSet> repeatresults = null;
                            boolean consistent = false;
                            if(!MainOptions.exp.equals("coverage") && !MainOptions.exp.equals("ablation") && !MainOptions.exp.equals("pattern") && !MainOptions.exp.equals("expression")&& !MainOptions.exp.equals("both")) {
                                if (MainOptions.mode != "neo4j" && results.get(0).getResult().size() > 0 && !results.get(0).getResult().get(0).containsKey("Crash"))
                                    repeatresults = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));
                                else if (results.get(0).getResult().size() > 0 && results.get(0).getResult().get(0).containsKey("Crash")) {
                                    for (int j = 0; j < globalState.getState().statements.size() - 1; j++) {
                                        formerQueries.add(String.valueOf(globalState.getState().statements.get(j)));
                                    }
                                    ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                    repeatresults = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));
                                }




                                for (int j = 0; repeatresults != null && j < repeatresults.size(); j++) {
                                    if (!repeatresults.get(j).compareWithOutOrder(correctResult.get(j))) {
                                        consistent = true;
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                consistent = true;
                            }
                            if(consistent) {
                                msgSb.append("The contents of the result sets mismatch!\n");
                                if (!MainOptions.exp.equals("coverage") && !MainOptions.exp.equals("pattern") && !MainOptions.exp.equals("expression")&& !MainOptions.exp.equals("both"))
                                    minimized = minimizeTestCase(sequence.getClauseList(), results, formerQueries);
                                found = true;
                                String msg = "";
                                msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                                msg = msg + "Difference between " + (i) + " and " + i;
                                msg = msg + "Computed Result: " + correctResult.get(i).getRowNum() + " --- " + correctResult.get(i).resultToStringList() + "\n";
                                msg = msg + "Executed Result: " + results.get(i).getRowNum() + " --- " + results.get(i).resultToStringList() + "\n";
                                Set<Integer> nodeNumbers = new HashSet<>(), relationNumbers = new HashSet<>();
                                for (ICypherClause clause : sequence.getClauseList()) {
                                    if (clause instanceof Match) {
                                        Match c = (Match) clause;
                                        c.getPatternTuple().forEach(pattern -> {
                                            List<IPatternElement> ele = pattern.getPatternElements();
                                            for (IPatternElement e : ele) {
                                                if (e instanceof NodeIdentifier)
                                                    nodeNumbers.add(((NodeIdentifier) e).getActualNode().getId());
                                                else if (e instanceof RelationIdentifier)
                                                    relationNumbers.add(((RelationIdentifier) e).actualRelationship.getId());
                                            }
                                        });
                                    }
                                }
                                msg = msg + "Node numbers of whole clause:" + nodeNumbers + "\n";
                                msg = msg + "Relation numbers of whole clause:" + relationNumbers + "\n";
                                Set<Integer> minimizedNodeNumbers = new HashSet<>(), minimizedRelationNumbers = new HashSet<>();
                                if (minimized != null) {
                                    for (ICypherClause clause : minimized.getClauseList()) {
                                        if (clause instanceof Match) {
                                            Match c = (Match) clause;
                                            c.getPatternTuple().forEach(pattern -> {
                                                List<IPatternElement> ele = pattern.getPatternElements();
                                                for (IPatternElement e : ele) {
                                                    if (e instanceof NodeIdentifier)
                                                        minimizedNodeNumbers.add(((NodeIdentifier) e).getActualNode().getId());
                                                    else if (e instanceof RelationIdentifier)
                                                        minimizedRelationNumbers.add(((RelationIdentifier) e).actualRelationship.getId());
                                                }
                                            });
                                        }
                                    }
                                }
                                msg = msg + "Node numbers of mini clause:" + minimizedNodeNumbers + "\n";
                                msg = msg + "Relation numbers of mini clause:" + minimizedRelationNumbers + "\n";
                                sb = new StringBuilder();
                                if(minimized != null)
                                    minimized.toTextRepresentation(sb);

                                msg = msg + "Minimized query: " + sb + "\n";
                                msgSb.append(msg);
                            }
                        }
                    }






                }

                }
                if(lastTime.equals(""))
                {
                    IClauseSequence newSeq = changeReturnColumn(sequence, correctResult);
                    StringBuilder ss = new StringBuilder();
                    newSeq.toTextRepresentation(ss);
                    lastTime = ss.toString();
                    lastCorrectResult = new ArrayList<>();
                    lastCorrectResult.add(new GQSResultSet());
                    for(int i = 0; i < correctResult.get(0).result.size(); i++)
                    {
                        lastCorrectResult.get(0).result.add(correctResult.get(0).result.get(i));
                    }

                }
                else if (false)
                {
                    IClauseSequence thisSeq = changeReturnColumn(sequence, correctResult);
                    StringBuilder ss = new StringBuilder();
                    thisSeq.toTextRepresentation(ss);
                    String unionQuery = lastTime + " UNION ALL "+ss.toString();
                    List<GQSResultSet> unionResults = new ArrayList<>();
                    unionResults.add(new GQSResultSet());
                    unionResults.get(0).resultRowNum = 2;
                    for(int i = 0; i < lastCorrectResult.get(0).result.size(); i++)
                    {
                        unionResults.get(0).result.add(lastCorrectResult.get(0).result.get(i));
                    }
                    for(int i = 0; i < correctResult.get(0).result.size(); i++)
                    {
                        unionResults.get(0).result.add(correctResult.get(0).result.get(i));
                    }
                    try {
                        results = globalState.executeStatementAndGet(new CypherQueryAdapter(unionQuery));
                    } catch (TimeoutException e)
                    {
                        results = new ArrayList<>();
                        String msg = "";
                        msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                        msg = msg + "TimeoutException: " + e.getMessage() + "\n";
                        throw new ResultMismatchException(msg);
                    }
                    catch(ClientException e)
                    {
                        String msg = "";
                        msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                        msg = msg + "DatabaseCrashed: " + e.getMessage() + "\n";

                        throw new ResultMismatchException(msg);
                    }

                    if(results.size()!=0) {
                        for (int i = 0; results != null && i < results.size(); i++) {

                            if (!results.get(i).compareWithOutOrder(unionResults.get(i))) {
                                if(true){
                                    IClauseSequence minimized = null;
                                    if (!found) {

                                        List<String> formerQueries = new ArrayList<>();
                                        List<GQSResultSet> repeatresults = null;
                                        boolean consistent = false;
                                        if(!MainOptions.exp.equals("coverage") && !MainOptions.exp.equals("ablation") && !MainOptions.exp.equals("pattern") && !MainOptions.exp.equals("expression")&& !MainOptions.exp.equals("both")) {
                                            if (MainOptions.mode != "neo4j" && results.get(0).getResult().size() > 0 && !results.get(0).getResult().get(0).containsKey("Crash"))
                                                repeatresults = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));
                                            else if (results.get(0).getResult().size() > 0 && results.get(0).getResult().get(0).containsKey("Crash")) {
                                                for (int j = 0; j < globalState.getState().statements.size() - 1; j++) {
                                                    formerQueries.add(String.valueOf(globalState.getState().statements.get(j)));
                                                }
                                                ((CypherConnection) (globalState.getConnection())).reproduce(formerQueries);
                                                repeatresults = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));
                                            }




                                            for (int j = 0; repeatresults != null && j < repeatresults.size(); j++) {
                                                if (!repeatresults.get(j).compareWithOutOrder(correctResult.get(j))) {
                                                    consistent = true;
                                                    break;
                                                }
                                            }
                                        }
                                        else
                                        {
                                            consistent = true;
                                        }
                                        if(consistent) {
                                            msgSb.append("The contents of the result sets mismatch!\n");
                                            if (!MainOptions.exp.equals("coverage") && !MainOptions.exp.equals("pattern") && !MainOptions.exp.equals("expression")&& !MainOptions.exp.equals("both"))
                                                minimized = minimizeTestCase(sequence.getClauseList(), results, formerQueries);
                                            found = true;
                                            String msg = "";
                                            msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                                            msg = msg + "Difference between " + (i) + " and " + i;
                                            msg = msg + "Computed Result: " + correctResult.get(i).getRowNum() + " --- " + correctResult.get(i).resultToStringList() + "\n";
                                            msg = msg + "Executed Result: " + results.get(i).getRowNum() + " --- " + results.get(i).resultToStringList() + "\n";
                                            Set<Integer> nodeNumbers = new HashSet<>(), relationNumbers = new HashSet<>();
                                            for (ICypherClause clause : sequence.getClauseList()) {
                                                if (clause instanceof Match) {
                                                    Match c = (Match) clause;
                                                    c.getPatternTuple().forEach(pattern -> {
                                                        List<IPatternElement> ele = pattern.getPatternElements();
                                                        for (IPatternElement e : ele) {
                                                            if (e instanceof NodeIdentifier)
                                                                nodeNumbers.add(((NodeIdentifier) e).getActualNode().getId());
                                                            else if (e instanceof RelationIdentifier)
                                                                relationNumbers.add(((RelationIdentifier) e).actualRelationship.getId());
                                                        }
                                                    });
                                                }
                                            }
                                            msg = msg + "Node numbers of whole clause:" + nodeNumbers + "\n";
                                            msg = msg + "Relation numbers of whole clause:" + relationNumbers + "\n";
                                            Set<Integer> minimizedNodeNumbers = new HashSet<>(), minimizedRelationNumbers = new HashSet<>();
                                            if (minimized != null) {
                                                for (ICypherClause clause : minimized.getClauseList()) {
                                                    if (clause instanceof Match) {
                                                        Match c = (Match) clause;
                                                        c.getPatternTuple().forEach(pattern -> {
                                                            List<IPatternElement> ele = pattern.getPatternElements();
                                                            for (IPatternElement e : ele) {
                                                                if (e instanceof NodeIdentifier)
                                                                    minimizedNodeNumbers.add(((NodeIdentifier) e).getActualNode().getId());
                                                                else if (e instanceof RelationIdentifier)
                                                                    minimizedRelationNumbers.add(((RelationIdentifier) e).actualRelationship.getId());
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                            msg = msg + "Node numbers of mini clause:" + minimizedNodeNumbers + "\n";
                                            msg = msg + "Relation numbers of mini clause:" + minimizedRelationNumbers + "\n";
                                            sb = new StringBuilder();
                                            if(minimized != null)
                                                minimized.toTextRepresentation(sb);

                                            msg = msg + "Minimized query: " + sb + "\n";
                                            msgSb.append(msg);
                                        }
                                    }
                                }






                            }

                        }
                    }
                }
            }
            else
            {
                String minimized = "";
                if (!found) {

                    List<GQSResultSet> repeatresults = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString()));
                    if (repeatresults.size() == 0) {
                        msgSb.append("The contents of the result sets mismatch!\n");
                        if(!MainOptions.exp.equals("coverage") &&  !MainOptions.exp.equals("pattern")&& !MainOptions.exp.equals("expression")&& !MainOptions.exp.equals("both")) {
                            minimizeTestCase(sequence.getClauseList(), results, new ArrayList<>()).toTextRepresentation(sb);
                            minimized = sb.toString();
                        }
                        found = true;
                    }
                    String msg = "";
                    msg = msg + "seed:" + globalState.getRandomly().getSeed() + "\n";
                    msg = msg + "Empty result found!";
                    msg = msg + "Computed Result: ";
                    for (int i = 0; i < correctResult.size(); i++)
                        msg = msg + correctResult.get(i).getRowNum() + " --- " + correctResult.get(i).resultToStringList() + "\n";
                    msg = msg + "Executed Result: EMPTY \n";
                    msg = msg + "Minimized query: " + minimized + "\n";
                    msgSb.append(msg);
                    if (minimized.contains("WHERE true") && MainOptions.mode=="memgraph") {

                        String sub = new String(minimized.replace("WHERE true", " "));
                        StringBuilder tryWhere = new StringBuilder(sub);
                        List<GQSResultSet> tryWhereResult = globalState.executeStatementAndGet(new CypherQueryAdapter(tryWhere.toString()));
                        if (tryWhereResult.size() == 0) {
                        } else if (tryWhereResult.size() != 0) {
                            boolean consistent = true;
                            if (tryWhereResult.size() != correctResult.size())
                                consistent = false;
                            else {
                                for (int i = 0; i < tryWhereResult.size(); i++) {
                                    if (!correctResult.get(i).compareWithOutOrder(correctResult.get(i))) {
                                        consistent = false;
                                    }
                                }
                            }
                            if (consistent) {
                                msgSb.append("KNOWN BUG: This is strongly related to WHERE true\n");
                            }
                        }
                    }
                }
            }

            if(found){
                throw new ResultMismatchException(msgSb.toString()+"seed: "+globalState.getRandomly().getSeed());
            }
            if(results != null)
                resultLength = results.get(0).getRowNum();
            else
                resultLength = 0;
        } catch (CompletionException e) {
            System.out.println("CypherGremlin！");
            System.out.println(e.getMessage());
        }
        boolean isBugDetected = false;


        List<CypherSchema.CypherLabelInfo> labels = globalState.getSchema().getLabels();
        List<CypherSchema.CypherRelationTypeInfo> relations = globalState.getSchema().getRelationTypes();

        //queryGenerator.addNewRecord(sequence, isBugDetected, resultLength, branchCoverage, branchPairCoverage);
    }
}
