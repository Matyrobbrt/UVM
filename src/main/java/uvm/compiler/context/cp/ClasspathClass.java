package uvm.compiler.context.cp;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import uvm.compiler.context.discovery.MethodInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface ClasspathClass {
    String name();

    int access();

    List<? extends ClasspathField> getFields();

    List<? extends ClasspathMethod> getMethods();

    List<String> supers();

    @Nullable
    default ClasspathField findField(String name) {
        return getFields().stream().filter(f -> f.name().equals(name))
                .findFirst().orElse(null);
    }

    static ClasspathClass of(ClassNode reader) {
        return new ClasspathClass() {
            @Override
            public String name() {
                return reader.name.replace('/', '.');
            }

            @Override
            public int access() {
                return reader.access;
            }

            private final List<String> supers = Stream.concat(
                    (reader.interfaces == null ? List.<String>of() : reader.interfaces).stream(),
                    Stream.of(reader.superName)
            ).filter(Objects::nonNull).toList();

            @Override
            public List<String> supers() {
                return supers;
            }

            private final List<? extends ClasspathField> fields = reader.fields.stream()
                    .map(n -> new ClasspathField() {
                        @Override
                        public int access() {
                            return n.access;
                        }

                        @Override
                        public String name() {
                            return n.name;
                        }

                        private final String type = Type.getType(n.desc)
                                .getInternalName().replace('/', '.');
                        @Override
                        public String type() {
                            return type;
                        }
                    }).toList();

            @Override
            public List<? extends ClasspathField> getFields() {
                return fields;
            }

            private final List<? extends ClasspathMethod> methods = reader.methods.stream()
                    .map(n -> new ClasspathMethod() {
                        @Override
                        public int access() {
                            return n.access;
                        }

                        @Override
                        public String name() {
                            return n.name;
                        }

                        @Override
                        public List<String> exceptions() {
                            return n.exceptions == null ? List.of() : n.exceptions;
                        }

                        private final String returnType = n.desc.substring(n.desc.indexOf(')') + 1);

                        @Override
                        public String returnType() {
                            return returnType.equals("V") ? null : returnType;
                        }

                        private final List<String> descParams = Arrays.stream(Type.getMethodType(n.desc).getArgumentTypes())
                                .map(Type::getDescriptor).toList();
                        private final List<MethodInfo.Parameter> parameters;

                        {
                            List<MethodInfo.Parameter> actual;
                            try {
                                actual = IntStream.range(0, n.parameters.size())
                                        .mapToObj(i -> new MethodInfo.Parameter(n.parameters.get(i).name, descParams.get(i), null))
                                        .toList();
                            } catch (Exception ignored) {
                                actual = IntStream.range(0, descParams.size())
                                        .mapToObj(i -> new MethodInfo.Parameter("arg_" + i, descParams.get(i), null))
                                        .toList();
                            }
                            parameters = actual;
                        }

                        @Override
                        public List<MethodInfo.Parameter> parameters() {
                            return parameters;
                        }

                    }).toList();

            @Override
            public List<? extends ClasspathMethod> getMethods() {
                return methods;
            }
        };
    }
}
