package cn.favouritesc.cookingshop.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerButton(
    selectedTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "选择时间"
) {
    var showDialog by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime?.hour ?: 12,
        initialMinute = selectedTime?.minute ?: 0,
        is24Hour = true
    )

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
        Text(
            text = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: label,
            modifier = Modifier.weight(1f)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "选择时间") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
