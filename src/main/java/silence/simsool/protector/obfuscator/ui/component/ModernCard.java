package silence.simsool.protector.obfuscator.ui.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import silence.simsool.protector.obfuscator.ui.UITheme;

public final class ModernCard extends JPanel {

	private static final int RADIUS = 14;

	public ModernCard() {
		this(null, null, null);
	}

	public ModernCard(VectorIcon.Type iconType, String title, String subtitle) {
		setOpaque(false);
		setLayout(new BorderLayout(0, 10));
		setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

		if (title != null) {
			JPanel header = new JPanel(new BorderLayout(0, 2));
			header.setOpaque(false);

			JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
			titleRow.setOpaque(false);

			if (iconType != null) {
				VectorIcon icon = new VectorIcon(iconType, 16, UITheme.ACCENT_CYAN);
				titleRow.add(icon);
			}

			JLabel titleLabel = new JLabel(title);
			titleLabel.setForeground(UITheme.TEXT_PRIMARY);
			titleLabel.setFont(UITheme.FONT_HEADER);
			titleRow.add(titleLabel);

			header.add(titleRow, BorderLayout.NORTH);

			if (subtitle != null && !subtitle.isEmpty()) {
				JLabel subLabel = new JLabel(subtitle);
				subLabel.setForeground(UITheme.TEXT_SECONDARY);
				subLabel.setFont(UITheme.FONT_SUB);
				subLabel.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
				header.add(subLabel, BorderLayout.CENTER);
			}

			add(header, BorderLayout.NORTH);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();

		// Background
		g2.setColor(UITheme.BG_CARD);
		g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);

		// Border
		g2.setColor(UITheme.BORDER_CARD);
		g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);

		g2.dispose();
		super.paintComponent(g);
	}
}