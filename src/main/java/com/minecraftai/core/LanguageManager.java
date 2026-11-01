package com.minecraftai.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static Map<String, String> translations = new HashMap<>();
    private static String currentLang = "pl_pl";

    public static void load(String lang) {
        currentLang = lang;
        String path = "/assets/lang/" + lang + ".json";
        try (InputStream is = LanguageManager.class.getResourceAsStream(path)) {
            if (is != null) {
                translations = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), 
                    new TypeToken<HashMap<String, String>>() {}.getType());
            } else {
                System.err.println("Language file not found: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return translations.getOrDefault(key, key);
    }
    
    public static String getItemName(ItemType type) {
        return get("item.minecraftai." + type.name().toLowerCase());
    }

    public static String getCurrentLang() {
        return currentLang;
    }

    public static void setCurrentLang(String lang) {
        currentLang = lang;
        load(lang);
    }
}