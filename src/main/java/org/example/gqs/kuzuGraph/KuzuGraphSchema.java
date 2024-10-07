package org.example.gqs.kuzuGraph;

import org.example.gqs.common.schema.AbstractTable;
import org.example.gqs.common.schema.AbstractTableColumn;
import org.example.gqs.common.schema.TableIndex;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IFunctionInfo;
import org.example.gqs.cypher.schema.IParamInfo;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.CypherTypeDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KuzuGraphSchema extends CypherSchema<KuzuGraphGlobalState, KuzuGraphSchema.KuzuGraphTable> {


    public static KuzuGraphSchema createEmptyNewSchema(){
        return new KuzuGraphSchema(new ArrayList<KuzuGraphTable>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public KuzuGraphSchema(List<KuzuGraphTable> databaseTables, List<CypherLabelInfo> labels,
                           List<CypherRelationTypeInfo> relationTypes, List<CypherPatternInfo> patternInfos) {
        super(databaseTables, labels, relationTypes, patternInfos);
    }

    @Override
    public List<IFunctionInfo> getFunctions() {
        return Arrays.asList(MemGraphBuiltInFunctions.values());
    }


    public enum MemGraphBuiltInFunctions implements IFunctionInfo{
        AVG("avg", "avg@number", CypherType.NUMBER, new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        MAX_NUMBER("max", "max@number", CypherType.NUMBER, new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        MAX_STRING("max", "max@string", CypherType.STRING, new CypherParamInfo(CypherType.STRING, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.STRING);
            }
        },
        MIN_NUMBER("min", "min@number", CypherType.NUMBER, new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        MIN_STRING("min", "min@string", CypherType.STRING, new CypherParamInfo(CypherType.STRING, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.STRING);
            }
        },
        PERCENTILE_COUNT_NUMBER("percentileCount", "percentileCount@number", CypherType.NUMBER,
                new CypherParamInfo(CypherType.NUMBER, false), new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        PERCENTILE_COUNT_STRING("percentileCount", "percentileCount@string", CypherType.NUMBER,
                new CypherParamInfo(CypherType.STRING, false), new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        PERCENTILE_DISC_STRING("percentileDisc", "percentileDisct@string", CypherType.NUMBER,
                new CypherParamInfo(CypherType.STRING, false), new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        SUM("sum", "sum", CypherType.NUMBER, new CypherParamInfo(CypherType.NUMBER, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.NUMBER);
            }
        },
        COLLECT("collect", "collect", CypherType.LIST, new CypherParamInfo(CypherType.ANY, false)){
            @Override
            public ICypherTypeDescriptor calculateReturnType(List<IExpression> params) {
                return new CypherTypeDescriptor(CypherType.UNKNOWN);
            }
        }
        ;

        MemGraphBuiltInFunctions(String name, String signature, CypherType expectedReturnType, IParamInfo... params){
            this.name = name;
            this.params = Arrays.asList(params);
            this.expectedReturnType = expectedReturnType;
            this.signature = signature;
        }

        private String name, signature;
        private List<IParamInfo> params;
        private CypherType expectedReturnType;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getSignature() {
            return signature;
        }

        @Override
        public List<IParamInfo> getParams() {
            return params;
        }

        @Override
        public CypherType getExpectedReturnType() {
            return expectedReturnType;
        }
    }

    public enum KuzuGraphDataType {

    }
    public static class KuzuGraphTable extends AbstractTable<KuzuGraphTableColumn, TableIndex, KuzuGraphGlobalState> {

        public KuzuGraphTable(String name, List<KuzuGraphTableColumn> columns, List<TableIndex> indexes, boolean isView) {
            super(name, columns, indexes, isView);
        }

        @Override
        public long getNrRows(KuzuGraphGlobalState globalState) {
            return 0;
        }
    }

    public static class KuzuGraphTableColumn extends AbstractTableColumn<KuzuGraphTable, KuzuGraphDataType> {
        public KuzuGraphTableColumn(String name, KuzuGraphTable table, KuzuGraphDataType type) {
            super(name, table, type);
        }
    }
}
