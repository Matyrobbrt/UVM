package uvm.compiler.parse;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;
import uvm.compiler.ast.ASTMethod;
import uvm.compiler.ast.insn.ASTInstruction;
import uvm.compiler.ast.insn.FieldInsn;
import uvm.compiler.ast.insn.LDCInstruction;
import uvm.compiler.ast.insn.LoadInsn;
import uvm.compiler.ast.insn.MethodInsn;
import uvm.compiler.ast.insn.ReturnInsn;
import uvm.compiler.ast.insn.StoreInsn;
import uvm.compiler.context.ClassResolver;
import uvm.compiler.context.MethodContext;
import uvm.compiler.context.discovery.ClassInfo;
import uvm.compiler.context.discovery.Imports;
import uvm.compiler.context.discovery.MethodInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static uvm.compiler.context.discovery.Imports.Impl.PRIMITIVES;

public class MethodParser {
    public static ASTMethod read(ClassResolver classResolver, ClassInfo clazz, MethodInfo method) throws Exception {
        final Map<String, LocalData> locals = new HashMap<>();
        final var asLines = new ArrayList<>(method.lines());
        int access = method.access();
        final List<ASTInstruction> instructions = new ArrayList<>();
        final var imports = Objects.requireNonNull(clazz.resolvedImports());
        final var ctw = new MethodContext() {
            @Override
            public Map<String, LocalData> getLocals() {
                return locals;
            }

            @Override
            public Imports imports() {
                return imports;
            }

            @Override
            public ClassResolver resolver() {
                return classResolver;
            }

            @Override
            public void addInsn(ASTInstruction insn) {
                instructions.add(insn);
            }

        };
        {
            if (!is(access, Opcodes.ACC_STATIC)) {
                locals.put("this", new LocalData(0, new ActualType(clazz.name())));
            }
            for (final var param : method.parameters()) {
                final var type = getType(imports, param.type());
                locals.put(param.name(), new LocalData(locals.size(), type));
            }
        }
        for (final var line : asLines) {
            if (line.trim().startsWith("define ")) {
                final var definition = line.substring(line.indexOf("define ") + "define ".length());
                final var split = definition.split(" as ");
                final var withType = split[1].split(" = ");
                final var localName = split[0];
                final var typeStr = withType[0];
                final ActualType type = getType(imports, typeStr);
                final var value = withType[1];
                resolveDefinition(ctw, type.usable, typeStr, value);
                locals.put(localName, new LocalData(locals.size(), type));
            } else {
                resolveMethodCall(ctw, line.trim());
            }
        }
        instructions.add(new ReturnInsn(Type.VOID_TYPE));
        final var ast = new ASTMethod();
        ast.access = access;
        ast.name = method.name();
        ast.instructions.addAll(instructions);
        final var desc = new StringBuilder();
        desc.append("(");
        method.parameters().forEach(param -> desc.append(getType(imports, param.type()).usable()));
        desc.append(")");
        if (method.returnType() == null) {
            desc.append("V");
        } else {
            desc.append(getType(imports, method.returnType()).usable().getDescriptor());
        }
        ast.desc = desc.toString();
        return ast;
    }

    private static void resolveDefinition(MethodContext ctx, Type type, String typeStr, String str) {
        str = str.trim();
        final var idx = ctx.getLocals().size();
        if (typeStr.trim().equals("null")) {
            ctx.addInsn(LDCInstruction.NULL);
            ctx.addInsn(new StoreInsn(type, idx));
            return;
        }
        if (PRIMITIVES.containsKey(typeStr)) {
            final var prim = PRIMITIVES.get(typeStr);
            try {
                final var ldc = new LDCInstruction(prim.decoder().apply(str));
                ctx.addInsn(ldc);
                ctx.addInsn(new StoreInsn(type, idx));
                return;
            } catch (Exception ignored) { }
        }
        resolveMethodCall(ctx, str);
        ctx.addInsn(new StoreInsn(type, idx));
    }

