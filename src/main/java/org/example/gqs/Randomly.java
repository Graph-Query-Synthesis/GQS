package org.example.gqs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public final class Randomly {

    private static StringGenerationStrategy stringGenerationStrategy = StringGenerationStrategy.SOPHISTICATED;
    private static long maxStringLength = 10;
    private static boolean useCaching = true;
    private static long cacheSize = 100;

    private final List<Long> cachedLongs = new ArrayList<>();
    private final List<String> cachedStrings = new ArrayList<>();
    private final List<Double> cachedDoubles = new ArrayList<>();
    private final List<byte[]> cachedBytes = new ArrayList<>();
    private Supplier<String> provider;
    public static ThreadLocal<Integer> cnt = new ThreadLocal<>();

    private static final ThreadLocal<Random> THREAD_RANDOM = new ThreadLocal<>();
    public static ThreadLocal<Long> THREAD_SEED = new ThreadLocal<>();

    private void addToCache(long val) {
        if (useCaching && cachedLongs.size() < cacheSize && !cachedLongs.contains(val)) {
            cachedLongs.add(val);
        }
    }

    private void addToCache(double val) {
        if (useCaching && cachedDoubles.size() < cacheSize && !cachedDoubles.contains(val)) {
            cachedDoubles.add(val);
        }
    }

    private void addToCache(String val) {
        if (useCaching && cachedStrings.size() < cacheSize && !cachedStrings.contains(val)) {
            cachedStrings.add(val);
        }
    }

    private Long getFromLongCache() {
        if (!useCaching || cachedLongs.isEmpty()) {
            return null;
        } else {
            if (MainOptions.mode == "falkordb")
                return Randomly.fromList(cachedLongs) % 128;
            return Randomly.fromList(cachedLongs);
        }
    }

    private Double getFromDoubleCache() {
        double value = 0;
        if (!useCaching) {
            return null;
        }
        if (Randomly.getBoolean() && !cachedLongs.isEmpty()) {
            value = (double) Randomly.fromList(cachedLongs);
        } else if (!cachedDoubles.isEmpty()) {
            value = Randomly.fromList(cachedDoubles);
        } else {
            return null;
        }
        if (MainOptions.mode == "falkordb")
            return value % 128;
        return value;
    }

    private String getFromStringCache() {
        if (!useCaching) {
            return null;
        }
        if (Randomly.getBoolean() && !cachedLongs.isEmpty()) {
            return String.valueOf(Randomly.fromList(cachedLongs));
        } else if (Randomly.getBoolean() && !cachedDoubles.isEmpty()) {
            return String.valueOf(Randomly.fromList(cachedDoubles));
        } else if (Randomly.getBoolean() && !cachedBytes.isEmpty()
                && stringGenerationStrategy == StringGenerationStrategy.SOPHISTICATED) {
            return new String(Randomly.fromList(cachedBytes));
        } else if (!cachedStrings.isEmpty()) {
            String randomString = Randomly.fromList(cachedStrings);
            if (Randomly.getBoolean()) {
                return randomString;
            } else {
                return stringGenerationStrategy.transformCachedString(this, randomString);
            }
        } else {
            return null;
        }
    }

    private static boolean cacheProbability() {
        return useCaching && getNextLong(0, 3) == 1;
    }

    public static <T> T fromList(List<T> list) {
        return list.get((int) getNextLong(0, list.size()));
    }

    @SafeVarargs
    public static <T> T fromOptions(T... options) {
        return options[(int) getNextInt(0, options.length)];
    }

    @SafeVarargs
    public static <T> List<T> nonEmptySubset(T... options) {
        long nr = 1 + getNextInt(0, options.length);
        return extractNrRandomColumns(Arrays.asList(options), nr);
    }

    public static <T> List<T> nonEmptySubset(List<T> columns) {
        long nr = 1 + getNextInt(0, columns.size());
        return nonEmptySubset(columns, nr);
    }

    public static <T> List<T> nonEmptySubset(List<T> columns, long nr) {
        if (nr > columns.size()) {
            throw new AssertionError(columns + " " + nr);
        }
        return extractNrRandomColumns(columns, nr);
    }

    public static <T> List<T> nonEmptySubsetPotentialDuplicates(List<T> columns) {
        List<T> arr = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            arr.add(Randomly.fromList(columns));
        }
        return arr;
    }

    public static <T> List<T> subset(List<T> columns) {
        long nr = getNextInt(0, columns.size() + 1);
        return extractNrRandomColumns(columns, nr);
    }

    public static <T> List<T> subset(long nr, @SuppressWarnings("unchecked") T... values) {
        List<T> list = new ArrayList<>();
        for (T val : values) {
            list.add(val);
        }
        return extractNrRandomColumns(list, nr);
    }

    public static <T> List<T> subset(@SuppressWarnings("unchecked") T... values) {
        List<T> list = new ArrayList<>();
        for (T val : values) {
            list.add(val);
        }
        return subset(list);
    }

    public static <T> List<T> extractNrRandomColumns(List<T> columns, long nr) {
        assert nr >= 0;
        List<T> selectedColumns = new ArrayList<>();
        List<T> remainingColumns = new ArrayList<>(columns);
        for (int i = 0; i < nr; i++) {
            selectedColumns.add(remainingColumns.remove(getNextInt(0, remainingColumns.size())));
        }
        return selectedColumns;
    }

    public static long smallNumber() {
        return (long) (Math.abs(getThreadRandom().get().nextGaussian()) * 2);
    }

    public static boolean getBoolean() {
        return getThreadRandom().get().nextBoolean();
    }

    private static ThreadLocal<Random> getThreadRandom() {
        if (THREAD_RANDOM.get() == null) {
            try {
                throw new Exception("Why are you calling getThreadRandom before I am ready??");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return THREAD_RANDOM;
    }

    public long getInteger() {
        if (MainOptions.mode == "falkordb")
            return getThreadRandom().get().nextInt() % 128;
        if (smallBiasProbability()) {
            long value = Randomly.fromOptions(-1L, Long.MAX_VALUE - 1L, Long.MIN_VALUE + 1L, 1L, 0L);
            return value;
        } else {
            if (cacheProbability()) {
                Long l = getFromLongCache();
                if (l != null) {
                    return l;
                }
            }
            long nextLong = -1;
            nextLong = getThreadRandom().get().nextInt();
            addToCache(nextLong);
            return nextLong;
        }
    }

    public enum StringGenerationStrategy {

        NUMERIC {
            @Override
            public String getString(Randomly r) {
                return getStringOfAlphabet(r, NUMERIC_ALPHABET);
            }

        },
        ALPHANUMERIC {
            @Override
            public String getString(Randomly r) {
                return getStringOfAlphabet(r, ALPHANUMERIC_ALPHABET);

            }

        },
        ALPHANUMERIC_SPECIALCHAR {
            @Override
            public String getString(Randomly r) {
                return getStringOfAlphabet(r, ALPHANUMERIC_SPECIALCHAR_ALPHABET);

            }

        },
        SOPHISTICATED {

            private static final String ALPHABET = ALPHANUMERIC_SPECIALCHAR_ALPHABET;

            @Override
            public String getString(Randomly r) {
                if (smallBiasProbability()) {
                    return Randomly.fromOptions("TRUE", "FALSE", "0.0", "-0.0", "1e500", "-1e500");
                }
                if (cacheProbability()) {
                    String s = r.getFromStringCache();
                    if (s != null) {
                        return s;
                    }
                }

                long n = ALPHABET.length();

                StringBuilder sb = new StringBuilder();

                long chars = getStringLength(r);
                for (int i = 0; i < chars; i++) {
                    if (Randomly.getBooleanWithRatherLowProbability()) {
                        char val = (char) r.getInteger();
                        if (val != 0) {
                            sb.append(val);
                        }
                    } else {
                        sb.append(ALPHABET.charAt(getNextInt(0, n)));
                    }
                }
                while (Randomly.getBooleanWithSmallProbability()) {
                    String[][] pairs = {{"{", "}"}, {"[", "]"}, {"(", ")"}};
                    int idx = (int) Randomly.getNotCachedInteger(0, pairs.length);
                    int left = (int) Randomly.getNotCachedInteger(0, sb.length() + 1);
                    sb.insert(left, pairs[idx][0]);
                    int right = (int) Randomly.getNotCachedInteger(left + 1, sb.length() + 1);
                    sb.insert(right, pairs[idx][1]);
                }
                if (r.provider != null) {
                    while (Randomly.getBooleanWithSmallProbability()) {
                        if (sb.length() == 0) {
                            sb.append(r.provider.get());
                        } else {
                            sb.insert((int) Randomly.getNotCachedInteger(0, sb.length()), r.provider.get());
                        }
                    }
                }

                String s = sb.toString();

                r.addToCache(s);
                return s;
            }

            public String transformCachedString(Randomly r, String randomString) {
                if (Randomly.getBoolean()) {
                    return randomString.toLowerCase();
                } else if (Randomly.getBoolean()) {
                    return randomString.toUpperCase();
                } else {
                    char[] chars = randomString.toCharArray();
                    if (chars.length != 0) {
                        for (int i = 0; i < Randomly.smallNumber(); i++) {
                            chars[(int) r.getInteger(0, chars.length)] = ALPHABET.charAt((int) r.getInteger(0, ALPHABET.length()));
                        }
                    }
                    return new String(chars);
                }
            }

        };

        private static final String ALPHANUMERIC_SPECIALCHAR_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#<>/.,~-+'*()[]{} ^*?%_\t\n\r|&\\";
        private static final String ALPHANUMERIC_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        private static final String NUMERIC_ALPHABET = "0123456789";

        private static long getStringLength(Randomly r) {
            long chars;
            chars = r.getInteger(5, maxStringLength);
            return (chars <= 0) ? 1 : chars;
        }

        private static String getStringOfAlphabet(Randomly r, String alphabet) {
            long chars = getStringLength(r);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chars; i++) {
                sb.append(alphabet.charAt(getNextInt(0, alphabet.length())));
            }
            return sb.toString();
        }

        public abstract String getString(Randomly r);

        public String transformCachedString(Randomly r, String s) {
            return s;
        }

    }

    public String getString() {
        return stringGenerationStrategy.getString(this);
    }

    public String getString(String a, String b) {
        int pos = 0, len = Math.min(a.length(), b.length());
        StringBuilder result = new StringBuilder();
        while (pos < len && a.charAt(pos) == b.charAt(pos)) {
            result.append(a.charAt(pos));
            pos++;
        }
        if (pos == len) {
            return result.toString();
        }
        while (pos < len) {
            if (a.charAt(pos) < b.charAt(pos)) {
                int diff = b.charAt(pos) - a.charAt(pos);
                if (diff == 1) {
                    result.append(a.charAt(pos));
                } else {
                    char next = (char) (a.charAt(pos) + Randomly.getNextInt(1, diff - 1));
                    if (!(next != '\\' && next != '\"' && next != '\'' && next != '\n' && next != '\r' && next != '\t' && next != '\b' && next != '\f' && next != '\0' && next != '\u001a' && next != '\u0007' && next != '\u0000')) {
                        result.append(a.charAt(pos));
                        pos++;
                        continue;
                    }
                    result.append(next);
                    return result.toString();
                }
            } else {
                result.append(a.charAt(pos));
            }
            pos++;
        }
        result = new StringBuilder(a);
        result.append('a' + Randomly.getNextLong(0, 25));
        return result.toString();
    }

    public byte[] getBytes() {
        long size = Randomly.smallNumber();
        byte[] arr = new byte[(int) size];
        getThreadRandom().get().nextBytes(arr);
        return arr;
    }

    public long getNonZeroInteger() {
        long value;
        if (smallBiasProbability()) {
            return Randomly.fromOptions(-1L, Long.MAX_VALUE, Long.MIN_VALUE, 1L);
        }
        if (cacheProbability()) {
            Long l = getFromLongCache();
            if (l != null && l != 0) {
                return l;
            }
        }
        do {
            value = getInteger();
        } while (value == 0);
        assert value != 0;
        addToCache(value);
        return value;
    }

    public long getPositiveInteger() {
        if (cacheProbability()) {
            Long value = getFromLongCache();
            if (value != null && value >= 0) {
                return value;
            }
        }
        long value;
        if (smallBiasProbability()) {
            value = Randomly.fromOptions(0L, Long.MAX_VALUE, 1L);
        } else {
            value = getNextLong(0, Long.MAX_VALUE);
        }
        addToCache(value);
        assert value >= 0;
        return value;
    }

    public double getFiniteDouble() {
        while (true) {
            double val = getDouble();
            if (Double.isFinite(val)) {
                return val;
            }
        }
    }

    public double getDouble() {
        if (MainOptions.mode == "falkordb")
            return getThreadRandom().get().nextDouble() % 128;
        if (smallBiasProbability()) {
            return Randomly.fromOptions(0.0, -0.0, Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY);
        } else if (cacheProbability()) {
            Double d = getFromDoubleCache();
            if (d != null) {
                return d;
            }
        }
        double value = getThreadRandom().get().nextDouble();
        addToCache(value);
        return value;
    }

    private static boolean smallBiasProbability() {
        return getThreadRandom().get().nextInt(100) == 1;
    }

    public static boolean getBooleanWithLowerProbability() {
        return getThreadRandom().get().nextInt(100) < 10;
    }

    public static boolean getBooleanWithRatherLowProbability() {
        return getThreadRandom().get().nextInt(3) == 1;
    }

    public static boolean getBooleanWithSmallProbability() {
        return smallBiasProbability();
    }

    public int getInteger(long left, long right) {
        if (left == right) {
            return (int) left;
        }
        return (int) getLong(left, right);
    }

    public long getLong(long left, long right) {
        if (left == right) {
            return left;
        }
        return getNextLong(left, right);
    }

    public BigDecimal getRandomBigDecimal() {
        return new BigDecimal(getThreadRandom().get().nextDouble());
    }

    public long getPositiveIntegerNotNull() {
        while (true) {
            long val = getPositiveInteger();
            if (val != 0) {
                return val;
            }
        }
    }

    public static long getNonCachedInteger() {
        long value = getThreadRandom().get().nextLong();
        return value;
    }

    public static long getPositiveOrZeroNonCachedInteger() {
        return getNextLong(0, Long.MAX_VALUE);
    }

    public static long getNotCachedInteger(long lower, long upper) {
        long value = getNextLong(lower, upper);
        return value;
    }

    public Randomly(Supplier<String> provider) {
        this.provider = provider;
    }

    public Randomly() {
        if (THREAD_SEED.get() == null) {
            try {
                throw new Exception("No seed has been set for this thread");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        if (THREAD_RANDOM.get() == null) {
            THREAD_RANDOM.set(new Random(THREAD_SEED.get()));
        }

    }

    public Randomly(long seed) {
        if (MainOptions.realRandomSeed != -1) {
            THREAD_SEED.set(MainOptions.realRandomSeed);
        } else {
            THREAD_SEED.set(seed);

        }
        THREAD_RANDOM.set(new Random(THREAD_SEED.get()));
    }

    public static double getUncachedDouble() {
        return getThreadRandom().get().nextDouble();
    }

    public String getChar() {
        while (true) {
            String s = getString();
            if (!s.isEmpty()) {
                return s.substring(0, 1);
            }
        }
    }

    public String getAlphabeticChar() {
        while (true) {
            String s = getChar();
            if (Character.isAlphabetic(s.charAt(0))) {
                return s;
            }
        }
    }

    private static long getNextLong(long lower, long upper) {
        if (lower > upper) {
            throw new IllegalArgumentException(lower + " " + upper);
        }
        if (lower == upper) {
            return lower;
        }
        return (long) (getThreadRandom().get().longs(lower, upper).findFirst().getAsLong());
    }

    private static int getNextInt(long lower, long upper) {
        return (int) getNextLong(lower, upper);
    }

    public long getSeed() {
        return THREAD_SEED.get();
    }

    public static void initialize(MainOptions options) {
        stringGenerationStrategy = options.getRandomStringGenerationStrategy();
        maxStringLength = options.getMaxStringConstantLength();
        useCaching = options.useConstantCaching();
        cacheSize = options.getConstantCacheSize();
    }

}
