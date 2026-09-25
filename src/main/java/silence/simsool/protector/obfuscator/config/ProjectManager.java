package silence.simsool.protector.obfuscator.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectManager {

	private ProjectManager() {}

	public record ProjectData(
		String input,
		String output,
		String mainClass,
		boolean renameClasses,
		boolean renameMethods,
		boolean renameFields,
		boolean randomizePackages,
		boolean protectStrings,
		boolean protectNumbers,
		boolean packageRootEnabled,
		String packageRoot,
		boolean randomizedSLogic,
		boolean preserveAnnotations,
		boolean updateFabricModJson,
		String seedMode,
		String packageDepth,
		String slogicTemplate,
		boolean jnicEnabled,
		String jnicPath,
		String javaPath,
		boolean slogicNameChange,
		String jnicXml,
		boolean mixinFixedPathEnabled,
		String mixinFixedPath
	) {}

	public static void exportToFile(Path path, ProjectData data) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("\t\"input\": \"").append(escape(data.input)).append("\",\n");
		sb.append("\t\"output\": \"").append(escape(data.output)).append("\",\n");
		sb.append("\t\"mainClass\": \"").append(escape(data.mainClass)).append("\",\n");
		sb.append("\t\"packageRoot\": \"").append(escape(data.packageRoot)).append("\",\n");
		sb.append("\t\"packageRootEnabled\": ").append(data.packageRootEnabled).append(",\n");
		sb.append("\t\"mixinFixedPath\": \"").append(escape(data.mixinFixedPath)).append("\",\n");
		sb.append("\t\"mixinFixedPathEnabled\": ").append(data.mixinFixedPathEnabled).append(",\n");
		sb.append("\t\"renameClasses\": ").append(data.renameClasses).append(",\n");
		sb.append("\t\"renameMethods\": ").append(data.renameMethods).append(",\n");
		sb.append("\t\"renameFields\": ").append(data.renameFields).append(",\n");
		sb.append("\t\"randomizePackages\": ").append(data.randomizePackages).append(",\n");
		sb.append("\t\"protectStrings\": ").append(data.protectStrings).append(",\n");
		sb.append("\t\"protectNumbers\": ").append(data.protectNumbers).append(",\n");
		sb.append("\t\"randomizedSLogic\": ").append(data.randomizedSLogic).append(",\n");
		sb.append("\t\"preserveAnnotations\": ").append(data.preserveAnnotations).append(",\n");
		sb.append("\t\"updateFabricModJson\": ").append(data.updateFabricModJson).append(",\n");
		sb.append("\t\"seedMode\": \"").append(escape(data.seedMode)).append("\",\n");
		sb.append("\t\"packageDepth\": \"").append(escape(data.packageDepth)).append("\",\n");
		sb.append("\t\"slogicTemplate\": \"").append(escape(data.slogicTemplate)).append("\",\n");
		sb.append("\t\"jnicEnabled\": ").append(data.jnicEnabled).append(",\n");
		sb.append("\t\"jnicPath\": \"").append(escape(data.jnicPath)).append("\",\n");
		sb.append("\t\"javaPath\": \"").append(escape(data.javaPath)).append("\",\n");
		sb.append("\t\"slogicNameChange\": ").append(data.slogicNameChange).append(",\n");
		sb.append("\t\"jnicXml\": \"").append(escape(data.jnicXml)).append("\"\n");
		sb.append("}\n");
		Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
	}

	public static ProjectData importFromFile(Path path) throws IOException {
		String json = Files.readString(path, StandardCharsets.UTF_8);
		Map<String, String> map = parseSimpleJson(json);

		return new ProjectData(
			map.getOrDefault("input", ""),
			map.getOrDefault("output", ""),
			map.getOrDefault("mainClass", ""),
			Boolean.parseBoolean(map.getOrDefault("renameClasses", "true")),
			Boolean.parseBoolean(map.getOrDefault("renameMethods", "true")),
			Boolean.parseBoolean(map.getOrDefault("renameFields", "true")),
			Boolean.parseBoolean(map.getOrDefault("randomizePackages", "true")),
			Boolean.parseBoolean(map.getOrDefault("protectStrings", "true")),
			Boolean.parseBoolean(map.getOrDefault("protectNumbers", "true")),
			Boolean.parseBoolean(map.getOrDefault("packageRootEnabled", "true")),
			map.getOrDefault("packageRoot", "silence"),
			Boolean.parseBoolean(map.getOrDefault("randomizedSLogic", "true")),
			Boolean.parseBoolean(map.getOrDefault("preserveAnnotations", "true")),
			Boolean.parseBoolean(map.getOrDefault("updateFabricModJson", "true")),
			map.getOrDefault("seedMode", "Random"),
			map.getOrDefault("packageDepth", "1 - 3"),
			map.getOrDefault("slogicTemplate", "Dynamic"),
			Boolean.parseBoolean(map.getOrDefault("jnicEnabled", "false")),
			map.getOrDefault("jnicPath", "D:\\FROZEN\\Dev Mod\\Obfuscator\\JNIC\\!jnic-3.6.0.jar"),
			map.getOrDefault("javaPath", "C:\\Program Files\\Java\\jdk-17\\bin\\java.exe"),
			Boolean.parseBoolean(map.getOrDefault("slogicNameChange", "true")),
			map.getOrDefault("jnicXml", JnicManager.DEFAULT_XML),
			Boolean.parseBoolean(map.getOrDefault("mixinFixedPathEnabled", "true")),
			map.getOrDefault("mixinFixedPath", "archtang")
		);
	}

	private static Map<String, String> parseSimpleJson(String json) {
		Map<String, String> map = new LinkedHashMap<>();
		Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(true|false|[0-9]+))");
		Matcher m = p.matcher(json);
		while (m.find()) {
			String key = m.group(1);
			String val = m.group(2) != null ? m.group(2) : m.group(3);
			map.put(key, unescape(val));
		}
		return map;
	}

	private static String escape(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\r", "\\r")
			.replace("\n", "\\n")
			.replace("\t", "\\t");
	}

	private static String unescape(String s) {
		if (s == null) return "";
		return s.replace("\\n", "\n")
			.replace("\\r", "\r")
			.replace("\\t", "\t")
			.replace("\\\"", "\"")
			.replace("\\\\", "\\");
	}
}