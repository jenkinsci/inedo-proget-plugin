package com.inedo.utils;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public class JsonCompare {
    static {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new GsonJsonProvider();
            private final MappingProvider mappingProvider = new GsonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                // return EnumSet.noneOf(Option.class);
                return EnumSet.of(Option.SUPPRESS_EXCEPTIONS);
            }
        });
    }

    private static boolean failOptionalFields = false;

    public static void failOptionalFields() {
        failOptionalFields = true;
    }

    public static void assertFieldsIdentical(String prefix, String expected, String actual, Class<?> clazz) {
        JsonCompareResult result = new JsonCompareResult(prefix);

        JsonObject expectedJson = getJsonObject(expected);
        JsonObject actualJson = getJsonObject(actual);

        checkFields(expectedJson, actualJson, actual, clazz, result);
    }

    /**
     * Filter examples
     * <ul>
     * <li>"Applications_Extended[?(@.Application_Name=='TestApplication')]"</li>
     * <li>"Applications_Extended[0]"</li>
     * </ul>
     * 
     * @param prefix
     * @param expected
     * @param actual
     * @param filter
     * @param clazz
     */
    public static void assertArrayFieldsIdentical(String prefix, String expected, String actual, String filter, Class<?> clazz) {
        JsonCompareResult result = new JsonCompareResult(prefix);

        JsonObject expectedJson = getJsonArrayItem(expected, filter, result);
        JsonObject actualJson = getJsonArrayItem(actual, filter, result);

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

    private static JsonObject getJsonArrayItem(String jsonArray, String filter, JsonCompareResult result) {
        JsonObject jsonObject = null;

        boolean indefiniteResult = filter.replaceAll(" ", "").contains("[?") || filter.contains(",") || filter.contains("..");

        if (indefiniteResult) {
            JsonArray array = JsonPath.parse(jsonArray).read(filter);
            if (array == null) {
                result.fail(String.format("No object found in array with filter %s", filter));
            } else if (array.size() != 1) {
                result.fail(String.format("%s object(s) found in array with filter %s", array.size(), filter));
                jsonObject = null;
            } else {
                jsonObject = array.get(0).getAsJsonObject();
            }
        } else {
            jsonObject = JsonPath.parse(jsonArray).read(filter);

            if (jsonObject == null) {
                result.fail(String.format("No object found in array with filter %s", filter));
            }
        }

        return jsonObject;
    }

    private static void checkJsonObjectKeysExpectedInActual(JsonObject expected, JsonObject actual, JsonCompareResult result) {
        Set<String> expectedKeys = expected.keySet();

        for (String key : expectedKeys) {
            if (!actual.has(key)) {
                result.fail(String.format("Expected field %s in response but none found", key));
            }
        }
    }

    private static void checkJsonObjectKeysActualInExpected(JsonObject expected, JsonObject actual, JsonCompareResult result) {
        Set<String> actualKeys = expected.keySet();

        for (String key : actualKeys) {
            if (!expected.has(key)) {
                result.fail(String.format("nUnexpected field %s found in mocked data", key));
            }
        }
    }

    private static void checkObjectFieldsInActual(Class<?> clazz, JsonObject actual, JsonCompareResult result) {
        if (clazz == null) {
            return;
        }

        Field[] fields = clazz.getFields();

        for (Field field : fields) {
            if (!actual.has(field.getName())) {
                if (failOptionalFields || !field.isAnnotationPresent(Optional.class)) {
                    result.fail(String.format("Class %s contains field %s but it does not exist in the response", clazz.getSimpleName(), field.getName()));
                }
            }
        }
    }
}