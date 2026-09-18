package silence.simsool.protector.obfuscator.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JComponent;

public final class VectorIcon extends JComponent implements Icon {

	public enum Type {
		SHIELD, FOLDER, GEAR, CUBE, SLIDERS, DOCUMENT, CODE, SHUFFLE, BULB,
		LOCK, HASH, NOTE, HOME, CLOSE, MINIMIZE, MAXIMIZE, ARROW_RIGHT, DISK, DOWNLOAD, UPLOAD,
		DISCORD, BOOK
	}

	private final Type type;
	private final int size;
	private Color color;

	public VectorIcon(Type type, int size, Color color) {
		this.type = type;
		this.size = size;
		this.color = color;
		Dimension d = new Dimension(size, size);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
		setOpaque(false);
	}

	public void setColor(Color color) {
		this.color = color;
		repaint();
	}

	@Override
	public int getIconWidth() { return size; }

	@Override
	public int getIconHeight() { return size; }

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate(x, y);
		paintIconContent(g2, size, size);
		g2.dispose();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		int x = (getWidth() - size) / 2;
		int y = (getHeight() - size) / 2;
		g2.translate(x, y);
		paintIconContent(g2, size, size);
		g2.dispose();
	}

	private void paintIconContent(Graphics2D g2, int w, int h) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setColor(color != null ? color : Color.WHITE);

		float s = w / 16.0f;
		g2.setStroke(new BasicStroke(Math.max(1.3f, 1.5f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		switch (type) {
			case SHIELD -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(8 * s, 1.5f * s);
				p.lineTo(14 * s, 3.5f * s);
				p.quadTo(14 * s, 10 * s, 8 * s, 14.5f * s);
				p.quadTo(2 * s, 10 * s, 2 * s, 3.5f * s);
				p.closePath();
				g2.draw(p);
				g2.drawLine((int) (8 * s), (int) (4 * s), (int) (8 * s), (int) (12 * s));
			}
			case FOLDER -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(2 * s, 4 * s);
				p.lineTo(6 * s, 4 * s);
				p.lineTo(8 * s, 6 * s);
				p.lineTo(14 * s, 6 * s);
				p.lineTo(14 * s, 13 * s);
				p.lineTo(2 * s, 13 * s);
				p.closePath();
				g2.draw(p);
			}
			case GEAR -> {
				g2.drawOval((int) (5 * s), (int) (5 * s), (int) (6 * s), (int) (6 * s));
				for (int i = 0; i < 8; i++) {
					double angle = i * Math.PI / 4.0;
					int x1 = (int) (8 * s + Math.cos(angle) * 4.5 * s);
					int y1 = (int) (8 * s + Math.sin(angle) * 4.5 * s);
					int x2 = (int) (8 * s + Math.cos(angle) * 7.0 * s);
					int y2 = (int) (8 * s + Math.sin(angle) * 7.0 * s);
					g2.drawLine(x1, y1, x2, y2);
				}
			}
			case CUBE -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(8 * s, 2 * s);
				p.lineTo(14 * s, 5 * s);
				p.lineTo(14 * s, 11 * s);
				p.lineTo(8 * s, 14 * s);
				p.lineTo(2 * s, 11 * s);
				p.lineTo(2 * s, 5 * s);
				p.closePath();
				g2.draw(p);
				g2.drawLine((int) (8 * s), (int) (2 * s), (int) (8 * s), (int) (8 * s));
				g2.drawLine((int) (8 * s), (int) (8 * s), (int) (14 * s), (int) (5 * s));
				g2.drawLine((int) (8 * s), (int) (8 * s), (int) (2 * s), (int) (5 * s));
			}
			case SLIDERS -> {
				g2.drawLine((int) (3 * s), (int) (4 * s), (int) (13 * s), (int) (4 * s));
				g2.drawOval((int) (5 * s), (int) (2.5f * s), (int) (3 * s), (int) (3 * s));
				g2.drawLine((int) (3 * s), (int) (8 * s), (int) (13 * s), (int) (8 * s));
				g2.drawOval((int) (9 * s), (int) (6.5f * s), (int) (3 * s), (int) (3 * s));
				g2.drawLine((int) (3 * s), (int) (12 * s), (int) (13 * s), (int) (12 * s));
				g2.drawOval((int) (4 * s), (int) (10.5f * s), (int) (3 * s), (int) (3 * s));
			}
			case DOCUMENT -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(4 * s, 2 * s);
				p.lineTo(10 * s, 2 * s);
				p.lineTo(13 * s, 5 * s);
				p.lineTo(13 * s, 14 * s);
				p.lineTo(4 * s, 14 * s);
				p.closePath();
				g2.draw(p);
				g2.drawLine((int) (6 * s), (int) (7 * s), (int) (11 * s), (int) (7 * s));
				g2.drawLine((int) (6 * s), (int) (10 * s), (int) (11 * s), (int) (10 * s));
			}
			case CODE -> {
				GeneralPath p1 = new GeneralPath();
				p1.moveTo(6 * s, 5 * s); p1.lineTo(3 * s, 8 * s); p1.lineTo(6 * s, 11 * s);
				g2.draw(p1);
				GeneralPath p2 = new GeneralPath();
				p2.moveTo(10 * s, 5 * s); p2.lineTo(13 * s, 8 * s); p2.lineTo(10 * s, 11 * s);
				g2.draw(p2);
				g2.drawLine((int) (9.5f * s), (int) (3 * s), (int) (6.5f * s), (int) (13 * s));
			}
			case SHUFFLE -> {
				g2.drawLine((int) (3 * s), (int) (4 * s), (int) (7 * s), (int) (4 * s));
				g2.drawLine((int) (7 * s), (int) (4 * s), (int) (11 * s), (int) (12 * s));
				g2.drawLine((int) (11 * s), (int) (12 * s), (int) (14 * s), (int) (12 * s));
				g2.drawLine((int) (12 * s), (int) (10 * s), (int) (14 * s), (int) (12 * s));
				g2.drawLine((int) (12 * s), (int) (14 * s), (int) (14 * s), (int) (12 * s));
			}
			case BULB -> {
				g2.drawOval((int) (5 * s), (int) (2 * s), (int) (6 * s), (int) (6 * s));
				g2.drawLine((int) (6 * s), (int) (8 * s), (int) (6 * s), (int) (11 * s));
				g2.drawLine((int) (10 * s), (int) (8 * s), (int) (10 * s), (int) (11 * s));
				g2.drawLine((int) (6 * s), (int) (11 * s), (int) (10 * s), (int) (11 * s));
				g2.drawLine((int) (7 * s), (int) (13 * s), (int) (9 * s), (int) (13 * s));
			}
			case LOCK -> {
				g2.draw(new RoundRectangle2D.Float(3.5f * s, 6.5f * s, 9 * s, 7.5f * s, 2 * s, 2 * s));
				GeneralPath p = new GeneralPath();
				p.moveTo(5.5f * s, 6.5f * s);
				p.lineTo(5.5f * s, 4 * s);
				p.quadTo(8 * s, 1.5f * s, 10.5f * s, 4 * s);
				p.lineTo(10.5f * s, 6.5f * s);
				g2.draw(p);
			}
			case HASH -> {
				g2.drawLine((int) (6 * s), (int) (2 * s), (int) (5 * s), (int) (14 * s));
				g2.drawLine((int) (11 * s), (int) (2 * s), (int) (10 * s), (int) (14 * s));
				g2.drawLine((int) (3 * s), (int) (6 * s), (int) (13 * s), (int) (6 * s));
				g2.drawLine((int) (2.5f * s), (int) (10 * s), (int) (12.5f * s), (int) (10 * s));
			}
			case NOTE -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(5 * s, 13 * s);
				p.lineTo(5 * s, 5 * s);
				p.lineTo(12 * s, 3 * s);
				p.lineTo(12 * s, 11 * s);
				g2.draw(p);
				g2.fillOval((int) (3 * s), (int) (11 * s), (int) (4 * s), (int) (3 * s));
				g2.fillOval((int) (10 * s), (int) (9 * s), (int) (4 * s), (int) (3 * s));
			}
			case HOME -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(2 * s, 8 * s);
				p.lineTo(8 * s, 2.5f * s);
				p.lineTo(14 * s, 8 * s);
				p.lineTo(12 * s, 8 * s);
				p.lineTo(12 * s, 13.5f * s);
				p.lineTo(4 * s, 13.5f * s);
				p.lineTo(4 * s, 8 * s);
				p.closePath();
				g2.draw(p);
			}
			case CLOSE -> {
				g2.drawLine((int) (4 * s), (int) (4 * s), (int) (12 * s), (int) (12 * s));
				g2.drawLine((int) (12 * s), (int) (4 * s), (int) (4 * s), (int) (12 * s));
			}
			case MINIMIZE -> {
				g2.drawLine((int) (3 * s), (int) (10 * s), (int) (13 * s), (int) (10 * s));
			}
			case MAXIMIZE -> {
				g2.drawRect((int) (3 * s), (int) (3 * s), (int) (10 * s), (int) (10 * s));
			}
			case ARROW_RIGHT -> {
				g2.drawLine((int) (3 * s), (int) (8 * s), (int) (13 * s), (int) (8 * s));
				g2.drawLine((int) (9 * s), (int) (4 * s), (int) (13 * s), (int) (8 * s));
				g2.drawLine((int) (9 * s), (int) (12 * s), (int) (13 * s), (int) (8 * s));
			}
			case DISK -> {
				g2.drawRect((int) (3 * s), (int) (3 * s), (int) (10 * s), (int) (10 * s));
				g2.fillRect((int) (5 * s), (int) (3 * s), (int) (6 * s), (int) (4 * s));
			}
			case DOWNLOAD -> {
				g2.drawLine((int) (8 * s), (int) (2 * s), (int) (8 * s), (int) (11 * s));
				g2.drawLine((int) (5 * s), (int) (8 * s), (int) (8 * s), (int) (11 * s));
				g2.drawLine((int) (11 * s), (int) (8 * s), (int) (8 * s), (int) (11 * s));
				g2.drawLine((int) (3 * s), (int) (14 * s), (int) (13 * s), (int) (14 * s));
			}
			case UPLOAD -> {
				g2.drawLine((int) (8 * s), (int) (12 * s), (int) (8 * s), (int) (3 * s));
				g2.drawLine((int) (5 * s), (int) (6 * s), (int) (8 * s), (int) (3 * s));
				g2.drawLine((int) (11 * s), (int) (6 * s), (int) (8 * s), (int) (3 * s));
				g2.drawLine((int) (3 * s), (int) (14 * s), (int) (13 * s), (int) (14 * s));
			}
			case DISCORD -> {
				GeneralPath p = new GeneralPath();
				p.moveTo(3 * s, 5 * s);
				p.quadTo(8 * s, 3.5f * s, 13 * s, 5 * s);
				p.lineTo(14 * s, 12 * s);
				p.quadTo(11 * s, 13.5f * s, 9.5f * s, 12.5f * s);
				p.lineTo(9 * s, 11.5f * s);
				p.quadTo(8 * s, 12 * s, 7 * s, 11.5f * s);
				p.lineTo(6.5f * s, 12.5f * s);
				p.quadTo(5 * s, 13.5f * s, 2 * s, 12 * s);
				p.closePath();
				g2.draw(p);
				g2.fillOval((int) (5 * s), (int) (7 * s), (int) (2 * s), (int) (2.5f * s));
				g2.fillOval((int) (9 * s), (int) (7 * s), (int) (2 * s), (int) (2.5f * s));
			}
			case BOOK -> {
				GeneralPath p1 = new GeneralPath();
				p1.moveTo(8 * s, 4 * s); p1.quadTo(5 * s, 3 * s, 2 * s, 4 * s);
				p1.lineTo(2 * s, 13 * s); p1.quadTo(5 * s, 12 * s, 8 * s, 13 * s);
				p1.closePath();
				g2.draw(p1);
				GeneralPath p2 = new GeneralPath();
				p2.moveTo(8 * s, 4 * s); p2.quadTo(11 * s, 3 * s, 14 * s, 4 * s);
				p2.lineTo(14 * s, 13 * s); p2.quadTo(11 * s, 12 * s, 8 * s, 13 * s);
				p2.closePath();
				g2.draw(p2);
			}
		}
	}
}