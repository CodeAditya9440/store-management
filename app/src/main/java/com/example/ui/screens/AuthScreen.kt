package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import com.example.data.model.StaffMemberEntity
import com.example.ui.components.RoleBadge

@Composable
fun VaultSetupScreen(
    authError: String? = null,
    onClearError: () -> Unit = {},
    onInitializeVault: (
        storeName: String,
        currencySymbol: String,
        taxId: String,
        ownerName: String,
        ownerPin: String,
        ownerContact: String,
        seedSampleData: Boolean
    ) -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("₹") }
    var taxId by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPin by remember { mutableStateOf("") }
    var ownerContact by remember { mutableStateOf("") }
    var seedSampleData by remember { mutableStateOf(true) }
    var showPin by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Store Manager",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set up your shop in under a minute",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = MaterialTheme.shapes.large
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = {
                            storeName = it
                            onClearError()
                        },
                        label = { Text("Shop Name") },
                        placeholder = { Text("e.g., Corner Goods Market") },
                        leadingIcon = { Icon(Icons.Rounded.Storefront, null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_store_name_input")
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = {
                            ownerName = it
                            onClearError()
                        },
                        label = { Text("Your Name (Owner)") },
                        placeholder = { Text("e.g., Alex Morgan") },
                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_owner_name_input")
                    )

                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = {
                            if (it.length <= 8 && it.all(Char::isDigit)) {
                                ownerPin = it
                                onClearError()
                            }
                        },
                        label = { Text("Create Passcode (4–8 digits)") },
                        placeholder = { Text("Used to sign in quickly") },
                        leadingIcon = { Icon(Icons.Rounded.Key, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPin) "Hide passcode" else "Show passcode"
                                )
                            }
                        },
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        isError = authError != null,
                        supportingText = authError?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_owner_pin_input")
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { seedSampleData = !seedSampleData }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = seedSampleData,
                                onCheckedChange = { seedSampleData = it },
                                modifier = Modifier.testTag("setup_seed_sample_checkbox")
                            )
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = "Start with sample shop items & staff",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Adds demo products, tasks, Manager (PIN 2222) & Cashier (PIN 3333)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { showMoreOptions = !showMoreOptions },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Optional: Currency, Tax ID & Contact")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showMoreOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showMoreOptions) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Currency:", style = MaterialTheme.typography.labelLarge)
                                listOf("₹", "€", "£").forEach { sym ->
                                    FilterChip(
                                        selected = currency == sym,
                                        onClick = { currency = sym },
                                        label = { Text(if (sym == "₹") "₹ INR" else sym) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = taxId,
                                onValueChange = { taxId = it },
                                label = { Text("Business / GST / Tax ID (Optional)") },
                                placeholder = { Text("27AABCU9603R1ZM") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_tax_id_input")
                            )

                            OutlinedTextField(
                                value = ownerContact,
                                onValueChange = { ownerContact = it },
                                label = { Text("Owner Email or Phone (Optional)") },
                                placeholder = { Text("owner@myshop.com") },
                                leadingIcon = { Icon(Icons.Rounded.Badge, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onInitializeVault(
                                storeName,
                                currency,
                                taxId,
                                ownerName,
                                ownerPin,
                                ownerContact,
                                seedSampleData
                            )
                        },
                        enabled = storeName.isNotBlank() && ownerName.isNotBlank() && ownerPin.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("initialize_vault_button")
                    ) {
                        Text("Open My Shop", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            onInitializeVault(
                                "Sunny Corner Shop",
                                currency,
                                "",
                                "Alex Morgan",
                                "1234",
                                "",
                                true
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("try_demo_shop_button")
                    ) {
                        Text("Quick Start Demo Shop (PIN 1234)")
                    }
                }
            }

            Text(
                text = "Works offline • Shop data is encrypted on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StaffPinLockScreen(
    storeName: String,
    staffList: List<StaffMemberEntity>,
    authError: String? = null,
    onClearError: () -> Unit = {},
    onUnlock: (staffId: String, pin: String) -> Unit
) {
    val activeStaff = remember(staffList) { staffList.filter { it.isActive } }
    var selectedStaff by remember(activeStaff) { mutableStateOf(activeStaff.firstOrNull()) }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = storeName.ifBlank { "Store Manager" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sign in to start your shift",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = MaterialTheme.shapes.large
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Who is signing in?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    activeStaff.forEach { member ->
                        val isSelected = selectedStaff?.id == member.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedStaff = member
                                    pin = ""
                                    onClearError()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("staff_select_${member.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.trim().firstOrNull()?.uppercase() ?: "S",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RoleBadge(role = member.role)
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = if (isSelected) "Selected" else "Not selected",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 8 && it.all(Char::isDigit)) {
                                pin = it
                                onClearError()
                            }
                        },
                        label = { Text("Enter Passcode (PIN)") },
                        leadingIcon = { Icon(Icons.Rounded.Key, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPin) "Hide passcode" else "Show passcode"
                                )
                            }
                        },
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (selectedStaff != null && pin.length >= 4) {
                                    selectedStaff?.let { onUnlock(it.id, pin) }
                                }
                            }
                        ),
                        singleLine = true,
                        isError = authError != null,
                        supportingText = authError?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_pin_input")
                    )

                    Button(
                        onClick = {
                            selectedStaff?.let { onUnlock(it.id, pin) }
                        },
                        enabled = selectedStaff != null && pin.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("unlock_vault_button")
                    ) {
                        Text("Sign In", style = MaterialTheme.typography.titleSmall)
                    }

                    if (pin.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                pin = ""
                                onClearError()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            Text(
                text = "Works offline • Shop records saved securely on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
