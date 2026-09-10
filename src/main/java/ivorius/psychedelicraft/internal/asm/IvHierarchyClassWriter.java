package ivorius.psychedelicraft.internal.asm;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/** Computes frames without defining classes through LaunchClassLoader. */
final class IvHierarchyClassWriter extends ClassWriter {

    private final Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();

    IvHierarchyClassWriter(int flags, ClassNode transformedClass, String actualName, String transformedName) {
        super(flags);

        ClassInfo current = new ClassInfo(
            transformedClass.name,
            transformedClass.superName,
            transformedClass.interfaces.toArray(new String[transformedClass.interfaces.size()]),
            transformedClass.access);
        classes.put(transformedClass.name, current);
        classes.put(actualName.replace('.', '/'), current);
        classes.put(transformedName.replace('.', '/'), current);
    }

    @Override
    protected String getCommonSuperClass(String left, String right) {
        if (left.equals(right)) {
            return left;
        }
        if (left.startsWith("[") || right.startsWith("[")) {
            return getCommonArrayType(left, right);
        }
        if (isAssignableFrom(left, right)) {
            return left;
        }
        if (isAssignableFrom(right, left)) {
            return right;
        }

        ClassInfo leftInfo = readClass(left);
        if (leftInfo.isInterface()) {
            return "java/lang/Object";
        }

        String superclass = leftInfo.superName;
        while (superclass != null) {
            if (isAssignableFrom(superclass, right)) {
                return superclass;
            }
            superclass = readClass(superclass).superName;
        }
        return "java/lang/Object";
    }

    private String getCommonArrayType(String left, String right) {
        if (!left.startsWith("[") || !right.startsWith("[")) {
            String nonArray = left.startsWith("[") ? right : left;
            if ("java/lang/Cloneable".equals(nonArray) || "java/io/Serializable".equals(nonArray)) {
                return nonArray;
            }
            return "java/lang/Object";
        }

        Type leftType = Type.getType(left);
        Type rightType = Type.getType(right);
        if (leftType.getDimensions() != rightType.getDimensions()) {
            return "java/lang/Object";
        }

        Type leftElement = leftType.getElementType();
        Type rightElement = rightType.getElementType();
        if (leftElement.getSort() != Type.OBJECT || rightElement.getSort() != Type.OBJECT) {
            return "java/lang/Object";
        }

        String element = getCommonSuperClass(leftElement.getInternalName(), rightElement.getInternalName());
        StringBuilder descriptor = new StringBuilder();
        for (int dimension = 0; dimension < leftType.getDimensions(); dimension++) {
            descriptor.append('[');
        }
        descriptor.append('L').append(element).append(';');
        return descriptor.toString();
    }

    private boolean isAssignableFrom(String target, String candidate) {
        if (target.equals(candidate) || "java/lang/Object".equals(target)) {
            return true;
        }
        return hasAncestor(candidate, target, new HashSet<String>());
    }

    private boolean hasAncestor(String candidate, String target, Set<String> visited) {
        if (!visited.add(candidate)) {
            return false;
        }

        ClassInfo info = readClass(candidate);
        if (target.equals(info.superName)) {
            return true;
        }
        if (info.superName != null && hasAncestor(info.superName, target, visited)) {
            return true;
        }
        for (String interfaceName : info.interfaces) {
            if (target.equals(interfaceName) || hasAncestor(interfaceName, target, visited)) {
                return true;
            }
        }
        return false;
    }

    private ClassInfo readClass(String name) {
        ClassInfo cached = classes.get(name);
        if (cached != null) {
            return cached;
        }

        for (String candidate : mappedNames(name)) {
            InputStream stream = openClass(candidate);
            if (stream == null) {
                continue;
            }
            try {
                ClassReader reader = new ClassReader(stream);
                ClassInfo info = new ClassInfo(
                    reader.getClassName(),
                    reader.getSuperName(),
                    reader.getInterfaces(),
                    reader.getAccess());
                classes.put(name, info);
                classes.put(candidate, info);
                classes.put(info.name, info);
                return info;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read class hierarchy for " + name, exception);
            } finally {
                try {
                    stream.close();
                } catch (IOException ignored) {
                    // The hierarchy bytes have already been consumed.
                }
            }
        }
        throw new IllegalStateException("Could not resolve class hierarchy for " + name);
    }

    private Set<String> mappedNames(String name) {
        Set<String> names = new HashSet<String>();
        names.add(name);
        names.add(FMLDeobfuscatingRemapper.INSTANCE.map(name));
        names.add(FMLDeobfuscatingRemapper.INSTANCE.unmap(name));
        return names;
    }

    private InputStream openClass(String name) {
        String resource = name + ".class";
        InputStream stream = Launch.classLoader.getResourceAsStream(resource);
        if (stream == null) {
            stream = IvHierarchyClassWriter.class.getClassLoader().getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = ClassLoader.getSystemResourceAsStream(resource);
        }
        return stream;
    }

    private static final class ClassInfo {

        private final String name;
        private final String superName;
        private final String[] interfaces;
        private final int access;

        private ClassInfo(String name, String superName, String[] interfaces, int access) {
            this.name = name;
            this.superName = superName;
            this.interfaces = interfaces;
            this.access = access;
        }

        private boolean isInterface() {
            return (access & Opcodes.ACC_INTERFACE) != 0;
        }
    }
}
