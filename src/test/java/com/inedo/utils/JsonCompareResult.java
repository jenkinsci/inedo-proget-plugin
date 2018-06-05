package com.inedo.utils;

/**
 * Bean for holding results from JSONCompare.
 */
public class JsonCompareResult {
    private boolean success = true;
    private StringBuilder sb;

    public JsonCompareResult(String prefix) {
        sb = new StringBuilder(prefix);
    }

    /**
     * Did the comparison pass?
     * 
     * @return True if it passed
     */
    public boolean passed() {
        return success;
    }

    /**
     * Did the comparison fail?
     * 
     * @return True if it failed
     */
    public boolean failed() {
        return !success;
    }

    /**
     * Result message
     * 
     * @return String explaining why if the comparison failed
     */
    public String getMessage() {
        return sb.toString();
    }

    public void fail(String message) {
        success = false;

        sb.append("\n\t").append(message);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
