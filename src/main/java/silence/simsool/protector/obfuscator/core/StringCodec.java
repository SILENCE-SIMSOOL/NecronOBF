package silence.simsool.protector.obfuscator.core;

import java.nio.charset.StandardCharsets;

final class StringCodec {

	private StringCodec() {}

	static byte[] encode(String value, int key) {
		byte[] data = value.getBytes(StandardCharsets.UTF_8);
		byte[] result = new byte[data.length];
		int state = key ^ 0x5A17C9E3;

		for (int i = 0; i < data.length; i++) {
			state = Integer.rotateLeft(state * 0x045D9F3B, 5);
			result[i] = (byte) (data[i] ^ state ^ (state >>> 8) ^ i);
		}

		return result;
	}
}