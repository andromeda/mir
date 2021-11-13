package de.tudarmstadt.sola.policy.extractor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class GenBuiltins {

	public static void main(String[] args) throws JsonIOException, JsonSyntaxException, FileNotFoundException {
		JsonParser parser = new JsonParser();
		
		JsonObject obj = (JsonObject)parser.parse(new FileReader("../globals.json"));		
		
		List<String> list = new ArrayList<String>();
		JsonArray array = obj.getAsJsonArray("module");
		for(int i = 0 ; i < array.size() ; i++){
		    list.add(array.get(i).getAsString());
		}
		array = obj.getAsJsonArray("es");
		for(int i = 0 ; i < array.size() ; i++){
		    list.add(array.get(i).getAsString());
		}
		array = obj.getAsJsonArray("node");
		for(int i = 0 ; i < array.size() ; i++){
		    list.add(array.get(i).getAsString());
		}
		for (String s: list)
			System.out.print("\"" + s + "\", ");
	}
	
}
