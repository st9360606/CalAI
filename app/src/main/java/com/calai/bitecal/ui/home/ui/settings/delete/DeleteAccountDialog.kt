// app/src/main/java/com/calai/app/ui/home/ui/settings/delete/DeleteAccountDialog.kt
package com.calai.bitecal.ui.home.ui.settings.delete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DeleteAccountDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    deleting: Boolean = false
) {
    if (!visible) return

    Dialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !deleting,
            dismissOnClickOutside = !deleting,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    // ✅ 四邊距離框更大一點點（但總高度不變：下面 Spacer 會縮）
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 24.dp)
                ) {
                    // Title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delete Account?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111114),
                            modifier = Modifier.weight(1f)
                        )

                        // Close (X) in light circle
                        Box(
                            modifier = Modifier
                                .size(33.dp)
                                .background(Color(0xFFECECEC), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { if (!deleting) onDismiss() },
                                enabled = !deleting,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "close",
                                    tint = Color.Black
                                )
                            }
                        }
                    }

                    // ✅ 原本 20.dp → 18.dp（抵消 padding 變大造成的高度增加）
                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Are you sure want to permanently delete\nyour account?",
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF2F3A4A),
                        textAlign = TextAlign.Start
                    )

                    // ✅ 原本 24.dp → 22.dp（同上）
                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (!deleting) onCancel() },
                            enabled = !deleting,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(0.8.dp, Color(0xFF24252A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF111114)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(47.dp)
                        ) {
                            Text("Cancel", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { if (!deleting) onDelete() },
                            enabled = !deleting,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE46A6A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(47.dp)
                        ) {
                            Text("Delete", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
