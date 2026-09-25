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
	private static final Pattern MIXIN_PACKAGE_PATTERN = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern MIXIN_PLUGIN_PATTERN = Pattern.compile("\"plugin\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern MIXIN_ARRAY_PATTERN = Pattern.compile("(\"(?:mixins|client|server)\"\\s*:\\s*\\[)([\\s\\S]*?)(\\])");
	private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("\"([^\"]+)\"");

	private final Random random;
	private final Map<String, ClassNode> classes = new LinkedHashMap<>();
	private final Map<String, String> classNames = new LinkedHashMap<>();
	private final Map<MemberKey, String> methodNames = new HashMap<>();
	private final Map<MemberKey, String> fieldNames = new HashMap<>();
	private final Set<String> usedClassNames = new HashSet<>();
	private final Set<String> usedPackages = new HashSet<>();
	private final Map<String, String> nestPackages = new HashMap<>();
	private final Set<String> mixinClasses = new HashSet<>();
	private final Set<String> remappedMixinClasses = new HashSet<>();
	private final List<MixinConfigData> mixinConfigs = new ArrayList<>();
	private ObfuscationConfig config;

	NameObfuscator(Random random) {
		this.random = random != null ? random : new Random();
	}

	Map<String, byte[]> remap(Map<String, byte[]> entries, ObfuscationConfig config) {
		this.config = config;
		loadClasses(entries);
		scanMixinConfigs(entries);
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

	boolean isRemappedMixinClass(String internalName) {
		if (remappedMixinClasses.contains(internalName)) {
			return true;
		}
		int dollar = internalName.indexOf('$');
		if (dollar > 0 && remappedMixinClasses.contains(internalName.substring(0, dollar))) {
			return true;
		}
		return false;
	}

	boolean isMixinClass(String internalName) {
		if (mixinClasses.contains(internalName)) {
			return true;
		}
		int dollar = internalName.indexOf('$');
		if (dollar > 0 && mixinClasses.contains(internalName.substring(0, dollar))) {
			return true;
		}
		return false;
	}

	void updateMixinConfigs(Map<String, byte[]> entries) {
		for (MixinConfigData data : mixinConfigs) {
			byte[] raw = entries.get(data.entryName);
			if (raw == null) {
				continue;
			}

			String json = new String(raw, StandardCharsets.UTF_8);

			String newPackageDot = data.newPackage.replace('/', '.');
			Matcher pkgMatcher = MIXIN_PACKAGE_PATTERN.matcher(json);
			if (pkgMatcher.find()) {
				json = json.substring(0, pkgMatcher.start(1)) + newPackageDot + json.substring(pkgMatcher.end(1));
			}

			if (data.oldPlugin != null) {
				String oldPluginInternal = data.oldPlugin.replace('.', '/');
				String newPluginInternal = classNames.get(oldPluginInternal);
				if (newPluginInternal != null) {
					String newPluginDot = newPluginInternal.replace('/', '.');
					Matcher pluginMatcher = MIXIN_PLUGIN_PATTERN.matcher(json);
					if (pluginMatcher.find()) {
						json = json.substring(0, pluginMatcher.start(1)) + newPluginDot + json.substring(pluginMatcher.end(1));
					}
				}
			}

			Matcher arrayMatcher = MIXIN_ARRAY_PATTERN.matcher(json);
			StringBuilder sb = new StringBuilder();
			while (arrayMatcher.find()) {
				String prefix = arrayMatcher.group(1);
				String body = arrayMatcher.group(2);
				String suffix = arrayMatcher.group(3);

				Matcher itemMatcher = STRING_LITERAL_PATTERN.matcher(body);
				StringBuilder newBody = new StringBuilder();
				while (itemMatcher.find()) {
					String oldRel = itemMatcher.group(1).trim();
					String newRel = data.relativeClassMappings.getOrDefault(oldRel, oldRel);
					itemMatcher.appendReplacement(newBody, Matcher.quoteReplacement("\"" + newRel + "\""));
				}
				itemMatcher.appendTail(newBody);

				arrayMatcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + newBody.toString() + suffix));
			}
			arrayMatcher.appendTail(sb);
			json = sb.toString();

			entries.put(data.entryName, json.getBytes(StandardCharsets.UTF_8));
		}
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

	private void scanMixinConfigs(Map<String, byte[]> entries) {
		mixinConfigs.clear();
		mixinClasses.clear();
		remappedMixinClasses.clear();
		usedPackages.clear();
		usedClassNames.clear();
		usedClassNames.addAll(classes.keySet());

		for (ClassNode node : classes.values()) {
			if (hasMixinAnnotation(node)) {
				mixinClasses.add(node.name);
			}
		}

		Map<String, String> mixinSubPkgMap = new HashMap<>();

		for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
			String entryName = entry.getKey();
			if (!entryName.endsWith(".json")) {
				continue;
			}
			String json = new String(entry.getValue(), StandardCharsets.UTF_8);
			Matcher pkgMatcher = MIXIN_PACKAGE_PATTERN.matcher(json);
			if (!pkgMatcher.find()) {
				continue;
			}
			Matcher arrayCheck = MIXIN_ARRAY_PATTERN.matcher(json);
			if (!entryName.contains("mixin") && !arrayCheck.find()) {
				continue;
			}

			String oldPackageDot = pkgMatcher.group(1).trim();
			String oldPackage = oldPackageDot.replace('.', '/');

			String oldPlugin = null;
			Matcher pluginMatcher = MIXIN_PLUGIN_PATTERN.matcher(json);
			if (pluginMatcher.find()) {
				oldPlugin = pluginMatcher.group(1).trim();
			}

			boolean fixedPath = config.mixinFixedPathEnabled();
			String newPackage = fixedPath ? config.mixinBasePackageInternalName() : randomUniquePackage();
			usedPackages.add(newPackage);
			MixinConfigData configData = new MixinConfigData(entryName, oldPackage, newPackage, oldPlugin);

			Matcher arrayMatcher = MIXIN_ARRAY_PATTERN.matcher(json);
			while (arrayMatcher.find()) {
				String arrayBody = arrayMatcher.group(2);
				Matcher itemMatcher = STRING_LITERAL_PATTERN.matcher(arrayBody);
				while (itemMatcher.find()) {
					String relativeName = itemMatcher.group(1).trim();
					if (relativeName.isEmpty()) {
						continue;
					}
					String oldFullInternal = oldPackage + "/" + relativeName.replace('.', '/');
					mixinClasses.add(oldFullInternal);

					int lastDot = relativeName.lastIndexOf('.');
					String originalSubPkg = (lastDot >= 0) ? relativeName.substring(0, lastDot) : "";
					String originalSimpleName = (lastDot >= 0) ? relativeName.substring(lastDot + 1) : relativeName;

					String targetClassPackage;
					String newRelativePrefix;
					if (fixedPath) {
						if (originalSubPkg.isEmpty()) {
							targetClassPackage = newPackage;
							newRelativePrefix = "";
						} else {
							String remappedSub;
							if (config.randomizePackages()) {
								remappedSub = mixinSubPkgMap.computeIfAbsent(originalSubPkg, p -> remapMixinSubPackage(newPackage, p));
							} else {
								remappedSub = originalSubPkg.replace('.', '/');
							}
							targetClassPackage = newPackage + "/" + remappedSub;
							usedPackages.add(targetClassPackage);
							newRelativePrefix = remappedSub.replace('/', '.') + ".";
						}
					} else {
						targetClassPackage = newPackage;
						newRelativePrefix = "";
					}

					String newSimpleName;
					if (config.renameClasses()) {
						for (int i = 0; ; i++) {
							String candidate = getOverloadedName(i);
							String fullCandidate = targetClassPackage + "/" + candidate;
							if (!usedClassNames.contains(fullCandidate)) {
								newSimpleName = candidate;
								break;
							}
						}
					} else {
						newSimpleName = originalSimpleName;
					}

					String newFullInternal = targetClassPackage + "/" + newSimpleName;
					String newRelativeName = newRelativePrefix + newSimpleName;
					configData.relativeClassMappings.put(relativeName, newRelativeName);
					classNames.put(oldFullInternal, newFullInternal);
					remappedMixinClasses.add(newFullInternal);
					usedClassNames.add(newFullInternal);
				}
			}

			mixinConfigs.add(configData);
		}
	}

	private String remapMixinSubPackage(String basePackage, String subPkg) {
		String[] segments = subPkg.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			if (i > 0) {
				sb.append('/');
			}
			String seg = null;
			for (int attempt = 0; attempt < 1000; attempt++) {
				String cand = randomName(2, 2);
				String fullCand = basePackage + "/" + (sb.isEmpty() ? "" : sb + "/") + cand;
				if (!usedPackages.contains(fullCand)) {
					seg = cand;
					break;
				}
			}
			if (seg == null) {
				seg = randomName(3, 4);
			}
			sb.append(seg);
			usedPackages.add(basePackage + "/" + sb);
		}
		return sb.toString();
	}

	private boolean hasMixinAnnotation(ClassNode node) {
		if (node.visibleAnnotations != null) {
			for (AnnotationNode ann : node.visibleAnnotations) {
				if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(ann.desc)) {
					return true;
				}
			}
		}
		if (node.invisibleAnnotations != null) {
			for (AnnotationNode ann : node.invisibleAnnotations) {
				if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(ann.desc)) {
					return true;
				}
			}
		}
		return false;
	}

	private void loadNestPackages() {
		nestPackages.clear();
		for (ClassNode node : classes.values()) {
			String host = nestRoot(node);
			if (!nestPackages.containsKey(host)) {
				nestPackages.put(host, randomUniquePackage());
			}
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

	String mappedLogicOwner() {
		return classNames.getOrDefault(LOGIC_OWNER, LOGIC_OWNER);
	}

	private void buildMappings() {
		methodNames.clear();
		fieldNames.clear();

		boolean renameSLogic = !config.jnicEnabled() && config.slogicNameChange();
		if (renameSLogic) {
			String targetPackage = config.randomizePackages() ? randomUniquePackage() : packageOf(LOGIC_OWNER);
			String simpleName = simpleName(LOGIC_OWNER);
			if (config.renameClasses()) {
				for (int i = 0; ; i++) {
					String name = getOverloadedName(i);
					String cand = joinPackage(targetPackage, name);
					if (!usedClassNames.contains(cand)) {
						simpleName = name;
						break;
					}
				}
			}
			String mapped = joinPackage(targetPackage, simpleName);
			classNames.put(LOGIC_OWNER, mapped);
			usedClassNames.add(mapped);
		} else {
			classNames.put(LOGIC_OWNER, LOGIC_OWNER);
			usedClassNames.add(LOGIC_OWNER);
		}

		for (ClassNode node : classes.values()) {
			if (classNames.containsKey(node.name)) {
				continue;
			}

			if (isIncludeOrExcludeAnnotation(node)) {
				classNames.put(node.name, node.name);
				continue;
			}

			if (classNames.containsKey(node.name)) {
				continue;
			}

			if (isMixinClass(node.name)) {
				int dollar = node.name.indexOf('$');
				if (dollar > 0) {
					String outerName = node.name.substring(0, dollar);
					String outerMapped = classNames.get(outerName);
					if (outerMapped != null) {
						String innerMapped = outerMapped + "$" + simpleName(node.name);
						classNames.put(node.name, innerMapped);
						remappedMixinClasses.add(innerMapped);
						usedClassNames.add(innerMapped);
						continue;
					}
				}
			}

			String oldPackage = packageOf(node.name);
			String oldSimpleName = simpleName(node.name);
			String targetPackage = oldPackage;

			if (isMixinClass(node.name) && config.mixinFixedPathEnabled()) {
				targetPackage = config.mixinBasePackageInternalName();
			} else if (config.randomizePackages()) {
				targetPackage = nestPackages.get(nestRoot(node));
				if (targetPackage == null) {
					targetPackage = randomUniquePackage();
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
			if (isMixinClass(node.name)) {
				continue;
			}

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

	private boolean canRenameMethod(ClassNode owner, MethodNode method) {
		if (isMixinClass(owner.name)) {
			return false;
		}
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

	private String randomUniquePackage() {
		String root = config.packageRootInternalName();
		String depthMode = config.packageDepth() != null ? config.packageDepth() : "1 - 3";

		int minD;
		int maxD;
		switch (depthMode) {
			case "Flat" -> {
				minD = 1;
				maxD = 1;
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

		for (int attempts = 0; attempts < 10000; attempts++) {
			int targetDepth = minD + random.nextInt(maxD - minD + 1);
			StringBuilder result = new StringBuilder(root);
			for (int i = 0; i < targetDepth; i++) {
				if (!result.isEmpty()) {
					result.append('/');
				}
				result.append(randomName(2, 2));
			}
			String candidate = result.toString();
			if (usedPackages.add(candidate)) {
				return candidate;
			}
		}

		String fallback = (root.isEmpty() ? "" : root + "/") + randomName(4, 6);
		usedPackages.add(fallback);
		return fallback;
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

	private static final class MixinConfigData {
		final String entryName;
		final String oldPackage;
		final String newPackage;
		final Map<String, String> relativeClassMappings = new LinkedHashMap<>();
		final String oldPlugin;

		MixinConfigData(String entryName, String oldPackage, String newPackage, String oldPlugin) {
			this.entryName = entryName;
			this.oldPackage = oldPackage;
			this.newPackage = newPackage;
			this.oldPlugin = oldPlugin;
		}
	}
}