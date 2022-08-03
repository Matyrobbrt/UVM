package uvm.compiler.context.discovery;

import org.jetbrains.annotations.Nullable;
import uvm.compiler.context.ClassResolver;

public interface Import {
    void resolve(Imports data, ClassResolver resolver);

    record Mapping(MappingType type, String name) {
    }

    enum MappingType {
        TYPE, METHOD, FIELD
    }

    static Import from(String string) {
        if (string.startsWith("import")) string = string.substring("import ".length());
        final var alias = string.contains(" as ") ? string.split(" as ")[1] : null;
        final var target = alias == null ? string.trim() : string.trim().replace(" as " + alias, "").trim();
        if (string.startsWith("static")) {
        }
        return new SingleClass(target, alias);
    }

    record SingleClass(String target, @Nullable String alias) implements Import {

        @Override
        public void resolve(Imports data, ClassResolver resolver) {
            final var resolved = resolver.findClass(target);
            if (resolved == null) throw new NullPointerException();
            final var names = resolved.name().split("\\.");
            data.put(MappingType.TYPE, alias == null ? names[names.length - 1] : alias, resolved.name());
        }
    }

    record SingleMember(String clazz, String name, @Nullable String alias) implements Import {

        @Override
        public void resolve(Imports data, ClassResolver resolver) {
            final var resolved = resolver.findClass(clazz);
            if (resolved == null) throw new NullPointerException();
            resolved.getMethods().stream()
                    .filter(f -> f.name().equals(name))
                    .findFirst().ifPresent(foundMethod -> data.put(MappingType.METHOD, alias == null ? name : alias, resolved.name() + "#" + foundMethod.name()));
            resolved.getFields().stream()
                    .filter(f -> f.name().equals(name))
                    .findFirst().ifPresent(foundField -> data.put(MappingType.FIELD, alias == null ? name : alias, resolved.name() + "#" + foundField.name()));
        }
    }
}
