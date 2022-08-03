package uvm.compiler.ast.insn;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

public class StoreInsn extends ASTInstruction {
    private final Type type;
    private final int index;

    public StoreInsn(Type type, int index) {
        this.type = type;
        this.index = index;
    }

    @Override
    public InsnList asASM() {
        return insn(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
    }
}
