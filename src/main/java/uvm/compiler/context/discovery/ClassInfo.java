package uvm.compiler.context.discovery;

import org.jetbrains.annotations.Nullable;
import uvm.compiler.context.cp.ClasspathClass;
import uvm.compiler.context.cp.ClasspathField;
import uvm.compiler.context.cp.ClasspathMethod;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ClassInfo(int access, String name, String signature,
                        List<String> supers, List<FieldInfo> fields,
                        List<MethodInfo> methods,
                        List<Import> imports, Type type,
                        @Nullable
                        Imports resolvedImports) implements ClasspathClass {

    @Override
    public List<? extends ClasspathField> getFields() {
        return fields();
    }

    @Override
    public List<? extends ClasspathMethod> getMethods() {
        return methods();
    }

    public enum Type {
        INTERFACE, CLASS, RECORD, ANNOTATION;

        public static final Map<String, Type> BY_NAME = Stream.of(values())
                .collect(Collectors.toUnmodifiableMap(t -> t.toString().toLowerCase(Locale.ROOT),
                        Function.identity()));
    }
}
