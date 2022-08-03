package uvm.compiler.ast.insn;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;

public class FieldInsn extends ASTInstruction {
    private final int opcode;
    private final String owner, name, descriptor;

    public FieldInsn(int opcode, String owner, String name, String descriptor) {
        this.opcode = opcode;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    public static FieldInsn get(boolean isStatic, String owner, String name, String desc) {
        return new FieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner, name, desc);
    }
    public static FieldInsn get(boolean isStatic, Type owner, String name, Type desc) {
        return new FieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner.getInternalName(), name, desc.getDescriptor());
    }

    @Override
    public InsnList asASM() {
        return insn(new FieldInsnNode(opcode, owner, name, descriptor));
    }
}
