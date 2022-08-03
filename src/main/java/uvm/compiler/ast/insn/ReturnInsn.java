package uvm.compiler.ast.insn;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

public class ReturnInsn extends ASTInstruction {
    private final Type type;

    public ReturnInsn(Type type) {
        this.type = type;
    }

    @Override
    public InsnList asASM() {
        return insn(new InsnNode(type.getOpcode(Opcodes.IRETURN)));
    }
}
