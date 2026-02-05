package net.reldo.taskstracker.data.route;

import com.google.gson.annotations.Expose;
import lombok.Data;

/**
 * A custom placeholder item in a route (bank, home teleport, fairy ring, etc.).
 * Can appear multiple times in a route.
 */
@Data
public class CustomRouteItem
{
	/**
	 * Unique identifier for this specific occurrence.
	 * Auto-generated on creation.
	 */
	@Expose
	private String id;

	/**
	 * Item type: "bank", "home_teleport", "fairy_ring"
	 */
	@Expose
	private String type;

	/**
	 * Gets a display name based on the type.
	 */
	public String getDisplayName()
	{
		if (type == null)
		{
			return "Custom";
		}
		switch (type)
		{
			case "bank":
				return "Bank";
			case "home_teleport":
				return "Home Teleport";
			case "fairy_ring":
				return "Fairy Ring";
			default:
				return type.substring(0, 1).toUpperCase() + type.substring(1).replace("_", " ");
		}
	}

	/**
	 * Creates a new custom item with a generated ID.
	 */
	public static CustomRouteItem create(String type)
	{
		CustomRouteItem item = new CustomRouteItem();
		item.setId(java.util.UUID.randomUUID().toString().substring(0, 8));
		item.setType(type);
		return item;
	}
}
