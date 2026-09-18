package silence.simsool.protector.obfuscator.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import silence.simsool.protector.obfuscator.config.ObfuscationConfig;

final class NameObfuscator {

	private static final String LOGIC_OWNER = "silence/simsool/protector/SLogic";
	private static final Pattern MIXIN_PACKAGE = Pattern.compile("\\\"package\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
	private final Random random;
	private final Map<String, ClassNode> classes = new LinkedHashMap<>();
	private final Map<String, String> classNames = new LinkedHashMap<>();
	private final Map<MemberKey, String> methodNames = new HashMap<>();
	private final Map<MemberKey, String> fieldNames = new HashMap<>();
	private final Set<String> usedClassNames = new HashSet<>();
	private final Map<String, String> groupedPackages = new HashMap<>();
	private final Map<String, String> nestPackages = new HashMap<>();
	private ObfuscationConfig config;

	NameObfuscator(Random random) {
		this.random = random != null ? random : new Random();
	}

	Map<String, byte[]> remap(Map<String, byte[]> entries, ObfuscationConfig config) {
		this.config = config;
		loadClasses(entries);
		loadGroupedPackages(entries);
		loadNestPackages();
		buildMappings();

		Map<String, byte[]> result = new LinkedHashMap<>();
		Remapper remapper = createRemapper();

		for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
			String entryName = entry.getKey();
			if (!entryName.endsWith(".class") || entryName.equals("module-info.class")) {
				result.put(entryName, entry.getValue());
				continue;
			}

			ClassReader reader = new ClassReader(entry.getValue());
			ClassNode node = new ClassNode();
			reader.accept(node, ClassReader.SKIP_DEBUG);
			if (config.randomizePackages()) {
				widenAccess(node);
			}
			ClassWriter writer = new ClassWriter(0);
			node.accept(new ClassRemapper(writer, remapper));
			String oldName = reader.getClassName();
			String newName = classNames.getOrDefault(oldName, oldName);
			result.put(newName + ".class", writer.toByteArray());
		}

		return result;
	}

	Map<String, String> classMappings() {
		return Map.copyOf(classNames);
	}

