package uvm.compiler.context;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import uvm.compiler.context.cp.ClasspathClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResolverImpl implements ClassResolver {
    private final Map<String, ClasspathClass> cache = new HashMap<>();

    @Override
    public @Nullable ClasspathClass findClass(String name) {
        return cache.computeIfAbsent(name, k -> resolveJDKClass(name));
    }

    @Override
    public void discover(List<? extends ClasspathClass> classes) {
        for (final var clazz : classes) {
            cache.put(clazz.name(), clazz);
        }
    }

    private ClasspathClass resolveJDKClass(String name) {
        try {
            final var cr = new ClassReader(name);
            final var cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_CODE);
            return ClasspathClass.of(cn);
        } catch (IOException e) {
            return null;
        }
    }
}
