package uvm.compiler.ast.insn;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

public abstract class ASTInstruction {

    public abstract InsnList asASM();

    protected static InsnList insn(AbstractInsnNode... nodes) {
        final var list = new InsnList();
        for (AbstractInsnNode node : nodes) {
            list.add(node);
        }
        return list;
    }

    public static ASTInstruction opcode(int code) {
        return new ASTInstruction() {
            @Override
            public InsnList asASM() {
                return insn(new InsnNode(code));
            }
        };
    }
}
