package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPreferencesSheet(
    preferences: NearbyPreferences,
    onDismiss: () -> Unit,
    onSave: (NearbyPreferences) -> Unit,
    onToggleEnable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentPrefs by remember(preferences) { mutableStateOf(preferences) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "تنظیمات کاوش",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enable / Disable discovery toggle card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VistaBrandColors.Indigo.copy(alpha = 0.08f))
                    .border(1.dp, VistaBrandColors.Indigo.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "نمایش من در «اطراف من»",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "روشن باشه تا دیده بشی و بقیه رو ببینی",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VistaFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                            ),
                        )
                    }
                    Switch(
                        checked = currentPrefs.isEnabled,
                        onCheckedChange = { enabled ->
                            currentPrefs = currentPrefs.copy(isEnabled = enabled)
                            onToggleEnable(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VistaBrandColors.Indigo,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interested In
            Text(
                text = "علاقه‌مند به",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrefSelectableChip(
                    label = "همه",
                    selected = currentPrefs.interestedIn == "all",
                    onClick = { currentPrefs = currentPrefs.copy(interestedIn = "all") },
                    modifier = Modifier.weight(1f),
                )
                PrefSelectableChip(
                    label = "خانم",
                    selected = currentPrefs.interestedIn == "female",
                    onClick = { currentPrefs = currentPrefs.copy(interestedIn = "female") },
                    modifier = Modifier.weight(1f),
                )
                PrefSelectableChip(
                    label = "آقا",
                    selected = currentPrefs.interestedIn == "male",
                    onClick = { currentPrefs = currentPrefs.copy(interestedIn = "male") },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Marital Status
            Text(
                text = "وضعیت تأهل",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrefSelectableChip(
                    label = "فرقی نداره",
                    selected = currentPrefs.maritalPref == "all",
                    onClick = { currentPrefs = currentPrefs.copy(maritalPref = "all") },
                    modifier = Modifier.weight(1f),
                )
                PrefSelectableChip(
                    label = "مجرد",
                    selected = currentPrefs.maritalPref == "single",
                    onClick = { currentPrefs = currentPrefs.copy(maritalPref = "single") },
                    modifier = Modifier.weight(1f),
                )
                PrefSelectableChip(
                    label = "متأهل",
                    selected = currentPrefs.maritalPref == "married",
                    onClick = { currentPrefs = currentPrefs.copy(maritalPref = "married") },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Age Range
            Text(
                text = "بازه سنی: ${currentPrefs.minAge} تا ${currentPrefs.maxAge} سال",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            RangeSlider(
                value = currentPrefs.minAge.toFloat()..currentPrefs.maxAge.toFloat(),
                onValueChange = { range ->
                    currentPrefs = currentPrefs.copy(
                        minAge = range.start.toInt(),
                        maxAge = range.endInclusive.toInt(),
                    )
                },
                valueRange = 18f..80f,
                steps = 62,
                colors = SliderDefaults.colors(
                    thumbColor = VistaBrandColors.Indigo,
                    activeTrackColor = VistaBrandColors.Indigo,
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = { onSave(currentPrefs) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VistaBrandColors.Indigo,
                ),
            ) {
                Text(
                    text = "ذخیره تنظیمات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PrefSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) VistaBrandColors.Indigo else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = VistaFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            ),
        )
    }
}
