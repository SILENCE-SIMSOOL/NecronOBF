package silence.simsool.protector.obfuscator.ui.component;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import silence.simsool.protector.obfuscator.ui.UITheme;

public final class ToggleSwitch extends JComponent {

	private static final int SWITCH_WIDTH = 38;
	private static final int SWITCH_HEIGHT = 20;
	private static final int THUMB_PADDING = 3;

	private boolean selected;
	private final List<ActionListener> actionListeners = new ArrayList<>();

	public ToggleSwitch(boolean initial) {
		this.selected = initial;
		Dimension d = new Dimension(SWITCH_WIDTH, SWITCH_HEIGHT);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
		setSize(d);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setFocusable(true);
		setOpaque(false);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (isEnabled()) {
					setSelected(!selected);
					requestFocusInWindow();
				}
			}
		});

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (isEnabled() && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER)) {
					setSelected(!selected);
				}
			}
		});
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			repaint();
			fireActionPerformed();
		}
	}

	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}

	private void fireActionPerformed() {
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle");
		for (ActionListener listener : actionListeners) {
			listener.actionPerformed(event);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(SWITCH_WIDTH, SWITCH_HEIGHT);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(SWITCH_WIDTH, SWITCH_HEIGHT);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(SWITCH_WIDTH, SWITCH_HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int x = Math.max(0, (getWidth() - SWITCH_WIDTH) / 2);
		int y = Math.max(0, (getHeight() - SWITCH_HEIGHT) / 2);
		int w = SWITCH_WIDTH;
		int h = SWITCH_HEIGHT;
		int arc = h;

		// Draw track
		if (!isEnabled()) {
			g2.setColor(new Color(25, 33, 48));
		} else if (selected) {
			g2.setColor(UITheme.SWITCH_TRACK_ON);
		} else {
			g2.setColor(UITheme.SWITCH_TRACK_OFF);
		}
		g2.fillRoundRect(x, y, w, h, arc, arc);

		// Track border
		g2.setColor(selected ? UITheme.ACCENT_BLUE_LIGHT : UITheme.BORDER_CARD);
		g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

		// Draw thumb
		int thumbSize = h - (THUMB_PADDING * 2);
		int thumbX = selected ? (x + w - thumbSize - THUMB_PADDING) : (x + THUMB_PADDING);
		int thumbY = y + THUMB_PADDING;

		g2.setColor(isEnabled() ? UITheme.SWITCH_THUMB : UITheme.TEXT_MUTED);
		g2.fillOval(thumbX, thumbY, thumbSize, thumbSize);

		g2.dispose();
	}
}