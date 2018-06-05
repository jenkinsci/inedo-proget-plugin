package com.inedo.utils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonCompare {
    public static void assertFieldsIdentical(String prefix, String expected, String actual, Class<?> clazz) {
        JsonCompareResult result = new JsonCompareResult(prefix);

        JsonObject expectedJson = getJsonObject(expected);
        JsonObject actualJson = getJsonObject(actual);

        checkFields(expectedJson, actualJson, actual, clazz, result);
    }

    public static void assertArrayFieldsIdentical(String prefix, String expected, String actual, String key, String filterValue, Class<?> clazz) {
        JsonCompareResult result = new JsonCompareResult(prefix);

        JsonObject expectedJson = getJsonArrayItem(expected, key, filterValue, result);
        JsonObject actualJson = getJsonArrayItem(actual, key, filterValue, result);

        checkFields(expectedJson, actualJson, actual, clazz, result);
    }

    private static void checkFields(JsonObject expectedJson, JsonObject actualJson, String actual, Class<?> clazz, JsonCompareResult result) {
        if (result.passed()) {
            checkJsonObjectKeysExpectedInActual(expectedJson, actualJson, result);
            checkJsonObjectKeysActualInExpected(expectedJson, actualJson, result);
        }

        if (result.passed()) {
            checkObjectFieldsInActual(clazz, actualJson, result);
        }

        if (result.failed()) {
            throw new AssertionError(result.getMessage() + "\n\n" + actual);
        }
    }

    private static JsonObject getJsonObject(String jsonObject) {
        return new JsonParser().parse(jsonObject).getAsJsonObject();
    }

    private static JsonObject getJsonArrayItem(String jsonArray, String field, String value, JsonCompareResult result) {
        JsonArray json = new JsonParser().parse(jsonArray).getAsJsonArray();

        Iterator<JsonElement> elements = json.iterator();
        while (elements.hasNext()) {
            JsonObject element = elements.next().getAsJsonObject();
            if (element.has(field)) {
                if (element.get(field).getAsString().equalsIgnoreCase(value)) {
                    return element;
                }
            }
        }

        result.fail(String.format("No object found in array with field %s = %s", field, value));

        return null;
    }

    private static void checkJsonObjectKeysExpectedInActual(JsonObject expected, JsonObject actual, JsonCompareResult result) {
        Set<String> expectedKeys = expected.keySet();

        for (String key : expectedKeys) {
            if (!actual.has(key)) {
                result.fail(String.format("Expected field %s but none found", key));
            }
        }
    }

    private static void checkJsonObjectKeysActualInExpected(JsonObject expected, JsonObject actual, JsonCompareResult result) {
        Set<String> actualKeys = expected.keySet();

        for (String key : actualKeys) {
            if (!expected.has(key)) {
                result.fail(String.format("nUnexpected field %s found", key));
            }
        }
    }

    private static void checkObjectFieldsInActual(Class<?> clazz, JsonObject actual, JsonCompareResult result) {
        Field[] fields = clazz.getFields();

        for (Field field : fields) {
            if (!actual.has(field.getName())) {
                result.fail(String.format("Class %s contains field %s but it does not exist in the response", clazz.getSimpleName(), field.getName()));
            }
        }
    }

}
