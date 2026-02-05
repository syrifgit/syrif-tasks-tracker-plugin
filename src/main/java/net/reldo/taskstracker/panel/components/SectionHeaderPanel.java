package net.reldo.taskstracker.panel.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.Setter;
import net.reldo.taskstracker.data.route.RouteSection;
import net.runelite.client.ui.ColorScheme;

/**
 * UI component for displaying section headers in route-based task lists.
 * Shows section name, optional description, and progress count.
 * Supports collapsing/expanding sections.
 */
public class SectionHeaderPanel extends JPanel
{
	private static final Color HEADER_BACKGROUND = new Color(60, 63, 65);
	private static final Color HEADER_BACKGROUND_HOVER = new Color(70, 73, 75);
	private static final Color HEADER_TEXT_COLOR = Color.WHITE;
	private static final Color DESCRIPTION_COLOR = new Color(150, 150, 150);
	private static final Color PROGRESS_COLOR = new Color(180, 180, 180);

	@Getter
	private final String sectionName;
	@Getter
	@Setter
	private boolean collapsed = false;
	private final JLabel collapseIcon;
	private final String labelText;
	private Consumer<Boolean> collapseCallback;

	public SectionHeaderPanel(RouteSection section, int completedCount, int totalCount)
	{
		this(section.getName(), section.getDescription(), completedCount, totalCount);
	}

	public SectionHeaderPanel(String sectionName, String description, int completedCount, int totalCount)
	{
		this.sectionName = sectionName;

		setLayout(new BorderLayout());
		setBackground(HEADER_BACKGROUND);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 10, 6, 10)
		));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Build section text with collapse indicator
		String displayName = sectionName != null ? sectionName : "Section";
		this.labelText = description != null && !description.isEmpty()
			? displayName + " - " + description
			: displayName;

		// Left side: collapse indicator + section name (using HTML for the indicator)
		collapseIcon = new JLabel(getCollapseHtml(false));
		collapseIcon.setForeground(HEADER_TEXT_COLOR);
		collapseIcon.setFont(collapseIcon.getFont().deriveFont(Font.BOLD));

		// Right side: progress indicator
		String progressColor = completedCount == totalCount ? "#00ff00" : "#b4b4b4";
		JLabel progressLabel = new JLabel("<html><font color='" + progressColor + "'>" + completedCount + "/" + totalCount + "</font></html>");

		add(collapseIcon, BorderLayout.CENTER);
		add(progressLabel, BorderLayout.EAST);

		// Add mouse listener for hover effect and click handling
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggleCollapsed();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(HEADER_BACKGROUND_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(HEADER_BACKGROUND);
			}
		});
	}

	/**
	 * Simple section header with just a name (no description).
	 */
	public SectionHeaderPanel(String sectionName, int completedCount, int totalCount)
	{
		this(sectionName, null, completedCount, totalCount);
	}

	/**
	 * Sets the callback to be called when the collapsed state changes.
	 */
	public void setCollapseCallback(Consumer<Boolean> callback)
	{
		this.collapseCallback = callback;
	}

	/**
	 * Toggles the collapsed state and updates the UI.
	 */
	public void toggleCollapsed()
	{
		collapsed = !collapsed;
		updateCollapseIcon();
		if (collapseCallback != null)
		{
			collapseCallback.accept(collapsed);
		}
	}

	/**
	 * Sets the collapsed state without triggering the callback.
	 */
	public void setCollapsedState(boolean collapsed)
	{
		this.collapsed = collapsed;
		updateCollapseIcon();
	}

	private void updateCollapseIcon()
	{
		collapseIcon.setText(getCollapseHtml(collapsed));
	}

	private String getCollapseHtml(boolean isCollapsed)
	{
		String arrow = isCollapsed ? "▶" : "▼";
		return "<html>" + arrow + " " + labelText + "</html>";
	}
}
