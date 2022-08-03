package uvm.compiler.context.cp;

import uvm.compiler.context.discovery.MethodInfo;

import java.util.List;

public interface ClasspathMethod {
    int access();
    String name();
    List<String> exceptions();

    String returnType();
    List<MethodInfo.Parameter> parameters();
}
