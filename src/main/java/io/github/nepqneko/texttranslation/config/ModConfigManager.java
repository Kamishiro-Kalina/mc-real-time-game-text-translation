package io.github.nepqneko.texttranslation.config;

import com.google.common.collect.Sets;
import com.google.gson.*;
import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import io.github.nepqneko.texttranslation.config.option.BooleanConfigOption;
import io.github.nepqneko.texttranslation.config.option.ConfigOptionStorage;
import io.github.nepqneko.texttranslation.config.option.EnumConfigOption;
import io.github.nepqneko.texttranslation.config.option.StringSetConfigOption;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

// Code from 'Mod Menu'

public class ModConfigManager {
    private static File file;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void prepareConfigFile() {
        if (file != null) {
            return;
        }

        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), RealTimeGameTextTranslation.MOD_ID);
        File dirs = new File(path.toString());

        if (!dirs.exists())
            dirs.mkdirs();

        file = new File(path.toFile(), "config.json");
    }

    public static void initializeConfig() {
        load();
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        prepareConfigFile();

        try {
            if (!file.exists()) {
                save();
            }
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                JsonObject json = new JsonParser().parse(br).getAsJsonObject();

                for (Field field : ModConfig.class.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                        if (StringSetConfigOption.class.isAssignableFrom(field.getType())) {
                            JsonArray jsonArray = json.getAsJsonArray(field.getName().toLowerCase(Locale.ROOT));
                            if (jsonArray != null) {
                                StringSetConfigOption option = (StringSetConfigOption) field.get(null);
                                ConfigOptionStorage.setStringSet(option.getKey(), Sets.newHashSet(jsonArray).stream().map(JsonElement::getAsString).collect(Collectors.toSet()));
                            }
                        } else if (BooleanConfigOption.class.isAssignableFrom(field.getType())) {
                            JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive(field.getName().toLowerCase(Locale.ROOT));
                            if (jsonPrimitive != null && jsonPrimitive.isBoolean()) {
                                BooleanConfigOption option = (BooleanConfigOption) field.get(null);
                                ConfigOptionStorage.setBoolean(option.getKey(), jsonPrimitive.getAsBoolean());
                            }
                        } else if (EnumConfigOption.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType) {
                            JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive(field.getName().toLowerCase(Locale.ROOT));
                            if (jsonPrimitive != null && jsonPrimitive.isString()) {
                                Type generic = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                                if (generic instanceof Class<?>) {
                                    EnumConfigOption<?> option = (EnumConfigOption<?>) field.get(null);
                                    Enum<?> found = null;
                                    for (Enum<?> value : ((Class<Enum<?>>) generic).getEnumConstants()) {
                                        if (value.name().toLowerCase(Locale.ROOT).equals(jsonPrimitive.getAsString())) {
                                            found = value;
                                            break;
                                        }
                                    }
                                    if (found != null) {
                                        ConfigOptionStorage.setEnumTypeless(option.getKey(), found);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException | IllegalAccessException e) {
            System.err.println("Couldn't load Real-time Game Text Translation configuration file; reverting to defaults");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void save() {
        prepareConfigFile();

        JsonObject config = new JsonObject();

        try {
            for (Field field : ModConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                    if (BooleanConfigOption.class.isAssignableFrom(field.getType())) {
                        BooleanConfigOption option = (BooleanConfigOption) field.get(null);
                        config.addProperty(field.getName().toLowerCase(Locale.ROOT), ConfigOptionStorage.getBoolean(option.getKey()));
                    } else if (StringSetConfigOption.class.isAssignableFrom(field.getType())) {
                        StringSetConfigOption option = (StringSetConfigOption) field.get(null);
                        JsonArray array = new JsonArray();
                        ConfigOptionStorage.getStringSet(option.getKey()).forEach(array::add);
                        config.add(field.getName().toLowerCase(Locale.ROOT), array);
                    } else if (EnumConfigOption.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType) {
                        Type generic = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                        if (generic instanceof Class<?>) {
                            EnumConfigOption<?> option = (EnumConfigOption<?>) field.get(null);
                            config.addProperty(field.getName().toLowerCase(Locale.ROOT), ConfigOptionStorage.getEnumTypeless(option.getKey(), (Class<Enum<?>>) generic).name().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        String jsonString = RealTimeGameTextTranslation.GSON.toJson(config);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(jsonString);
        } catch (IOException e) {
            System.err.println("Couldn't save Real-time Game Text Translation configuration file");
            e.printStackTrace();
        }
    }
}
