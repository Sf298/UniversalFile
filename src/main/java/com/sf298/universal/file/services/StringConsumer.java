package com.sf298.universal.file.services;

public class StringConsumer {

    private final StringBuffer str;
    int i = 0;

    public StringConsumer(String str) {
        this.str = new StringBuffer(str);
    }

    public String consume(int charCount) {
        String out = peek(charCount);
        i += charCount;
        return out;
    }

    public String peek(int charCount) {
        return str.substring(i, Math.min(i+charCount, str.length()));
    }

    public void skip(int chars) {
        if(chars > 0) {
            i += chars;
        }
    }

    public void back(int chars) {
        if(chars > 0) {
            i -= chars;
        }
    }

    public boolean hasNext() {
        return i < str.length()-1;
    }

    /**
     * Calculates the distance to the start of the next occurrence of s
     * @param s the string to look for
     * @return the distance to the next occurrence or -1 if not found.
     */
    public int dTo(String s) {
        int pos = str.indexOf(s, i);
        if(pos == -1) {
            return -1;
        }
        return pos - i;
    }

    /**
     * Calculates the distance to the start of the next occurrence of s
     * @param s the string to look for
     * @return the distance to the next occurrence or Integer.MAX_VALUE if not found.
     */
    public int dToMax(String s) {
        int pos = str.indexOf(s, i);
        if(pos == -1) {
            return Integer.MAX_VALUE;
        }
        return pos - i;
    }
    public int dToEnd() {
        return str.length() - i;
    }

    public int dConsumed() {
        return i;
    }

    public String consumeCSV() {
        if(peek(1).equals(",")) {
            skip(1);
        }
        boolean hasQuote = peek(1).equals("\"");
        if(hasQuote) {
            skip(1);
        }

        int n = hasQuote ? dTo("\"") : dTo(",");
        if(n < 0) {
            n = dToEnd();
        }

        String token = consume(n).trim();

        if(hasQuote) {
            skip(1);
        }

        int startMod = token.startsWith("\"") ? 1 : 0;
        int endMod = token.endsWith("\"") ? -1 : 0;
        token = token.substring(startMod, token.length()+endMod);
        return token;
    }

}
