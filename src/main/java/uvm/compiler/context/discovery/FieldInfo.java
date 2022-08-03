package uvm.compiler.context.discovery;

import org.jetbrains.annotations.Nullable;
import uvm.compiler.context.cp.ClasspathField;

public record FieldInfo(int access, String name, String type,
                        String signature, @Nullable Object initialValue) implements ClasspathField {
}
