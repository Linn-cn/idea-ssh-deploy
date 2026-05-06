package com.sshdeploy;

import com.sshdeploy.deploy.settings.PluginSettingsService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MyMessageBundle {
    @NonNls
    private static final String BUNDLE = "messages.MyMessageBundle";
    private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

    private MyMessageBundle() {
    }

    public static @Nls String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        Locale locale = resolveLocale();
        String raw = findValue(locale, key);
        if (raw == null) {
            return "!" + key + "!";
        }
        return params == null || params.length == 0 ? raw : MessageFormat.format(raw, params);
    }

    public static Supplier<@Nls String> lazyMessage(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return () -> message(key, params);
    }

    private static Locale resolveLocale() {
        try {
            PluginSettingsService.LanguageMode mode = PluginSettingsService.getInstance().getLanguageMode();
            return switch (mode) {
                case ZH_CN -> Locale.SIMPLIFIED_CHINESE;
                case EN_US -> Locale.US;
                case FOLLOW_IDE -> Locale.getDefault();
            };
        } catch (Exception ignore) {
            return Locale.getDefault();
        }
    }

    private static String findValue(Locale locale, String key) {
        String localeSuffix = toBundleSuffix(locale);
        if (!localeSuffix.isEmpty()) {
            Properties localized = loadProperties(localeSuffix);
            if (localized.containsKey(key)) {
                return localized.getProperty(key);
            }
        }
        Properties base = loadProperties("");
        return base.getProperty(key);
    }

    private static String toBundleSuffix(Locale locale) {
        if (locale == null) {
            return "";
        }
        String language = locale.getLanguage();
        String country = locale.getCountry();
        if (language == null || language.isBlank()) {
            return "";
        }
        if (country == null || country.isBlank()) {
            return "_" + language;
        }
        return "_" + language + "_" + country;
    }

    private static Properties loadProperties(String suffix) {
        return CACHE.computeIfAbsent(suffix, s -> {
            String resource = BUNDLE.replace('.', '/') + s + ".properties";
            Properties properties = new Properties();
            try (InputStream is = MyMessageBundle.class.getClassLoader().getResourceAsStream(resource)) {
                if (is == null) {
                    return properties;
                }
                properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            } catch (Exception ignore) {
                return properties;
            }
            return properties;
        });
    }
}
