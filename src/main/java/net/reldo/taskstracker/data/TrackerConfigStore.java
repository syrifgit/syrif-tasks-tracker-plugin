package net.reldo.taskstracker.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.config.ConfigValues;
import net.reldo.taskstracker.data.route.CustomRoute;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.ConfigTaskSave;
import net.reldo.taskstracker.data.task.TaskService;
import net.reldo.taskstracker.data.task.TaskType;
import net.runelite.client.config.ConfigManager;

@Singleton
@Slf4j
public class TrackerConfigStore
{
	public static final String CONFIG_TASKS_PREFIX = "tasks";
	public static final String CONFIG_GROUP_PREFIX_SEPARATOR = "-";
	public static final String CONFIG_GROUP_NAME = TasksTrackerPlugin.CONFIG_GROUP_NAME;

	private final Gson customGson;
	@Inject
	private TaskService taskService;
	@Inject
	private ConfigManager configManager;

	@Inject
	public TrackerConfigStore(Gson gson)
	{
		this.customGson = gson.newBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(float.class, new LongSerializer())
			.create();
	}

	public void loadCurrentTaskTypeFromConfig()
	{
		TaskType currentTaskType = taskService.getCurrentTaskType();
		if (currentTaskType == null)
		{
			log.debug("loadTaskTypeFromConfig type is null, skipping");
			return;
		}
		log.debug("loadTaskTypeFromConfig {}", currentTaskType.getName());
		String configKey = getCurrentTaskTypeConfigKey();
		String configJson = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, configKey);
		if (configJson == null)
		{
			log.debug("No save information for task type {}, not applying save", currentTaskType.getName());
			return;
		}