	private void loadClasses(Map<String, byte[]> entries) {
		classes.clear();
		for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
			if (!entry.getKey().endsWith(".class") || entry.getKey().equals("module-info.class")) {
				continue;
			}
			ClassNode node = new ClassNode();
			new ClassReader(entry.getValue()).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			classes.put(node.name, node);
		}
	}

	private void loadGroupedPackages(Map<String, byte[]> entries) {
		groupedPackages.clear();
		for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
			if (!entry.getKey().endsWith(".json") || !entry.getKey().contains("mixin")) {
				continue;
			}
			String json = new String(entry.getValue(), StandardCharsets.UTF_8);
			Matcher matcher = MIXIN_PACKAGE.matcher(json);
			if (matcher.find()) {
				String oldPackage = matcher.group(1).replace('.', '/');
				groupedPackages.putIfAbsent(oldPackage, randomPackage());
			}
		}
	}

	private void loadNestPackages() {
		nestPackages.clear();
		for (ClassNode node : classes.values()) {
			String host = nestRoot(node);
			nestPackages.putIfAbsent(host, randomPackage());
		}
	}

	private String nestRoot(ClassNode node) {
		if (node.nestHostClass != null) {
			return node.nestHostClass;
		}
		if (node.nestMembers != null && !node.nestMembers.isEmpty()) {
			return node.name;
		}
		int inner = node.name.indexOf('$');
		if (inner > 0 && classes.containsKey(node.name.substring(0, inner))) {
			return node.name.substring(0, inner);
		}
		return node.name;
	}

	private void widenAccess(ClassNode node) {
		node.access = makePublic(node.access);
		if (node.innerClasses != null) {
			node.innerClasses.forEach(inner -> {
				if (classes.containsKey(inner.name) && (inner.access & Opcodes.ACC_PRIVATE) == 0) {
					inner.access = makePublic(inner.access);
				}
			});
		}
		for (FieldNode field : node.fields) {
			if ((field.access & Opcodes.ACC_PRIVATE) == 0) {
				field.access = makePublic(field.access);
			}
		}
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_PRIVATE) == 0) {
				method.access = makePublic(method.access);
			}
		}
	}

	private int makePublic(int access) {
		return (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}

	private void buildMappings() {
		classNames.clear();
		methodNames.clear();
		fieldNames.clear();
		usedClassNames.clear();
		usedClassNames.addAll(classes.keySet());

		for (ClassNode node : classes.values()) {
			if (node.name.equals(LOGIC_OWNER)) {
				classNames.put(node.name, node.name);
				continue;
			}

			if (isIncludeOrExcludeAnnotation(node)) {
				classNames.put(node.name, node.name);
				continue;
			}

			String oldPackage = packageOf(node.name);
			String oldSimpleName = simpleName(node.name);
			String targetPackage = oldPackage;

			if (config.randomizePackages()) {
				targetPackage = groupedPackage(node.name);
				if (targetPackage == null) {
					targetPackage = nestPackages.get(nestRoot(node));
				}
			}

			boolean canRenameClass = config.renameClasses();
			if (canRenameClass && config.preserveAnnotations() && hasPreservedAnnotations(node)) {
				canRenameClass = false;
			}

			String simpleName = oldSimpleName;
			if (canRenameClass) {
				for (int i = 0; ; i++) {
					String name = getOverloadedName(i);
					String cand = joinPackage(targetPackage, name);
					if (cand.equals(node.name) || !usedClassNames.contains(cand)) {
						simpleName = name;
						break;
					}
				}
			}

			String candidate = joinPackage(targetPackage, simpleName);
			classNames.put(node.name, candidate);
			usedClassNames.add(candidate);
		}

		for (ClassNode node : classes.values()) {
			Set<String> usedMethods = new HashSet<>();
			Set<String> usedFields = new HashSet<>();
			for (MethodNode method : node.methods) {
				usedMethods.add(method.name + method.desc);
			}
			for (FieldNode field : node.fields) {
				usedFields.add(field.name + field.desc);
			}

			if (config.renameMethods()) {
				for (MethodNode method : node.methods) {
					if (!canRenameMethod(node, method)) {
						continue;
					}

					String candidate = null;
					for (int i = 0; ; i++) {
						String name = getOverloadedName(i);
						if (!usedMethods.contains(name + method.desc)) {
							candidate = name;
							break;
						}
					}
					methodNames.put(new MemberKey(node.name, method.name, method.desc), candidate);
					usedMethods.add(candidate + method.desc);
				}
			}

			if (config.renameFields()) {
				for (FieldNode field : node.fields) {
					if (field.name.startsWith("$sp$") || (field.access & Opcodes.ACC_PRIVATE) == 0) {
						continue;
					}
					if (config.preserveAnnotations() && hasPreservedAnnotations(field)) {
						continue;
					}

					String candidate = null;
					for (int i = 0; ; i++) {
						String name = getOverloadedName(i);
						if (!usedFields.contains(name + field.desc)) {
							candidate = name;
							break;
						}
					}
					fieldNames.put(new MemberKey(node.name, field.name, field.desc), candidate);
					usedFields.add(candidate + field.desc);
				}
			}
		}
	}

	private String packageOf(String className) {
		int slash = className.lastIndexOf('/');
		return slash < 0 ? "" : className.substring(0, slash);
	}

	private String simpleName(String className) {
		int slash = className.lastIndexOf('/');
		return slash < 0 ? className : className.substring(slash + 1);
	}

	private String joinPackage(String packageName, String simpleName) {
		return packageName == null || packageName.isEmpty() ? simpleName : packageName + "/" + simpleName;
	}

	private String groupedPackage(String className) {
		String best = null;
		for (String oldPackage : groupedPackages.keySet()) {
			if (className.startsWith(oldPackage + "/") && (best == null || oldPackage.length() > best.length())) {
				best = oldPackage;
			}
		}
		return best == null ? null : groupedPackages.get(best);
	}

	private boolean canRenameMethod(ClassNode owner, MethodNode method) {
		if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
			return false;
		}
		if (method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V") && (method.access & Opcodes.ACC_STATIC) != 0) {
			return false;
		}
		if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0) {
			return false;
		}
		if ((method.access & Opcodes.ACC_PRIVATE) == 0) {
			if (owner.superName != null && !owner.superName.equals("java/lang/Object")) {
				return false;
			}
			if (owner.interfaces != null && !owner.interfaces.isEmpty()) {
				return false;
			}
		}
		if (config.preserveAnnotations() && hasPreservedAnnotations(method)) {
			return false;
		}
		return true;
	}

	private boolean isIncludeOrExcludeAnnotation(ClassNode node) {
		String simple = simpleName(node.name);
		if (!simple.equalsIgnoreCase("Include") && !simple.equalsIgnoreCase("Exclude")) {
			return false;
		}
		return (node.access & Opcodes.ACC_ANNOTATION) != 0
			|| (node.interfaces != null && node.interfaces.contains("java/lang/annotation/Annotation"));
	}

	private boolean isIgnoredAnnotation(String desc) {
		if (desc == null) {
			return false;
		}
		String name = desc;
		if (name.startsWith("L") && name.endsWith(";")) {
			name = name.substring(1, name.length() - 1);
		}
		int slash = name.lastIndexOf('/');
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		return name.equalsIgnoreCase("Include") || name.equalsIgnoreCase("Exclude");
	}

	private boolean hasPreservedAnnotations(List<? extends AnnotationNode> annotations) {
		if (annotations == null || annotations.isEmpty()) {
			return false;
		}
		for (AnnotationNode ann : annotations) {
			if (ann != null && !isIgnoredAnnotation(ann.desc)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasPreservedAnnotations(ClassNode node) {
		return hasPreservedAnnotations(node.visibleAnnotations)
			|| hasPreservedAnnotations(node.invisibleAnnotations)
			|| hasPreservedAnnotations(node.visibleTypeAnnotations)
			|| hasPreservedAnnotations(node.invisibleTypeAnnotations);
	}

	private boolean hasPreservedAnnotations(FieldNode field) {
		return hasPreservedAnnotations(field.visibleAnnotations)
			|| hasPreservedAnnotations(field.invisibleAnnotations)
			|| hasPreservedAnnotations(field.visibleTypeAnnotations)
			|| hasPreservedAnnotations(field.invisibleTypeAnnotations);
	}

	private boolean hasPreservedAnnotations(MethodNode method) {
		if (hasPreservedAnnotations(method.visibleAnnotations)
			|| hasPreservedAnnotations(method.invisibleAnnotations)
			|| hasPreservedAnnotations(method.visibleTypeAnnotations)
			|| hasPreservedAnnotations(method.invisibleTypeAnnotations)) {
			return true;
		}

		if (method.visibleParameterAnnotations != null) {
			for (List<AnnotationNode> annotations : method.visibleParameterAnnotations) {
				if (hasPreservedAnnotations(annotations)) {
					return true;
				}
			}
		}
		if (method.invisibleParameterAnnotations != null) {
			for (List<AnnotationNode> annotations : method.invisibleParameterAnnotations) {
				if (hasPreservedAnnotations(annotations)) {
					return true;
				}
			}
		}

		return false;
	}

	private Remapper createRemapper() {
		return new Remapper() {
			@Override
			public String map(String internalName) {
				return classNames.getOrDefault(internalName, internalName);
			}

			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				String mapped = findInheritedMapping(methodNames, owner, name, descriptor, new HashSet<>());
				return mapped == null ? name : mapped;
			}

			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				String mapped = findInheritedMapping(fieldNames, owner, name, descriptor, new HashSet<>());
				return mapped == null ? name : mapped;
			}
		};
	}

	private String findInheritedMapping(Map<MemberKey, String> mappings, String owner, String name, String desc, Set<String> visited) {
		if (!visited.add(owner)) {
			return null;
		}
		String direct = mappings.get(new MemberKey(owner, name, desc));
		if (direct != null) {
			return direct;
		}
		ClassNode node = classes.get(owner);
		if (node == null) {
			return null;
		}
		if (node.superName != null) {
			String inherited = findInheritedMapping(mappings, node.superName, name, desc, visited);
			if (inherited != null) {
				return inherited;
			}
		}
		for (String iface : node.interfaces) {
			String inherited = findInheritedMapping(mappings, iface, name, desc, visited);
			if (inherited != null) {
				return inherited;
			}
		}
		return null;
	}

	private String randomPackage() {
		String root = config.packageRootInternalName();
		String depthMode = config.packageDepth() != null ? config.packageDepth() : "1 - 3";

		int minD;
		int maxD;
		switch (depthMode) {
			case "Flat" -> {
				minD = 0;
				maxD = 0;
			}
			case "2 - 4" -> {
				minD = 2;
				maxD = 4;
			}
			default -> { // "1 - 3"
				minD = 1;
				maxD = 3;
			}
		}

		int depth;
		if (minD == 0 && maxD == 0) {
			depth = root.isEmpty() ? 1 : 0;
		} else {
			int rootDepth = root.isEmpty() ? 0 : root.split("/").length;
			int targetDepth = minD + random.nextInt(maxD - minD + 1);
			depth = Math.max(1, targetDepth - rootDepth);
		}

		StringBuilder result = new StringBuilder(root);
		for (int i = 0; i < depth; i++) {
			if (!result.isEmpty()) {
				result.append('/');
			}
			result.append(randomName(2, 2));
		}
		return result.toString();
	}

	private String getOverloadedName(int index) {
		StringBuilder sb = new StringBuilder();
		int n = index;
		while (n >= 0) {
			sb.append((char) ('a' + (n % 26)));
			n = (n / 26) - 1;
		}
		return sb.reverse().toString();
	}

	private String randomName(int minLength, int maxLength) {
		int length = minLength + random.nextInt(maxLength - minLength + 1);
		StringBuilder value = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			value.append((char) ('a' + random.nextInt(26)));
		}
		return value.toString();
	}

	private record MemberKey(String owner, String name, String desc) {}
}