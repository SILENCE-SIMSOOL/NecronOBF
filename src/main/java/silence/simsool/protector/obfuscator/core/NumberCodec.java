package silence.simsool.protector.obfuscator.core;

final class NumberCodec {

	private NumberCodec() {}

	static EncodedInt encodeInt(int value, int rule) {
		return switch (rule & 7) {
			case 0 -> new EncodedInt(Integer.rotateLeft(value + 0x13579BDF, 7) ^ 0x6D2B79F5, rule);
			case 1 -> new EncodedInt(Integer.rotateRight(value ^ 0x51ED270B, 11) - 0x2468ACE1, rule);
			case 2 -> new EncodedInt(Integer.rotateLeft(value ^ 0x73A4C921, 3) + 0x10203040, rule);
			case 3 -> new EncodedInt(Integer.rotateRight(value + 0x31415926, 5) ^ 0x4F1BBCDC, rule);
			case 4 -> new EncodedInt((value - 0x2C9277B5) ^ 0x7F4A7C15, rule);
			case 5 -> new EncodedInt(Integer.reverseBytes(value ^ 0x5BD1E995) + 0x1B873593, rule);
			case 6 -> new EncodedInt(Integer.rotateLeft(value - 0x3C6EF372, 13) ^ 0x9E3779B9, rule);
			default -> new EncodedInt(Integer.rotateRight(value ^ 0x85EBCA6B, 9) + 0xC2B2AE35, rule);
		};
	}

	static EncodedLong encodeLong(long value, int rule) {
		return switch (rule & 3) {
			case 0 -> new EncodedLong(Long.rotateLeft(value + 0x13579BDF2468ACE1L, 17) ^ 0x6D2B79F551ED270BL, rule);
			case 1 -> new EncodedLong(Long.rotateRight(value ^ 0x51ED270B73A4C921L, 23) - 0x2468ACE110203040L, rule);
			case 2 -> new EncodedLong(Long.reverseBytes(value ^ 0x4F1BBCDC31415926L) + 0x1B8735935BD1E995L, rule);
			default -> new EncodedLong(Long.rotateLeft(value - 0x3C6EF3729E3779B9L, 29) ^ 0x85EBCA6BC2B2AE35L, rule);
		};
	}

	record EncodedInt(int value, int rule) {}

	record EncodedLong(long value, int rule) {}
}