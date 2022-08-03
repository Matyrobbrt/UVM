package uvm.compiler.util;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Modifiers {
    public static final Map<String, Integer> BY_NAME;
    private static final Map<String, Integer> ACCESS_MODS;
    static {
        final var byName = new HashMap<String, Integer>();
        final var accessMods = new HashMap<String, Integer>();

        class Helper {
            void addAccess(String name, int access) {
                byName.put(name, access);
                accessMods.put(name, access);
            }
        }
        final var helper = new Helper();

        helper.addAccess("public", Opcodes.ACC_PUBLIC);
        helper.addAccess("publicf", Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

        helper.addAccess("private", Opcodes.ACC_PRIVATE);
        helper.addAccess("protected", Opcodes.ACC_PROTECTED);
        helper.addAccess("package", 0);

        byName.put("static", Opcodes.ACC_STATIC);
        byName.put("final", Opcodes.ACC_FINAL);

        BY_NAME = Map.copyOf(byName);
        ACCESS_MODS = Map.copyOf(accessMods);
    }
    @Nullable
    public static Integer get(String name) {
        return BY_NAME.get(name);
    }
    public static boolean startsWithModifier(String text) {
        text = text.trim();
        return BY_NAME.keySet().stream().anyMatch(text::startsWith);
    }
    public static int collect(List<String> modifiers, int defaultAccess) {
        int acc = modifiers.stream()
                .filter(BY_NAME::containsKey)
                .mapToInt(BY_NAME::get)
                .reduce((a, b) -> a | b)
                .orElse(0);
        if (!containsAccess(modifiers)) {
            acc |= defaultAccess;
        }
        return acc;
    }

    public static boolean containsAccess(List<String> modifier) {
        return ACCESS_MODS.keySet().stream().anyMatch(modifier::contains);
    }
}
