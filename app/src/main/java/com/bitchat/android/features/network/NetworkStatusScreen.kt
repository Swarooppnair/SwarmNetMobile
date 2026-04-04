package com.bitchat.android.features.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitchat.android.core.ui.component.sheet.BitchatBottomSheet
import com.bitchat.android.ui.ChatViewModel
import kotlin.math.cos
import kotlin.math.sin

// ── Pure helper functions ─────────────────────────────────────────────────────

/** Returns a mesh health score in [0, 100] based on connected peer count. */
fun meshHealthScore(connectedPeerCount: Int): Int = minOf(connectedPeerCount * 10, 100)

/**
 * Maps an optional RSSI value to a signal level:
 * - null  → 0 (no signal)
 * - < -80 → 1 (weak)
 * - -80..-61 → 2 (medium)
 * - >= -60 → 3 (strong)
 */
fun rssiToSignalLevel(rssi: Int?): Int = when {
    rssi == null -> 0
    rssi >= -60  -> 3
    rssi >= -80  -> 2
    else         -> 1
}

/**
 * Maps an optional RSSI value to a radar ring index:
 * - >= -60      → 0 (inner / strong)
 * - -80..-61    → 1 (middle / medium)
 * - < -80 / null → 2 (outer / weak)
 */
fun rssiToRingIndex(rssi: Int?): Int = when {
    rssi != null && rssi >= -60 -> 0
    rssi != null && rssi >= -80 -> 1
    else                        -> 2
}

/**
 * Returns the display name for a peer: the nickname from [peerNicknames] if present,
 * otherwise the first 8 characters of [peerID].
 */
fun peerDisplayName(peerID: String, peerNicknames: Map<String, String>): String =
    peerNicknames[peerID] ?: peerID.take(8)

/**
 * Returns a truncated version of [peerID]: first 8 characters followed by "..." when
 * the ID is longer than 8 characters, otherwise the ID as-is.
 */
fun truncatedKey(peerID: String): String =
    if (peerID.length > 8) "${peerID.take(8)}..." else peerID

/**
 * Returns the connection badge text for a peer:
 * - "direct" when [peerDirect][peerID] is true
 * - "relay"  otherwise (false or absent)
 */
fun connectionBadge(peerID: String, peerDirect: Map<String, Boolean>): String =
    if (peerDirect[peerID] == true) "direct" else "relay"

/**
 * Returns the RSSI display string:
 * - "$rssi dBm" when [rssi] is non-null
 * - "N/A" when [rssi] is null
 */
fun rssiText(rssi: Int?): String = if (rssi != null) "$rssi dBm" else "N/A"

// ── Color palette ─────────────────────────────────────────────────────────────
private val BgBlack     = Color(0xFF000000)
private val SurfaceDark = Color(0xFF141414)
private val RingColor   = Color(0xFF1A1A3A)
private val PurpleText  = Color(0xFF7B68EE)
private val PurpleBg    = Color(0xFF1A1A3A)
private val GreenText   = Color(0xFF4CAF50)
private val GreenBg     = Color(0xFF1A3A1A)
private val RedText     = Color(0xFFFF6B6B)
private val RedBg       = Color(0xFF3A1A1A)
private val OrangeBg    = Color(0xFF3A2A1A)
private val OrangeText  = Color(0xFFFFAA44)
private val YellowText  = Color(0xFFFFDD44)
private val GrayText    = Color(0xFF888888)
private val WhiteText   = Color(0xFFE5E5E5)

// ── 2.1 PeerRow ───────────────────────────────────────────────────────────────

