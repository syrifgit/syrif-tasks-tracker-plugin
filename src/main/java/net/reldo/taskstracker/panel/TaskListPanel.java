package net.reldo.taskstracker.panel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.config.ConfigValues;
import net.reldo.taskstracker.data.jsondatastore.types.TaskDefinitionSkill;
import net.reldo.taskstracker.data.route.CustomRoute;
import net.reldo.taskstracker.data.route.CustomRouteItem;
import net.reldo.taskstracker.data.route.RouteItem;
import net.reldo.taskstracker.data.route.RouteSection;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.TaskService;
import net.reldo.taskstracker.data.task.filters.FilterMatcher;
import net.reldo.taskstracker.panel.components.FixedWidthPanel;
import net.reldo.taskstracker.panel.components.SectionHeaderPanel;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;

@Slf4j
public class TaskListPanel extends JScrollPane
{
	public TasksTrackerPlugin plugin;
	private final int TASK_LIST_BUFFER_COUNT = 2;
	private final HashMap<Integer, TaskPanel> taskPanelsByStructId = new HashMap<>();
	public ArrayList<TaskPanel> taskPanels = new ArrayList<>();
	private final ArrayList<TaskListListPanel> taskListBuffers = new ArrayList<>(TASK_LIST_BUFFER_COUNT);
	private int currentTaskListBufferIndex;
	private final TaskService taskService;
	private final JLabel emptyTasks = new JLabel();
	@Setter
    private int batchSize;
	// Section header panels, keyed by section name
	private final Map<String, SectionHeaderPanel> sectionHeaderPanels = new HashMap<>();
	// Track which sections are collapsed (by section name)
	private final Set<String> collapsedSections = new HashSet<>();
	// Custom item panels, keyed by custom item ID
	private final Map<String, CustomItemPanel> customItemPanels = new HashMap<>();
	// Track completed custom items (per route)
	private final Set<String> completedCustomItems = new HashSet<>();

	public TaskListPanel(TasksTrackerPlugin plugin, TaskService taskService)
	{
		this.plugin = plugin;
		this.taskService = taskService;
		batchSize = plugin.getConfig().taskPanelBatchSize();

		FixedWidthPanel taskListListPanelWrapper = new FixedWidthPanel();

		for(int i = 0; i < TASK_LIST_BUFFER_COUNT; i++)
		{
			taskListBuffers.add(new TaskListListPanel(plugin));
			taskListListPanelWrapper.add(taskListBuffers.get(i));
		}

		setViewportView(taskListListPanelWrapper);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		setCurrentTaskListListPanel(0);
	}

	private void setCurrentTaskListListPanel(int index)
	{
		if (index != currentTaskListBufferIndex)
		{
			taskListBuffers.get(currentTaskListBufferIndex).setVisible(false);
			taskListBuffers.get(index).setVisible(true);
			currentTaskListBufferIndex = index;
		}
	}

	private TaskListListPanel getCurrentTaskListListPanel()
	{
		return taskListBuffers.get(currentTaskListBufferIndex);
	}

	private TaskListListPanel getNextTaskListListPanel()
	{
		return taskListBuffers.get(getNextBufferIndex());
	}

	private void showNextTaskListListPanel()
	{
		log.debug("Showing next task list list panel: {}", getNextBufferIndex());
		TaskListListPanel previousPanel = getCurrentTaskListListPanel();
		setCurrentTaskListListPanel(getNextBufferIndex());
		previousPanel.prepEmptyTaskListPanel();
	}

	private int getNextBufferIndex()
	{
		return (currentTaskListBufferIndex + 1) % TASK_LIST_BUFFER_COUNT;
	}

	public void drawNewTaskType()
	{
		log.debug("Drawing new Task Type taskListListPanel");
		getNextTaskListListPanel().drawNewTaskType();
	}

	public void redraw()
	{
		log.debug("Redrawing taskListListPanel");
		getCurrentTaskListListPanel().redraw();
	}

	public void refreshAllTasks()
	{
		log.debug("TaskListPanel.refreshAllTasks");
		if (!SwingUtilities.isEventDispatchThread())
		{
			log.error("Task list panel refresh failed - not event dispatch thread.");
			return;
		}
		for (TaskPanel taskPanel : taskPanelsByStructId.values())
		{
			taskPanel.refresh();
			// Hide tasks in collapsed sections
			if (taskPanel.isVisible() && isTaskInCollapsedSection(taskPanel.task))
			{
				taskPanel.setVisible(false);
			}
		}
		refreshEmptyPanel();
	}

