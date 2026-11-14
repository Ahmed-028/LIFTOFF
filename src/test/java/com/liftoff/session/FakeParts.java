package com.liftoff.session;

import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipBuild;
import com.liftoff.session.stub.ShipPart;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class FakeParts {

    private FakeParts() {
    }

    static ShipPart part(String id, ModuleSlot slot) {
        return new ShipPart(id, slot, id, id, "test fake", "nothing real", List.of(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, id, null);
    }


    static List<ShipPart> all() {
        return Stream.of(ModuleSlot.values())
                .flatMap(slot -> Stream.of("a", "b").map(suffix -> part(idOf(slot, suffix), slot)))
                .toList();
    }

    static String idOf(ModuleSlot slot, String suffix) {
        return slot.name().toLowerCase().replace('_', '-') + "-" + suffix;
    }

    static Function<String, Optional<ShipPart>> lookup() {
        Map<String, ShipPart> byId = all().stream().collect(Collectors.toMap(ShipPart::id, part -> part));
        return id -> Optional.ofNullable(byId.get(id));
    }


    static ShipBuild completeBuild() {
        ShipBuild build = ShipBuild.empty();
        for (ModuleSlot slot : ModuleSlot.values()) {
            build = build.with(part(idOf(slot, "a"), slot));
        }
        return build;
    }
}
