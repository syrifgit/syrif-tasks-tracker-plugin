package net.reldo.taskstracker.data.route;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * A custom route containing ordered sections with task IDs.
 * Routes are used to define a specific order for completing tasks,
 * typically shared by content creators for leagues routing.
 */
@Data
public class CustomRoute
{
	/**
	 * Route name (e.g., "Syrif's Route v1.0")
	 */
	@Expose
	private String name;

	/**
	 * Task type this route is for (e.g., "LEAGUE_5")
	 */
	@Expose
	private String taskType;

	/**
	 * Optional author name
	 */
	@Expose
	private String author;

	/**
	 * Optional route description
	 */
	@Expose
	private String description;

	/**
	 * Ordered list of sections in this route
	 */
	@Expose
	private List<RouteSection> sections;

	/**
	 * Gets a flattened list of all task IDs in route order.
	 * Useful for sorting tasks by route position.
	 */
	public List<Integer> getFlattenedOrder()
	{
		if (sections == null)
		{
			return new ArrayList<>();
		}
		return sections.stream()
			.filter(s -> s.getTaskIds() != null)
			.flatMap(s -> s.getTaskIds().stream())
			.collect(Collectors.toList());
	}

	/**
	 * Finds the section containing a specific task.
	 */
	public RouteSection getSectionForTask(int taskId)
	{
		if (sections == null)
		{
			return null;
		}
		return sections.stream()
			.filter(s -> s.containsTask(taskId))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Gets the total number of tasks across all sections.
	 */
	public int getTaskCount()
	{
		return getFlattenedOrder().size();
	}

	/**
	 * Checks if a task is the first task in its section.
	 * Used to determine when to show section headers.
	 */
	public boolean isFirstTaskInSection(int taskId)
	{
		if (sections == null)
		{
			return false;
		}
		for (RouteSection section : sections)
		{
			if (section.getTaskIds() != null && !section.getTaskIds().isEmpty())
			{
				if (section.getTaskIds().get(0).equals(taskId))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Gets a flattened list of all items (tasks + custom items) in route order.
	 */
	public List<RouteItem> getFlattenedItems()
	{
		if (sections == null)
		{
			return new ArrayList<>();
		}
		return sections.stream()
			.flatMap(s -> s.getItems().stream())
			.collect(Collectors.toList());
	}

	/**
	 * Inserts a custom item relative to a task.
	 * @param taskId The task ID to insert relative to
	 * @param customType The type of custom item
	 * @param insertAfter true to insert after, false to insert before
	 * @return The created custom item, or null if task not found
	 */
	public CustomRouteItem insertCustomItem(int taskId, String customType, boolean insertAfter)
	{
		RouteSection section = getSectionForTask(taskId);
		if (section == null)
		{
			return null;
		}
		return section.insertCustomItem(taskId, customType, insertAfter);
	}

	/**
	 * Finds a custom item by its ID across all sections.
	 */
	public CustomRouteItem findCustomItem(String customItemId)
	{
		if (sections == null)
		{
			return null;
		}
		for (RouteSection section : sections)
		{
			for (CustomRouteItem item : section.getCustomItems())
			{
				if (customItemId.equals(item.getId()))
				{
					return item;
				}
			}
		}
		return null;
	}

	/**
	 * Removes a custom item by its ID from any section.
	 * @return true if the item was found and removed
	 */
	public boolean removeCustomItem(String customItemId)
	{
		if (sections == null)
		{
			return false;
		}
		for (RouteSection section : sections)
		{
			if (section.removeCustomItem(customItemId))
			{
				return true;
			}
		}
		return false;
	}
}
