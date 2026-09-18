package silence.simsool.protector.obfuscator;

import java.nio.file.Path;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import silence.simsool.protector.obfuscator.config.ObfuscationConfig;
import silence.simsool.protector.obfuscator.core.JarObfuscator;
import silence.simsool.protector.obfuscator.ui.ProtectorFrame;

public final class Main {

	public static void main(String[] args) throws Exception {
		if (args.length >= 2) {
			String mainClass = args.length >= 3 ? args[2] : "";
			new JarObfuscator().obfuscate(
				Path.of(args[0]),
				Path.of(args[1]),
				ObfuscationConfig.defaults(mainClass)
			);
			return;
		}

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeLater(() -> new ProtectorFrame().setVisible(true));
	}

}