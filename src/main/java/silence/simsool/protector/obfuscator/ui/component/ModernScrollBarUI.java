package silence.simsool.protector.obfuscator.ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

import silence.simsool.protector.obfuscator.ui.UITheme;

public final class ModernScrollBarUI extends BasicScrollBarUI {

	private static final int THICKNESS = 6;
	private static final Dimension ZERO_DIM = new Dimension(0, 0);

	public static void applyTo(JScrollPane scrollPane) {
		scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
		scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(THICKNESS + 2, 0));
		scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, THICKNESS + 2));
		scrollPane.getVerticalScrollBar().setOpaque(false);
		scrollPane.getHorizontalScrollBar().setOpaque(false);
	}

	@Override
	protected JButton createDecreaseButton(int orientation) {
		return createZeroButton();
	}

	@Override
	protected JButton createIncreaseButton(int orientation) {
		return createZeroButton();
	}

	private static JButton createZeroButton() {
		JButton btn = new JButton();
		btn.setPreferredSize(ZERO_DIM);
		btn.setMinimumSize(ZERO_DIM);
		btn.setMaximumSize(ZERO_DIM);
		btn.setOpaque(false);
		btn.setBorder(null);
		return btn;
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
		if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color color;
		if (isDragging) color = UITheme.ACCENT_BLUE_LIGHT;
		else if (isThumbRollover()) color = new Color(75, 95, 130);
		else color = new Color(45, 60, 85);

		g2.setColor(color);

		int x = thumbBounds.x;
		int y = thumbBounds.y;
		int w = thumbBounds.width;
		int h = thumbBounds.height;

		if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
			w = Math.min(w, THICKNESS);
			x = thumbBounds.x + (thumbBounds.width - w) / 2;
		} else {
			h = Math.min(h, THICKNESS);
			y = thumbBounds.y + (thumbBounds.height - h) / 2;
		}

		int radius = Math.min(w, h);
		g2.fillRoundRect(x, y, Math.max(w, 1), Math.max(h, 1), radius, radius);
		g2.dispose();
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		if (scrollbar.getOrientation() == JScrollBar.VERTICAL) return new Dimension(THICKNESS + 2, super.getPreferredSize(c).height);
		else return new Dimension(super.getPreferredSize(c).width, THICKNESS + 2);
	}

}