@Composable
fun PeerRow(
    peerID: String,
    displayName: String,
    truncatedKey: String,
    rssi: Int?,
    isDirect: Boolean,
    signalLevel: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Signal bars drawn with Canvas
        Canvas(modifier = Modifier.size(width = 20.dp, height = 16.dp)) {
            val barWidth = size.width / 5f
            val gap = barWidth
            val maxH = size.height
            val barCount = 3
            for (i in 0 until barCount) {
                val barH = maxH * (i + 1) / barCount.toFloat()
                val x = i * (barWidth + gap)
                val top = maxH - barH
                val filled = signalLevel > i
                drawRect(
                    color = if (filled) GreenText else GrayText.copy(alpha = 0.3f),
                    topLeft = Offset(x, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barH),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Name + key
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = WhiteText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = truncatedKey,
                color = GrayText,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Direct / relay badge
        val badgeText = if (isDirect) "direct" else "relay"
        val badgeBg   = if (isDirect) GreenBg  else PurpleBg
        val badgeFg   = if (isDirect) GreenText else PurpleText
        Box(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(text = badgeText, color = badgeFg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.width(8.dp))

        // RSSI text
        Text(
            text = rssiText(rssi),
            color = GrayText,
            fontSize = 11.sp,
        )
    }
}

// ── 2.2 RadarVisualization ────────────────────────────────────────────────────

@Composable
fun RadarVisualization(
    peers: List<String>,
    peerRSSI: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy) * 0.95f

        // Dark background circle
        drawCircle(color = Color(0xFF0A0A1A), radius = maxR, center = Offset(cx, cy))

        // 3 concentric rings at 30%, 60%, 90%
        val ringFractions = listOf(0.30f, 0.60f, 0.90f)
        for (frac in ringFractions) {
            drawCircle(
                color = RingColor,
                radius = maxR * frac,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Crosshair lines
        drawLine(RingColor, Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1.dp.toPx())
        drawLine(RingColor, Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 1.dp.toPx())

        // Peer dots
        for (peerID in peers) {
            val rssi = peerRSSI[peerID]
            val ringIndex = rssiToRingIndex(rssi)
            val ringR = maxR * ringFractions[ringIndex]
            val angleDeg = (Math.abs(peerID.hashCode()) % 360).toDouble()
            val angleRad = Math.toRadians(angleDeg)
            val px = cx + (ringR * cos(angleRad)).toFloat()
            val py = cy + (ringR * sin(angleRad)).toFloat()

            // Glow effect: larger semi-transparent circle first
            drawCircle(
                color = PurpleText.copy(alpha = 0.25f),
                radius = 10.dp.toPx(),
                center = Offset(px, py),
            )
            // Solid dot
            drawCircle(
                color = PurpleText,
                radius = 5.dp.toPx(),
                center = Offset(px, py),
            )
        }

        // Center dot (local device)
        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(cx, cy))
    }
}

// ── 2.3 NetworkStatusContent ──────────────────────────────────────────────────

@Composable
fun NetworkStatusContent(
    connectedPeers: List<String>,
    peerNicknames: Map<String, String>,
    peerRSSI: Map<String, Int>,
    peerDirect: Map<String, Boolean>,
) {
    val peerCount = connectedPeers.size
    val score = meshHealthScore(peerCount)
    val progress = score / 100f

    val progressColor = when {
        score >= 70 -> GreenText
        score >= 50 -> YellowText
        else        -> OrangeText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(PurpleBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("MESH", color = PurpleText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(GreenText, RoundedCornerShape(50)),
            )
            Spacer(Modifier.width(4.dp))
            Text("$peerCount peers", color = GrayText, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        Text("Network Status", color = WhiteText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Live mesh topology", color = GrayText, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))

        // ── Stat cards ────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(PurpleBg, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                Text("Connected Peers", color = GrayText, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text("$peerCount", color = PurpleText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            val healthBg = if (score >= 70) GreenBg else if (score >= 50) OrangeBg else RedBg
            val healthFg = if (score >= 70) GreenText else if (score >= 50) OrangeText else RedText
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(healthBg, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                Text("Mesh Health", color = GrayText, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text("$score%", color = healthFg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Radar ─────────────────────────────────────────────────────────────
        Text(
            "Radar",
            color = GrayText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        RadarVisualization(peers = connectedPeers, peerRSSI = peerRSSI)

        Spacer(Modifier.height(20.dp))

        // ── Mesh Health Score section ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mesh Health Score", color = WhiteText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("$score / 100", color = progressColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = progressColor,
            trackColor = SurfaceDark,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Score = min(peers × 10, 100)",
            color = GrayText,
            fontSize = 11.sp,
        )

        Spacer(Modifier.height(20.dp))

        // ── Peer list ─────────────────────────────────────────────────────────
        Text("Connected Peers", color = WhiteText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (connectedPeers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No peers connected", color = GrayText, fontSize = 14.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (peerID in connectedPeers) {
                    val rssi = peerRSSI[peerID]
                    PeerRow(
                        peerID = peerID,
                        displayName = peerDisplayName(peerID, peerNicknames),
                        truncatedKey = truncatedKey(peerID),
                        rssi = rssi,
                        isDirect = peerDirect[peerID] == true,
                        signalLevel = rssiToSignalLevel(rssi),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── 2.4 NetworkStatusSheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStatusSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
) {
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val peerNicknames  by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val peerRSSI       by viewModel.peerRSSI.collectAsStateWithLifecycle()
    val peerDirect     by viewModel.peerDirect.collectAsStateWithLifecycle()

    BitchatBottomSheet(onDismissRequest = onDismiss) {
        NetworkStatusContent(
            connectedPeers = connectedPeers,
            peerNicknames  = peerNicknames,
            peerRSSI       = peerRSSI,
            peerDirect     = peerDirect,
        )
    }
}
