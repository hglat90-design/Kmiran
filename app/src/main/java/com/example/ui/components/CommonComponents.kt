package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldOwner
import com.example.ui.theme.ManagerPurple
import com.example.ui.theme.ModBlue
import com.example.ui.theme.TrustedOrange

@Composable
fun RoleBadge(
    role: String,
    modifier: Modifier = Modifier
) {
    val (text, bgBrush, icon, textColor) = when (role.lowercase()) {
        "owner" -> Quadruple(
            "👑 مالك",
            Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))),
            Icons.Default.WorkspacePremium,
            Color(0xFF3E2723)
        )
        "manager" -> Quadruple(
            "💎 مدير",
            Brush.horizontalGradient(listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A))),
            Icons.Default.Star,
            Color.White
        )
        "mod" -> Quadruple(
            "🛡️ مشرف",
            Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0))),
            Icons.Default.Shield,
            Color.White
        )
        "trusted" -> Quadruple(
            "✅ موثوق",
            Brush.horizontalGradient(listOf(Color(0xFFFFB74D), Color(0xFFE65100))),
            Icons.Default.CheckCircle,
            Color.White
        )
        else -> Quadruple(
            "👤 عضو",
            Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))),
            Icons.Default.Person,
            Color(0xFFCBD5E1)
        )
    }

    Row(
        modifier = modifier
            .background(bgBrush, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun UserAvatar(
    avatarUrl: String?,
    isOnline: Boolean = false,
    size: Dp = 44.dp,
    isOwner: Boolean = false,
    hasUnseenStatus: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val borderModifier = when {
        isOwner -> Modifier.border(
            width = 2.dp,
            brush = Brush.sweepGradient(listOf(GoldOwner, Color(0xFFFFA000), GoldOwner)),
            shape = CircleShape
        )
        hasUnseenStatus -> Modifier.border(
            width = 2.dp,
            brush = Brush.sweepGradient(listOf(EmeraldPrimary, CyanAccent, EmeraldPrimary)),
            shape = CircleShape
        )
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(avatarUrl?.ifEmpty { "https://cdn-icons-png.flaticon.com/512/847/847969.png" })
                .crossfade(true)
                .build(),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size - 4.dp)
                .clip(CircleShape)
                .then(borderModifier)
        )

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size / 3.8f)
                    .align(Alignment.BottomEnd)
                    .background(EmeraldPrimary, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
