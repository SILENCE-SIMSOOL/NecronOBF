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
	String slogicTemplate
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
			"Dynamic"
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
}