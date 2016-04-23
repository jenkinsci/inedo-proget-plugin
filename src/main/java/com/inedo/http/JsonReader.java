package com.inedo.http;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * A wrapper around "com.google.gson.Gson" for simplifying the parsing JSON strings.
 * 
 * @author Andrew Sumner
 */
public class JsonReader {
	private final JsonElement json;	
	
	/**
	 * A json reader.
	 * 
	 * @param json Json string
	 * @throws JsonParseException JsonParseException
	 */
	public JsonReader(String json) {
		this.json = new JsonParser().parse(json);
	}
	
	/**
	 * A json reader.
	 * @param element Json Element
	 */
	public JsonReader(JsonElement element) {
		this.json = element;
	}

	/**
	 * @return A nicely formatted JSON string
	 * @throws IOException If unable to read the response
	 */
	public String asPrettyString() throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(json);
	}
	
	/**
	 * Deserialize the returned Json string.
	 * @return A {@link JsonElement}
	 * @throws IOException if the response string is not valid JSON
	 */
	public JsonElement asJson() throws IOException {
		return json;
	}
	
	/**
	 * Search the JSON response for the requested element.
	 * 
	 * <p><pre>
	 * JsonElement value = reader.jsonPath("anArray[0].aValue").
	 * String details = (value == null ? "" : value.getAsString());
	 * </pre></p>
	 * 
	 * @param path Json path
	 * @return JsonElement or null if not found
	 */
	public JsonElement jsonPath (String path) {
		return jsonPath(json, path);
	} 

	/**
	 * Search a JSON element's children for the requested element.
	 * 
	 * @param json Json element
	 * @param path search path
	 * @return JsonElement or null if not found
	 */
	public JsonElement jsonPath(JsonElement json, String path) {
		if (!path.contains(".")) {
			return json.getAsJsonObject().get(path); 
		} else {
			JsonElement newJson;
			String next = path.split("[/.]")[0]; 

			if (next.endsWith("]")) {
				int pos = next.lastIndexOf('[');
				String index = next.substring(pos + 1, next.length() - 1);
				next = next.substring(0, pos);

				newJson = json.getAsJsonObject().get(next).getAsJsonArray().get(Integer.valueOf(index));
			} else {
				newJson = json.getAsJsonObject().get(next);
			}

			String newPath = path.substring(path.indexOf(".") + 1); 

			return jsonPath(newJson, newPath);
		} 
	}
	
	/**
	 * Deserialize the Json into an object of the specified class.
	 * 
	 * @param <T> The type of the desired object
	 * @param type Class to populate
	 * @return A new class of the supplied type
	 * @throws IOException  if json is not a valid representation for an object of type classOfT
	 */
	public <T> T asJson(Class<T> type) throws IOException {
		return new Gson().fromJson(json, type);
	}
	
	/**
	 * Deserialize the Json into an object of the specified class.
	 * 
	 * @param <T> The type of the desired object
	 * @param type Class to populate
	 * @return A new class of the supplied type
	 * @throws IOException  if json is not a valid representation for an object of type classOfT
	 */
	public <T> T asJson(Type type) throws JsonSyntaxException {
		return new Gson().fromJson(json, type);
	}
	
	
}
