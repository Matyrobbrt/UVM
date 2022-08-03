package uvm.compiler.ast;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import uvm.compiler.ast.insn.ASTInstruction;

import java.util.ArrayList;
import java.util.List;

public class ASTMethod {
    public final List<ASTInstruction> instructions = new ArrayList<>();

    public String name;
    public String desc = "()V";
    public int access = Opcodes.ACC_PUBLIC;

    public MethodNode asASM() {
        final var mn = new MethodNode(Opcodes.ASM9);
        instructions.forEach(as -> mn.instructions.add(as.asASM()));
        mn.access = access;
        mn.desc = desc;
        mn.name = name;
        return mn;
    }
}
