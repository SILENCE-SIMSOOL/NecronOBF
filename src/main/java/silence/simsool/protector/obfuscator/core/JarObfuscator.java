package silence.simsool.protector.obfuscator.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import silence.simsool.protector.obfuscator.config.ObfuscationConfig;

public final class JarObfuscator {

	private static final String LOGIC_OWNER = "silence/simsool/protector/SLogic";
	private static final String STRING_CACHE_FIELD = "$sp$c";
	private static final Pattern MIXIN_PACKAGE = Pattern.compile("\\\"package\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

	public void obfuscate(Path input, Path output, ObfuscationConfig config) throws Exception {
		Random random = createRandom(config.seedMode());

		Map<String, byte[]> entries = readJar(input);
		NameObfuscator nameObfuscator = new NameObfuscator(random);
		entries = nameObfuscator.remap(entries, config);
		entries = updateMetadata(entries, nameObfuscator.classMappings(), config);

		Random slogicRandom = config.randomizedSLogic() ? random : new Random(0x511EACE20260919L);
		DynamicSLogic dynamicSLogic = new DynamicSLogic(LOGIC_OWNER, slogicRandom, config.slogicTemplate());

		Map<String, byte[]> transformed = new LinkedHashMap<>();
		for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
			String name = entry.getKey();
			byte[] data = entry.getValue();

			if (!name.endsWith(".class") || name.equals(LOGIC_OWNER + ".class") || name.equals("module-info.class")) {
				transformed.put(name, data);
				continue;
			}

			transformed.put(name, transformClass(data, entries, config, dynamicSLogic, random));
		}

		if (config.protectStrings() || config.protectNumbers()) {
			transformed.put(LOGIC_OWNER + ".class", dynamicSLogic.generateClassBytes());
		}
		writeJar(output, transformed);
	}

	private Random createRandom(String seedMode) {
		if ("Fixed Seed".equalsIgnoreCase(seedMode)) {
			return new Random(0x51134CE1L);
		} else if ("Timestamp".equalsIgnoreCase(seedMode)) {
			return new Random(System.currentTimeMillis());
		} else {
			return new SecureRandom();
		}
	}

	private Map<String, byte[]> updateMetadata(Map<String, byte[]> entries, Map<String, String> classMappings, ObfuscationConfig config) {
		Map<String, byte[]> result = new LinkedHashMap<>(entries);

		byte[] fabricData = result.get("fabric.mod.json");
		if (fabricData != null && config.updateFabricModJson()) {
			String json = new String(fabricData, StandardCharsets.UTF_8);
			for (Map.Entry<String, String> mapping : classMappings.entrySet()) {
				json = json.replace(mapping.getKey().replace('/', '.'), mapping.getValue().replace('/', '.'));
			}
			result.put("fabric.mod.json", json.getBytes(StandardCharsets.UTF_8));
		}

		for (Map.Entry<String, byte[]> entry : new ArrayList<>(result.entrySet())) {
			if (!entry.getKey().endsWith(".json") || !entry.getKey().contains("mixin")) {
				continue;
			}

			String json = new String(entry.getValue(), StandardCharsets.UTF_8);
			Matcher matcher = MIXIN_PACKAGE.matcher(json);
			if (!matcher.find()) {
				continue;
			}

			String oldPackageDot = matcher.group(1);
			String oldPackage = oldPackageDot.replace('.', '/');
			String newPackage = null;

			for (Map.Entry<String, String> mapping : classMappings.entrySet()) {
				if (!mapping.getKey().startsWith(oldPackage + "/")) {
					continue;
				}
				String mapped = mapping.getValue();
				int slash = mapped.lastIndexOf('/');
				if (slash > 0) {
					newPackage = mapped.substring(0, slash);
					break;
				}
			}

			if (newPackage == null) {
				continue;
			}

			for (Map.Entry<String, String> mapping : classMappings.entrySet()) {
				String oldName = mapping.getKey();
				if (!oldName.startsWith(oldPackage + "/")) {
					continue;
				}
				String oldRelative = oldName.substring(oldPackage.length() + 1).replace('/', '.');
				String newName = mapping.getValue();
				String newRelative = newName.substring(newName.lastIndexOf('/') + 1);
				json = json.replace("\"" + oldRelative + "\"", "\"" + newRelative + "\"");
				json = json.replace(oldName.replace('/', '.'), newName.replace('/', '.'));
			}

			json = json.replace(oldPackageDot, newPackage.replace('/', '.'));
			result.put(entry.getKey(), json.getBytes(StandardCharsets.UTF_8));
		}

		String configuredMain = config.mainClassInternalName();
		if (configuredMain != null && !classMappings.containsKey(configuredMain)) {
			throw new IllegalArgumentException("Main class not found: " + config.mainClass());
		}

		return result;
	}

