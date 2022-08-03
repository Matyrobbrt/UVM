package uvm.compiler.ast.insn;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

public class MethodInsn extends ASTInstruction {
    private final int opcode;
    private final String owner, name, descriptor;

    public MethodInsn(int opcode, String owner, String name, String descriptor) {
        this.opcode = opcode;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    public static MethodInsn invoke(boolean isStatic, String owner, String name, String desc) {
        return new MethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, owner, name, desc);
    }

    @Override
    public InsnList asASM() {
        return insn(new MethodInsnNode(opcode, owner, name, descriptor));
    }
}
