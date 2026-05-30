package com.sshdeploy.deploy.ui.console;

import com.intellij.openapi.components.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service(Service.Level.PROJECT)
public final class DeployConsoleService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final List<String> logs = new CopyOnWriteArrayList<>();

    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    public String appendLine(String message) {
        String line = formatTimestampedLine(message);
        logs.add(line);
        for (Consumer<String> listener : listeners) {
            listener.accept(line);
        }
        return line;
    }

    public static String formatTimestampedLine(String message) {
        return "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;
    }

    public List<String> snapshot() {
        return List.copyOf(logs);
    }

    /** Clears the in-memory log buffer (tool window display should be cleared by the caller). */
    public void clear() {
        logs.clear();
    }
}
