package uvm.compiler.context;

import uvm.compiler.ast.insn.ASTInstruction;
import uvm.compiler.context.cp.ClasspathClass;
import uvm.compiler.context.discovery.Imports;
import uvm.compiler.parse.MethodParser;

import java.util.Map;

public interface MethodContext {
    Map<String, MethodParser.LocalData> getLocals();

    void addInsn(ASTInstruction insn);

    Imports imports();

    ClassResolver resolver();

    default ClasspathClass getClass(String name) {
        return resolver().findClass(imports().getClass(name));
    }
}
