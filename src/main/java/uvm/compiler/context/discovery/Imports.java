package uvm.compiler.context.discovery;

import org.objectweb.asm.Type;
import uvm.compiler.parse.MethodParser;

import java.util.HashMap;
import java.util.Map;

public interface Imports {
    void put(Import.MappingType type, String in, String out);

    String getClass(String in);

    Type resolveAsType(String in);

    class Impl implements Imports {
        public static final Map<String, MethodParser.PrimitiveData> PRIMITIVES;
        static {
            final var primvs = new HashMap<String, MethodParser.PrimitiveData>();
            primvs.put("int", new MethodParser.PrimitiveData(Type.INT_TYPE, Integer::parseInt));
            primvs.put("str", new MethodParser.PrimitiveData(Type.getType(String.class), val -> val.substring(1, val.length() - 1)));
            PRIMITIVES = Map.copyOf(primvs);
        }

        private final Map<Import.Mapping, String> data = new HashMap<>();

        public Impl() {
            put(Import.MappingType.TYPE, "str", "java.lang.String");
        }

        @Override
        public void put(Import.MappingType type, String in, String out) {
            data.put(new Import.Mapping(type, in), out);
        }

        @Override
        public Type resolveAsType(String in) {
            final var priv = PRIMITIVES.get(in);
            if (priv != null) return priv.type();
            final var transformed = getClass(in);
            return Type.getObjectType(transformed.replace('.', '/'));
        }

        @Override
        public String getClass(String in) {
            if (in == null) return null;
            return data.getOrDefault(new Import.Mapping(Import.MappingType.TYPE, in), in);
        }
    }
}