		Type deserializeType = TypeToken.getParameterized(HashMap.class, Integer.class, ConfigTaskSave.class).getType();
		try
		{
			HashMap<Integer, ConfigTaskSave> saveData = customGson.fromJson(configJson, deserializeType);
			taskService.applySave(currentTaskType, saveData);
		}
		catch (JsonParseException ex)
		{
			log.error("{} {} json invalid. wiping saved data", CONFIG_GROUP_NAME, configKey, ex);
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP_NAME, configKey);
		}
	}

	public void saveCurrentTaskTypeData()
	{
		log.debug("saveTaskTypeToConfig");
		Map<Integer, ConfigTaskSave> saveDataByStructId = taskService.getTasks().stream()
			.filter(task -> task.getCompletedOn() != 0 || task.getIgnoredOn() != 0 || task.getTrackedOn() != 0)
			.collect(Collectors.toMap(
				TaskFromStruct::getStructId,
				TaskFromStruct::getSaveData,
				(existing, replacement) -> existing,
				HashMap::new
			));

		String configValue = this.customGson.toJson(saveDataByStructId);
		String configKey = CONFIG_TASKS_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + taskService.getCurrentTaskType().getTaskJsonName();
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, configKey, configValue);
	}

	private String getCurrentTaskTypeConfigKey()
	{
		return CONFIG_TASKS_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + taskService.getCurrentTaskType().getTaskJsonName();
	}

	// --- Route Management ---

	private static final String CONFIG_ROUTES_PREFIX = "routes";
	private static final String CONFIG_ACTIVE_ROUTE_PREFIX = "activeRoute";
	private static final String CONFIG_TAG_FILTER_PREFIX = "tagFilter";

	public void saveTabRoutes(ConfigValues.TaskListTabs tab, String taskType, List<CustomRoute> routes)
	{
		String key = CONFIG_ROUTES_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + tab.configID + CONFIG_GROUP_PREFIX_SEPARATOR + taskType;
		String json = routes != null && !routes.isEmpty() ? customGson.toJson(routes) : "";
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, key, json);
	}

	public List<CustomRoute> loadTabRoutes(ConfigValues.TaskListTabs tab, String taskType)
	{
		String key = CONFIG_ROUTES_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + tab.configID + CONFIG_GROUP_PREFIX_SEPARATOR + taskType;
		String json = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, key);
		if (json == null || json.isEmpty())
		{
			return new ArrayList<>();
		}
		try
		{
			Type listType = new TypeToken<List<CustomRoute>>(){}.getType();
			List<CustomRoute> routes = customGson.fromJson(json, listType);
			if (routes == null)
			{
				return new ArrayList<>();
			}
			// Filter out invalid routes (null or empty names from before @Expose fix)
			return routes.stream()
				.filter(r -> r != null && r.getName() != null && !r.getName().isEmpty())
				.collect(Collectors.toCollection(ArrayList::new));
		}
		catch (JsonParseException ex)
		{
			log.error("Failed to parse routes for tab {} taskType {}: {}", tab.configID, taskType, ex.getMessage());
			return new ArrayList<>();
		}
	}

	public void saveActiveRouteName(ConfigValues.TaskListTabs tab, String taskType, String routeName)
	{
		String key = CONFIG_ACTIVE_ROUTE_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + tab.configID + CONFIG_GROUP_PREFIX_SEPARATOR + taskType;
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, key, routeName != null ? routeName : "");
	}

	public String loadActiveRouteName(ConfigValues.TaskListTabs tab, String taskType)
	{
		String key = CONFIG_ACTIVE_ROUTE_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + tab.configID + CONFIG_GROUP_PREFIX_SEPARATOR + taskType;
		String value = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, key);
		return value != null && !value.isEmpty() ? value : null;
	}

	public CustomRoute getActiveRoute(ConfigValues.TaskListTabs tab, String taskType)
	{
		String activeName = loadActiveRouteName(tab, taskType);
		if (activeName == null || activeName.isEmpty())
		{
			return null;
		}

		List<CustomRoute> routes = loadTabRoutes(tab, taskType);
		return routes.stream()
			.filter(r -> activeName.equals(r.getName()))
			.findFirst()
			.orElse(null);
	}

	public void addRouteToTab(ConfigValues.TaskListTabs tab, String taskType, CustomRoute route)
	{
		List<CustomRoute> routes = new ArrayList<>(loadTabRoutes(tab, taskType));

		// Replace if same name exists, otherwise add (null-safe comparison)
		String newName = route.getName();
		routes.removeIf(r -> r.getName() != null && r.getName().equals(newName));
		routes.add(route);

		saveTabRoutes(tab, taskType, routes);
	}

	public void removeRouteFromTab(ConfigValues.TaskListTabs tab, String taskType, String routeName)
	{
		List<CustomRoute> routes = new ArrayList<>(loadTabRoutes(tab, taskType));
		routes.removeIf(r -> r.getName() != null && r.getName().equals(routeName));
		saveTabRoutes(tab, taskType, routes);

		// Clear active if it was the removed route
		String active = loadActiveRouteName(tab, taskType);
		if (routeName.equals(active))
		{
			saveActiveRouteName(tab, taskType, routes.isEmpty() ? null : routes.get(0).getName());
		}
	}

	// --- Tag Filters ---

	public void saveTabTagFilter(ConfigValues.TaskListTabs tab, Set<String> tags)
	{
		String key = CONFIG_TAG_FILTER_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + tab.configID;
		String value = tags != null && !tags.isEmpty() ? String.join(",", tags) : "";
		configManager.setConfiguration(CONFIG_GROUP_NAME, key, value);
	}

	public Set<String> loadTabTagFilter(ConfigValues.TaskListTabs tab)
	{
		String key = CONFIG_TAG_FILTER_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR + tab.configID;
		String value = configManager.getConfiguration(CONFIG_GROUP_NAME, key);
		if (value == null || value.isEmpty())
		{
			return new HashSet<>();
		}
		return new HashSet<>(Arrays.asList(value.split(",")));
	}

	// --- Custom Item Completion ---

	private static final String CONFIG_CUSTOM_COMPLETION_PREFIX = "customCompletion";

	/**
	 * Saves the completion state of custom items for a specific route.
	 */
	public void saveCustomItemCompletion(ConfigValues.TaskListTabs tab, String taskType, String routeName, Set<String> completedIds)
	{
		String key = CONFIG_CUSTOM_COMPLETION_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR +
			tab.configID + CONFIG_GROUP_PREFIX_SEPARATOR +
			taskType + CONFIG_GROUP_PREFIX_SEPARATOR +
			routeName;
		String value = completedIds != null && !completedIds.isEmpty() ? String.join(",", completedIds) : "";
		configManager.setRSProfileConfiguration(CONFIG_GROUP_NAME, key, value);
	}

	/**
	 * Loads the completion state of custom items for a specific route.
	 */
	public Set<String> loadCustomItemCompletion(ConfigValues.TaskListTabs tab, String taskType, String routeName)
	{
		String key = CONFIG_CUSTOM_COMPLETION_PREFIX + CONFIG_GROUP_PREFIX_SEPARATOR +
			tab.configID + CONFIG_GROUP_PREFIX_SEPARATOR +
			taskType + CONFIG_GROUP_PREFIX_SEPARATOR +
			routeName;
		String value = configManager.getRSProfileConfiguration(CONFIG_GROUP_NAME, key);
		if (value == null || value.isEmpty())
		{
			return new HashSet<>();
		}
		return new HashSet<>(Arrays.asList(value.split(",")));
	}
}
