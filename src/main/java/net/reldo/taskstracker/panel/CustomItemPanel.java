package net.reldo.taskstracker.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.data.route.CustomRouteItem;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

/**
 * UI panel for displaying a custom route item (bank, home teleport, fairy ring).
 * Similar to TaskPanel but with a checkmark button for completion.
 */
public class CustomItemPanel extends JPanel
{
	private static final Color CUSTOM_ITEM_BACKGROUND = new Color(50, 55, 60);
	private static final Color CUSTOM_ITEM_BACKGROUND_HOVER = new Color(60, 65, 70);
	private static final Color CUSTOM_ITEM_COMPLETED = new Color(40, 60, 40);
	private static final Color CUSTOM_ITEM_BORDER_COLOR = new Color(100, 150, 200);

	@Getter
	private final CustomRouteItem item;
	private final TasksTrackerPlugin plugin;

	private final JPanel container = new JPanel(new BorderLayout());
	private final JPanel body = new JPanel(new BorderLayout());
	private final JLabel iconLabel = new JLabel();
	private final JLabel nameLabel = new JLabel();
	private final JPanel buttons = new JPanel();
	private final JToggleButton toggleComplete = new JToggleButton();

	@Getter
	private boolean completed = false;

	private Consumer<CustomItemPanel> onCompletionChanged;
	private Consumer<CustomItemPanel> onRemove;

	public CustomItemPanel(TasksTrackerPlugin plugin, CustomRouteItem item, boolean isCompleted)
	{
		super(new BorderLayout());
		this.plugin = plugin;
		this.item = item;
		this.completed = isCompleted;

		createPanel();
		refresh();
	}

	private void createPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 7, 0));

		container.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CUSTOM_ITEM_BORDER_COLOR),
			new EmptyBorder(7, 7, 6, 0)
		));

		// Icon based on type
		iconLabel.setIcon(getIconForType(item.getType()));
		iconLabel.setBorder(new EmptyBorder(0, 0, 0, 5));

		// Name label
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(Color.WHITE);
		body.add(nameLabel, BorderLayout.CENTER);

		// Buttons panel
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
		buttons.setBorder(new EmptyBorder(0, 0, 0, 7));

		// Checkmark toggle button
		toggleComplete.setPreferredSize(new Dimension(8, 8));
		toggleComplete.setIcon(Icons.INCOMPLETE_ONLY_ICON);
		toggleComplete.setSelectedIcon(Icons.COMPLETE_ONLY_ICON);
		toggleComplete.setBorder(new EmptyBorder(5, 0, 5, 0));
		toggleComplete.setToolTipText("Mark as completed");
		toggleComplete.addActionListener(e -> {
			setCompleted(toggleComplete.isSelected());
			if (onCompletionChanged != null)
			{
				onCompletionChanged.accept(this);
			}
		});
		SwingUtil.removeButtonDecorations(toggleComplete);

		buttons.add(toggleComplete);

		// Assemble
		container.add(iconLabel, BorderLayout.WEST);
		container.add(body, BorderLayout.CENTER);
		container.add(buttons, BorderLayout.EAST);

		add(container, BorderLayout.NORTH);

		// Right-click menu for removing
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showPopupMenu(e);
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showPopupMenu(e);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!completed)
				{
					setBackgroundColor(CUSTOM_ITEM_BACKGROUND_HOVER);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackgroundColor(completed ? CUSTOM_ITEM_COMPLETED : CUSTOM_ITEM_BACKGROUND);
			}
		});
	}

	private void showPopupMenu(MouseEvent e)
	{
		JPopupMenu popup = new JPopupMenu();

		JMenuItem removeItem = new JMenuItem("Remove");
		removeItem.addActionListener(ev -> {
			if (onRemove != null)
			{
				onRemove.accept(this);
			}
		});
		popup.add(removeItem);

		popup.show(e.getComponent(), e.getX(), e.getY());
	}

	public void refresh()
	{
		String displayName = item.getDisplayName();
		if (completed)
		{
			nameLabel.setText("<html><s>" + displayName + "</s></html>");
			setBackgroundColor(CUSTOM_ITEM_COMPLETED);
		}
		else
		{
			nameLabel.setText(displayName);
			setBackgroundColor(CUSTOM_ITEM_BACKGROUND);
		}
		toggleComplete.setSelected(completed);
		revalidate();
	}

	public void setCompleted(boolean completed)
	{
		this.completed = completed;
		refresh();
	}

	private void setBackgroundColor(Color color)
	{
		container.setBackground(color);
		body.setBackground(color);
		buttons.setBackground(color);
	}

	public void setOnCompletionChanged(Consumer<CustomItemPanel> callback)
	{
		this.onCompletionChanged = callback;
	}

	public void setOnRemove(Consumer<CustomItemPanel> callback)
	{
		this.onRemove = callback;
	}

	private javax.swing.Icon getIconForType(String type)
	{
		if (type == null)
		{
			return null;
		}
		switch (type)
		{
			case "bank":
				return Icons.BANK_ICON;
			case "home_teleport":
				return Icons.HOME_TELEPORT_ICON;
			case "fairy_ring":
				return Icons.FAIRY_RING_ICON;
			default:
				return null;
		}
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH, getPreferredSize().height);
	}
}