	/**
	 * Checks if a task is in a collapsed section.
	 */
	private boolean isTaskInCollapsedSection(TaskFromStruct task)
	{
		ConfigValues.TaskListTabs currentTab = plugin.getConfig().taskListTab();
		CustomRoute activeRoute = taskService.getActiveRoute(currentTab);
		if (activeRoute == null || activeRoute.getSections() == null)
		{
			return false;
		}

		int taskId = task.getIntParam("id");
		for (RouteSection section : activeRoute.getSections())
		{
			if (section.getTaskIds() != null && section.getTaskIds().contains(taskId))
			{
				String sectionKey = section.getName() != null ? section.getName() : "Section";
				return collapsedSections.contains(sectionKey);
			}
		}
		return false;
	}

	public void refreshMultipleTasks(Collection<TaskFromStruct> tasks)
	{
		log.debug("TaskListPanel.refreshMultipleTasks {}", tasks.size());
		if (!SwingUtilities.isEventDispatchThread())
		{
			log.error("Task list panel refresh failed - not event dispatch thread.");
			return;
		}
		for (TaskFromStruct task : tasks)
		{
			refresh(task);
		}
	}

	public void refreshTask(TaskFromStruct task)
	{
		log.debug("TaskListPanel.refreshMultipleTasks {}", task.getName());
		refresh(task);
	}

