package silence.simsool.protector.obfuscator.ui;

import java.awt.Color;
import java.awt.Font;

public final class UITheme {

	private UITheme() {}

	// Base backgrounds
	public static final Color BG_DARK = new Color(8, 11, 17);
	public static final Color BG_SIDEBAR = new Color(7, 10, 16);
	public static final Color BG_MAIN = new Color(10, 14, 22);
	public static final Color BG_CARD = new Color(15, 20, 31);
	public static final Color BG_CARD_LIGHTER = new Color(18, 25, 39);
	public static final Color BG_INFO_BOX = new Color(12, 24, 46);

	// Borders
	public static final Color BORDER_CARD = new Color(27, 36, 53);
	public static final Color BORDER_CARD_HOVER = new Color(37, 50, 74);
	public static final Color BORDER_FOCUS = new Color(59, 130, 246);
	public static final Color BORDER_INFO = new Color(30, 58, 105);

	// Accents
	public static final Color ACCENT_BLUE = new Color(37, 99, 235);
	public static final Color ACCENT_BLUE_LIGHT = new Color(59, 130, 246);
	public static final Color ACCENT_BLUE_DARK = new Color(29, 78, 216);
	public static final Color ACCENT_CYAN = new Color(56, 189, 248);

	// Text colors
	public static final Color TEXT_PRIMARY = new Color(241, 245, 249);
	public static final Color TEXT_SECONDARY = new Color(148, 163, 184);
	public static final Color TEXT_MUTED = new Color(100, 116, 139);
	public static final Color TEXT_DISABLED = new Color(71, 85, 105);

	// Switch / Button colors
	public static final Color SWITCH_TRACK_OFF = new Color(30, 41, 59);
	public static final Color SWITCH_TRACK_ON = new Color(37, 99, 235);
	public static final Color SWITCH_THUMB = new Color(255, 255, 255);
	public static final Color BTN_BG = new Color(18, 25, 38);
	public static final Color BTN_BORDER = new Color(37, 49, 71);
	public static final Color BTN_HOVER = new Color(26, 36, 56);

	// Radii
	public static final int RADIUS_CARD = 14;
	public static final int RADIUS_INPUT = 10;
	public static final int RADIUS_BTN = 10;
	public static final int RADIUS_SMALL = 8;
	public static final int RADIUS_PILL = 20;

	// Font Family detection (Supports both Korean and English smoothly)
	private static final String FONT_FAMILY = detectFontFamily();

	private static String detectFontFamily() {
		String[] preferred = { "Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", "Noto Sans CJK KR" };
		try {
			java.util.Set<String> available = java.util.Set.of(
				java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
			);
			for (String fontName : preferred) {
				if (available.contains(fontName)) {
					return fontName;
				}
			}
		} catch (Exception ignored) {
		}
		return Font.SANS_SERIF;
	}

	// Fonts (Smooth support for English and Korean)
	public static final Font FONT_TITLE = createFont(Font.BOLD, 20);
	public static final Font FONT_HEADER = createFont(Font.BOLD, 14);
	public static final Font FONT_SUB = createFont(Font.PLAIN, 12);
	public static final Font FONT_BOLD_SUB = createFont(Font.BOLD, 12);
	public static final Font FONT_SMALL = createFont(Font.PLAIN, 11);
	public static final Font FONT_BUTTON = createFont(Font.BOLD, 12);

	public static Font createFont(int style, int size) {
		return new Font(FONT_FAMILY, style, size);
	}
}