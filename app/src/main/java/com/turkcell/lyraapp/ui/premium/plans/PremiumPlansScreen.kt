package com.turkcell.lyraapp.ui.premium.plans

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.lyraapp.data.network.MembershipPlanDto
import com.turkcell.lyraapp.ui.icons.LyraIcons

@Composable
fun PremiumPlansRoute(
    onNavigateToPayment: (planId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PremiumPlansViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PremiumPlansEffect.NavigateToPayment -> onNavigateToPayment(effect.planId)
                PremiumPlansEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    PremiumPlansScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun PremiumPlansScreen(
    state: PremiumPlansUiState,
    onIntent: (PremiumPlansIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            IconButton(
                onClick = { onIntent(PremiumPlansIntent.NavigateBack) },
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            ) {
                Icon(
                    imageVector = LyraIcons.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(8.dp))

            PremiumHeader()

            Spacer(Modifier.height(24.dp))

            FeatureList()

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Planini sec",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isSelected = plan.id == state.selectedPlanId,
                            onClick = { onIntent(PremiumPlansIntent.SelectPlan(plan.id)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onIntent(PremiumPlansIntent.Proceed) },
                enabled = state.selectedPlanId != null && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Devam et",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PremiumHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LyraIcons.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
        }

        Text(
            text = "LyraApp Premium",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "Reklamsiz, sinırsiz ve cevrimdisi muzıgin keyfini cıkar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun FeatureList() {
    val features = listOf(
        Triple(LyraIcons.Close, "Reklamsiz dinleme", "Kesintisiz, sinırsiz muzik"),
        Triple(LyraIcons.SkipNext, "Sinırsiz atlama", "Istedigin sarkıya gec"),
        Triple(LyraIcons.Download, "Cevrimdisi indirme", "Internet olmadan dinle"),
        Triple(LyraIcons.Waveform, "Yuksek ses kalitesi", "320 kbps net ses"),
        Triple(LyraIcons.Smartphone, "Tum cihazlarında", "Telefon, tablet ve masaustu"),
    )

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        features.forEach { (icon, title, subtitle) ->
            FeatureRow(icon = icon, title = title, subtitle = subtitle)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: MembershipPlanDto,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isPopular = plan.type == "recurring"
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (plan.type == "recurring") "Aylık abonelik" else "Tek seferlik",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isPopular) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Populer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
                Text(
                    text = if (plan.type == "recurring") "Istedigin zaman iptal et"
                    else "${plan.durationDays} gun erisim · otomatik yenileme yok",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (plan.type == "recurring") "₺${plan.price.toInt()} / ay"
                else "₺${plan.price.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
