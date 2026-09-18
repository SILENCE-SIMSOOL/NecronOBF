package silence.simsool.protector.obfuscator.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import silence.simsool.protector.obfuscator.config.ObfuscationConfig;
import silence.simsool.protector.obfuscator.config.ProjectManager;
import silence.simsool.protector.obfuscator.core.JarObfuscator;
import silence.simsool.protector.obfuscator.ui.component.ModernCard;
import silence.simsool.protector.obfuscator.ui.component.ModernComboBox;
import silence.simsool.protector.obfuscator.ui.component.ToggleSwitch;
import silence.simsool.protector.obfuscator.ui.component.VectorIcon;

public final class ProtectorFrame extends JFrame {

	private static final Preferences PREFS = Preferences.userNodeForPackage(ProtectorFrame.class);
	private static final String DISCORD_INVITE_URL = "https://discord.gg/2Zt8HDksJs";

	// Controls
	private final JTextField inputField = modernField("Select input JAR file...");
	private final JTextField outputField = modernField("Select output JAR file...");
	private final JTextField mainClassField = modernField("com.example.Main");
	private final JTextField packageRootField = modernField("silence");

	// Toggle Switches
	private final ToggleSwitch fabricSwitch = new ToggleSwitch(true);
	private final ToggleSwitch renameClassesSwitch = new ToggleSwitch(true);
	private final ToggleSwitch protectStringsSwitch = new ToggleSwitch(true);
	private final ToggleSwitch renameMethodsSwitch = new ToggleSwitch(true);
	private final ToggleSwitch protectNumbersSwitch = new ToggleSwitch(true);
	private final ToggleSwitch renameFieldsSwitch = new ToggleSwitch(true);
	private final ToggleSwitch randomizedSLogicSwitch = new ToggleSwitch(true);
	private final ToggleSwitch randomizePackagesSwitch = new ToggleSwitch(true);
	private final ToggleSwitch preserveAnnotationsSwitch = new ToggleSwitch(true);
	private final ToggleSwitch packageRootSwitch = new ToggleSwitch(true);

	// Custom Dark ComboBoxes
	private final ModernComboBox<String> seedModeCombo = new ModernComboBox<>(new String[]{"Random", "Fixed Seed", "Timestamp"});
	private final ModernComboBox<String> packageDepthCombo = new ModernComboBox<>(new String[]{"1 - 3", "2 - 4", "Flat"});
	private final ModernComboBox<String> slogicTemplateCombo = new ModernComboBox<>(new String[]{"Dynamic", "Ultra Polymorphic", "Stealth"});