	private byte[] transformClass(byte[] input, Map<String, byte[]> entries, ObfuscationConfig config, DynamicSLogic dynamicSLogic, Random random) {
		ClassNode classNode = new ClassNode();
		new ClassReader(input).accept(classNode, 0);

		// Strip debug & source metadata
		classNode.sourceFile = null;
		classNode.sourceDebug = null;

		List<StringField> stringFields = new ArrayList<>();
		int[] stringIndex = {0};

		for (MethodNode method : classNode.methods) {
			if (method.localVariables != null) {
				method.localVariables.clear();
			}
			if (method.parameters != null) {
				method.parameters.clear();
			}

			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
				AbstractInsnNode next = insn.getNext();

				if (config.protectStrings() && insn instanceof InvokeDynamicInsnNode indy && rewriteStringConcat(classNode, method, indy, stringFields, stringIndex, dynamicSLogic, random)) {
					insn = next;
					continue;
				}

				if (insn instanceof LdcInsnNode ldc) {
					Object constant = ldc.cst;
					if (config.protectStrings() && constant instanceof String value) {
						StringField field = createStringField(value, stringIndex[0]++, dynamicSLogic, random);
						stringFields.add(field);
						method.instructions.insertBefore(insn, createStringLoad(classNode.name, field));
						method.instructions.remove(insn);
					} else if (config.protectNumbers() && constant instanceof Integer value && shouldProtectInt(value)) {
						replaceInt(method, insn, value, dynamicSLogic, random);
					} else if (config.protectNumbers() && constant instanceof Long value) {
						replaceLong(method, insn, value, dynamicSLogic, random);
					} else if (config.protectNumbers() && constant instanceof Float value) {
						replaceFloat(method, insn, value, dynamicSLogic, random);
					} else if (config.protectNumbers() && constant instanceof Double value) {
						replaceDouble(method, insn, value, dynamicSLogic, random);
					}
				} else if (insn instanceof IntInsnNode intInsn) {
					if (config.protectNumbers() && (intInsn.getOpcode() == Opcodes.BIPUSH || intInsn.getOpcode() == Opcodes.SIPUSH) && shouldProtectInt(intInsn.operand)) {
						replaceInt(method, insn, intInsn.operand, dynamicSLogic, random);
					}
				}

				insn = next;
			}
		}

		if (!stringFields.isEmpty()) {
			int fieldAccess = Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
			fieldAccess |= (classNode.access & Opcodes.ACC_INTERFACE) != 0 ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE;
			classNode.fields.add(new FieldNode(fieldAccess, STRING_CACHE_FIELD, "[Ljava/lang/String;", null, null));
			initializeStringCache(classNode, stringFields);
		}

