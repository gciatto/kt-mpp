package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer

@Suppress("LeakingThis")
internal abstract class TaskSorter(private val project: Project) {
    private val before = project.tasks.selectBefore()

    private val after = project.tasks.selectAfter()

    private val logged: MutableSet<Pair<Task, Task>> = mutableSetOf()

    private fun forceOrdering(before: Task, after: Task) {
        after.mustRunAfter(before)
        val logNotice = Pair(before, after)
        if (logNotice !in logged) {
            logged += logNotice
            project.log(
                message = "make task ${after.path} run after ${before.path}",
                logLevel = LogLevel.LIFECYCLE
            )
        }
    }

    protected abstract fun TaskContainer.selectBefore(): TaskCollection<out Task>

    protected abstract fun TaskContainer.selectAfter(): TaskCollection<out Task>

    fun enforceOrdering() {
        after.all { afterTask ->
            before.all { beforeTask ->
                forceOrdering(beforeTask, afterTask)
            }
        }
    }

    companion object {
        fun Project.enforceOrderingAmongTasks(
            potentiallyBefore: TaskContainer.() -> TaskCollection<out Task>,
            potentiallyAfter: TaskContainer.() -> TaskCollection<out Task>
        ) {
            val enforcer = object : TaskSorter(this@enforceOrderingAmongTasks) {
                override fun TaskContainer.selectBefore() = potentiallyBefore()
                override fun TaskContainer.selectAfter() = potentiallyAfter()
            }
            enforcer.enforceOrdering()
        }
    }
}
