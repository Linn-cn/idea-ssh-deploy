package com.sshdeploy.deploy.domain;

import com.sshdeploy.MyMessageBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in file match rules (fixed ids, not persisted).
 */
public final class BuiltinFileMatchRules {
    /** Spring Boot executable jar: excludes {@code *-sources.jar} and {@code *.original.jar}. */
    public static final String SPRING_BOOT_JAR_ID = "__builtin_spring_boot_jar";
    public static final String DEFAULT_SPRING_BOOT_PATTERN =
            "^(?!.*(?:-sources|\\.original)\\.jar$).+\\.jar$";

    private BuiltinFileMatchRules() {
    }

    public static boolean isBuiltinId(String id) {
        return SPRING_BOOT_JAR_ID.equals(id);
    }

    public static List<FileMatchRule> builtinRules() {
        List<FileMatchRule> list = new ArrayList<>(1);
        FileMatchRule spring = new FileMatchRule();
        spring.setId(SPRING_BOOT_JAR_ID);
        spring.setName("");
        spring.setPattern(DEFAULT_SPRING_BOOT_PATTERN);
        list.add(spring);
        return list;
    }

    public static String displayNameForId(String id) {
        if (SPRING_BOOT_JAR_ID.equals(id)) {
            return MyMessageBundle.message("runconfig.upload.regex.springboot");
        }
        return id == null ? "" : id;
    }
}
