package org.example.gqs.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DBMSCommon {

    private static final Pattern SQLANCER_INDEX_PATTERN = Pattern.compile("^i\\d+");

    private DBMSCommon() {
    }

    public static String createTableName(long nr) {
        return String.format("t%d", nr);
    }

    public static String createColumnName(long nr) {
        return String.format("c%d", nr);
    }

    public static String createIndexName(long nr) {
        return String.format("i%d", nr);
    }

    public static boolean matchesIndexName(String indexName) {
        Matcher matcher = SQLANCER_INDEX_PATTERN.matcher(indexName);
        return matcher.matches();
    }

}
