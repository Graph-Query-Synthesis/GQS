package org.example.gqs.common.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExpectedErrors {

    private final Set<String> errors = new HashSet<>();

    public ExpectedErrors add(String error) {
        if (error == null) {
            throw new IllegalArgumentException();
        }
        errors.add(error);
        return this;
    }

    public boolean errorIsExpected(String error) {
        if (error == null) {
            throw new IllegalArgumentException();
        }
        for (String s : errors) {
            if (error.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public ExpectedErrors addAll(Collection<String> list) {
        errors.addAll(list);
        return this;
    }

    public static ExpectedErrors from(String... errors) {
        ExpectedErrors expectedErrors = new ExpectedErrors();
        for (String error : errors) {
            expectedErrors.add(error);
        }
        return expectedErrors;
    }

}
