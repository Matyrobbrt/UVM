package uvm.compiler.ast;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class ASTClass {
    public final String name;
    public final int access;
    public final List<ASTMethod> methods = new ArrayList<>();

    public ASTClass(String name, int access) {
        this.name = name;
        this.access = access;
    }

    public ClassNode asASM() {
        final var cn = new ClassNode(Opcodes.ASM9);
        cn.name = this.name;
        cn.access = access;
        cn.methods = methods.stream().map(ASTMethod::asASM).toList();
        return cn;
    }
}
