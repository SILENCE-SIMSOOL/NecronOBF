package silence.simsool.protector.obfuscator.ui.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import silence.simsool.protector.obfuscator.ui.UITheme;

public final class ModernComboBox<E> extends JComboBox<E> {

	public ModernComboBox(E[] items) {
		super(items);
		setUI(new ModernComboBoxUI());
		setBackground(UITheme.BG_CARD);
		setForeground(UITheme.TEXT_PRIMARY);
		setFont(UITheme.FONT_SUB);
		setPreferredSize(new Dimension(140, 28));
		setFocusable(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setOpaque(true);
				if (isSelected) {
					setBackground(UITheme.ACCENT_BLUE);
					setForeground(Color.WHITE);
				} else {
					setBackground(UITheme.BG_CARD);
					setForeground(UITheme.TEXT_PRIMARY);
				}
				setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
				return this;
			}
		});
	}

	private static final class ModernComboBoxUI extends BasicComboBoxUI {
		@Override
		protected JButton createArrowButton() {
			JButton btn = new JButton() {
				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(UITheme.BG_CARD);
					g2.fillRect(0, 0, getWidth(), getHeight());

					// Draw small chevron
					g2.setColor(UITheme.TEXT_SECONDARY);
					int cx = getWidth() / 2;
					int cy = getHeight() / 2;
					g2.drawLine(cx - 3, cy - 2, cx, cy + 1);
					g2.drawLine(cx, cy + 1, cx + 3, cy - 2);
					g2.dispose();
				}
			};
			btn.setBorder(BorderFactory.createEmptyBorder());
			btn.setContentAreaFilled(false);
			btn.setFocusPainted(false);
			btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			btn.setPreferredSize(new Dimension(22, 28));
			return btn;
		}

		@Override
		public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(UITheme.BG_CARD);
			g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			g2.dispose();
		}

		@Override
		public void paint(Graphics g, JComponent c) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Fill dark background
			g2.setColor(UITheme.BG_CARD);
			g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);

			// Dark border
			g2.setColor(UITheme.BORDER_CARD);
			g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
			g2.dispose();

			super.paint(g, c);
		}

		@Override
		protected ComboPopup createPopup() {
			BasicComboPopup popup = (BasicComboPopup) super.createPopup();
			popup.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
			popup.getList().setBackground(UITheme.BG_CARD);
			popup.getList().setSelectionBackground(UITheme.ACCENT_BLUE);
			return popup;
		}
	}
}