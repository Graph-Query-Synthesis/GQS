package org.example.gqs.cypher.gen;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPatternElementInfo;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.exceptions.MustRestartDatabaseException;
import org.javatuples.Pair;

import java.util.*;

public abstract class CypherSchemaGenerator <S extends CypherSchema<G,?>, G extends CypherGlobalState<?, S>>{

    protected G globalState;
    protected List<CypherSchema.CypherLabelInfo> labels = new ArrayList<>();
    protected List<CypherSchema.CypherRelationTypeInfo> relationTypes = new ArrayList<>();
    protected List<CypherSchema.CypherPatternInfo> patternInfos = new ArrayList<>();
    public Map<Pair<String, String>, Set<CypherSchema.CypherRelationTypeInfo>> relationTypeMap = new HashMap<>();

    public CypherSchemaGenerator(G globalState){
        this.globalState = globalState;
    }

    public S generateSchema() {
        Randomly r = new Randomly();
        long numOfLabels = r.getInteger((long) (3), (long) (globalState.getOptions().getLabelNum()));

        long numOfRelationTypes = r.getInteger(numOfLabels * 3, numOfLabels * 4);
        long numOfPatternInfos = 4;
        long indexOfProperty = 0;

        for (int i = 0; i < numOfLabels; i++) {
            long numOfProperties = r.getInteger(5, 8);
            List<IPropertyInfo> properties = new ArrayList<>();
            for (int j = 0; j < numOfProperties; j++) {
                String key = "k" + indexOfProperty;
                CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN);
                boolean isOptional = Randomly.getBoolean();
                CypherSchema.CypherPropertyInfo p = new CypherSchema.CypherPropertyInfo(key, type, isOptional);
                properties.add(p);
                indexOfProperty++;
            }
            CypherSchema.CypherPropertyInfo m = new CypherSchema.CypherPropertyInfo("id", CypherType.NUMBER, false);
            properties.add(m);
            String name = "L" + i;
            CypherSchema.CypherLabelInfo t = new CypherSchema.CypherLabelInfo(name, properties);
            labels.add(t);
        }

        for (int i = 0; i < numOfRelationTypes; i++) {
            long numOfProperties = r.getInteger(5, 8);
            List<IPropertyInfo> properties = new ArrayList<>();
            for (int j = 0; j < numOfProperties; j++) {
                String key = "k" + indexOfProperty;
                CypherType type = Randomly.fromOptions(CypherType.NUMBER, CypherType.STRING, CypherType.BOOLEAN);
                boolean isOptional = Randomly.getBoolean();
                CypherSchema.CypherPropertyInfo p = new CypherSchema.CypherPropertyInfo(key, type, isOptional);
                properties.add(p);
                indexOfProperty++;
            }
            String name = "T" + i;
            CypherSchema.CypherLabelInfo from, to;
            CypherSchema.CypherPropertyInfo m = new CypherSchema.CypherPropertyInfo("id", CypherType.NUMBER, false);
            properties.add(m);
            CypherSchema.CypherRelationTypeInfo re = new CypherSchema.CypherRelationTypeInfo(name, properties);
            relationTypes.add(re);
        }