    private static void resolveMethodCall(MethodContext context, String str) {
        final var splitDot = str.split("\\.");
        final LocalData local = context.getLocals().get(splitDot[0]);
        final boolean isMethodCall = splitDot[1].contains("(") && splitDot[1].contains(")");
        final var owner = local == null ?
                context.imports().getClass(splitDot[0]).replace('.', '/') :
                local.type().usable().getInternalName();
        if (local == null) {
            if (isMethodCall) {
                final var callData = loadArgs(context, splitDot[1]);
                context.addInsn(MethodInsn.invoke(
                        true, owner, callData.name(), callData.desc()
                ));
            } else {
                final var field = Optional.ofNullable(context.resolver().findClass(owner))
                        .flatMap(c -> Optional.ofNullable(c.findField(splitDot[1])))
                        .orElseThrow();
                context.addInsn(FieldInsn.get(
                        true, owner, field.name(),
                        getType(context.imports(), field.type()).usable().getDescriptor()
                ));
            }
        } else {
            context.addInsn(new LoadInsn(local.type().usable(), local.index()));
            if (isMethodCall) {
                final var callData = loadArgs(context, splitDot[1]);
                context.addInsn(MethodInsn.invoke(
                        false, owner, callData.name(), callData.desc()
                ));
            } else {
                final var field = Optional.ofNullable(context.resolver().findClass(owner))
                        .flatMap(c -> Optional.ofNullable(c.findField(splitDot[1])))
                        .orElseThrow();
                context.addInsn(FieldInsn.get(
                        false, owner, field.name(),
                        getType(context.imports(), field.type()).usable().getDescriptor()
                ));
            }
        }
    }

    private static CallData loadArgs(MethodContext context, String str) {
        final var indexOfFirstParan = str.indexOf('(');
        final var mthdName = str.substring(0, indexOfFirstParan);
        final var args = str.substring(indexOfFirstParan + 1, str.lastIndexOf(')')).split(",");
        final var desc = new StringBuilder();
        desc.append("(");
        for (String arg : args) {
            arg = arg.trim();
            if (arg.indexOf('[') > 0 && arg.indexOf(']') > 0) {
                final int idx = Integer.parseInt(arg.substring(arg.indexOf('[') + 1, arg.indexOf(']')));
                final var local = context.getLocals().get(arg.substring(0, arg.indexOf('[')));
                context.addInsn(new LoadInsn(local.type.usable, local.index));
                context.addInsn(new LDCInstruction(idx));
                context.addInsn(ASTInstruction.opcode(local.type.actual.getOpcode(Opcodes.IALOAD)));
                desc.append(local.type.actual.getDescriptor());
            } else {
                final var local = context.getLocals().get(arg.trim());
                context.addInsn(new LoadInsn(local.type.usable, local.index));
                desc.append(local.type.usable.getDescriptor());
            }
        }
        desc.append(")V");
        return new CallData(mthdName, desc.toString());
    }

    private static ActualType getType(Imports imports, String input) {
        final boolean isArray = input.endsWith("[]");
        if (isArray) {
            input = input.substring(0, input.lastIndexOf("[]"));
        }
        final Type actual = imports.resolveAsType(input);
        return new ActualType(isArray ? Type.getType("[" + actual) : actual, actual);
    }

    private static boolean is(final int modifier, final int toFind) {
        return (modifier & toFind) != 0;
    }

    public record LocalData(int index, ActualType type) {
    }

    public record PrimitiveData(Type type, Function<String, Object> decoder) {
    }

    record CallData(String name, String desc) {
    }

    private static Type getType(String normal) {
        return Type.getObjectType(normal.replace('.', '/'));
    }

    record ActualType(Type usable, Type actual) {
        public ActualType(Type type) {
            this(type, type);
        }

        public ActualType(String name) {
            this(getType(name));
        }
    }
}
