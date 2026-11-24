package exiledsector;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record CompiledClass(String name, String superName, List<String> interfaces, int access, Set<String> referencedTypes,
                     List<MemberRef> memberRefs, Map<String, Integer> memberAccess) {

    static final int ACC_PRIVATE = 0x0002;
    static final int ACC_STATIC = 0x0008;
    static final int ACC_INTERFACE = 0x0200;
    static final int ACC_ABSTRACT = 0x0400;

    private static final int CLASS_FILE_HEADER_BYTES = 8;
    private static final Pattern TYPE_IN_DESCRIPTOR = Pattern.compile("L([\\w/$]+)[;<]");

    record MemberRef(String owner, String name, String descriptor, boolean field) {

        String signature() {
            return signature(field, name, descriptor);
        }

        static String signature(boolean field, String name, String descriptor) {
            return (field ? "field " : "method ") + name + descriptor;
        }

        @Override
        public String toString() {
            return owner + "." + name + (field ? " " : "") + descriptor;
        }
    }

    boolean isConcrete() {
        return (access & (ACC_INTERFACE | ACC_ABSTRACT)) == 0;
    }

    boolean isInterface() {
        return (access & ACC_INTERFACE) != 0;
    }

    List<String> supertypes() {
        List<String> supertypes = new ArrayList<>(interfaces);
        if (superName != null) {
            supertypes.add(0, superName);
        }
        return supertypes;
    }

    boolean declares(String signature, boolean includingPrivate) {
        Integer memberFlags = memberAccess.get(signature);
        return memberFlags != null && (includingPrivate || (memberFlags & ACC_PRIVATE) == 0);
    }

    Set<String> abstractMethods() {
        return methodsWhere(flags -> (flags & ACC_ABSTRACT) != 0);
    }

    Set<String> implementedMethods() {
        return methodsWhere(flags -> (flags & (ACC_ABSTRACT | ACC_STATIC | ACC_PRIVATE)) == 0);
    }

    private Set<String> methodsWhere(IntPredicate flagsMatch) {
        Set<String> methods = new TreeSet<>();
        memberAccess.forEach((signature, flags) -> {
            if (signature.startsWith("method ") && flagsMatch.test(flags)) {
                methods.add(signature);
            }
        });
        return methods;
    }

    static CompiledClass read(InputStream classBytes) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(classBytes));
        in.skipNBytes(CLASS_FILE_HEADER_BYTES);
        ConstantPool pool = ConstantPool.read(in);
        int access = in.readUnsignedShort();
        String name = pool.className(in.readUnsignedShort());
        int superIndex = in.readUnsignedShort();
        String superName = superIndex == 0 ? null : pool.className(superIndex);
        List<String> interfaces = new ArrayList<>();
        int interfaceCount = in.readUnsignedShort();
        for (int i = 0; i < interfaceCount; i++) {
            interfaces.add(pool.className(in.readUnsignedShort()));
        }
        Map<String, Integer> memberAccess = new HashMap<>();
        readMembers(in, pool, true, memberAccess);
        readMembers(in, pool, false, memberAccess);
        return new CompiledClass(name, superName, interfaces, access, pool.referencedTypes(), pool.memberRefs(), memberAccess);
    }

    private static void readMembers(DataInputStream in, ConstantPool pool, boolean fields, Map<String, Integer> memberAccess)
            throws IOException {
        int memberCount = in.readUnsignedShort();
        for (int i = 0; i < memberCount; i++) {
            int access = in.readUnsignedShort();
            memberAccess.put(MemberRef.signature(fields, pool.utf8(in.readUnsignedShort()), pool.utf8(in.readUnsignedShort())), access);
            skipAttributes(in);
        }
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int attributeCount = in.readUnsignedShort();
        for (int i = 0; i < attributeCount; i++) {
            in.readUnsignedShort();
            in.skipNBytes(in.readInt() & 0xFFFFFFFFL);
        }
    }

    private static final class ConstantPool {

        private static final int UTF8 = 1;
        private static final int INTEGER = 3;
        private static final int FLOAT = 4;
        private static final int LONG = 5;
        private static final int DOUBLE = 6;
        private static final int CLASS = 7;
        private static final int STRING = 8;
        private static final int FIELD_REF = 9;
        private static final int METHOD_REF = 10;
        private static final int INTERFACE_METHOD_REF = 11;
        private static final int NAME_AND_TYPE = 12;
        private static final int METHOD_HANDLE = 15;
        private static final int METHOD_TYPE = 16;
        private static final int DYNAMIC = 17;
        private static final int INVOKE_DYNAMIC = 18;
        private static final int MODULE = 19;
        private static final int PACKAGE = 20;

        private final int[] tags;
        private final int[] firstIndexes;
        private final int[] secondIndexes;
        private final String[] utf8Values;

        private ConstantPool(int size) {
            tags = new int[size];
            firstIndexes = new int[size];
            secondIndexes = new int[size];
            utf8Values = new String[size];
        }

        static ConstantPool read(DataInputStream in) throws IOException {
            ConstantPool pool = new ConstantPool(in.readUnsignedShort());
            for (int index = 1; index < pool.tags.length; index++) {
                int tag = in.readUnsignedByte();
                pool.tags[index] = tag;
                switch (tag) {
                    case UTF8 -> pool.utf8Values[index] = in.readUTF();
                    case INTEGER, FLOAT -> in.skipNBytes(4);
                    case LONG, DOUBLE -> {
                        in.skipNBytes(8);
                        index++;
                    }
                    case CLASS, STRING, METHOD_TYPE, MODULE, PACKAGE -> pool.firstIndexes[index] = in.readUnsignedShort();
                    case FIELD_REF, METHOD_REF, INTERFACE_METHOD_REF, NAME_AND_TYPE, DYNAMIC, INVOKE_DYNAMIC -> {
                        pool.firstIndexes[index] = in.readUnsignedShort();
                        pool.secondIndexes[index] = in.readUnsignedShort();
                    }
                    case METHOD_HANDLE -> {
                        in.readUnsignedByte();
                        pool.firstIndexes[index] = in.readUnsignedShort();
                    }
                    default -> throw new IOException("Unknown constant pool tag " + tag);
                }
            }
            return pool;
        }

        String utf8(int index) {
            return utf8Values[index];
        }

        String className(int classIndex) {
            return utf8(firstIndexes[classIndex]);
        }

        Set<String> referencedTypes() {
            Set<String> types = new TreeSet<>();
            for (int index = 1; index < tags.length; index++) {
                if (tags[index] == CLASS) {
                    String name = className(index);
                    if (name.startsWith("[")) {
                        addTypesInDescriptor(name, types);
                    } else {
                        types.add(name);
                    }
                } else if (tags[index] == UTF8) {
                    addTypesInDescriptor(utf8Values[index], types);
                }
            }
            return types;
        }

        private static void addTypesInDescriptor(String text, Set<String> types) {
            Matcher type = TYPE_IN_DESCRIPTOR.matcher(text);
            while (type.find()) {
                types.add(type.group(1));
            }
        }

        List<MemberRef> memberRefs() {
            List<MemberRef> refs = new ArrayList<>();
            for (int index = 1; index < tags.length; index++) {
                int tag = tags[index];
                if (tag == FIELD_REF || tag == METHOD_REF || tag == INTERFACE_METHOD_REF) {
                    int nameAndType = secondIndexes[index];
                    refs.add(new MemberRef(className(firstIndexes[index]), utf8(firstIndexes[nameAndType]),
                            utf8(secondIndexes[nameAndType]), tag == FIELD_REF));
                }
            }
            return refs;
        }
    }
}
