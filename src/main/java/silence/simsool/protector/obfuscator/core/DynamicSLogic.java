package silence.simsool.protector.obfuscator.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class DynamicSLogic {

	private final Random random;
	private final String ownerInternalName;
	private final String template;

	// Number of rules determined by template
	private final int intRuleCount;
	private final int longRuleCount;
	private final int minOpSteps;
	private final int maxOpSteps;

	// String codec parameters (Randomized per build)
	private final int strMask1;
	private final int strMultiplier;
	private final int strRotAmount;
	private final int strXorShift;
	private final int strIndexMul;
	private final byte strByteOffset;

	// Number codec rules (Randomized per build)
	private final List<IntRule> intRules = new ArrayList<>();
	private final List<LongRule> longRules = new ArrayList<>();

	DynamicSLogic(String ownerInternalName, Random random, String template) {
		this.ownerInternalName = ownerInternalName;
		this.random = random != null ? random : new Random();
		this.template = template != null ? template : "Dynamic";

		// Configure template complexity
		switch (this.template) {
			case "Ultra Polymorphic" -> {
				this.intRuleCount = 16;
				this.longRuleCount = 8;
				this.minOpSteps = 3;
				this.maxOpSteps = 5;
			}
			case "Stealth" -> {
				this.intRuleCount = 4;
				this.longRuleCount = 2;
				this.minOpSteps = 1;
				this.maxOpSteps = 2;
			}
			default -> { // Dynamic
				this.intRuleCount = 8;
				this.longRuleCount = 4;
				this.minOpSteps = 2;
				this.maxOpSteps = 4;
			}
		}

		// Randomize string parameters
		this.strMask1 = this.random.nextInt();
		this.strMultiplier = (this.random.nextInt() | 1); // Must be odd for invertible modular multiplication
		this.strRotAmount = 3 + this.random.nextInt(26); // 3..28
		this.strXorShift = 4 + this.random.nextInt(12); // 4..15
		this.strIndexMul = 1 + (this.random.nextInt(8) * 2); // 1, 3, 5, 7, 9, 11, 13, 15
		this.strByteOffset = (byte) (1 + this.random.nextInt(120));

		// Generate randomized integer rules
		for (int i = 0; i < intRuleCount; i++) {
			intRules.add(generateIntRule());
		}

		// Generate randomized long rules
		for (int i = 0; i < longRuleCount; i++) {
			longRules.add(generateLongRule());
		}
	}

	String ownerInternalName() {
		return ownerInternalName;
	}

	byte[] encodeString(String value, int key) {
		byte[] data = value.getBytes(StandardCharsets.UTF_8);
		byte[] result = new byte[data.length];
		int state = key ^ strMask1;

		for (int i = 0; i < data.length; i++) {
			state = Integer.rotateLeft(state * strMultiplier, strRotAmount);
			int k = state ^ (state >>> strXorShift) ^ (i * strIndexMul);
			result[i] = (byte) ((data[i] ^ k) + strByteOffset);
		}

		return result;
	}

	EncodedInt encodeInt(int value, int ruleIndex) {
		int rule = ruleIndex & (intRuleCount - 1);
		IntRule intRule = intRules.get(rule);
		int current = value;
		for (IntOp op : intRule.ops) {
			current = op.applyForward(current);
		}
		return new EncodedInt(current, rule);
	}

	EncodedLong encodeLong(long value, int ruleIndex) {
		int rule = ruleIndex & (longRuleCount - 1);
		LongRule longRule = longRules.get(rule);
		long current = value;
		for (LongOp op : longRule.ops) {
			current = op.applyForward(current);
		}
		return new EncodedLong(current, rule);
	}

	byte[] generateClassBytes() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, ownerInternalName, null, "java/lang/Object", null);

		// Constructor
		MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
		ctor.visitCode();
		ctor.visitVarInsn(Opcodes.ALOAD, 0);
		ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		ctor.visitInsn(Opcodes.RETURN);
		ctor.visitMaxs(0, 0);
		ctor.visitEnd();

		// Methods
		emitStringMethod(writer);
		emitIntMethod(writer);
		emitLongMethod(writer);
		emitFloatMethod(writer);
		emitDoubleMethod(writer);

		writer.visitEnd();
		return writer.toByteArray();
	}

	private void emitStringMethod(ClassWriter writer) {
		MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "string", "([BI)Ljava/lang/String;", null, null);
		mv.visitCode();

		Label start = new Label();
		Label end = new Label();
		mv.visitLabel(start);

		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitInsn(Opcodes.ARRAYLENGTH);
		mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
		mv.visitVarInsn(Opcodes.ASTORE, 2);

		mv.visitVarInsn(Opcodes.ILOAD, 1);
		mv.visitLdcInsn(strMask1);
		mv.visitInsn(Opcodes.IXOR);
		mv.visitVarInsn(Opcodes.ISTORE, 3);

		mv.visitInsn(Opcodes.ICONST_0);
		mv.visitVarInsn(Opcodes.ISTORE, 4);

		Label loop = new Label();
		mv.visitLabel(loop);
		mv.visitVarInsn(Opcodes.ILOAD, 4);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitInsn(Opcodes.ARRAYLENGTH);
		mv.visitJumpInsn(Opcodes.IF_ICMPGE, end);

		mv.visitVarInsn(Opcodes.ILOAD, 3);
		mv.visitLdcInsn(strMultiplier);
		mv.visitInsn(Opcodes.IMUL);
		mv.visitIntInsn(Opcodes.BIPUSH, strRotAmount);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false);
		mv.visitVarInsn(Opcodes.ISTORE, 3);

		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitVarInsn(Opcodes.ILOAD, 4);

		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ILOAD, 4);
		mv.visitInsn(Opcodes.BALOAD);

		mv.visitIntInsn(Opcodes.BIPUSH, strByteOffset);
		mv.visitInsn(Opcodes.ISUB);

		mv.visitVarInsn(Opcodes.ILOAD, 3);
		mv.visitInsn(Opcodes.IXOR);

		mv.visitVarInsn(Opcodes.ILOAD, 3);
		mv.visitIntInsn(Opcodes.BIPUSH, strXorShift);
		mv.visitInsn(Opcodes.IUSHR);
		mv.visitInsn(Opcodes.IXOR);

		mv.visitVarInsn(Opcodes.ILOAD, 4);
		if (strIndexMul != 1) {
			mv.visitIntInsn(Opcodes.BIPUSH, strIndexMul);
			mv.visitInsn(Opcodes.IMUL);
		}
		mv.visitInsn(Opcodes.IXOR);

		mv.visitInsn(Opcodes.I2B);
		mv.visitInsn(Opcodes.BASTORE);

		// i++
		mv.visitIincInsn(4, 1);
		mv.visitJumpInsn(Opcodes.GOTO, loop);

		mv.visitLabel(end);
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitFieldInsn(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void emitIntMethod(ClassWriter writer) {
		MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "number", "(II)I", null, null);
		mv.visitCode();

		Label[] labels = new Label[intRuleCount];
		for (int i = 0; i < intRuleCount; i++) {
			labels[i] = new Label();
		}
		Label dflt = labels[intRuleCount - 1];

		mv.visitVarInsn(Opcodes.ILOAD, 1);
		mv.visitIntInsn(Opcodes.BIPUSH, intRuleCount - 1);
		mv.visitInsn(Opcodes.IAND);
		mv.visitTableSwitchInsn(0, intRuleCount - 1, dflt, labels);

		for (int i = 0; i < intRuleCount; i++) {
			mv.visitLabel(labels[i]);
			mv.visitVarInsn(Opcodes.ILOAD, 0);

			IntRule rule = intRules.get(i);
			List<IntOp> reversedOps = new ArrayList<>(rule.ops);
			Collections.reverse(reversedOps);

			for (IntOp op : reversedOps) {
				op.emitReverse(mv);
			}

			mv.visitInsn(Opcodes.IRETURN);
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void emitLongMethod(ClassWriter writer) {
		MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "number", "(JI)J", null, null);
		mv.visitCode();

		Label[] labels = new Label[longRuleCount];
		for (int i = 0; i < longRuleCount; i++) {
			labels[i] = new Label();
		}
		Label dflt = labels[longRuleCount - 1];

		mv.visitVarInsn(Opcodes.ILOAD, 2);
		mv.visitIntInsn(Opcodes.BIPUSH, longRuleCount - 1);
		mv.visitInsn(Opcodes.IAND);
		mv.visitTableSwitchInsn(0, longRuleCount - 1, dflt, labels);

		for (int i = 0; i < longRuleCount; i++) {
			mv.visitLabel(labels[i]);
			mv.visitVarInsn(Opcodes.LLOAD, 0);

			LongRule rule = longRules.get(i);
			List<LongOp> reversedOps = new ArrayList<>(rule.ops);
			Collections.reverse(reversedOps);

			for (LongOp op : reversedOps) {
				op.emitReverse(mv);
			}

			mv.visitInsn(Opcodes.LRETURN);
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void emitFloatMethod(ClassWriter writer) {
		MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "numberFloat", "(II)F", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ILOAD, 0);
		mv.visitVarInsn(Opcodes.ILOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerInternalName, "number", "(II)I", false);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false);
		mv.visitInsn(Opcodes.FRETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void emitDoubleMethod(ClassWriter writer) {
		MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "numberDouble", "(JI)D", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.LLOAD, 0);
		mv.visitVarInsn(Opcodes.ILOAD, 2);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerInternalName, "number", "(JI)J", false);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false);
		mv.visitInsn(Opcodes.DRETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private IntRule generateIntRule() {
		int steps = minOpSteps + random.nextInt(maxOpSteps - minOpSteps + 1);
		List<IntOp> ops = new ArrayList<>();
		for (int s = 0; s < steps; s++) {
			int type = random.nextInt(6);
			switch (type) {
				case 0 -> ops.add(new IntXorOp(random.nextInt()));
				case 1 -> ops.add(new IntAddOp(random.nextInt()));
				case 2 -> ops.add(new IntSubOp(random.nextInt()));
				case 3 -> ops.add(new IntRotlOp(1 + random.nextInt(30)));
				case 4 -> ops.add(new IntRotrOp(1 + random.nextInt(30)));
				default -> ops.add(new IntReverseBytesOp());
			}
		}
		return new IntRule(ops);
	}

	private LongRule generateLongRule() {
		int steps = minOpSteps + random.nextInt(maxOpSteps - minOpSteps + 1);
		List<LongOp> ops = new ArrayList<>();
		for (int s = 0; s < steps; s++) {
			int type = random.nextInt(6);
			switch (type) {
				case 0 -> ops.add(new LongXorOp(random.nextLong()));
				case 1 -> ops.add(new LongAddOp(random.nextLong()));
				case 2 -> ops.add(new LongSubOp(random.nextLong()));
				case 3 -> ops.add(new LongRotlOp(1 + random.nextInt(62)));
				case 4 -> ops.add(new LongRotrOp(1 + random.nextInt(62)));
				default -> ops.add(new LongReverseBytesOp());
			}
		}
		return new LongRule(ops);
	}

	record EncodedInt(int value, int rule) {}
	record EncodedLong(long value, int rule) {}

	private record IntRule(List<IntOp> ops) {}
	private record LongRule(List<LongOp> ops) {}

	// Integer Operations
	private sealed interface IntOp permits IntXorOp, IntAddOp, IntSubOp, IntRotlOp, IntRotrOp, IntReverseBytesOp {
		int applyForward(int val);
		void emitReverse(MethodVisitor mv);
	}

	private record IntXorOp(int constant) implements IntOp {
		@Override
		public int applyForward(int val) { return val ^ constant; }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitLdcInsn(constant);
			mv.visitInsn(Opcodes.IXOR);
		}
	}

	private record IntAddOp(int constant) implements IntOp {
		@Override
		public int applyForward(int val) { return val + constant; }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitLdcInsn(constant);
			mv.visitInsn(Opcodes.ISUB);
		}
	}

	private record IntSubOp(int constant) implements IntOp {
		@Override
		public int applyForward(int val) { return val - constant; }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitLdcInsn(constant);
			mv.visitInsn(Opcodes.IADD);
		}
	}

	private record IntRotlOp(int amount) implements IntOp {
		@Override
		public int applyForward(int val) { return Integer.rotateLeft(val, amount); }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitIntInsn(Opcodes.BIPUSH, amount);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateRight", "(II)I", false);
		}
	}

	private record IntRotrOp(int amount) implements IntOp {
		@Override
		public int applyForward(int val) { return Integer.rotateRight(val, amount); }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitIntInsn(Opcodes.BIPUSH, amount);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false);
		}
	}

	private record IntReverseBytesOp() implements IntOp {
		@Override
		public int applyForward(int val) { return Integer.reverseBytes(val); }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "reverseBytes", "(I)I", false);
		}
	}

	// Long Operations
	private sealed interface LongOp permits LongXorOp, LongAddOp, LongSubOp, LongRotlOp, LongRotrOp, LongReverseBytesOp {
		long applyForward(long val);
		void emitReverse(MethodVisitor mv);
	}

	private record LongXorOp(long constant) implements LongOp {
		@Override
		public long applyForward(long val) { return val ^ constant; }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitLdcInsn(constant);
			mv.visitInsn(Opcodes.LXOR);
		}
	}

	private record LongAddOp(long constant) implements LongOp {
		@Override
		public long applyForward(long val) { return val + constant; }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitLdcInsn(constant);
			mv.visitInsn(Opcodes.LSUB);
		}
	}

	private record LongSubOp(long constant) implements LongOp {
		@Override
		public long applyForward(long val) { return val - constant; }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitLdcInsn(constant);
			mv.visitInsn(Opcodes.LADD);
		}
	}

	private record LongRotlOp(int amount) implements LongOp {
		@Override
		public long applyForward(long val) { return Long.rotateLeft(val, amount); }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitIntInsn(Opcodes.BIPUSH, amount);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "rotateRight", "(JI)J", false);
		}
	}

	private record LongRotrOp(int amount) implements LongOp {
		@Override
		public long applyForward(long val) { return Long.rotateRight(val, amount); }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitIntInsn(Opcodes.BIPUSH, amount);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "rotateLeft", "(JI)J", false);
		}
	}

	private record LongReverseBytesOp() implements LongOp {
		@Override
		public long applyForward(long val) { return Long.reverseBytes(val); }
		@Override
		public void emitReverse(MethodVisitor mv) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "reverseBytes", "(J)J", false);
		}
	}
}