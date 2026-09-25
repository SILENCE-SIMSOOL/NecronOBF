package silence.simsool.protector.obfuscator.config;

public record ObfuscationConfig(
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
) {

	public static ObfuscationConfig defaults(String mainClass) {
		return new ObfuscationConfig(
			mainClass,
			true,
			true,
			true,
			true,
			true,
			true,
			false,
			"",
			true,
			true,
			true,
			"Random",
			"1 - 3",
			"Dynamic",
			false,
			"D:\\FROZEN\\Dev Mod\\Obfuscator\\JNIC\\!jnic-3.6.0.jar",
			"C:\\Program Files\\Java\\jdk-17\\bin\\java.exe",
			true,
			JnicManager.DEFAULT_XML,
			true,
			"archtang"
		);
	}

	public String mainClassInternalName() {
		if (mainClass == null || mainClass.isBlank()) {
			return null;
		}
		return mainClass.trim().replace('.', '/');
	}

	public String packageRootInternalName() {
		if (!packageRootEnabled || packageRoot == null || packageRoot.isBlank()) {
			return "";
		}
		return packageRoot.trim().replace('.', '/').replaceAll("^/+|/+$", "");
	}

	public String mixinBasePackageInternalName() {
		String base = (mixinFixedPath == null || mixinFixedPath.isBlank()) ? "archtang" : mixinFixedPath.trim().replace('.', '/').replaceAll("^/+|/+$", "");
		if (base.isEmpty()) {
			base = "archtang";
		}
		String root = packageRootInternalName();
		if (!root.isEmpty()) {
			return root + "/" + base;
		}
		return base;
	}
}