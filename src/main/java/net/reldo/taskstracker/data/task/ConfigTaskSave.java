package net.reldo.taskstracker.data.task;

import com.google.gson.annotations.Expose;
import java.util.HashSet;
import java.util.Set;

public class ConfigTaskSave
{
	@Expose public final long completed;
	@Expose public final long tracked;
	@Expose public final Integer structId;
	@Expose public final long ignored;
	@Expose public final Set<String> tags;

	public ConfigTaskSave(TaskFromStruct task)
	{
		completed = task.getCompletedOn();
		tracked = task.getTrackedOn();
		ignored = task.getIgnoredOn();
		structId = task.getStructId();
		tags = task.getTags() != null ? new HashSet<>(task.getTags()) : new HashSet<>();
	}
}
