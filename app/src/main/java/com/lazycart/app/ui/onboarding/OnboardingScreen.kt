package com.lazycart.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lazycart.app.data.local.datastore.UserPreferencesDataStore
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for managing the onboarding flow and seeding initial demo data.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    /**
     * Completes onboarding. If [addSampleData] is true, seeds 6 realistic starter pantry items.
     */
    fun finishOnboarding(addSampleData: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isFinishing.value = true
            if (addSampleData) {
                seedSampleData()
            }
            userPrefs.setOnboardingDone(true)
            _isFinishing.value = false
            onDone()
        }
    }

    private suspend fun seedSampleData() {
        val today = LocalDate.now()
        val sampleItems = listOf(
            PantryItem(
                name = "Milk",
                category = ItemCategory.DAIRY,
                quantity = 2f,
                unit = ItemUnit.LITRES,
                purchaseDate = today.minusDays(3),
                expiryDate = today.plusDays(4),
                priceHint = 60f
            ),
            PantryItem(
                name = "Eggs",
                category = ItemCategory.PROTEIN,
                quantity = 12f,
                unit = ItemUnit.PIECES,
                purchaseDate = today.minusDays(5),
                expiryDate = today.plusDays(10),
                priceHint = 80f
            ),
            PantryItem(
                name = "Rice",
                category = ItemCategory.GRAINS,
                quantity = 3f,
                unit = ItemUnit.KILOGRAMS,
                purchaseDate = today.minusDays(7),
                expiryDate = today.plusMonths(6),
                priceHint = 150f
            ),
            PantryItem(
                name = "Cooking Oil",
                category = ItemCategory.CONDIMENTS,
                quantity = 1f,
                unit = ItemUnit.LITRES,
                purchaseDate = today.minusDays(6),
                expiryDate = today.plusMonths(4),
                priceHint = 180f
            ),
            PantryItem(
                name = "Bread",
                category = ItemCategory.GRAINS,
                quantity = 1f,
                unit = ItemUnit.PIECES,
                purchaseDate = today.minusDays(2),
                expiryDate = today.plusDays(3),
                priceHint = 40f
            ),
            PantryItem(
                name = "Coffee",
                category = ItemCategory.BEVERAGES,
                quantity = 200f,
                unit = ItemUnit.GRAMS,
                purchaseDate = today.minusDays(4),
                expiryDate = today.plusMonths(8),
                priceHint = 220f
            )
        )
        sampleItems.forEach { pantryRepository.addItem(it) }
    }
}

private data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPageData(
        icon = Icons.Outlined.Kitchen,
        title = "Your Smart Pantry",
        description = "Track everything in your pantry. Know exactly what you have and how much."
    ),
    OnboardingPageData(
        icon = Icons.Outlined.Info,
        title = "Smart Predictions",
        description = "LazyCart learns how fast you use things and predicts when you need to restock — based on your real usage, not guesses."
    ),
    OnboardingPageData(
        icon = Icons.Outlined.ShoppingCart,
        title = "Shopping Made Easy",
        description = "Your shopping list builds itself. Just confirm what to buy and you are done."
    )
)

/**
 * 3-page interactive onboarding screen introducing LazyCart core features.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var addSampleGroceries by remember { mutableStateOf(true) }
    val isFinishing by viewModel.isFinishing.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage < onboardingPages.size - 1) {
                    TextButton(
                        onClick = { currentPage = onboardingPages.size - 1 },
                        enabled = !isFinishing
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Animated page content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f),
                label = "OnboardingPageTransition"
            ) { pageIndex ->
                val page = onboardingPages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Icon
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // On the last page: sample data toggle
                    if (pageIndex == onboardingPages.size - 1) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Add sample groceries",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Start with 6 common pantry items",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = addSampleGroceries,
                                    onCheckedChange = { addSampleGroceries = it },
                                    enabled = !isFinishing
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Page Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = currentPage == index
                    val width = if (isSelected) 24.dp else 8.dp
                    val color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Button
            if (currentPage < onboardingPages.size - 1) {
                Button(
                    onClick = { currentPage++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isFinishing
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Button(
                    onClick = {
                        viewModel.finishOnboarding(addSampleGroceries) {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isFinishing
                ) {
                    if (isFinishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
