package uvm.compiler.context;

import org.jetbrains.annotations.Nullable;
import uvm.compiler.context.cp.ClasspathClass;

import java.util.List;

public interface ClassResolver {
    @Nullable
    ClasspathClass findClass(String name);

    void discover(List<? extends ClasspathClass> classes);
}