	private void refresh(TaskFromStruct task)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			log.error("Task list panel refresh failed - not event dispatch thread.");
			return;
		}
		if (task == null)
		{
			log.debug("Attempted to refresh null task");
			return;
		}

		emptyTasks.setVisible(false);

		TaskPanel panel = taskPanelsByStructId.get(task.getStructId());
		if (panel != null)
		{
			panel.refresh();
		}

		refreshEmptyPanel();

	}

	private void refreshEmptyPanel()
	{
		boolean isAnyTaskPanelVisible = taskPanelsByStructId.values().stream()
				.anyMatch(TaskPanel::isVisible);

		if (!isAnyTaskPanelVisible)
		{
			emptyTasks.setVisible(true);
		}
		else
		{
			emptyTasks.setVisible(false);
		}
	}

	public void refreshTaskPanelsWithSkill(Skill skill)
	{
		// Refresh all task panels for tasks with 'skill' or
		// 'SKILLS' (any skill) or 'TOTAL LEVEL' as a requirement.
		taskPanelsByStructId.values().stream()
			.filter(tp ->
			{
				List<TaskDefinitionSkill> skillsList = tp.task.getTaskDefinition().getSkills();
				if (skillsList == null || skillsList.isEmpty())
				{
					return false;
				}

				return skillsList.stream()
					.map(TaskDefinitionSkill::getSkill)
					.anyMatch(s -> s.equalsIgnoreCase(skill.getName()) ||
						s.equalsIgnoreCase("SKILLS") ||
						s.equalsIgnoreCase("TOTAL LEVEL")
					);
			})
			.forEach(TaskPanel::refresh);
	}

	public String getEmptyTaskListMessage()
	{
		return "No tasks match the current filters.";
	}

    private class TaskListListPanel extends FixedWidthPanel
	{
		private final TasksTrackerPlugin plugin;

		public TaskListListPanel(TasksTrackerPlugin plugin)
		{
			this.plugin = plugin;

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(new EmptyBorder(0, 10, 10, 10));
			setAlignmentX(Component.LEFT_ALIGNMENT);

			emptyTasks.setBorder(new EmptyBorder(10,0,10,0));
			emptyTasks.setText("<html><center>" + getEmptyTaskListMessage() + "</center></html>");
			emptyTasks.setFont(FontManager.getRunescapeSmallFont());
			emptyTasks.setHorizontalAlignment(JLabel.CENTER);
			emptyTasks.setVerticalAlignment(JLabel.CENTER);
			add(emptyTasks);
			emptyTasks.setVisible(false);
		}

		public void prepEmptyTaskListPanel()
		{
			SwingUtilities.invokeLater(this::removeAll);
		}

		public void drawNewTaskType()
		{
			log.debug("TaskListPanel.drawNewTaskType");
			if(SwingUtilities.isEventDispatchThread())
			{
				log.debug("TaskListPanel creating panels");
				taskPanelsByStructId.clear();
				// Clear section headers when changing task type
				for (SectionHeaderPanel header : sectionHeaderPanels.values())
				{
					remove(header);
				}
				sectionHeaderPanels.clear();
				add(emptyTasks);

				List<TaskFromStruct> tasks = taskService.getTasks();
				if (tasks == null || tasks.isEmpty())
				{
					emptyTasks.setVisible(true);
					return;
				}

				emptyTasks.setVisible(false);

				// Buffer to hold newly created task panels before they are swapped in
				ArrayList<TaskPanel> newTaskPanels = new ArrayList<>(tasks.size());

				processInBatches(tasks.size(), indexPosition ->
				{
					TaskFromStruct task = tasks.get(indexPosition);
					TaskPanel taskPanel = new TaskPanel(plugin, task, plugin.getFilterMatcher());
					add(taskPanel);
					newTaskPanels.add(taskPanel);
					taskPanelsByStructId.put(task.getStructId(), taskPanel);
					if (indexPosition == (batchSize - 1)) taskPanels = newTaskPanels; // replace taskPanels list at end of first batch
				});
			}
			else
			{
				log.error("Task list panel drawNewTaskType failed - not event dispatch thread.");
			}
		}

		public void redraw()
		{
			log.debug("TaskListPanel.redraw");
			if(SwingUtilities.isEventDispatchThread())
			{
				if (taskPanels == null || taskPanels.isEmpty())
				{
					emptyTasks.setVisible(true);
					return;
				}

				// Check if there's an active route for the current tab
				ConfigValues.TaskListTabs currentTab = plugin.getConfig().taskListTab();
				CustomRoute activeRoute = taskService.getActiveRoute(currentTab);

				// Hide all existing section headers first
				for (SectionHeaderPanel header : sectionHeaderPanels.values())
				{
					header.setVisible(false);
				}

				if (activeRoute != null && activeRoute.getSections() != null)
				{
					// Route is active - render with section headers
					redrawWithSections(activeRoute, currentTab);
				}
				else
				{
					// No route - render normally
					redrawWithoutSections(currentTab);
				}

				SwingUtilities.invokeLater(TaskListPanel.this::refreshAllTasks);
			}
			else
			{
				log.error("Task list panel redraw failed - not event dispatch thread.");
			}
		}

		private void redrawWithoutSections(ConfigValues.TaskListTabs currentTab)
		{
			for (int indexPosition = 0; indexPosition < taskPanels.size(); indexPosition++)
			{
				int adjustedIndexPosition = indexPosition;
				if (plugin.getConfig().sortDirection().equals(ConfigValues.SortDirections.DESCENDING))
				{
					adjustedIndexPosition = taskPanels.size() - (adjustedIndexPosition + 1);
				}

				int taskIndex = taskService.getSortedTaskIndex(plugin.getConfig().sortCriteria(), adjustedIndexPosition);
				TaskPanel taskPanel = taskPanels.get(taskIndex);
				setComponentZOrder(taskPanel, indexPosition);
			}
		}

		private void redrawWithSections(CustomRoute route, ConfigValues.TaskListTabs currentTab)
		{
			// Load custom item completion state
			completedCustomItems.clear();
			if (route.getName() != null)
			{
				completedCustomItems.addAll(plugin.loadCustomItemCompletion(currentTab, route.getName()));
			}

			// Build a map of task ID to TaskPanel for quick lookup
			Map<Integer, TaskPanel> taskPanelById = new HashMap<>();
			for (TaskPanel tp : taskPanels)
			{
				taskPanelById.put(tp.task.getIntParam("id"), tp);
			}

			// Remove old custom item panels
			for (CustomItemPanel panel : customItemPanels.values())
			{
				remove(panel);
			}
			customItemPanels.clear();

			// Track which tasks have been positioned
			Set<Integer> positionedTaskIds = new HashSet<>();

			int componentPosition = 0;

			// Iterate through sections and position components
			for (RouteSection section : route.getSections())
			{
				List<RouteItem> items = section.getItems();
				if (items == null || items.isEmpty())
				{
					continue;
				}

				// Count visible/completed items for this section
				int visibleTaskCount = 0;
				int completedTaskCount = 0;
				List<Component> visiblePanels = new ArrayList<>();

				for (RouteItem item : items)
				{
					if (item.isTask())
					{
						TaskPanel tp = taskPanelById.get(item.getTaskId());
						if (tp != null)
						{
							// Check if task will be visible (passes filters)
							if (plugin.getFilterMatcher() != null &&
								plugin.getFilterMatcher().meetsFilterCriteria(tp.task, plugin.taskTextFilter))
							{
								visibleTaskCount++;
								visiblePanels.add(tp);
								if (tp.task.isCompleted())
								{
									completedTaskCount++;
								}
							}
							positionedTaskIds.add(item.getTaskId());
						}
					}
					else
					{
						// Custom item
						CustomRouteItem customItem = item.getCustomItem();
						if (customItem != null)
						{
							boolean isCompleted = completedCustomItems.contains(customItem.getId());

							// Check completion filter - custom items should respect the same filter as tasks
							ConfigValues.CompletedFilterValues completedFilter = plugin.getConfig().completedFilter();
							if (completedFilter == ConfigValues.CompletedFilterValues.INCOMPLETE && isCompleted)
							{
								continue; // Skip completed items when showing incomplete only
							}
							if (completedFilter == ConfigValues.CompletedFilterValues.COMPLETE && !isCompleted)
							{
								continue; // Skip incomplete items when showing complete only
							}

							CustomItemPanel panel = new CustomItemPanel(plugin, customItem, isCompleted);
							panel.setOnCompletionChanged(p -> {
								if (p.isCompleted())
								{
									completedCustomItems.add(p.getItem().getId());
								}
								else
								{
									completedCustomItems.remove(p.getItem().getId());
								}
								saveCustomItemCompletion();
							});
							panel.setOnRemove(p -> {
								route.removeCustomItem(p.getItem().getId());
								saveRoute(route);
								redraw();
							});
							customItemPanels.put(customItem.getId(), panel);
							add(panel);
							visiblePanels.add(panel);
						}
					}
				}

				// Skip sections with no visible items
				if (visiblePanels.isEmpty())
				{
					continue;
				}

				// Get or create section header
				String sectionKey = section.getName() != null ? section.getName() : "Section";
				SectionHeaderPanel header = sectionHeaderPanels.get(sectionKey);
				if (header == null)
				{
					header = new SectionHeaderPanel(section, completedTaskCount, visibleTaskCount);
					sectionHeaderPanels.put(sectionKey, header);
					add(header);
				}
				else
				{
					// Update existing header - need to recreate to update counts
					remove(header);
					header = new SectionHeaderPanel(section, completedTaskCount, visibleTaskCount);
					sectionHeaderPanels.put(sectionKey, header);
					add(header);
				}

				// Restore collapsed state and set up callback
				boolean isCollapsed = collapsedSections.contains(sectionKey);
				header.setCollapsedState(isCollapsed);
				final String finalSectionKey = sectionKey;
				final List<Component> finalVisiblePanels = visiblePanels;
				header.setCollapseCallback(collapsed -> {
					if (collapsed)
					{
						collapsedSections.add(finalSectionKey);
					}
					else
					{
						collapsedSections.remove(finalSectionKey);
					}
					// Toggle visibility of panels in this section
					for (Component panel : finalVisiblePanels)
					{
						if (panel instanceof TaskPanel)
						{
							TaskPanel tp = (TaskPanel) panel;
							tp.setVisible(!collapsed && meetsFilterCriteria(tp));
						}
						else
						{
							panel.setVisible(!collapsed);
						}
					}
					revalidate();
					repaint();
				});

				header.setVisible(true);
				setComponentZOrder(header, componentPosition++);

				// Position all visible panels in this section (in route order)
				// Hide them if section is collapsed
				for (Component panel : visiblePanels)
				{
					setComponentZOrder(panel, componentPosition++);
					if (isCollapsed)
					{
						panel.setVisible(false);
					}
				}
			}

			// Position any tasks not in the route at the end (they'll be hidden by filter anyway)
			for (TaskPanel tp : taskPanels)
			{
				int taskId = tp.task.getIntParam("id");
				if (!positionedTaskIds.contains(taskId))
				{
					setComponentZOrder(tp, componentPosition++);
				}
			}
		}

		private void saveCustomItemCompletion()
		{
			// Save to plugin config
			ConfigValues.TaskListTabs currentTab = plugin.getConfig().taskListTab();
			CustomRoute route = taskService.getActiveRoute(currentTab);
			if (route != null)
			{
				plugin.saveCustomItemCompletion(currentTab, route.getName(), completedCustomItems);
			}
		}

		private void saveRoute(CustomRoute route)
		{
			// Save route changes through plugin
			ConfigValues.TaskListTabs currentTab = plugin.getConfig().taskListTab();
			plugin.saveRoute(currentTab, route);
		}

		/**
		 * Helper to check if a task panel meets filter criteria.
		 */
		private boolean meetsFilterCriteria(TaskPanel tp)
		{
			if (plugin.getFilterMatcher() == null)
			{
				return true;
			}
			return plugin.getFilterMatcher().meetsFilterCriteria(tp.task, plugin.taskTextFilter);
		}

		private void processInBatches(int objectCount, IntConsumer method)
		{
			processBatch(0, objectCount, method);
			showNextTaskListListPanel();
		}

		private void processBatch(int batch, int objectCount, IntConsumer method)
		{
			log.debug("TaskListPanel.processBatch {}", batch);

			for (int index = 0; index < batchSize; index++)
			{
				int indexPosition = index + (batch * batchSize);
				if (indexPosition < objectCount)
				{
					method.accept(indexPosition);
				}
				else
				{
					break;
				}
			}

			refreshEmptyPanel();
			validate();
			repaint();

			// queue next batch if not done or refresh after last batch
			int batchIndex = batch + 1;
			if (batchIndex * batchSize < objectCount)
			{
				SwingUtilities.invokeLater(() -> processBatch(batchIndex, objectCount, method));
			}
			else
			{
				SwingUtilities.invokeLater(TaskListPanel.this::refreshAllTasks);
			}
		}
	}
}
