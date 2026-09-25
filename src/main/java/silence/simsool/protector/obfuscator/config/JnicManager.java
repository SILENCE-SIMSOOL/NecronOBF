package silence.simsool.protector.obfuscator.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JnicManager {

	public static final String DEFAULT_XML = """
<jnic>
	<targets>
		<target>WINDOWS_X86_64</target>
		<target>WINDOWS_AARCH64</target>
		<target>MACOS_X86_64</target>
		<target>MACOS_AARCH64</target>
		<target>LINUX_X86_64</target>
		<target>LINUX_AARCH64</target>
	</targets>

	<options>
		<stringObf>true</stringObf>
		<flowObf>true</flowObf>
		<fastCompile>false</fastCompile>
		<useIntrinsics>true</useIntrinsics>
	</options>
	
	<includeAnnotation>silence/simsool/silenceextra/addon/Include</includeAnnotation>
	<excludeAnnotation>silence/simsool/silenceextra/addon/Exclude</excludeAnnotation>
	
	<include>
		<match className="silence/simsool/protector/SLogic" />
	</include>

	<exclude>

	</exclude>
	
</jnic>
""";

	public static final String XML_FILE_NAME = "NecronOBF.xml";

	private JnicManager() {}

	public static Path resolveXmlPath(String jnicJarPath) {
		if (jnicJarPath == null || jnicJarPath.isBlank()) {
			return Path.of(XML_FILE_NAME);
		}
		Path jarPath = Path.of(jnicJarPath.trim());
		Path parent = jarPath.getParent();
		if (parent != null) {
			return parent.resolve(XML_FILE_NAME);
		}
		return Path.of(XML_FILE_NAME);
	}

	public static String ensureAndLoadXml(String jnicJarPath) throws IOException {
		Path xmlPath = resolveXmlPath(jnicJarPath);
		if (!Files.exists(xmlPath)) {
			Path parent = xmlPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.writeString(xmlPath, DEFAULT_XML, StandardCharsets.UTF_8);
			return DEFAULT_XML;
		}
		return Files.readString(xmlPath, StandardCharsets.UTF_8);
	}

	public static void saveXml(String jnicJarPath, String content) throws IOException {
		Path xmlPath = resolveXmlPath(jnicJarPath);
		Path parent = xmlPath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(xmlPath, content != null ? content : DEFAULT_XML, StandardCharsets.UTF_8);
	}
}
