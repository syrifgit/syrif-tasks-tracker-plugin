package net.reldo.taskstracker.data.route;

import com.google.gson.annotations.Expose;
import lombok.Data;

/**
 * Wrapper for items in a route section.
 * Can represent either a task (by ID) or a custom placeholder item.
 */
@Data
public class RouteItem
{
	/**
	 * Task ID if this is a task reference. Null for custom items.
	 */
	@Expose
	private Integer taskId;

	/**
	 * Custom item data if this is a custom item. Null for tasks.
	 */
	@Expose
	private CustomRouteItem customItem;

	/**
	 * Whether this item represents a real task (vs a custom placeholder).
	 */
	public boolean isTask()
	{
		return taskId != null;
	}

	/**
	 * Creates a RouteItem wrapping a task ID.
	 */
	public static RouteItem forTask(int taskId)
	{
		RouteItem item = new RouteItem();
		item.setTaskId(taskId);
		return item;
	}

	/**
	 * Creates a RouteItem wrapping a custom item.
	 */
	public static RouteItem forCustom(CustomRouteItem customItem)
	{
		RouteItem item = new RouteItem();
		item.setCustomItem(customItem);
		return item;
	}

	/**
	 * Creates a RouteItem for a new custom item of the given type.
	 */
	public static RouteItem forCustomType(String type)
	{
		return forCustom(CustomRouteItem.create(type));
	}

	/**
	 * Gets a unique identifier for this item.
	 */
	public String getItemId()
	{
		if (isTask())
		{
			return "task:" + taskId;
		}
		return "custom:" + (customItem != null ? customItem.getId() : "null");
	}
}
