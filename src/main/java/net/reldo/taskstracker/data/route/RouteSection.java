package net.reldo.taskstracker.data.route;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * A section within a route containing ordered items (tasks and custom items).
 * Represents a phase or area in a route (e.g., "IMMEDIATE", "VARROCK pt 1", "Draynor").
 */
@Data
public class RouteSection
{
	/**
	 * Section name (e.g., "IMMEDIATE", "VARROCK pt 1", "Draynor")
	 */
	@Expose
	private String name;

	/**
	 * Optional description with timing info (e.g., "10-15 minutes")
	 */
	@Expose
	private String description;

	/**
	 * Legacy field - list of task IDs only.
	 * Used for backwards compatibility during import.
	 */
	@Expose
	private List<Integer> taskIds;

	/**
	 * New field - list of route items (tasks + custom items).
	 * Takes precedence over taskIds if present.
	 */
	@Expose
	private List<RouteItem> items;

	/**
	 * Gets all items in this section.
	 * Handles both legacy taskIds and new items format.
	 */
	public List<RouteItem> getItems()
	{
		if (items != null && !items.isEmpty())
		{
			return items;
		}
		// Convert legacy taskIds to RouteItems
		if (taskIds != null)
		{
			return taskIds.stream()
				.filter(id -> id != null)
				.map(RouteItem::forTask)
				.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	/**
	 * Sets the items list and clears the legacy taskIds.
	 */
	public void setItems(List<RouteItem> items)
	{
		this.items = items;
		// Clear legacy field when using new format
		this.taskIds = null;
	}

	/**
	 * Gets only the task IDs from this section (for filtering/sorting).
	 */
	public List<Integer> getTaskIds()
	{
		if (items != null && !items.isEmpty())
		{
			return items.stream()
				.filter(RouteItem::isTask)
				.map(RouteItem::getTaskId)
				.collect(Collectors.toList());
		}
		if (taskIds != null)
		{
			return taskIds.stream()
				.filter(id -> id != null)
				.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	/**
	 * Gets only the custom items from this section.
	 */
	public List<CustomRouteItem> getCustomItems()
	{
		return getItems().stream()
			.filter(item -> !item.isTask())
			.map(RouteItem::getCustomItem)
			.collect(Collectors.toList());
	}

	public boolean containsTask(int taskId)
	{
		return getTaskIds().contains(taskId);
	}

	public int getTaskCount()
	{
		return getTaskIds().size();
	}

	/**
	 * Inserts a custom item before or after a task.
	 * @param taskId The task ID to insert relative to
	 * @param customType The type of custom item ("bank", "home_teleport", "fairy_ring")
	 * @param insertAfter true to insert after, false to insert before
	 * @return The created custom item, or null if task not found
	 */
	public CustomRouteItem insertCustomItem(int taskId, String customType, boolean insertAfter)
	{
		// Ensure we're using the items list
		List<RouteItem> currentItems = new ArrayList<>(getItems());

		// Find the position of the task
		int position = -1;
		for (int i = 0; i < currentItems.size(); i++)
		{
			RouteItem item = currentItems.get(i);
			if (item.isTask() && item.getTaskId() == taskId)
			{
				position = i;
				break;
			}
		}

		if (position == -1)
		{
			return null;
		}

		// Create the custom item
		CustomRouteItem customItem = CustomRouteItem.create(customType);
		RouteItem newItem = RouteItem.forCustom(customItem);

		// Insert at the appropriate position
		int insertPosition = insertAfter ? position + 1 : position;
		currentItems.add(insertPosition, newItem);

		// Update the items list
		this.items = currentItems;
		this.taskIds = null; // Clear legacy format

		return customItem;
	}

	/**
	 * Removes a custom item by its ID.
	 * @return true if item was found and removed
	 */
	public boolean removeCustomItem(String customItemId)
	{
		if (items == null)
		{
			return false;
		}

		return items.removeIf(item ->
			!item.isTask() &&
			item.getCustomItem() != null &&
			customItemId.equals(item.getCustomItem().getId())
		);
	}
}