	// Action & Log
	private final ProtectButton protectButton = new ProtectButton();
	private final JProgressBar progressBar = new JProgressBar() {
		{
			setOpaque(false);
			setBorder(null);
		}
		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			// Track background
			g2.setColor(new Color(20, 27, 42));
			g2.fillRoundRect(0, 0, w, h, UITheme.RADIUS_PILL, UITheme.RADIUS_PILL);
			// Indeterminate animation
			if (isIndeterminate()) {
				int barW = Math.max(30, w / 3);
				long time = System.currentTimeMillis();
				int x = (int) ((time / 6) % (w + barW)) - barW;
				g2.setColor(UITheme.ACCENT_BLUE_LIGHT);
				g2.fillRoundRect(Math.max(0, x), 0, Math.min(barW, w - Math.max(0, x)), h, UITheme.RADIUS_PILL, UITheme.RADIUS_PILL);
			}
			g2.setColor(UITheme.BORDER_CARD);
			g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.RADIUS_PILL, UITheme.RADIUS_PILL);
			g2.dispose();
		}
	};
	private final JTextArea logArea = new JTextArea();
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel mainCardContainer = new JPanel(cardLayout);
	private final List<SidebarButton> navButtons = new ArrayList<>();

	// Dynamic Text Labels for Language Switching
	private JLabel brandSubLabel;
	private JLabel pageTitleLabel;
	private JLabel pageDescLabel;
	private JLabel projSubLabel;
	private JLabel discordLabel;
	private BannerCard bannerCard;
	private LangSwitchButton langBtn;

	// Window drag support
	private Point dragClickPoint;

	public ProtectorFrame() {
		super(I18n.get("title"));
		setUndecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(1180, 780));
		setSize(1240, 800);
		setLocationRelativeTo(null);

		JPanel root = new JPanel(new BorderLayout(0, 0));
		root.setBackground(UITheme.BG_DARK);
		root.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CARD, 1));
		setContentPane(root);

		root.add(createTopTitleBar(), BorderLayout.NORTH);

		JPanel body = new JPanel(new BorderLayout(0, 0));
		body.setOpaque(false);
		body.add(createSidebar(), BorderLayout.WEST);

		setupCards();
		body.add(mainCardContainer, BorderLayout.CENTER);
		root.add(body, BorderLayout.CENTER);

		packageRootSwitch.addActionListener(e -> updatePackageRootState());
		randomizePackagesSwitch.addActionListener(e -> updatePackageRootState());
		protectButton.addActionListener(e -> startObfuscation());

		I18n.addListener(this::refreshUiTexts);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				saveSettings();
			}
		});

		loadSettings();
		updatePackageRootState();
	}

	public void showTab(String targetCard) {
		cardLayout.show(mainCardContainer, targetCard);
		String key = switch (targetCard) {
			case "PHILOSOPHY" -> "nav_philosophy";
			case "OUTPUT" -> "nav_output";
			default -> "nav_project";
		};
		for (SidebarButton b : navButtons) {
			b.setActive(b.getTextKey().equals(key));
		}
	}

	private JPanel createTopTitleBar() {
		JPanel bar = new JPanel(new BorderLayout());
		bar.setBackground(UITheme.BG_SIDEBAR);
		bar.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CARD),
			BorderFactory.createEmptyBorder(8, 16, 8, 12)
		));

		bar.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dragClickPoint = e.getPoint();
			}
		});
		bar.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Point current = e.getLocationOnScreen();
				setLocation(current.x - dragClickPoint.x, current.y - dragClickPoint.y);
			}
		});

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		left.setOpaque(false);

		VectorIcon shieldLogo = new VectorIcon(VectorIcon.Type.SHIELD, 18, UITheme.ACCENT_CYAN);
		JLabel brand = new JLabel("SilenceProtector");
		brand.setForeground(UITheme.TEXT_PRIMARY);
		brand.setFont(UITheme.FONT_HEADER);

		brandSubLabel = new JLabel(I18n.get("slogan"));
		brandSubLabel.setForeground(UITheme.TEXT_MUTED);
		brandSubLabel.setFont(UITheme.FONT_SMALL);

		left.add(shieldLogo);
		left.add(brand);
		left.add(brandSubLabel);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
		right.setOpaque(false);

		JLabel badge = new JLabel(I18n.get("badge"));
		badge.setForeground(UITheme.TEXT_MUTED);
		badge.setFont(UITheme.createFont(Font.BOLD, 10));
		right.add(badge);

		// Language Toggle Button (EN | KO)
		langBtn = new LangSwitchButton();
		right.add(langBtn);

		// Window Controls
		WindowControlButton minBtn = new WindowControlButton(VectorIcon.Type.MINIMIZE, false);
		minBtn.addActionListener(e -> setState(Frame.ICONIFIED));

		WindowControlButton closeBtn = new WindowControlButton(VectorIcon.Type.CLOSE, true);
		closeBtn.addActionListener(e -> {
			saveSettings();
			System.exit(0);
		});

		right.add(Box.createHorizontalStrut(6));
		right.add(minBtn);
		right.add(closeBtn);

		bar.add(left, BorderLayout.WEST);
		bar.add(right, BorderLayout.EAST);
		return bar;
	}

	private JPanel createSidebar() {
		JPanel sidebar = new JPanel(new BorderLayout(0, 14));
		sidebar.setBackground(UITheme.BG_SIDEBAR);
		sidebar.setPreferredSize(new Dimension(210, 0));
		sidebar.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER_CARD),
			BorderFactory.createEmptyBorder(14, 12, 14, 12)
		));

		// Project loaded indicator with unified rounded style
		JPanel projectBox = new JPanel(new BorderLayout(10, 0)) {
			{ setOpaque(false); }
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(UITheme.BG_CARD);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.setColor(UITheme.BORDER_CARD);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		projectBox.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

		JLabel iconP = new JLabel("P", SwingConstants.CENTER) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(30, 41, 59));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_SMALL, UITheme.RADIUS_SMALL);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		iconP.setOpaque(false);
		iconP.setForeground(UITheme.TEXT_PRIMARY);
		iconP.setFont(UITheme.FONT_BUTTON);
		iconP.setPreferredSize(new Dimension(28, 28));

		JPanel projText = new JPanel(new GridLayout(2, 1, 0, 1));
		projText.setOpaque(false);
		JLabel projTitle = new JLabel("MyProject");
		projTitle.setForeground(UITheme.TEXT_PRIMARY);
		projTitle.setFont(UITheme.FONT_BOLD_SUB);
		projSubLabel = new JLabel(I18n.get("active_config"));
		projSubLabel.setForeground(UITheme.TEXT_MUTED);
		projSubLabel.setFont(UITheme.FONT_SMALL);
		projText.add(projTitle);
		projText.add(projSubLabel);

		projectBox.add(iconP, BorderLayout.WEST);
		projectBox.add(projText, BorderLayout.CENTER);

		// Core Meaningful Navigation Items
		JPanel navList = new JPanel();
		navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
		navList.setOpaque(false);

		Object[][] menuItems = {
			{VectorIcon.Type.HOME, "nav_project", "PROJECT"},
			{VectorIcon.Type.BOOK, "nav_philosophy", "PHILOSOPHY"},
			{VectorIcon.Type.DOCUMENT, "nav_output", "OUTPUT"}
		};

		navButtons.clear();
		for (int i = 0; i < menuItems.length; i++) {
			VectorIcon.Type iconType = (VectorIcon.Type) menuItems[i][0];
			String key = (String) menuItems[i][1];
			String targetCard = (String) menuItems[i][2];
			SidebarButton btn = new SidebarButton(iconType, key, i == 0);
			btn.addActionListener(e -> {
				for (SidebarButton b : navButtons) b.setActive(b == btn);
				cardLayout.show(mainCardContainer, targetCard);
			});
			navButtons.add(btn);
			navList.add(btn);
			navList.add(Box.createVerticalStrut(4));
		}

		JPanel topContainer = new JPanel(new BorderLayout(0, 14));
		topContainer.setOpaque(false);
		topContainer.add(projectBox, BorderLayout.NORTH);
		topContainer.add(navList, BorderLayout.CENTER);
		sidebar.add(topContainer, BorderLayout.NORTH);

		// Sidebar Bottom: Discord + Copyright
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		bottomPanel.setOpaque(false);

		// Discord Community Button
		bottomPanel.add(createDiscordButton());
		bottomPanel.add(Box.createVerticalStrut(14));

		JLabel motto1 = new JLabel("STRONGER");
		motto1.setForeground(UITheme.TEXT_MUTED);
		motto1.setFont(UITheme.createFont(Font.BOLD, 10));

		JLabel motto2 = new JLabel("SOFTER");
		motto2.setForeground(UITheme.TEXT_MUTED);
		motto2.setFont(UITheme.createFont(Font.BOLD, 10));

		JLabel motto3 = new JLabel("QUIETER");
		motto3.setForeground(UITheme.TEXT_MUTED);
		motto3.setFont(UITheme.createFont(Font.BOLD, 10));

		JLabel copyright = new JLabel(I18n.get("copyright"));
		copyright.setForeground(UITheme.TEXT_DISABLED);
		copyright.setFont(UITheme.FONT_SMALL);

		JPanel verRow = new JPanel(new BorderLayout());
		verRow.setOpaque(false);
		JLabel verLabel = new JLabel("v1.0.0");
		verLabel.setForeground(UITheme.TEXT_MUTED);
		verLabel.setFont(UITheme.FONT_SMALL);

		verRow.add(verLabel, BorderLayout.WEST);

		bottomPanel.add(motto1);
		bottomPanel.add(motto2);
		bottomPanel.add(motto3);
		bottomPanel.add(Box.createVerticalStrut(6));
		bottomPanel.add(copyright);
		bottomPanel.add(Box.createVerticalStrut(10));
		bottomPanel.add(verRow);

		sidebar.add(bottomPanel, BorderLayout.SOUTH);
		return sidebar;
	}

	private JButton createDiscordButton() {
		VectorIcon discordIcon = new VectorIcon(VectorIcon.Type.DISCORD, 16, new Color(129, 140, 248));
		discordLabel = new JLabel(I18n.get("join_discord"));
		discordLabel.setForeground(UITheme.TEXT_PRIMARY);
		discordLabel.setFont(UITheme.FONT_BUTTON);

		return new JButton() {
			{
				setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
				setOpaque(false);
				setContentAreaFilled(false);
				setFocusPainted(false);
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
				add(discordIcon);
				add(discordLabel);
				addActionListener(e -> {
					try {
						Desktop.getDesktop().browse(URI.create(DISCORD_INVITE_URL));
					} catch (Exception ex) {
						log("Could not open browser: " + DISCORD_INVITE_URL);
					}
				});
			}

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = getModel().isRollover() ? new Color(79, 70, 229, 70) : new Color(49, 46, 129, 45);
				g2.setColor(bg);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.setColor(new Color(99, 102, 241, 120));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.dispose();
				super.paintComponent(g);
			}
		};
	}

	private void setupCards() {
		mainCardContainer.setOpaque(false);
		mainCardContainer.add(createProjectPanel(), "PROJECT");
		mainCardContainer.add(new PhilosophyPanel(), "PHILOSOPHY");
		mainCardContainer.add(createOutputPanel(), "OUTPUT");
	}

	private JPanel createProjectPanel() {
		JPanel container = new JPanel(new BorderLayout(0, 10));
		container.setBackground(UITheme.BG_MAIN);
		container.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

		// Top Header Row
		JPanel headerRow = new JPanel(new BorderLayout());
		headerRow.setOpaque(false);

		JPanel titleCol = new JPanel();
		titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
		titleCol.setOpaque(false);

		pageTitleLabel = new JLabel(I18n.get("header_title"));
		pageTitleLabel.setForeground(UITheme.TEXT_PRIMARY);
		pageTitleLabel.setFont(UITheme.FONT_TITLE);

		pageDescLabel = new JLabel(I18n.get("header_desc"));
		pageDescLabel.setForeground(UITheme.TEXT_SECONDARY);
		pageDescLabel.setFont(UITheme.FONT_SUB);

		titleCol.add(pageTitleLabel);
		titleCol.add(Box.createVerticalStrut(2));
		titleCol.add(pageDescLabel);

		// Right Banner Card (Clickable -> opens Philosophy tab)
		bannerCard = new BannerCard();
		bannerCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		bannerCard.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				cardLayout.show(mainCardContainer, "PHILOSOPHY");
				for (SidebarButton b : navButtons) b.setActive(b.getTextKey().equals("nav_philosophy"));
			}
		});
		headerRow.add(titleCol, BorderLayout.WEST);
		headerRow.add(bannerCard, BorderLayout.EAST);

		container.add(headerRow, BorderLayout.NORTH);

		// Center Grid (2 Columns)
		JPanel grid = new ScrollableGrid(new GridLayout(1, 2, 16, 0));
		grid.setOpaque(false);

		// Left Column: Input/Output + Protection Options
		JPanel leftCol = new JPanel();
		leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
		leftCol.setOpaque(false);

		leftCol.add(createInputOutputCard());
		leftCol.add(Box.createVerticalStrut(8));
		leftCol.add(createProtectionOptionsCard());

		// Right Column: Main Class / Fabric + Package Root + Randomization / Logic
		JPanel rightCol = new JPanel();
		rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
		rightCol.setOpaque(false);

		rightCol.add(createMainClassCard());
		rightCol.add(Box.createVerticalStrut(8));
		rightCol.add(createPackageRootCard());
		rightCol.add(Box.createVerticalStrut(8));
		rightCol.add(createRandomizationCard());

		grid.add(leftCol);
		grid.add(rightCol);

		JScrollPane scrollPane = new JScrollPane(grid);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(14);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));

		container.add(scrollPane, BorderLayout.CENTER);
		container.add(createBottomBar(), BorderLayout.SOUTH);
		return container;
	}

	private ModernCard createInputOutputCard() {
		ModernCard card = new ModernCard(VectorIcon.Type.FOLDER, I18n.get("card_io"), I18n.get("card_io_sub"));

		JPanel content = new JPanel(new GridBagLayout());
		content.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 0, 2, 0);

		c.gridx = 0; c.gridy = 0; c.weightx = 1.0; c.gridwidth = 2;
		content.add(createFieldLabel(I18n.get("input_jar")), c);

		c.gridy = 1; c.gridwidth = 1;
		content.add(inputField, c);

		c.gridx = 1; c.weightx = 0; c.insets = new Insets(2, 6, 2, 0);
		OutlineButton inputBrowse = new OutlineButton(VectorIcon.Type.FOLDER, I18n.get("browse"));
		inputBrowse.addActionListener(e -> chooseInput());
		content.add(inputBrowse, c);

		c.gridx = 0; c.gridy = 2; c.weightx = 1.0; c.gridwidth = 2; c.insets = new Insets(6, 0, 2, 0);
		content.add(createFieldLabel(I18n.get("output_jar")), c);

		c.gridy = 3; c.gridwidth = 1; c.insets = new Insets(2, 0, 2, 0);
		content.add(outputField, c);

		c.gridx = 1; c.weightx = 0; c.insets = new Insets(2, 6, 2, 0);
		OutlineButton outputBrowse = new OutlineButton(VectorIcon.Type.FOLDER, I18n.get("browse"));
		outputBrowse.addActionListener(e -> chooseOutput());
		content.add(outputBrowse, c);

		card.add(content, BorderLayout.CENTER);
		return card;
	}

	private ModernCard createMainClassCard() {
		ModernCard card = new ModernCard(VectorIcon.Type.CUBE, I18n.get("card_main"), I18n.get("card_main_sub"));

		JPanel content = new JPanel(new GridBagLayout());
		content.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 0, 2, 0);

		c.gridx = 0; c.gridy = 0; c.weightx = 1.0;
		content.add(createFieldLabel(I18n.get("main_class")), c);

		c.gridy = 1;
		content.add(mainClassField, c);

		c.gridy = 2; c.insets = new Insets(8, 0, 0, 0);
		JPanel toggleRow = createToggleRow(
			fabricSwitch,
			I18n.get("fabric_toggle"),
			I18n.get("fabric_toggle_sub")
		);
		content.add(toggleRow, c);

		card.add(content, BorderLayout.CENTER);
		return card;
	}

	private ModernCard createProtectionOptionsCard() {
		ModernCard card = new ModernCard(VectorIcon.Type.SHIELD, I18n.get("card_opts"), I18n.get("card_opts_sub"));

		JPanel grid = new JPanel(new GridLayout(4, 2, 12, 10));
		grid.setOpaque(false);

		grid.add(createOptionItem(VectorIcon.Type.NOTE, I18n.get("opt_class"), I18n.get("opt_class_sub"), renameClassesSwitch));
		grid.add(createOptionItem(VectorIcon.Type.LOCK, I18n.get("opt_string"), I18n.get("opt_string_sub"), protectStringsSwitch));
		grid.add(createOptionItem(VectorIcon.Type.SHUFFLE, I18n.get("opt_method"), I18n.get("opt_method_sub"), renameMethodsSwitch));
		grid.add(createOptionItem(VectorIcon.Type.HASH, I18n.get("opt_number"), I18n.get("opt_number_sub"), protectNumbersSwitch));
		grid.add(createOptionItem(VectorIcon.Type.CUBE, I18n.get("opt_field"), I18n.get("opt_field_sub"), renameFieldsSwitch));
		grid.add(createOptionItem(VectorIcon.Type.SHUFFLE, I18n.get("opt_slogic"), I18n.get("opt_slogic_sub"), randomizedSLogicSwitch));
		grid.add(createOptionItem(VectorIcon.Type.CUBE, I18n.get("opt_package"), I18n.get("opt_package_sub"), randomizePackagesSwitch));
		grid.add(createOptionItem(VectorIcon.Type.CODE, I18n.get("opt_anno"), I18n.get("opt_anno_sub"), preserveAnnotationsSwitch));

		card.add(grid, BorderLayout.CENTER);
		return card;
	}

	private ModernCard createPackageRootCard() {
		ModernCard card = new ModernCard(VectorIcon.Type.CUBE, I18n.get("card_pkg"), I18n.get("card_pkg_sub"));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);

		JPanel toggleRow = createToggleRow(
			packageRootSwitch,
			I18n.get("force_root"),
			I18n.get("force_root_sub")
		);
		content.add(toggleRow);
		content.add(Box.createVerticalStrut(6));

		content.add(packageRootField);
		content.add(Box.createVerticalStrut(3));

		JLabel note = new JLabel("Example: silence");
		note.setForeground(UITheme.TEXT_MUTED);
		note.setFont(UITheme.FONT_SMALL);
		content.add(note);

		card.add(content, BorderLayout.CENTER);
		return card;
	}

	private ModernCard createRandomizationCard() {
		ModernCard card = new ModernCard(VectorIcon.Type.SHUFFLE, I18n.get("card_rand"), I18n.get("card_rand_sub"));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);

		content.add(createComboRow(I18n.get("seed_mode"), seedModeCombo));
		content.add(Box.createVerticalStrut(4));
		content.add(createComboRow(I18n.get("package_depth"), packageDepthCombo));
		content.add(Box.createVerticalStrut(4));
		content.add(createComboRow(I18n.get("slogic_template"), slogicTemplateCombo));
		content.add(Box.createVerticalStrut(6));

		// Blue Info Callout Box with rounded styling
		JPanel infoBox = new JPanel(new BorderLayout(8, 0)) {
			{ setOpaque(false); }
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(UITheme.BG_INFO_BOX);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.setColor(UITheme.BORDER_INFO);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		infoBox.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

		VectorIcon bulb = new VectorIcon(VectorIcon.Type.BULB, 15, UITheme.ACCENT_CYAN);

		JPanel textCol = new JPanel();
		textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
		textCol.setOpaque(false);
		JLabel infoTitle = new JLabel(I18n.get("dynamic_info_title"));
		infoTitle.setForeground(UITheme.ACCENT_CYAN);
		infoTitle.setFont(UITheme.FONT_BOLD_SUB);
		JLabel infoDesc = new JLabel(I18n.get("dynamic_info_desc"));
		infoDesc.setForeground(UITheme.TEXT_SECONDARY);
		infoDesc.setFont(UITheme.FONT_SMALL);
		textCol.add(infoTitle);
		textCol.add(Box.createVerticalStrut(1));
		textCol.add(infoDesc);

		Runnable updateDynamicInfo = () -> {
			String seed = String.valueOf(seedModeCombo.getSelectedItem());
			String tpl = String.valueOf(slogicTemplateCombo.getSelectedItem());
			String seedDesc = switch (seed) {
				case "Fixed Seed" -> I18n.get("tip_seed_fixed");
				case "Timestamp" -> I18n.get("tip_seed_timestamp");
				default -> I18n.get("tip_seed_random");
			};
			String tplDesc = switch (tpl) {
				case "Ultra Polymorphic" -> I18n.get("tip_slogic_ultra");
				case "Stealth" -> I18n.get("tip_slogic_stealth");
				default -> I18n.get("tip_slogic_dynamic");
			};
			infoTitle.setText(tpl + "  \u2022  " + seed);
			infoDesc.setText("<html>" + tplDesc + " <font color='#64748b'>(" + seedDesc + ")</font></html>");
		};

		seedModeCombo.addActionListener(e -> updateDynamicInfo.run());
		slogicTemplateCombo.addActionListener(e -> updateDynamicInfo.run());
		updateDynamicInfo.run();

		infoBox.add(bulb, BorderLayout.WEST);
		infoBox.add(textCol, BorderLayout.CENTER);
		content.add(infoBox);

		card.add(content, BorderLayout.CENTER);
		return card;
	}

	private JPanel createBottomBar() {
		JPanel bar = new JPanel(new BorderLayout());
		bar.setOpaque(false);
		bar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		left.setOpaque(false);

		OutlineButton importBtn = new OutlineButton(VectorIcon.Type.DOWNLOAD, I18n.get("btn_import"));
		OutlineButton saveBtn = new OutlineButton(VectorIcon.Type.DISK, I18n.get("btn_save"));
		OutlineButton exportBtn = new OutlineButton(VectorIcon.Type.UPLOAD, I18n.get("btn_export"));

		saveBtn.addActionListener(e -> {
			saveSettings();
			log("Project settings successfully saved.");
		});

		exportBtn.addActionListener(e -> exportProject());
		importBtn.addActionListener(e -> importProject());

		left.add(importBtn);
		left.add(saveBtn);
		left.add(exportBtn);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		right.setOpaque(false);

		progressBar.setPreferredSize(new Dimension(150, 18));
		progressBar.setVisible(false);
		right.add(progressBar);
		right.add(protectButton);

		bar.add(left, BorderLayout.WEST);
		bar.add(right, BorderLayout.EAST);
		return bar;
	}

	private void exportProject() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Export Project Configuration");
		chooser.setFileFilter(new FileNameExtensionFilter("Silence Project (*.json)", "json"));
		chooser.setSelectedFile(new File("silence-project.json"));

		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			String pathStr = file.getAbsolutePath();
			if (!pathStr.endsWith(".json")) {
				file = new File(pathStr + ".json");
			}
			try {
				ProjectManager.ProjectData data = new ProjectManager.ProjectData(
					inputField.getText().trim(),
					outputField.getText().trim(),
					mainClassField.getText().trim(),
					renameClassesSwitch.isSelected(),
					renameMethodsSwitch.isSelected(),
					renameFieldsSwitch.isSelected(),
					randomizePackagesSwitch.isSelected(),
					protectStringsSwitch.isSelected(),
					protectNumbersSwitch.isSelected(),
					packageRootSwitch.isSelected(),
					packageRootField.getText().trim(),
					randomizedSLogicSwitch.isSelected(),
					preserveAnnotationsSwitch.isSelected(),
					fabricSwitch.isSelected(),
					(String) seedModeCombo.getSelectedItem(),
					(String) packageDepthCombo.getSelectedItem(),
					(String) slogicTemplateCombo.getSelectedItem()
				);
				ProjectManager.exportToFile(file.toPath(), data);
				log("Project exported successfully to: " + file.getAbsolutePath());
			} catch (Exception ex) {
				log("Failed to export project: " + ex.getMessage());
			}
		}
	}

	private void importProject() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Import Project Configuration");
		chooser.setFileFilter(new FileNameExtensionFilter("Silence Project (*.json)", "json"));

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			try {
				ProjectManager.ProjectData data = ProjectManager.importFromFile(file.toPath());
				inputField.setText(data.input());
				outputField.setText(data.output());
				mainClassField.setText(data.mainClass());
				renameClassesSwitch.setSelected(data.renameClasses());
				renameMethodsSwitch.setSelected(data.renameMethods());
				renameFieldsSwitch.setSelected(data.renameFields());
				randomizePackagesSwitch.setSelected(data.randomizePackages());
				protectStringsSwitch.setSelected(data.protectStrings());
				protectNumbersSwitch.setSelected(data.protectNumbers());
				packageRootSwitch.setSelected(data.packageRootEnabled());
				packageRootField.setText(data.packageRoot());
				randomizedSLogicSwitch.setSelected(data.randomizedSLogic());
				preserveAnnotationsSwitch.setSelected(data.preserveAnnotations());
				fabricSwitch.setSelected(data.updateFabricModJson());
				seedModeCombo.setSelectedItem(data.seedMode());
				packageDepthCombo.setSelectedItem(data.packageDepth());
				slogicTemplateCombo.setSelectedItem(data.slogicTemplate());

				updatePackageRootState();
				saveSettings();
				log("Project imported successfully from: " + file.getAbsolutePath());
			} catch (Exception ex) {
				log("Failed to import project: " + ex.getMessage());
			}
		}
	}

	private JPanel createOutputPanel() {
		ModernCard card = new ModernCard(VectorIcon.Type.DOCUMENT, I18n.get("output_title"), null);
		card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

		logArea.setEditable(false);
		logArea.setOpaque(false);
		logArea.setForeground(UITheme.TEXT_PRIMARY);
		logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
		logArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

		JScrollPane scroll = new JScrollPane(logArea);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);

		JPanel roundedContainer = new JPanel(new BorderLayout()) {
			{ setOpaque(false); }
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(11, 16, 26));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_CARD, UITheme.RADIUS_CARD);
				g2.setColor(UITheme.BORDER_CARD);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_CARD, UITheme.RADIUS_CARD);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		roundedContainer.add(scroll, BorderLayout.CENTER);

		card.add(roundedContainer, BorderLayout.CENTER);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UITheme.BG_MAIN);
		wrapper.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
		wrapper.add(card, BorderLayout.CENTER);
		return wrapper;
	}

	private JPanel createOptionItem(VectorIcon.Type iconType, String title, String subtitle, ToggleSwitch toggle) {
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setOpaque(false);

		JPanel left = new JPanel(new BorderLayout(8, 0));
		left.setOpaque(false);

		VectorIcon icon = new VectorIcon(iconType, 15, UITheme.ACCENT_CYAN);

		JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 1));
		textCol.setOpaque(false);
		JLabel t = new JLabel(title);
		t.setForeground(UITheme.TEXT_PRIMARY);
		t.setFont(UITheme.FONT_BOLD_SUB);
		JLabel s = new JLabel(subtitle);
		s.setForeground(UITheme.TEXT_MUTED);
		s.setFont(UITheme.FONT_SMALL);
		textCol.add(t);
		textCol.add(s);

		left.add(icon, BorderLayout.WEST);
		left.add(textCol, BorderLayout.CENTER);

		JPanel switchWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		switchWrapper.setOpaque(false);
		switchWrapper.setPreferredSize(new Dimension(42, 22));
		switchWrapper.add(toggle);

		panel.add(left, BorderLayout.CENTER);
		panel.add(switchWrapper, BorderLayout.EAST);
		return panel;
	}

	private JPanel createToggleRow(ToggleSwitch toggle, String title, String subtitle) {
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);

		JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 1));
		textCol.setOpaque(false);
		JLabel t = new JLabel(title);
		t.setForeground(UITheme.TEXT_PRIMARY);
		t.setFont(UITheme.FONT_BOLD_SUB);
		JLabel s = new JLabel(subtitle);
		s.setForeground(UITheme.TEXT_MUTED);
		s.setFont(UITheme.FONT_SMALL);
		textCol.add(t);
		textCol.add(s);

		JPanel switchWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		switchWrapper.setOpaque(false);
		switchWrapper.setPreferredSize(new Dimension(42, 22));
		switchWrapper.add(toggle);

		row.add(textCol, BorderLayout.CENTER);
		row.add(switchWrapper, BorderLayout.EAST);
		return row;
	}

	private JPanel createComboRow(String labelText, JComponent combo) {
		JPanel row = new JPanel(new BorderLayout(10, 0));
		row.setOpaque(false);

		JLabel label = new JLabel(labelText);
		label.setForeground(UITheme.TEXT_SECONDARY);
		label.setFont(UITheme.FONT_SUB);

		row.add(label, BorderLayout.WEST);
		row.add(combo, BorderLayout.EAST);
		return row;
	}

	private JLabel createFieldLabel(String text) {
		JLabel label = new JLabel(text);
		label.setForeground(UITheme.TEXT_SECONDARY);
		label.setFont(UITheme.FONT_SUB);
		return label;
	}

	private static JTextField modernField(String placeholder) {
		JTextField field = new JTextField() {
			@Override
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				d.width = 100;
				d.height = 32;
				return d;
			}

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(UITheme.BG_CARD);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.setColor(hasFocus() ? UITheme.BORDER_FOCUS : UITheme.BORDER_CARD);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		field.setOpaque(false);
		field.setBackground(new Color(0, 0, 0, 0));
		field.setForeground(UITheme.TEXT_PRIMARY);
		field.setCaretColor(UITheme.TEXT_PRIMARY);
		field.setFont(UITheme.FONT_SUB);
		field.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
		return field;
	}

	private void refreshUiTexts() {
		brandSubLabel.setText(I18n.get("slogan"));
		pageTitleLabel.setText(I18n.get("header_title"));
		pageDescLabel.setText(I18n.get("header_desc"));
		if (projSubLabel != null) {
			projSubLabel.setText(I18n.get("active_config"));
		}
		if (discordLabel != null) {
			discordLabel.setText(I18n.get("join_discord"));
		}
		if (bannerCard != null) {
			bannerCard.updateTexts();
		}
		for (SidebarButton b : navButtons) {
			b.updateText();
		}
		// Rebuild Project Panel cards
		mainCardContainer.remove(0);
		mainCardContainer.add(createProjectPanel(), "PROJECT", 0);
		cardLayout.show(mainCardContainer, "PROJECT");
		mainCardContainer.revalidate();
		mainCardContainer.repaint();
	}

	private void startObfuscation() {
		String input = inputField.getText().trim();
		String output = outputField.getText().trim();

		if (input.isEmpty() || output.isEmpty()) {
			log("Input and output JAR are required.");
			cardLayout.show(mainCardContainer, "OUTPUT");
			for (SidebarButton b : navButtons) b.setActive(b.getTextKey().equals("nav_output"));
			return;
		}

		String packageRoot = packageRootField.getText().trim();
		if (packageRootSwitch.isSelected() && !isValidPackageRoot(packageRoot)) {
			log("Package root must contain 1-3 valid Java package segments.");
			cardLayout.show(mainCardContainer, "OUTPUT");
			for (SidebarButton b : navButtons) b.setActive(b.getTextKey().equals("nav_output"));
			return;
		}

		saveSettings();
		ObfuscationConfig config = currentConfig();
		protectButton.setEnabled(false);
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);
		logArea.setText("");
		log("Initializing Randomized Protection Engine...");
		log("Input: " + input);
		log("Output: " + output);
		log("Seed Mode: " + config.seedMode() + " | Package Depth: " + config.packageDepth() + " | SLogic Template: " + config.slogicTemplate());
		log("Generating polymorphic SLogic logic & cryptographic rules...");

		new SwingWorker<Void, Void>() {
			private Exception error;

			@Override
			protected Void doInBackground() {
				try {
					new JarObfuscator().obfuscate(Path.of(input), Path.of(output), config);
				} catch (Exception e) {
					error = e;
				}
				return null;
			}

			@Override
			protected void done() {
				protectButton.setEnabled(true);
				progressBar.setIndeterminate(false);
				progressBar.setVisible(false);
				if (error == null) {
					log("Protection completed successfully!");
					log("Output written to: " + output);
				} else {
					log("Failed: " + error.getMessage());
					error.printStackTrace();
				}
				cardLayout.show(mainCardContainer, "OUTPUT");
				for (SidebarButton b : navButtons) b.setActive(b.getTextKey().equals("nav_output"));
			}
		}.execute();
	}

	private ObfuscationConfig currentConfig() {
		return new ObfuscationConfig(
			mainClassField.getText(),
			renameClassesSwitch.isSelected(),
			renameMethodsSwitch.isSelected(),
			renameFieldsSwitch.isSelected(),
			randomizePackagesSwitch.isSelected(),
			protectStringsSwitch.isSelected(),
			protectNumbersSwitch.isSelected(),
			packageRootSwitch.isSelected(),
			packageRootField.getText(),
			randomizedSLogicSwitch.isSelected(),
			preserveAnnotationsSwitch.isSelected(),
			fabricSwitch.isSelected(),
			(String) seedModeCombo.getSelectedItem(),
			(String) packageDepthCombo.getSelectedItem(),
			(String) slogicTemplateCombo.getSelectedItem()
		);
	}

	private void loadSettings() {
		inputField.setText(PREFS.get("input", ""));
		outputField.setText(PREFS.get("output", ""));
		mainClassField.setText(PREFS.get("mainClass", ""));
		renameClassesSwitch.setSelected(PREFS.getBoolean("renameClasses", true));
		renameMethodsSwitch.setSelected(PREFS.getBoolean("renameMethods", true));
		renameFieldsSwitch.setSelected(PREFS.getBoolean("renameFields", true));
		randomizePackagesSwitch.setSelected(PREFS.getBoolean("randomizePackages", true));
		protectStringsSwitch.setSelected(PREFS.getBoolean("protectStrings", true));
		protectNumbersSwitch.setSelected(PREFS.getBoolean("protectNumbers", true));
		packageRootSwitch.setSelected(PREFS.getBoolean("packageRootEnabled", true));
		packageRootField.setText(PREFS.get("packageRoot", "silence"));
		fabricSwitch.setSelected(PREFS.getBoolean("updateFabricModJson", true));
		randomizedSLogicSwitch.setSelected(PREFS.getBoolean("randomizedSLogic", true));
		preserveAnnotationsSwitch.setSelected(PREFS.getBoolean("preserveAnnotations", true));
		seedModeCombo.setSelectedItem(PREFS.get("seedMode", "Random"));
		packageDepthCombo.setSelectedItem(PREFS.get("packageDepth", "1 - 3"));
		slogicTemplateCombo.setSelectedItem(PREFS.get("slogicTemplate", "Dynamic"));
	}

	private void saveSettings() {
		PREFS.put("input", inputField.getText().trim());
		PREFS.put("output", outputField.getText().trim());
		PREFS.put("mainClass", mainClassField.getText().trim());
		PREFS.putBoolean("renameClasses", renameClassesSwitch.isSelected());
		PREFS.putBoolean("renameMethods", renameMethodsSwitch.isSelected());
		PREFS.putBoolean("renameFields", renameFieldsSwitch.isSelected());
		PREFS.putBoolean("randomizePackages", randomizePackagesSwitch.isSelected());
		PREFS.putBoolean("protectStrings", protectStringsSwitch.isSelected());
		PREFS.putBoolean("protectNumbers", protectNumbersSwitch.isSelected());
		PREFS.putBoolean("packageRootEnabled", packageRootSwitch.isSelected());
		PREFS.put("packageRoot", packageRootField.getText().trim());
		PREFS.putBoolean("updateFabricModJson", fabricSwitch.isSelected());
		PREFS.putBoolean("randomizedSLogic", randomizedSLogicSwitch.isSelected());
		PREFS.putBoolean("preserveAnnotations", preserveAnnotationsSwitch.isSelected());
		PREFS.put("seedMode", (String) seedModeCombo.getSelectedItem());
		PREFS.put("packageDepth", (String) packageDepthCombo.getSelectedItem());
		PREFS.put("slogicTemplate", (String) slogicTemplateCombo.getSelectedItem());
	}

	private void updatePackageRootState() {
		boolean enabled = randomizePackagesSwitch.isSelected();
		packageRootSwitch.setEnabled(enabled);
		packageRootField.setEnabled(enabled && packageRootSwitch.isSelected());
	}

	private boolean isValidPackageRoot(String value) {
		if (value.isBlank()) {
			return false;
		}
		String normalized = value.replace('/', '.');
		String[] parts = normalized.split("\\.");
		if (parts.length < 1 || parts.length > 3) {
			return false;
		}
		for (String part : parts) {
			if (!part.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
				return false;
			}
		}
		return true;
	}

	private void log(String text) {
		logArea.append(text + System.lineSeparator());
	}

	private void chooseInput() {
		JFileChooser chooser = jarChooser(inputField.getText());
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			inputField.setText(chooser.getSelectedFile().getAbsolutePath());
			if (outputField.getText().isBlank()) {
				String path = chooser.getSelectedFile().getAbsolutePath();
				if (path.endsWith(".jar")) {
					outputField.setText(path.substring(0, path.length() - 4) + "-protected.jar");
				}
			}
		}
	}

	private void chooseOutput() {
		JFileChooser chooser = jarChooser(outputField.getText().isBlank() ? inputField.getText() : outputField.getText());
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String path = chooser.getSelectedFile().getAbsolutePath();
			if (!path.endsWith(".jar")) {
				path += ".jar";
			}
			outputField.setText(path);
		}
	}

	private JFileChooser jarChooser(String currentPath) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Java Archive (*.jar)", "jar"));
		if (currentPath != null && !currentPath.isBlank()) {
			try {
				Path current = Path.of(currentPath);
				Path directory = current.toFile().isDirectory() ? current : current.getParent();
				if (directory != null) {
					chooser.setCurrentDirectory(directory.toFile());
				}
			} catch (Exception ignored) {
			}
		}
		return chooser;
	}

	// Custom Components
	private static final class SidebarButton extends JButton {
		private boolean active;
		private final VectorIcon icon;
		private final String textKey;
		private final JLabel textLabel;

		SidebarButton(VectorIcon.Type iconType, String textKey, boolean active) {
			this.active = active;
			this.textKey = textKey;
			this.icon = new VectorIcon(iconType, 15, active ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);

			setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
			add(icon);

			this.textLabel = new JLabel(I18n.get(textKey));
			textLabel.setFont(UITheme.FONT_SUB);
			textLabel.setForeground(active ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);
			add(textLabel);

			setText("");
			setFocusPainted(false);
			setContentAreaFilled(false);
			setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		String getTextKey() {
			return textKey;
		}

		void updateText() {
			textLabel.setText(I18n.get(textKey));
		}

		void setActive(boolean active) {
			this.active = active;
			icon.setColor(active ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);
			textLabel.setForeground(active ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (active) {
				g2.setColor(new Color(30, 58, 138, 130));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
				g2.setColor(UITheme.ACCENT_BLUE);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
			} else if (getModel().isRollover()) {
				g2.setColor(new Color(255, 255, 255, 12));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_INPUT, UITheme.RADIUS_INPUT);
			}

			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static final class OutlineButton extends JButton {
		OutlineButton(VectorIcon.Type iconType, String text) {
			setFocusPainted(false);
			setContentAreaFilled(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
			setLayout(new FlowLayout(FlowLayout.CENTER, 6, 0));

			if (iconType != null) {
				add(new VectorIcon(iconType, 13, UITheme.TEXT_PRIMARY));
			}
			JLabel label = new JLabel(text);
			label.setFont(UITheme.FONT_BUTTON);
			label.setForeground(UITheme.TEXT_PRIMARY);
			add(label);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Color bg = getModel().isRollover() ? UITheme.BTN_HOVER : UITheme.BTN_BG;
			g2.setColor(bg);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_BTN, UITheme.RADIUS_BTN);

			g2.setColor(UITheme.BTN_BORDER);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_BTN, UITheme.RADIUS_BTN);

			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static final class ProtectButton extends JButton {
		private final VectorIcon shieldIcon = new VectorIcon(VectorIcon.Type.SHIELD, 16, Color.WHITE);

		ProtectButton() {
			setFocusPainted(false);
			setContentAreaFilled(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setPreferredSize(new Dimension(145, 38));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int w = getWidth();
			int h = getHeight();

			Color c1 = getModel().isRollover() ? UITheme.ACCENT_BLUE_LIGHT : UITheme.ACCENT_BLUE;
			Color c2 = UITheme.ACCENT_BLUE_DARK;
			g2.setPaint(new java.awt.GradientPaint(0, 0, c1, w, h, c2));
			g2.fillRoundRect(0, 0, w, h, UITheme.RADIUS_BTN, UITheme.RADIUS_BTN);

			g2.setColor(UITheme.ACCENT_CYAN);
			g2.drawRoundRect(0, 0, w - 1, h - 1, UITheme.RADIUS_BTN, UITheme.RADIUS_BTN);

			String text = I18n.get("btn_protect");
			Font font = UITheme.FONT_BUTTON;
			g2.setFont(font);
			FontMetrics fm = g2.getFontMetrics(font);

			int textWidth = fm.stringWidth(text);
			int iconSize = 16;
			int gap = 8;
			int totalWidth = iconSize + gap + textWidth;

			int startX = (w - totalWidth) / 2;
			int iconY = (h - iconSize) / 2;
			int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();

			shieldIcon.paintIcon(this, g2, startX, iconY);
			g2.setColor(Color.WHITE);
			g2.drawString(text, startX + iconSize + gap, textY);

			g2.dispose();
		}
	}

	private static final class LangSwitchButton extends JButton {
		LangSwitchButton() {
			setFocusPainted(false);
			setContentAreaFilled(false);
			setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setText(I18n.current() == I18n.Lang.EN ? "EN" : "KO");
			setFont(UITheme.createFont(Font.BOLD, 11));
			setForeground(UITheme.ACCENT_CYAN);

			addActionListener(e -> {
				I18n.setLang(I18n.current() == I18n.Lang.EN ? I18n.Lang.KO : I18n.Lang.EN);
				setText(I18n.current() == I18n.Lang.EN ? "EN" : "KO");
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(new Color(255, 255, 255, getModel().isRollover() ? 25 : 12));
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_SMALL, UITheme.RADIUS_SMALL);
			g2.setColor(UITheme.BORDER_CARD);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, UITheme.RADIUS_SMALL, UITheme.RADIUS_SMALL);
			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static final class WindowControlButton extends JButton {
		private final boolean isClose;

		WindowControlButton(VectorIcon.Type type, boolean isClose) {
			this.isClose = isClose;
			setFocusPainted(false);
			setContentAreaFilled(false);
			setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setPreferredSize(new Dimension(32, 24));
			setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
			add(new VectorIcon(type, 12, UITheme.TEXT_MUTED));
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (getModel().isRollover()) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(isClose ? new Color(220, 38, 38) : new Color(255, 255, 255, 20));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
				g2.dispose();
			}
			super.paintComponent(g);
		}
	}

	private static final class BannerCard extends JPanel {
		private final JLabel quoteLabel;
		private final JLabel subLabel;

		BannerCard() {
			setOpaque(false);
			setLayout(new BorderLayout(12, 0));
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(UITheme.BORDER_INFO, 1, true),
				BorderFactory.createEmptyBorder(8, 14, 8, 16)
			));

			JPanel textCol = new JPanel(new GridLayout(2, 1, 0, 1));
			textCol.setOpaque(false);

			quoteLabel = new JLabel(I18n.get("quote"));
			quoteLabel.setForeground(UITheme.TEXT_PRIMARY);
			quoteLabel.setFont(UITheme.FONT_BOLD_SUB);

			subLabel = new JLabel(I18n.get("quote_sub"));
			subLabel.setForeground(UITheme.ACCENT_CYAN);
			subLabel.setFont(UITheme.createFont(Font.BOLD, 10));

			textCol.add(quoteLabel);
			textCol.add(subLabel);

			VectorIcon shieldIcon = new VectorIcon(VectorIcon.Type.SHIELD, 24, UITheme.ACCENT_BLUE_LIGHT);

			add(textCol, BorderLayout.CENTER);
			add(shieldIcon, BorderLayout.EAST);
		}

		void updateTexts() {
			quoteLabel.setText(I18n.get("quote"));
			subLabel.setText(I18n.get("quote_sub"));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(UITheme.BG_INFO_BOX);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.RADIUS_BTN, UITheme.RADIUS_BTN);

			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static final class ScrollableGrid extends JPanel implements Scrollable {
		ScrollableGrid(LayoutManager layout) {
			super(layout);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 32;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
}