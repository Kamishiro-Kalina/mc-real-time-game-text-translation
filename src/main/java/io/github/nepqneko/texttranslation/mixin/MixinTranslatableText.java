package io.github.nepqneko.texttranslation.mixin;

import com.google.gson.JsonObject;
import io.github.nepqneko.texttranslation.RealTimeGameTextTranslation;
import io.github.nepqneko.texttranslation.config.ModConfig;
import io.github.nepqneko.texttranslation.translation.Translator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.TranslationException;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(TranslatableText.class)
public abstract class MixinTranslatableText {
    private final TranslatableText self = (TranslatableText) (Object) this;
    @Shadow
    private @Nullable Language languageCache;
    @Shadow
    @Final
    private String key;
    @Shadow
    @Final
    private List<StringVisitable> translations;

    private static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    // Fuck me (IDE has swear word detection function ha ha ha ha)
    private static String deleteAlphanumeric(final CharSequence cs) {
        CharSequence cs2 = cs;

        if (isEmpty(cs)) {
            return (String) cs2;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetterOrDigit(cs.charAt(i))) {
                cs2 = ((String) cs2).replace(cs.charAt(i), Character.MIN_VALUE);
            }
        }

        return (String) cs2;
    }

    @Shadow
    protected abstract void setTranslation(String translation);

    @Inject(at = @At("TAIL"), method = "updateTranslations()V")
    private void updateTranslations(CallbackInfo ci) {
        RealTimeGameTextTranslation.IsSuccessfullyLoaded = true;

        for (String block : RealTimeGameTextTranslation.TranslationBlockKeys) {
            if (key.contains(block)) return;
        }

        String CurrentLanguage = MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode();

        if (CurrentLanguage.startsWith("en_") || RealTimeGameTextTranslation.EnglishTranslations == null) return;

        assert languageCache != null;
        String text = languageCache.get(key);
        String english_text = RealTimeGameTextTranslation.EnglishTranslations.get(key);

        if (text.equals(key) || text.isEmpty() || english_text.isEmpty()) return;

        String text2 = text;

        text2 = text2.replaceAll("%s", "");
        text2 = deleteAlphanumeric(text2.trim());

        if (text2.isEmpty()) return;

        for (String text3 : RealTimeGameTextTranslation.DontTranslationTexts) {
            if (text.equals(text3)) return;
        }

        if (text.equals(english_text)) {
            boolean isMiss = false;

            if (RealTimeGameTextTranslation.ManualTranslationsMap.containsKey(CurrentLanguage)) {
                Map<String, String> translationsMap = RealTimeGameTextTranslation.ManualTranslationsMap.get(CurrentLanguage);

                if (translationsMap.containsKey(key)) {
                    String newTranslation = translationsMap.get(key);

                    translations.clear();

                    try {
                        setTranslation(newTranslation);
                    } catch (TranslationException var4) {
                        translations.clear();
                        translations.add(StringVisitable.plain(newTranslation));
                    }

                    return;
                }
            }

            if (RealTimeGameTextTranslation.GoogleTranslationsMap.containsKey(CurrentLanguage)) {
                Map<String, String> translationsMap = RealTimeGameTextTranslation.GoogleTranslationsMap.get(CurrentLanguage);

                if (translationsMap.containsKey(key)) {
                    String newTranslation = translationsMap.get(key);

                    if (ModConfig.SHOW_ORIGINAL_TEXT.getValue())
                        newTranslation += "(" + text + ")";

                    translations.clear();

                    try {
                        setTranslation(newTranslation);
                    } catch (TranslationException var4) {
                        translations.clear();
                        translations.add(StringVisitable.plain(newTranslation));
                    }
                } else
                    isMiss = true;
            } else
                isMiss = true;

            if (isMiss) {
                boolean hasThread = Translator.threads.containsKey(CurrentLanguage) && Translator.threads.get(CurrentLanguage).containsKey(key);

                if (!RealTimeGameTextTranslation.UntranslatedMap.containsKey(key))
                    RealTimeGameTextTranslation.UntranslatedMap.put(key, text);

                if (!hasThread && ModConfig.ENABLE_DEBUG.getValue()) {
                    JsonObject obj = new JsonObject();

                    obj.addProperty("key", key);
                    obj.addProperty("language", CurrentLanguage);
                    obj.addProperty("text", text);

                    RealTimeGameTextTranslation.LOGGER.info("[" + RealTimeGameTextTranslation.FULL_NAME + "] Untranslated text found: " + obj);
                }

                new Translator(key, CurrentLanguage, english_text).start();
            }
        }
    }
}
