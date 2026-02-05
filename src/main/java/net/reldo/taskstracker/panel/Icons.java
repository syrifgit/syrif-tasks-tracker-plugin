package net.reldo.taskstracker.panel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.runelite.client.util.ImageUtil;

public class Icons
{
	// Custom route item icons (created programmatically)
	public static final Icon BANK_ICON = createTextIcon("B", new Color(255, 215, 0)); // Gold
	public static final Icon HOME_TELEPORT_ICON = createTextIcon("H", new Color(100, 200, 255)); // Light blue
	public static final Icon FAIRY_RING_ICON = createTextIcon("F", new Color(150, 255, 150)); // Light green

	private static final String completeBtnPath = "panel/components/complete_button/";
	public static final Icon INCOMPLETE_ONLY_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, completeBtnPath + "incomplete_only_icon.png"));
	public static final Icon COMPLETE_ONLY_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, completeBtnPath + "complete_only_icon.png"));
	public static final Icon COMPLETE_INCOMPLETE_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, completeBtnPath + "complete_and_incomplete_icon.png"));

	private static final String ignoredBtnPath = "panel/components/ignored_button/";
	public static final BufferedImage semivisibleimg = ImageUtil.loadImageResource(TasksTrackerPlugin.class, ignoredBtnPath + "semivisible_icon.png");
	public static final Icon UNIGNORED_ONLY_ICON = new ImageIcon(ImageUtil.alphaOffset(semivisibleimg, -180));
	public static final Icon IGNORED_ONLY_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, ignoredBtnPath + "invisible_icon.png"));
	public static final Icon IGNORED_UNIGNORED_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, ignoredBtnPath + "visible_icon.png"));

	private static final String trackedBtnPath = "panel/components/tracked_button/";
	public static final Icon UNTRACKED_ONLY_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, trackedBtnPath + "untracked_icon.png"));
	public static final Icon TRACKED_ONLY_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, trackedBtnPath + "tracked_icon.png"));
	public static final Icon TRACKED_UNTRACKED_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, trackedBtnPath + "tracked_and_untracked_icon.png"));

	private static final String sortBtnPath = "panel/components/sort_button/";
	public static final Icon ASCENDING_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, sortBtnPath + "ascending_icon.png"));
	public static final Icon DESCENDING_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, sortBtnPath + "descending_icon.png"));

	private static final String expandBtnPath = "panel/components/";
	public static final Icon MENU_EXPANDED_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, expandBtnPath + "filter_menu_expanded.png"));
	public static final BufferedImage collapseImg = ImageUtil.loadImageResource(TasksTrackerPlugin.class, expandBtnPath + "filter_menu_collapsed.png");
	public static final Icon MENU_ICON_HOVER = new ImageIcon(collapseImg);

	public static final Icon MENU_COLLAPSED_ICON = new ImageIcon(ImageUtil.alphaOffset(collapseImg, -180));
	public static final ImageIcon PLUS_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, "plus.png"));
	public static final ImageIcon MINUS_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, "minus.png"));
	public static final ImageIcon EYE_ICON = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, "eye.png"));
	public static final ImageIcon EYE_CROSS_GREY = new ImageIcon(ImageUtil.loadImageResource(TasksTrackerPlugin.class, "eye-cross-grey.png"));

	/**
	 * Creates a simple icon with a letter and background color.
	 */
	private static Icon createTextIcon(String text, Color color)
	{
		int size = 16;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Draw rounded background
		g2d.setColor(color);
		g2d.fillRoundRect(0, 0, size, size, 4, 4);

		// Draw border
		g2d.setColor(color.darker());
		g2d.drawRoundRect(0, 0, size - 1, size - 1, 4, 4);

		// Draw text
		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
		int textWidth = g2d.getFontMetrics().stringWidth(text);
		int textHeight = g2d.getFontMetrics().getAscent();
		g2d.drawString(text, (size - textWidth) / 2, (size + textHeight) / 2 - 2);

		g2d.dispose();
		return new ImageIcon(image);
	}
}
