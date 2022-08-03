package uvm.compiler.ast.insn;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;

public class LDCInstruction extends ASTInstruction {
    public static final LDCInstruction NULL = new LDCInstruction(null);

    private final Object value;

    public LDCInstruction(Object value) {
        this.value = value;
    }

    @Override
    public InsnList asASM() {
        return insn(new LdcInsnNode(value));
    }
}
