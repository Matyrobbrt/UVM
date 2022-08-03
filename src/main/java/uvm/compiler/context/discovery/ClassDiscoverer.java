package uvm.compiler.context.discovery;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import uvm.compiler.ast.ASTClass;
import uvm.compiler.context.ClassResolver;
import uvm.compiler.context.ResolverImpl;
import uvm.compiler.parse.MethodParser;
import uvm.compiler.util.Modifiers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClassDiscoverer {

    public static void main(String[] args) throws Exception {
        final var res = new ResolverImpl();
        final var clz = discover(Files.readString(Path.of("SomeTest.uvm")));
        final var out = finishResolution(List.of(clz), res).get(0);

        final var cn = new ASTClass(out.name(), out.access());
        for (final var mn : out.methods()) {
            cn.methods.add(MethodParser.read(
                    res, out, mn
            ));
        }
        final var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.asASM().accept(cw);
        // TODO figure out supers and the other stuff
        cw.visit(Opcodes.V17, out.access(), out.name().replace('.', '/'), null, "java/lang/Object", null);
        Files.write(Path.of(out.name() + ".class"), cw.toByteArray());
    }

    public static List<ClassInfo> finishResolution(List<ClassInfo> in, ClassResolver resolver) {
        final var classes = new ArrayList<ClassInfo>();
        resolver.discover(in);
        for (final var clazz : in) {
            final var data = new Imports.Impl();
            clazz.imports().forEach(imp -> imp.resolve(data, resolver));
            classes.add(new ClassInfo(
                    clazz.access(), clazz.name(), clazz.signature(),
                    clazz.supers().stream().map(data::getClass).toList(),
                    clazz.fields().stream().map(f -> new FieldInfo(
                            f.access(), f.name(),
                            data.getClass(f.type()),
                            f.signature(), f.initialValue()
                    )).toList(),
                    clazz.methods().stream().map(m -> new MethodInfo(
                            m.access(), m.name(), data.getClass(m.returnType()),
                            m.signature(), m.exceptions().stream().map(data::getClass).toList(),
                            m.lines(), m.parameters().stream().map(param -> new MethodInfo.Parameter(
                                    param.name(), data.getClass(param.type()), param.defaultValue()
                            )).toList()
                    )).toList(),
                    clazz.imports(), clazz.type(),
                    data
            ));
        }
        return classes;
    }

    // TODO throw exceptions
    // TODO figure out generics
    public static ClassInfo discover(String data) {
        data = data.replace(System.lineSeparator(), "\n");
        @Nullable
        Comment comment = null;
        final var chars = data.toCharArray();
        final StringBuilder withoutComments = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            final var isEscaped = i != 0 && chars[i - 1] == '\\';
            final Character next = i < chars.length - 1 ? chars[i + 1] : null;
            final Character prev = i > 0 ? chars[i - 1] : null;
            final var ch = chars[i];
            // TODO figure out better comment parsing
            if (!isEscaped) {
                if (ch == '/') {
                    if (next != null) {
                        if (next == '*') {
                            comment = Comment.MULTILINE;
                        } else if (next == '/') {
                            comment = Comment.SINGLE;
                        }
                    }
                    if (prev != null && prev == '*' && comment != null) {
                        comment = null;
                    }
                } else if (ch == '#') comment = Comment.SINGLE;
            }
            if (comment == Comment.SINGLE && ch == '\n') {
                comment = null;
            }
            if (comment != null) continue;
            withoutComments.append(ch);
        }
        final var withoutComment = withoutComments.toString();
        final var normalDelimiter = Pattern.compile("( |;|\n)");
        var scanner = new Scanner(withoutComment);
        final List<Import> imports = new ArrayList<>();
        final List<String> modifiers = new ArrayList<>();
        final List<String> toExtend = new ArrayList<>();
        String next;
        scanner.useDelimiter("(\n|;)");
        String pkg = "";
        final var packageDeclaration = scanner.next();
        if (packageDeclaration.startsWith("package ")) {
            pkg = packageDeclaration.replace("package ", "");
        } else {
            final var remainder = packageDeclaration + remaining(scanner);
            scanner = new Scanner(remainder)
                    .useDelimiter("(\n|;)");
        }
        while (!Modifiers.startsWithModifier(next = scanner.next())) {
            if (!next.isBlank())
                imports.add(Import.from(next.trim()));
        }
        scanner = new Scanner(next + "" + remaining(scanner)).useDelimiter(" ");
        while (!ClassInfo.Type.BY_NAME.containsKey(next = scanner.next().trim())) {
            modifiers.add(next.trim());
        }
        scanner.useDelimiter(normalDelimiter);
        final ClassInfo.Type type = ClassInfo.Type.BY_NAME.get(next);
        scanner.useDelimiter("(:|\\{)");
        next = scanner.next().trim();
        // TODO figure out signature from this
        final String simpleName = next;
        final String name = (pkg.isBlank() ? "" : pkg + ".") + next;
        scanner.useDelimiter(normalDelimiter);
        next = scanner.next().trim();
        if (next.equals(":")) {
            scanner.useDelimiter(",|\n");
            while (scanner.hasNext()) {
                next = scanner.next().trim();
                final var ends = next.endsWith("{");
                if (ends) {
                    next = next.substring(0, next.indexOf('{')).trim();
                }
                if (!next.isBlank())
                    toExtend.add(next.trim());
                if (ends) break;
            }
        }
        scanner.useDelimiter("(;|\n)");

        final var fields = new ArrayList<FieldInfo>();
        final var methods = new ArrayList<MethodInfo>();

        boolean insideMethod = false;
        var methodLine = new StringBuilder();
        while (scanner.hasNext()) {
            next = scanner.next().trim();
            if (next.isBlank()) continue;
            else if (next.endsWith("}")) {
                if (insideMethod || next.contains("fn ")) {
                    next = next.substring(0, next.lastIndexOf('}')).trim();
                    insideMethod = false;
                    methodLine.append(next);

                    final var mthdModifiers = new ArrayList<String>();
                    final var mthScanner = new Scanner(methodLine.toString()).useDelimiter(" ");
                    while (mthScanner.hasNext() && Modifiers.get(next = mthScanner.next().trim()) != null || next.equals("fn")) {
                        if (!next.equals("fn"))
                            mthdModifiers.add(next);
                    }
                    final var remaining = next + remaining(mthScanner);
                    final var bracketIndex = remaining.indexOf('{');
                    final var declaration = remaining.substring(0, bracketIndex).trim();
                    final var methodText = remaining.substring(bracketIndex + 1).split("\n");
                    final String returnType;
                    final List<MethodInfo.Parameter> parameters = new ArrayList<>();
                    String methodName;
                    final List<String> exceptions = new ArrayList<>();

                    {
                        methodName = declaration.substring(0, declaration.indexOf('(')).replace(" ", "");
                        if (methodName.equals(simpleName)) methodName = "<init>";
                        final var argsSplit = declaration.substring(declaration.indexOf('(') + 1, declaration.indexOf(')')).split(",");
                        for (String arg : argsSplit) {
                            if (!arg.isBlank()) {
                                final var split = arg.split(" ");
                                final var reminder = new StringBuilder();
                                for (int x = 2; x < split.length; x++) {
                                    reminder.append(split[x]);
                                }
                                final String defaultValue;
                                final var rem = reminder.toString();
                                if (rem.indexOf('=') >= 0) {
                                    defaultValue = rem.substring(rem.indexOf('=') + 1);
                                } else {
                                    defaultValue = null;
                                }
                                parameters.add(new MethodInfo.Parameter(split[1], split[0], defaultValue));
                            }
                        }

                        final var returnIndex = declaration.indexOf("):");
                        if (!methodName.equals("<init>") && returnIndex >= 0) {
                            String actual = null;
                            final var retDecl = declaration.substring(returnIndex + 2).split(" ");
                            for (String decl : retDecl) {
                                if (!decl.isBlank() && actual == null) {
                                    actual = decl;
                                }
                            }
                            returnType = actual;
                        } else {
                            returnType = null;
                        }

                        final var throwsIndex = declaration.indexOf("_>");
                        if (throwsIndex >= 0) {
                            final var fromThrows = declaration.substring(throwsIndex + 2);
                            final var open = fromThrows.indexOf('(');
                            final var close = fromThrows.indexOf(')');
                            final var throwsIn = fromThrows.substring(open + 1, close).trim();
                            for (String doThrow : throwsIn.split(",")) {
                                doThrow = doThrow.replace(" ", "");
                                if (!doThrow.isBlank()) {
                                    exceptions.add(doThrow);
                                }
                            }
                        }
                    }

                    methods.add(new MethodInfo(
                            Modifiers.collect(mthdModifiers, Opcodes.ACC_PUBLIC),
                            methodName, returnType, null, exceptions,
                            List.of(methodText), parameters
                    ));
                    methodLine = new StringBuilder();
                    continue;
                } else {
                    break;
                }
            }
            if (insideMethod) {
                methodLine.append(next).append('\n');
                continue;
            }
            if (next.contains("fn ")) {
                insideMethod = true;
                methodLine.append(next);
            } else {
                final var fModifiers = new ArrayList<String>();
                final var lineScanner = new Scanner(next).useDelimiter(" ");
                while (lineScanner.hasNext() && Modifiers.get(next = lineScanner.next().trim()) != null) {
                    fModifiers.add(next);
                }
                final var fieldType = next;
                final var fieldName = lineScanner.next();
                final var remaining = lineScanner.hasNext() ? lineScanner.useDelimiter("").next().trim() : "";
                final Object defaultValue;
                if (!remaining.isBlank() && remaining.startsWith("=")) {
                    String def = remaining.substring(1);
                    if (def.endsWith(";")) def = def.substring(0, def.length() - 1);
                    defaultValue = def;
                } else {
                    defaultValue = null;
                }
                fields.add(new FieldInfo(
                        Modifiers.collect(fModifiers, Opcodes.ACC_PUBLIC),
                        fieldName, fieldType, null, defaultValue
                ));
            }
        }
        return new ClassInfo(
                Modifiers.collect(modifiers, Opcodes.ACC_PUBLIC),
                name, null, toExtend, fields,
                methods, imports, type, null
        );
    }

    enum Comment {
        MULTILINE, SINGLE
    }

    private static String remaining(Scanner scanner) {
        final var sb = new StringBuilder();
        scanner.useDelimiter("").forEachRemaining(sb::append);
        return sb.toString();
    }
}
