package silence.simsool.protector.obfuscator.core;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class HierarchyClassWriter extends ClassWriter {

	private final Map<String, ClassInfo> classes;

	HierarchyClassWriter(int flags, Map<String, byte[]> entries) {
		super(flags);
		this.classes = readClasses(entries);
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		if (type1.equals(type2)) {
			return type1;
		}
		if (type1.startsWith("[") || type2.startsWith("[")) {
			return commonArrayType(type1, type2);
		}
		if (isAssignableFrom(type1, type2)) {
			return type1;
		}
		if (isAssignableFrom(type2, type1)) {
			return type2;
		}
		if (isInterface(type1) || isInterface(type2)) {
			return "java/lang/Object";
		}

		String current = type1;
		while (current != null) {
			ClassInfo info = classes.get(current);
			if (info == null) {
				break;
			}
			current = info.superName;
			if (current != null && isAssignableFrom(current, type2)) {
				return current;
			}
		}

		return "java/lang/Object";
	}

	private boolean isAssignableFrom(String target, String source) {
		if (target.equals(source) || target.equals("java/lang/Object")) {
			return true;
		}

		ArrayDeque<String> queue = new ArrayDeque<>();
		Set<String> visited = new HashSet<>();
		queue.add(source);

		while (!queue.isEmpty()) {
			String current = queue.removeFirst();
			if (!visited.add(current)) {
				continue;
			}
			if (target.equals(current)) {
				return true;
			}

			ClassInfo info = classes.get(current);
			if (info == null) {
				continue;
			}
			if (info.superName != null) {
				queue.addLast(info.superName);
			}
			for (String iface : info.interfaces) {
				queue.addLast(iface);
			}
		}

		return false;
	}

	private boolean isInterface(String type) {
		ClassInfo info = classes.get(type);
		return info != null && (info.access & Opcodes.ACC_INTERFACE) != 0;
	}

	private String commonArrayType(String type1, String type2) {
		if (!type1.startsWith("[") || !type2.startsWith("[")) {
			return "java/lang/Object";
		}

		Type first = Type.getType(type1);
		Type second = Type.getType(type2);
		if (first.getDimensions() != second.getDimensions()) {
			return "java/lang/Object";
		}

		Type firstElement = first.getElementType();
		Type secondElement = second.getElementType();
		if (firstElement.getSort() != Type.OBJECT || secondElement.getSort() != Type.OBJECT) {
			return "java/lang/Object";
		}

		String commonElement = getCommonSuperClass(firstElement.getInternalName(), secondElement.getInternalName());
		return "[".repeat(first.getDimensions()) + "L" + commonElement + ";";
	}

	private static Map<String, ClassInfo> readClasses(Map<String, byte[]> entries) {
		Map<String, ClassInfo> result = new HashMap<>();
		for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
			if (!entry.getKey().endsWith(".class") || entry.getKey().equals("module-info.class")) {
				continue;
			}

			ClassReader reader = new ClassReader(entry.getValue());
			result.put(
				reader.getClassName(),
				new ClassInfo(reader.getAccess(), reader.getSuperName(), reader.getInterfaces())
			);
		}
		return result;
	}

	private record ClassInfo(int access, String superName, String[] interfaces) {}
}