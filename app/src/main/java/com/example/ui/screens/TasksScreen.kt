package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StoreTaskEntity
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkShift
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.AmberTertiaryContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.OnAlertRedContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<StoreTaskEntity>,
    staffList: List<StaffMemberEntity>,
    activeStaff: StaffMemberEntity,
    onCycleStatus: (StoreTaskEntity) -> Unit,
    onSaveTask: (
        existingId: String?,
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        shift: WorkShift,
        assignee: StaffMemberEntity?,
        linkedBarcode: String?,
        dueLabel: String
    ) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedShift by remember { mutableStateOf<WorkShift?>(null) }
    var onlyMine by remember { mutableStateOf(false) }
    var hideDone by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StoreTaskEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, searchQuery, selectedShift, onlyMine, hideDone, activeStaff.id) {
        tasks.filter { task ->
            val matchSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)
            val matchShift = selectedShift == null || task.shift == selectedShift
            val matchMine = !onlyMine || task.assignedStaffId == activeStaff.id
            val matchDone = !hideDone || task.status != TaskStatus.DONE
            matchSearch && matchShift && matchMine && matchDone
        }
    }

    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val progress = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 450),
        label = "task_progress"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_add_item))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.85f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                MaterialTheme.shapes.medium
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Shift Operations",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$doneCount of ${tasks.size} tasks completed today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = EmeraldPrimaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search daily tasks, aisles, checklists...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = onlyMine,
                        onClick = { onlyMine = !onlyMine },
                        label = { Text("My Tasks") }
                    )
                    FilterChip(
                        selected = hideDone,
                        onClick = { hideDone = !hideDone },
                        label = { Text("Pending Only") }
                    )
                    WorkShift.entries.forEach { shift ->
                        FilterChip(
                            selected = selectedShift == shift,
                            onClick = { selectedShift = if (selectedShift == shift) null else shift },
                            label = { Text(shift.label) }
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_tasks),
                                contentDescription = stringResource(R.string.cd_empty_tasks),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Text(
                                text = "All clear for this view",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap + to organize opening checks, restock runs, or end-of-day cash audits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        canDelete = activeStaff.role.canAssignTasks(),
                        onCycleStatus = { onCycleStatus(task) },
                        onClick = {
                            editingTask = task
                            showSheet = true
                        },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            TaskEditorSheet(
                existing = editingTask,
                staffList = staffList,
                activeStaff = activeStaff,
                onDismiss = { showSheet = false },
                onSave = { title, desc, cat, prio, shift, assignee, barcode, due ->
                    onSaveTask(
                        editingTask?.id,
                        title,
                        desc,
                        cat,
                        prio,
                        shift,
                        assignee,
                        barcode,
                        due
                    )
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: StoreTaskEntity,
    canDelete: Boolean,
    onCycleStatus: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE
    val priorityColors = when (task.priority) {
        TaskPriority.HIGH -> AlertRedContainer to OnAlertRedContainer
        TaskPriority.MEDIUM -> AmberTertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TaskPriority.LOW -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onCycleStatus,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("task_toggle_${task.id}")
            ) {
                Icon(
                    imageVector = when (task.status) {
                        TaskStatus.DONE -> Icons.Rounded.CheckCircle
                        TaskStatus.IN_PROGRESS -> Icons.Rounded.Timelapse
                        TaskStatus.TODO -> Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = "Cycle task status: ${task.status.label}",
                    tint = when (task.status) {
                        TaskStatus.DONE -> EmeraldPrimary
                        TaskStatus.IN_PROGRESS -> AmberTertiary
                        TaskStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = priorityColors.first, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = task.priority.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColors.second,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${task.shift.label} • ${task.category.label}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (!task.linkedBarcode.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(12.dp))
                                Text(
                                    text = task.linkedBarcode.takeLast(6),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assigned: ${task.assignedStaffName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = task.dueLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete task",
                        tint = AlertRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskEditorSheet(
    existing: StoreTaskEntity?,
    staffList: List<StaffMemberEntity>,
    activeStaff: StaffMemberEntity,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        shift: WorkShift,
        assignee: StaffMemberEntity?,
        linkedBarcode: String?,
        dueLabel: String
    ) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: TaskCategory.RESTOCK) }
    var priority by remember(existing) { mutableStateOf(existing?.priority ?: TaskPriority.MEDIUM) }
    var shift by remember(existing) { mutableStateOf(existing?.shift ?: WorkShift.MORNING) }
    var assignee by remember(existing) {
        mutableStateOf(
            staffList.firstOrNull { it.id == existing?.assignedStaffId } ?: activeStaff
        )
    }
    var barcode by remember(existing) { mutableStateOf(existing?.linkedBarcode ?: "") }
    var dueLabel by remember(existing) { mutableStateOf(existing?.dueLabel ?: "Today") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (existing == null) "New Daily Operation Task" else "Edit Task",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_title_input")
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Instructions / Notes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = dueLabel,
                onValueChange = { dueLabel = it },
                label = { Text("Due Time") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Linked SKU (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskPriority.entries.forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { priority = p },
                    label = { Text(p.label) }
                )
            }
        }

        Text("Shift", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkShift.entries.forEach { s ->
                FilterChip(
                    selected = shift == s,
                    onClick = { shift = s },
                    label = { Text(s.label) }
                )
            }
        }

        Text("Category", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c.label) }
                )
            }
        }

        if (activeStaff.role.canAssignTasks()) {
            Text("Assign To Staff", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                staffList.filter { it.isActive }.forEach { member ->
                    FilterChip(
                        selected = assignee?.id == member.id,
                        onClick = { assignee = member },
                        label = { Text("${member.name} (${member.role.displayName})") }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSave(title, description, category, priority, shift, assignee, barcode, dueLabel)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .weight(2f)
                    .testTag("save_task_button")
            ) {
                Text("Save Task")
            }
        }
    }
}
