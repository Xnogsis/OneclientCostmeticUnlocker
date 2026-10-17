package localcosmetics;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.polyfrost.polyplus.client.network.http.responses.BodySlot;
import org.polyfrost.polyplus.client.utils.ClientPlatform;

public final class PersistenceCheck {
    private static final Path SAVE = Path.of("config", "oneclient-local-cosmetics.properties");
    public static final class Equipment {
        private final Map<Object,Integer> equipped;
        public Equipment(Map<Object,Integer> equipped) { this.equipped = equipped; }
        public Map<Object,Integer> getEquipped() { return equipped; }
    }
    public static final class Catalog {
        public static Equipment localEquipped;
    }
    private static final Catalog CATALOG = new Catalog();
    private static void serverEquipment() {
        Catalog.localEquipped = new Equipment(new HashMap<>(Map.of(
            BodySlot.Cape, 1, BodySlot.Hat, 2, BodySlot.Wings, 3)));
    }
    private static void choose() {
        Map<Object,Integer> choices = new HashMap<>();
        choices.put(BodySlot.Cape, 42);
        choices.put(BodySlot.Hat, null);
        assert LocalCosmetics.equip(CATALOG, new Equipment(choices)) == kotlin.Unit.INSTANCE;
    }
    private static void restored() {
        Map<?,?> equipped = (Map<?,?>) LocalCosmetics.localMap(CATALOG);
        assert equipped.get(BodySlot.Cape).equals(42) : equipped;
        assert !equipped.containsKey(BodySlot.Hat) : equipped;
        assert equipped.get(BodySlot.Wings).equals(3) : equipped;
    }
    public static void main(String[] args) throws Exception {
        serverEquipment();
        switch (args[0]) {
            case "write" -> {
                assert !LocalCosmetics.overridesPlayer(null);
                choose();
                restored();
                assert SelectionStore.load(SAVE).containsKey("Hat");
                assert SelectionStore.load(SAVE).get("Hat") == null;
            }
            case "read" -> {
                UUID local = UUID.randomUUID();
                ClientPlatform.INSTANCE.uuid = local;
                assert LocalCosmetics.overridesPlayer(local) : "Must load before equipment lookup";
                assert !LocalCosmetics.overridesPlayer(UUID.randomUUID());
                assert !LocalCosmetics.overridesPlayer(null);
                restored();
                LocalCosmetics.reset();
                serverEquipment();
                restored();
                Map<String,Integer> saved = SelectionStore.load(SAVE);
                saved.put("FutureSlot", 123);
                SelectionStore.save(SAVE, saved);
                LocalCosmetics.reset();
                serverEquipment();
                restored();
                choose();
                assert SelectionStore.load(SAVE).get("FutureSlot").equals(123);
                try (var files = Files.list(SAVE.getParent())) {
                    assert files.noneMatch(p -> p.toString().endsWith(".tmp"));
                }
            }
            case "corrupt" -> {
                Files.createDirectories(SAVE.getParent());
                for (String contents : List.of("Cape=broken\n", "Cape=42\nHat=\\uZZZZ\n")) {
                    Files.writeString(SAVE, contents);
                    try {
                        SelectionStore.load(SAVE);
                        throw new AssertionError("Damaged save accepted");
                    } catch (IOException expected) { }
                    LocalCosmetics.reset();
                    choose();
                    restored();
                    assert Files.readString(SAVE).equals(contents) : "Damaged save overwritten";
                }
            }
            case "writeFailure" -> {
                Files.createDirectories(SAVE.getParent());
                assert !LocalCosmetics.overridesPlayer(null);
                Files.createDirectory(SAVE);
                Files.writeString(SAVE.resolve("keep.txt"), "keep");
                choose();
                restored();
                assert Files.readString(SAVE.resolve("keep.txt")).equals("keep");
            }
            default -> throw new AssertionError("Unknown check: " + args[0]);
        }
        System.out.println("PASS: " + args[0]);
    }
}