        for (int i = 0; i < numOfPatternInfos; i++) {
            List<IPatternElementInfo> patternElementInfos = new ArrayList<>();

            long index = r.getInteger(0, numOfLabels - 1);
            CypherSchema.CypherLabelInfo tLeft = labels.get((int) index);
            index = r.getInteger(0, numOfRelationTypes - 1);
            CypherSchema.CypherRelationTypeInfo re = relationTypes.get((int) index);
            index = r.getInteger(0, numOfLabels - 1);
            CypherSchema.CypherLabelInfo tRight = labels.get((int) index);

            patternElementInfos.add(tLeft);
            patternElementInfos.add(re);
            patternElementInfos.add(tRight);

            CypherSchema.CypherPatternInfo pi = new CypherSchema.CypherPatternInfo(patternElementInfos);
            patternInfos.add(pi);
        }
        if (MainOptions.mode == "kuzu") {
            for (int i = 0; i < labels.size(); i++) {
                String createLabel = "CREATE NODE TABLE ";
                createLabel = createLabel + labels.get(i).getName() + " (";
                for (int j = 0; j < labels.get(i).getProperties().size(); j++) {
                    createLabel = createLabel + labels.get(i).getProperties().get(j).getKey() + " ";
                    if (labels.get(i).getProperties().get(j).getType() == CypherType.STRING) {
                        createLabel = createLabel + "STRING";
                    } else if (labels.get(i).getProperties().get(j).getType() == CypherType.NUMBER) {
                        createLabel = createLabel + "INT64";
                    } else if (labels.get(i).getProperties().get(j).getType() == CypherType.BOOLEAN) {
                        createLabel = createLabel + "BOOLEAN";
                    }
                    createLabel = createLabel + ", ";
                }
                createLabel = createLabel + "PRIMARY KEY (id));";
                try {
                    globalState.executeStatement(new CypherQueryAdapter(createLabel));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < relationTypes.size(); i++) {
                String createRelation = "CREATE REL TABLE ";
                createRelation = createRelation + relationTypes.get(i).getName() + " (";
                String fromto = new String();
                for (int j = 0; j < 1; j++) {
                    CypherSchema.CypherLabelInfo from = labels.get(r.getInteger(0, labels.size() - 1));
                    CypherSchema.CypherLabelInfo to = labels.get(r.getInteger(0, labels.size() - 1));
                    fromto = fromto + "FROM " + from.getName() + " TO " + to.getName() + ", ";
                    relationTypes.get(i).fromto.add(new Pair<>(from.getName(), to.getName()));
                }
                createRelation = createRelation + fromto;
                for (int j = 0; j < relationTypes.get(i).getProperties().size(); j++) {
                    createRelation = createRelation + relationTypes.get(i).getProperties().get(j).getKey() + " ";
                    if (relationTypes.get(i).getProperties().get(j).getType() == CypherType.STRING) {
                        createRelation = createRelation + "STRING";
                    } else if (relationTypes.get(i).getProperties().get(j).getType() == CypherType.NUMBER) {
                        createRelation = createRelation + "INT64";
                    } else if (relationTypes.get(i).getProperties().get(j).getType() == CypherType.BOOLEAN) {
                        createRelation = createRelation + "BOOLEAN";
                    }
                    if (j != relationTypes.get(i).getProperties().size() - 1)
                        createRelation = createRelation + ", ";
                }
                createRelation = createRelation + ")";
                try {
                    globalState.executeStatement(new CypherQueryAdapter(createRelation));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        long cnt = 0;
        for (int i = 0; i < labels.size(); i++) {
            for (int j = 0; j < labels.get(i).getProperties().size(); j++) {
                if (labels.get(i).getProperties().get(j).isOptional() || (labels.get(i).getProperties().get(j).getType() != CypherType.STRING && labels.get(i).getProperties().get(j).getType() != CypherType.NUMBER))
                    continue;
                CypherSchema.CypherLabelInfo n = labels.get(i);
                IPropertyInfo p = n.getProperties().get(j);
                if (MainOptions.mode == "neo4j") {
                    if (Randomly.getBoolean()) {
                        String createConstraint = "CREATE CONSTRAINT c" + cnt;
                        cnt++;
                        createConstraint += " IF NOT EXISTS FOR (n:";
                        n = labels.get(i);
                        createConstraint = createConstraint + n.getName() + ") REQUIRE (n.";
                        p = n.getProperties().get(j);
                        createConstraint = createConstraint + p.getKey() + ") IS UNIQUE";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (MainOptions.mode == "memgraph") {
                    if (Randomly.getBoolean()) {
                        String createConstraint = "CREATE CONSTRAINT ";
                        createConstraint += "ON (n:";
                        n = labels.get(i);
                        createConstraint = createConstraint + n.getName() + ") ASSERT n.";
                        p = n.getProperties().get(j);
                        createConstraint = createConstraint + p.getKey() + " IS UNIQUE";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (MainOptions.mode == "thinker") {
                } else if (MainOptions.mode == "falkordb") {
                } else if (MainOptions.mode == "kuzu") {
                } else {
                    throw new RuntimeException("undefined mode!");
                }
            }
        }
        for (int i = 0; i < labels.size(); i++) {
            for (int j = 0; j < labels.get(i).getProperties().size(); j++) {
                if (labels.get(i).getProperties().get(j).isOptional() || (labels.get(i).getProperties().get(j).getType() != CypherType.STRING && labels.get(i).getProperties().get(j).getType() != CypherType.NUMBER))
                    continue;
                CypherSchema.CypherLabelInfo n = labels.get(i);
                IPropertyInfo p = n.getProperties().get(j);
                if (MainOptions.mode == "neo4j") {

                    String createIndex = "CREATE INDEX i" + cnt;
                    cnt++;
                    createIndex += " IF NOT EXISTS FOR (n:";

                    createIndex = createIndex + n.getName() + ") ON (n.";

                    createIndex = createIndex + p.getKey() + ")";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createIndex));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    n.indexedProp.add(p.getKey());
                    if (labels.get(i).getProperties().get(j).getType() == CypherType.STRING) {
                        createIndex = "CREATE TEXT INDEX i" + cnt;
                        cnt++;
                        createIndex += " IF NOT EXISTS FOR (n:";
                        n = labels.get(i);
                        createIndex = createIndex + n.getName() + ") ON (n.";
                        p = n.getProperties().get(j);
                        createIndex = createIndex + p.getKey() + ")";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createIndex));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                } else if (MainOptions.mode == "memgraph" || MainOptions.mode == "falkordb") {
                    String createIndex = "CREATE INDEX ";
                    cnt++;
                    createIndex += "ON :";
                    n = labels.get(i);
                    createIndex = createIndex + n.getName() + "(";
                    p = n.getProperties().get(j);
                    createIndex = createIndex + p.getKey() + ")";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createIndex));
                    } catch (MustRestartDatabaseException e) {
                        throw e;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    n.indexedProp.add(p.getKey());

                } else if (MainOptions.mode == "thinker") {
                } else if (MainOptions.mode == "kuzu") {
                } else {
                    throw new RuntimeException("undefined mode!");
                }
            }
        }
        for (int i = 0; i < labels.size(); i++) {
            if (MainOptions.mode != "memgraph" && Randomly.getBoolean()) {
                long numOfProperties = r.getInteger(1, labels.get(i).getProperties().size());
                Set<String> propertyKeys = new HashSet<>();
                for (int j = 0; j < numOfProperties; j++) {
                    long index = r.getInteger(0, labels.get(i).getProperties().size() - 1);
                    if (labels.get(i).getProperties().get((int) index).isOptional() || (labels.get(i).getProperties().get((int) index).getType() != CypherType.STRING && labels.get(i).getProperties().get((int) index).getType() != CypherType.NUMBER))
                        continue;
                    propertyKeys.add(labels.get(i).getProperties().get((int) index).getKey());
                }
                if (propertyKeys.size() <= 1)
                    continue;
                if (MainOptions.mode == "neo4j") {
                    String createConstraint = "CREATE CONSTRAINT c" + cnt;
                    cnt++;
                    createConstraint += " IF NOT EXISTS FOR (n:";
                    CypherSchema.CypherLabelInfo n = labels.get(i);
                    createConstraint = createConstraint + n.getName() + ") REQUIRE (";
                    for (String key : propertyKeys) {
                        createConstraint = createConstraint + "n." + key + ", ";
                    }
                    createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                    createConstraint = createConstraint + ") IS UNIQUE";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (MainOptions.mode == "memgraph") {
                    String createConstraint = "CREATE CONSTRAINT ";
                    createConstraint += "ON (n:";
                    CypherSchema.CypherLabelInfo n = labels.get(i);
                    createConstraint = createConstraint + n.getName() + ") ASSERT ";
                    for (String key : propertyKeys) {
                        createConstraint = createConstraint + "n." + key + ", ";
                    }
                    createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                    createConstraint = createConstraint + ") IS UNIQUE";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (MainOptions.mode == "thinker" || MainOptions.mode == "falkordb") {
                } else if (MainOptions.mode == "kuzu") {
                } else {
                    throw new RuntimeException("undefined mode!");
                }
            } else {
                long numOfProperties = r.getInteger(1, labels.get(i).getProperties().size());
                Set<String> propertyKeys = new HashSet<>();
                for (int j = 0; j < numOfProperties; j++) {
                    long index = r.getInteger(0, labels.get(i).getProperties().size() - 1);
                    propertyKeys.add(labels.get(i).getProperties().get((int) index).getKey());
                }
                if (propertyKeys.size() <= 1)
                    continue;
                if (MainOptions.mode == "neo4j") {
                    String createConstraint = "CREATE INDEX i" + cnt;
                    cnt++;
                    createConstraint += " IF NOT EXISTS FOR (n:";
                    CypherSchema.CypherLabelInfo n = labels.get(i);
                    createConstraint = createConstraint + n.getName() + ") ON (";
                    for (String key : propertyKeys) {
                        createConstraint = createConstraint + "n." + key + ", ";
                    }
                    createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                    createConstraint = createConstraint + ")";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (MainOptions.mode == "memgraph") {
                } else if (MainOptions.mode == "thinker") {
                } else if (MainOptions.mode == "falkordb") {
                } else if (MainOptions.mode == "kuzu") {
                } else {
                    throw new RuntimeException("undefined mode!");
                }
            }
        }
        for (int i = 0; i < relationTypes.size(); i++) {
            for (int j = 0; j < relationTypes.get(i).getProperties().size(); j++) {
                if (relationTypes.get(i).getProperties().get(j).isOptional() || (relationTypes.get(i).getProperties().get(j).getType() != CypherType.STRING && relationTypes.get(i).getProperties().get(j).getType() != CypherType.NUMBER))
                    continue;
                CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                IPropertyInfo p = n.getProperties().get(j);
                if (MainOptions.mode == "neo4j") {
                    if (Randomly.getBoolean()) {
                        String createConstraint = "CREATE CONSTRAINT c" + cnt;
                        cnt++;
                        createConstraint += " IF NOT EXISTS FOR (r:";
                        n = relationTypes.get(i);
                        createConstraint = createConstraint + n.getName() + ") REQUIRE (r.";
                        p = n.getProperties().get(j);
                        createConstraint = createConstraint + p.getKey() + ") IS UNIQUE";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        n.indexedProp.add(p.getKey());
                    }
                } else if (MainOptions.mode == "memgraph") {
                    if (Randomly.getBoolean()) {
                        String createConstraint = "CREATE CONSTRAINT ";
                        createConstraint += "ON (r:";
                        n = relationTypes.get(i);
                        createConstraint = createConstraint + n.getName() + ") ASSERT r.";
                        p = n.getProperties().get(j);
                        createConstraint = createConstraint + p.getKey() + " IS UNIQUE";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        n.indexedProp.add(p.getKey());
                    }
                }
            }
        }
        for (int i = 0; i < relationTypes.size(); i++) {
            for (int j = 0; j < relationTypes.get(i).getProperties().size(); j++) {
                if (relationTypes.get(i).getProperties().get(j).isOptional() || (relationTypes.get(i).getProperties().get(j).getType() != CypherType.STRING && relationTypes.get(i).getProperties().get(j).getType() != CypherType.NUMBER))
                    continue;
                if (MainOptions.mode == "neo4j") {
                    String createIndex = "CREATE INDEX i" + cnt;
                    cnt++;
                    createIndex += " IF NOT EXISTS FOR (n:";
                    CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                    createIndex = createIndex + n.getName() + ") ON (n.";
                    IPropertyInfo p = n.getProperties().get(j);
                    createIndex = createIndex + p.getKey() + ")";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createIndex));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (relationTypes.get(i).getProperties().get(j).getType() == CypherType.STRING) {
                        createIndex = "CREATE TEXT INDEX i" + cnt;
                        cnt++;
                        createIndex += " IF NOT EXISTS FOR (n:";
                        n = relationTypes.get(i);
                        createIndex = createIndex + n.getName() + ") ON (n.";
                        p = n.getProperties().get(j);
                        createIndex = createIndex + p.getKey() + ")";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createIndex));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (MainOptions.mode == "memgraph") {
                    String createIndex = "CREATE INDEX ";
                    cnt++;
                    createIndex += "ON :";
                    CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                    createIndex = createIndex + n.getName() + "(";
                    IPropertyInfo p = n.getProperties().get(j);
                    createIndex = createIndex + p.getKey() + ")";
                    try {
                        globalState.executeStatement(new CypherQueryAdapter(createIndex));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (MainOptions.mode != "memgraph") {
            for (int i = 0; i < relationTypes.size(); i++) {
                if (Randomly.getBoolean()) {
                    long numOfProperties = r.getInteger(1, relationTypes.get(i).getProperties().size());
                    Set<String> propertyKeys = new HashSet<>();
                    for (int j = 0; j < numOfProperties; j++) {
                        long index = r.getInteger(0, relationTypes.get(i).getProperties().size() - 1);
                        if (relationTypes.get(i).getProperties().get((int) index).isOptional() || (relationTypes.get(i).getProperties().get((int) index).getType() != CypherType.STRING && relationTypes.get(i).getProperties().get((int) index).getType() != CypherType.NUMBER))
                            continue;
                        propertyKeys.add(relationTypes.get(i).getProperties().get((int) index).getKey());
                    }
                    if (propertyKeys.size() <= 1)
                        continue;
                    if (MainOptions.mode == "neo4j") {
                        String createConstraint = "CREATE CONSTRAINT c" + cnt;
                        cnt++;
                        createConstraint += " IF NOT EXISTS FOR (r:";
                        CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                        createConstraint = createConstraint + n.getName() + ") REQUIRE (";
                        for (String key : propertyKeys) {
                            createConstraint = createConstraint + "r." + key + ", ";
                        }
                        createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                        createConstraint = createConstraint + ") IS UNIQUE";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (MainOptions.mode == "memgraph") {
                        String createConstraint = "CREATE CONSTRAINT ";
                        createConstraint += "ON (n:";
                        CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                        createConstraint = createConstraint + n.getName() + ") ASSERT ";
                        for (String key : propertyKeys) {
                            createConstraint = createConstraint + "r." + key + ", ";
                        }
                        createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                        createConstraint = createConstraint + ") IS UNIQUE";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (MainOptions.mode == "thinker" || MainOptions.mode == "falkordb") {
                    } else if (MainOptions.mode == "kuzu") {
                    } else {
                        throw new RuntimeException("undefined mode!");
                    }
                } else {
                    long numOfProperties = r.getInteger(1, relationTypes.get(i).getProperties().size());
                    Set<String> propertyKeys = new HashSet<>();
                    for (int j = 0; j < numOfProperties; j++) {
                        long index = r.getInteger(0, relationTypes.get(i).getProperties().size() - 1);
                        propertyKeys.add(relationTypes.get(i).getProperties().get((int) index).getKey());
                    }
                    if (propertyKeys.size() <= 1)
                        continue;
                    if (MainOptions.mode == "neo4j") {
                        String createConstraint = "CREATE INDEX i" + cnt;
                        cnt++;
                        createConstraint += " IF NOT EXISTS FOR (r:";
                        CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                        createConstraint = createConstraint + n.getName() + ") ON (";
                        for (String key : propertyKeys) {
                            createConstraint = createConstraint + "r." + key + ", ";
                        }
                        createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                        createConstraint = createConstraint + ")";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (MainOptions.mode == "memgraph") {
                        String createConstraint = "CREATE INDEX ";
                        createConstraint += "ON :";
                        CypherSchema.CypherRelationTypeInfo n = relationTypes.get(i);
                        createConstraint = createConstraint + n.getName() + "(";
                        for (String key : propertyKeys) {
                            createConstraint = createConstraint + "r." + key + ", ";
                        }
                        createConstraint = createConstraint.substring(0, createConstraint.length() - 2);
                        createConstraint = createConstraint + ")";
                        try {
                            globalState.executeStatement(new CypherQueryAdapter(createConstraint));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (MainOptions.mode == "thinker" || MainOptions.mode == "falkordb") {
                    } else if (MainOptions.mode == "kuzu") {
                    } else {
                        throw new RuntimeException("undefined mode!");
                    }
                }
            }
        }


        return generateSchemaObject(globalState, labels, relationTypes, patternInfos);
    }

    public abstract S generateSchemaObject(G globalState,
            List<CypherSchema.CypherLabelInfo> labels,
            List<CypherSchema.CypherRelationTypeInfo> relationTypes,
            List<CypherSchema.CypherPatternInfo> patternInfos);
}
