package uvm.compiler.context.discovery;

import org.jetbrains.annotations.Nullable;
import uvm.compiler.context.cp.ClasspathMethod;

import java.util.List;

public record MethodInfo(int access, String name, String returnType,
                         @Nullable String signature,
                         List<String> exceptions,
                         List<String> lines, List<Parameter> parameters) implements ClasspathMethod {

    public record Parameter(String name, String type, @Nullable String defaultValue) {
    }

}
