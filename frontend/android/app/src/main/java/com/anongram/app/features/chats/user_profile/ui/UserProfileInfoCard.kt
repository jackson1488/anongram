package com.anongram.app.features.chats.user_profile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings

@Composable
internal fun UserProfileInfoCard(
    anonIdOrFingerprint: String,
    username: String?,
    bio: String?,
    birthday: String?,
    securityLabel: String,
    strings: UserProfileStrings,
    onOpenQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)

    fun copyText(value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("AnonGram", value))
        Toast.makeText(context, strings.copiedToast, Toast.LENGTH_SHORT).show()
    }

    val rows = buildList {
        if (anonIdOrFingerprint.isNotBlank()) {
            add(Triple(anonIdOrFingerprint, strings.labelAnonId, false))
        }
        if (!bio.isNullOrBlank()) {
            add(Triple(bio, strings.labelBio, false))
        }
        if (!username.isNullOrBlank()) {
            val formatted = if (username.startsWith("@")) username else "@$username"
            add(Triple(formatted, strings.labelUsername, true))
        }
        if (!birthday.isNullOrBlank()) {
            add(Triple(birthday, strings.labelBirthday, false))
        }
        if (securityLabel.isNotBlank()) {
            add(Triple(securityLabel, strings.labelEncryption, false))
        }
    }

    if (rows.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(shape)
            .background(scheme.card)
            .border(0.6.dp, scheme.islandBorder, shape)
    ) {
        rows.forEachIndexed { idx, (value, label, hasQr) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { copyText(value) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value,
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = scheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
                if (hasQr) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenQr),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = "QR",
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            if (idx < rows.lastIndex) {
                HorizontalDivider(
                    color = scheme.divider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun AddToContactsCard(
    strings: UserProfileStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(shape)
            .background(scheme.card)
            .border(0.6.dp, scheme.islandBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = strings.addToContacts,
            tint = scheme.textSecondary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = strings.addToContacts,
            color = scheme.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