		ClassWriter writer = new HierarchyClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, entries);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private boolean rewriteStringConcat(ClassNode owner, MethodNode method, InvokeDynamicInsnNode indy, List<StringField> stringFields, int[] stringIndex, DynamicSLogic dynamicSLogic, Random random) {
		Handle bootstrap = indy.bsm;
		if (!bootstrap.getOwner().equals("java/lang/invoke/StringConcatFactory") || !indy.name.equals("makeConcatWithConstants")) {
			return false;
		}
		if (indy.bsmArgs.length == 0 || !(indy.bsmArgs[0] instanceof String recipe)) {
			return false;
		}

		Type[] argumentTypes = Type.getArgumentTypes(indy.desc);
		int[] locals = new int[argumentTypes.length];
		int local = method.maxLocals;
		for (int i = argumentTypes.length - 1; i >= 0; i--) {
			locals[i] = local;
			local += argumentTypes[i].getSize();
		}
		method.maxLocals = Math.max(method.maxLocals, local);

		InsnList list = new InsnList();
		for (int i = argumentTypes.length - 1; i >= 0; i--) {
			list.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ISTORE), locals[i]));
		}

		list.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
		list.add(new InsnNode(Opcodes.DUP));
		list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));

		int dynamicIndex = 0;
		int constantIndex = 1;
		StringBuilder literal = new StringBuilder();

		for (int i = 0; i < recipe.length(); i++) {
			char ch = recipe.charAt(i);
			if (ch != '\u0001' && ch != '\u0002') {
				literal.append(ch);
				continue;
			}

			appendEncryptedLiteral(list, owner.name, literal, stringFields, stringIndex, dynamicSLogic, random);
			if (ch == '\u0001') {
				if (dynamicIndex >= argumentTypes.length) {
					return false;
				}
				Type type = argumentTypes[dynamicIndex];
				list.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), locals[dynamicIndex]));
				appendBuilderCall(list, type);
				dynamicIndex++;
			} else {
				if (constantIndex >= indy.bsmArgs.length) {
					return false;
				}
				appendBootstrapConstant(list, owner.name, indy.bsmArgs[constantIndex++], stringFields, stringIndex, dynamicSLogic, random);
			}
		}

		appendEncryptedLiteral(list, owner.name, literal, stringFields, stringIndex, dynamicSLogic, random);
		list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
		method.instructions.insertBefore(indy, list);
		method.instructions.remove(indy);
		return true;
	}

	private void appendEncryptedLiteral(InsnList list, String owner, StringBuilder literal, List<StringField> stringFields, int[] stringIndex, DynamicSLogic dynamicSLogic, Random random) {
		if (literal.isEmpty()) {
			return;
		}
		String text = literal.toString();
		literal.setLength(0);
		StringField field = createStringField(text, stringIndex[0]++, dynamicSLogic, random);
		stringFields.add(field);
		list.add(createStringLoad(owner, field));
		list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
	}

	private void appendBootstrapConstant(InsnList list, String owner, Object constant, List<StringField> stringFields, int[] stringIndex, DynamicSLogic dynamicSLogic, Random random) {
		if (constant instanceof String text) {
			StringField field = createStringField(text, stringIndex[0]++, dynamicSLogic, random);
			stringFields.add(field);
			list.add(createStringLoad(owner, field));
			list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
		} else if (constant instanceof Integer || constant instanceof Short || constant instanceof Byte) {
			list.add(new LdcInsnNode(constant));
			list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false));
		} else if (constant instanceof Long) {
			list.add(new LdcInsnNode(constant));
			list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false));
		} else if (constant instanceof Float) {
			list.add(new LdcInsnNode(constant));
			list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(F)Ljava/lang/StringBuilder;", false));
		} else if (constant instanceof Double) {
			list.add(new LdcInsnNode(constant));
			list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(D)Ljava/lang/StringBuilder;", false));
		}
	}

	private void appendBuilderCall(InsnList list, Type type) {
		String descriptor = switch (type.getSort()) {
			case Type.BOOLEAN -> "(Z)Ljava/lang/StringBuilder;";
			case Type.CHAR -> "(C)Ljava/lang/StringBuilder;";
			case Type.BYTE, Type.SHORT, Type.INT -> "(I)Ljava/lang/StringBuilder;";
			case Type.FLOAT -> "(F)Ljava/lang/StringBuilder;";
			case Type.LONG -> "(J)Ljava/lang/StringBuilder;";
			case Type.DOUBLE -> "(D)Ljava/lang/StringBuilder;";
			default -> type.getClassName().equals("java.lang.String")
				? "(Ljava/lang/String;)Ljava/lang/StringBuilder;"
				: "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
		};
		list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false));
	}

	private StringField createStringField(String value, int index, DynamicSLogic dynamicSLogic, Random random) {
		int key = random.nextInt();
		byte[] encrypted = dynamicSLogic.encodeString(value, key);
		return new StringField(index, encrypted, key);
	}

	private InsnList createStringLoad(String owner, StringField field) {
		InsnList list = new InsnList();
		LabelNode done = new LabelNode();

		list.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, STRING_CACHE_FIELD, "[Ljava/lang/String;"));
		list.add(new LdcInsnNode(field.index));
		list.add(new InsnNode(Opcodes.AALOAD));
		list.add(new InsnNode(Opcodes.DUP));
		list.add(new JumpInsnNode(Opcodes.IFNONNULL, done));
		list.add(new InsnNode(Opcodes.POP));

		list.add(new FieldInsnNode(Opcodes.GETSTATIC, owner, STRING_CACHE_FIELD, "[Ljava/lang/String;"));
		list.add(new LdcInsnNode(field.index));
		list.add(new LdcInsnNode(field.index));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "$sp$s", "(I)Ljava/lang/String;", false));
		list.add(new InsnNode(Opcodes.DUP_X2));
		list.add(new InsnNode(Opcodes.AASTORE));

		list.add(done);
		return list;
	}

	private void initializeStringCache(ClassNode classNode, List<StringField> stringFields) {
		int count = stringFields.size();
		MethodNode stringDecoder = new MethodNode(
			Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
			"$sp$s",
			"(I)Ljava/lang/String;",
			null,
			null
		);

		LabelNode defaultLabel = new LabelNode();
		LabelNode[] labels = new LabelNode[count];
		for (int i = 0; i < count; i++) {
			labels[i] = new LabelNode();
		}

		stringDecoder.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
		stringDecoder.instructions.add(new org.objectweb.asm.tree.TableSwitchInsnNode(0, count - 1, defaultLabel, labels));

		for (int i = 0; i < count; i++) {
			stringDecoder.instructions.add(labels[i]);
			stringDecoder.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, "$sp$g" + i, "()Ljava/lang/String;", false));
			stringDecoder.instructions.add(new InsnNode(Opcodes.ARETURN));
		}

		stringDecoder.instructions.add(defaultLabel);
		stringDecoder.instructions.add(new LdcInsnNode(""));
		stringDecoder.instructions.add(new InsnNode(Opcodes.ARETURN));
		classNode.methods.add(stringDecoder);

		for (int i = 0; i < count; i++) {
			StringField sf = stringFields.get(i);
			MethodNode provider = new MethodNode(
				Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
				"$sp$g" + i,
				"()Ljava/lang/String;",
				null,
				null
			);
			provider.instructions.add(createByteArrayInsn(sf.encrypted()));
			provider.instructions.add(new LdcInsnNode(sf.key()));
			provider.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOGIC_OWNER, "string", "([BI)Ljava/lang/String;", false));
			provider.instructions.add(new InsnNode(Opcodes.ARETURN));
			classNode.methods.add(provider);
		}

		MethodNode clinit = null;
		for (MethodNode method : classNode.methods) {
			if (method.name.equals("<clinit>")) {
				clinit = method;
				break;
			}
		}

		if (clinit == null) {
			clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			clinit.instructions.add(new InsnNode(Opcodes.RETURN));
			classNode.methods.add(clinit);
		}

		InsnList init = new InsnList();
		init.add(new LdcInsnNode(count));
		init.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
		init.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, STRING_CACHE_FIELD, "[Ljava/lang/String;"));
		clinit.instructions.insert(init);
	}

	private InsnList createByteArrayInsn(byte[] bytes) {
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(bytes.length));
		list.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
		for (int i = 0; i < bytes.length; i++) {
			list.add(new InsnNode(Opcodes.DUP));
			list.add(new LdcInsnNode(i));
			list.add(new IntInsnNode(Opcodes.BIPUSH, bytes[i]));
			list.add(new InsnNode(Opcodes.BASTORE));
		}
		return list;
	}

	private boolean shouldProtectInt(int value) {
		return value != 0 && value != 1 && value != -1;
	}

	private void replaceInt(MethodNode method, AbstractInsnNode target, int value, DynamicSLogic dynamicSLogic, Random random) {
		DynamicSLogic.EncodedInt encoded = dynamicSLogic.encodeInt(value, random.nextInt());
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(encoded.value()));
		list.add(new LdcInsnNode(encoded.rule()));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOGIC_OWNER, "number", "(II)I", false));
		method.instructions.insertBefore(target, list);
		method.instructions.remove(target);
	}

	private void replaceLong(MethodNode method, AbstractInsnNode target, long value, DynamicSLogic dynamicSLogic, Random random) {
		DynamicSLogic.EncodedLong encoded = dynamicSLogic.encodeLong(value, random.nextInt());
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(encoded.value()));
		list.add(new LdcInsnNode(encoded.rule()));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOGIC_OWNER, "number", "(JI)J", false));
		method.instructions.insertBefore(target, list);
		method.instructions.remove(target);
	}

	private void replaceFloat(MethodNode method, AbstractInsnNode target, float value, DynamicSLogic dynamicSLogic, Random random) {
		DynamicSLogic.EncodedInt encoded = dynamicSLogic.encodeInt(Float.floatToRawIntBits(value), random.nextInt());
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(encoded.value()));
		list.add(new LdcInsnNode(encoded.rule()));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOGIC_OWNER, "numberFloat", "(II)F", false));
		method.instructions.insertBefore(target, list);
		method.instructions.remove(target);
	}

	private void replaceDouble(MethodNode method, AbstractInsnNode target, double value, DynamicSLogic dynamicSLogic, Random random) {
		DynamicSLogic.EncodedLong encoded = dynamicSLogic.encodeLong(Double.doubleToRawLongBits(value), random.nextInt());
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(encoded.value()));
		list.add(new LdcInsnNode(encoded.rule()));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LOGIC_OWNER, "numberDouble", "(JI)D", false));
		method.instructions.insertBefore(target, list);
		method.instructions.remove(target);
	}

	private Map<String, byte[]> readJar(Path path) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (JarFile jar = new JarFile(path.toFile())) {
			Enumeration<JarEntry> enumeration = jar.entries();
			while (enumeration.hasMoreElements()) {
				JarEntry entry = enumeration.nextElement();
				if (!entry.isDirectory()) {
					entries.put(entry.getName(), jar.getInputStream(entry).readAllBytes());
				}
			}
		}
		return entries;
	}

	private void writeJar(Path path, Map<String, byte[]> entries) throws IOException {
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(path))) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
				JarEntry jarEntry = new JarEntry(entry.getKey());
				output.putNextEntry(jarEntry);
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
	}

	private record StringField(int index, byte[] encrypted, int key) {}
}