package localcosmetics;

import java.io.*;
import java.nio.file.*;
import java.util.*;

final class SelectionStore {
    static Map<String,Integer> load(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (NoSuchFileException missing) {
            return new HashMap<>();
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid properties file", invalid);
        }
        Map<String,Integer> selections = new HashMap<>();
        for (String slot : properties.stringPropertyNames()) {
            String value = properties.getProperty(slot).trim();
            try {
                selections.put(slot, value.equals("none") ? null : Integer.valueOf(value));
            } catch (NumberFormatException invalid) {
                throw new IOException("Invalid cosmetic ID for slot " + slot, invalid);
            }
        }
        return selections;
    }

    static void save(Path path, Map<String,Integer> selections) throws IOException {
        Properties properties = new Properties();
        selections.forEach((slot, id) -> properties.setProperty(slot, id == null ? "none" : id.toString()));
        Path destination = path.toAbsolutePath();
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "local-cosmetics-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Local cosmetic equipment; none means unequipped");
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
