package com.sshdeploy.deploy.importer;

import org.jetbrains.annotations.Nullable;

public interface ActConfigParser {
    String getParserId();

    boolean canParse(String content);

    @Nullable
    ImportResult parse(String content);
}
