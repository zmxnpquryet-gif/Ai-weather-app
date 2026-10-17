package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CityLocation
import com.example.ui.theme.LocalSkyMindColors
import kotlin.math.abs

enum class FavoritePlaceCategory(
    val tagKo: String,
    val defaultAliasSuggestion: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    HOME("집", "우리집", Icons.Default.Home, Color(0xFF0D9488)),
    WORK("회사", "회사 · 오피스", Icons.Default.Business, Color(0xFF0284C7)),
    CAFE("카페", "자주 가는 카페", Icons.Default.LocalCafe, Color(0xFFD97706)),
    SCHOOL("학교", "학교 · 캠퍼스", Icons.Default.School, Color(0xFF7C3AED)),
    FITNESS("운동", "운동 · 공원", Icons.Default.DirectionsRun, Color(0xFF059669)),
    TRAVEL("여행", "주말 나들이", Icons.Default.FlightTakeoff, Color(0xFFE11D48)),
    GENERAL("일반", "자주 가는 곳", Icons.Default.Bookmark, Color(0xFF0284C7));

    companion object {
        fun fromTag(tag: String): FavoritePlaceCategory {
            return entries.firstOrNull { it.tagKo == tag } ?: GENERAL
        }
    }
}

val QuickFavoriteAliasSuggestions = listOf(
    "우리집" to "집",
    "회사" to "회사",
    "단골 카페" to "카페",
    "학교" to "학교",
    "헬스장 · 공원" to "운동",
    "본가 · 부모님 댁" to "집",
    "주말 나들이" to "여행"
)

/**
 * Main Screen Card Section for '나의 장소' 즐겨찾기 (My Favorite Places).
 * Lets the user view saved places with custom names, switch weather with 1 tap,
 * rename any saved place, or add new places with custom aliases.
 */
@Composable
fun MyFavoritePlacesSection(
    selectedLocation: CityLocation,
    favoritePlaces: List<CityLocation>,
    isCurrentSaved: Boolean,
    onSelectPlace: (CityLocation) -> Unit,
    onOpenAddOrEditDialog: (CityLocation?) -> Unit,
    onQuickSaveWithPreset: (CityLocation, String, String) -> Unit,
    onRemovePlace: (CityLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSkyMindColors.current

    SeaGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("my_favorite_places_section"),
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = SkyMindSunOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "나의 장소 즐겨찾기",
                            style = MaterialTheme.typography.titleLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "자주 방문하는 장소를 별도 이름으로 저장하고 날씨를 빠르게 확인하세요",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyMindTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                SeaGlassPill(
                    isSelected = true,
                    activeColor = Color(0xFF0D9488),
                    onClick = { onOpenAddOrEditDialog(selectedLocation) },
                    modifier = Modifier.testTag("open_my_places_modal_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "장소 추가 · 관리",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick 1-Tap Save Banner for Current Location if not yet named or saved
            SeaGlassSurface(
                cornerRadius = 18.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.25f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = SkyMindGreenGood,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "현재 보고 있는 '${selectedLocation.name}'에 이름 붙여 바로 저장:",
                                style = MaterialTheme.typography.labelLarge,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        SeaGlassPill(
                            isSelected = false,
                            onClick = { onOpenAddOrEditDialog(selectedLocation) },
                            modifier = Modifier.testTag("custom_name_current_place_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = SkyMindBlueAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "직접 이름 입력",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindBlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(QuickFavoriteAliasSuggestions) { (aliasLabel, categoryTag) ->
                            val cat = FavoritePlaceCategory.fromTag(categoryTag)
                            val isAlreadyThisAlias = isCurrentSaved && selectedLocation.customAlias == aliasLabel
                            SeaGlassPill(
                                isSelected = isAlreadyThisAlias,
                                activeColor = cat.accentColor,
                                onClick = {
                                    onQuickSaveWithPreset(selectedLocation, aliasLabel, categoryTag)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isAlreadyThisAlias) Color.White else cat.accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ $aliasLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAlreadyThisAlias) Color.White else SkyMindTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Saved '나의 장소' Cards List
            if (favoritePlaces.isEmpty()) {
                SeaGlassSurface(
                    cornerRadius = 18.dp,
                    tintColor = Color(0xFFCFFAFE),
                    surfaceAlpha = 0.22f,
                    onClick = { onOpenAddOrEditDialog(selectedLocation) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            tint = SkyMindGreenGood,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "아직 저장된 '나의 장소'가 없어요. 터치해서 우리집·회사 등을 등록해 보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyMindTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    favoritePlaces.forEach { place ->
                        val cat = FavoritePlaceCategory.fromTag(place.categoryTag)
                        val isActive = !selectedLocation.isCurrentGps &&
                            abs(place.latitude - selectedLocation.latitude) < 0.01 &&
                            abs(place.longitude - selectedLocation.longitude) < 0.01

                        SeaGlassSurface(
                            cornerRadius = 18.dp,
                            tintColor = if (isActive) Color(0xFF99F6E4) else Color(0xFFCFFAFE),
                            surfaceAlpha = if (isActive) 0.36f else 0.24f,
                            onClick = { onSelectPlace(place) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("favorite_place_item_${place.displayTitle}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(cat.accentColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = cat.tagKo,
                                            tint = cat.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = place.displayTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = SkyMindTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            SeaGlassPill(
                                                isSelected = true,
                                                activeColor = cat.accentColor.copy(alpha = 0.85f),
                                                onClick = { onSelectPlace(place) }
                                            ) {
                                                Text(
                                                    text = cat.tagKo,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "현재 선택됨",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SkyMindGreenGood,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (place.customAlias.isNotBlank() && place.customAlias != place.name) {
                                                "${place.name} · ${place.adminArea}".trimEnd(' ', '·')
                                            } else {
                                                "${place.adminArea} ${place.country}".trim()
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyMindTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Edit Name / Category Button
                                    IconButton(
                                        onClick = { onOpenAddOrEditDialog(place) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "이름 수정",
                                            tint = SkyMindBlueAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    // Delete Favorite Place Button
                                    IconButton(
                                        onClick = { onRemovePlace(place) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "즐겨찾기 삭제",
                                            tint = tokens.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Modal Dialog to Add, Search, Rename, and Manage '나의 장소' 즐겨찾기.
 */
@Composable
fun MyFavoritePlacesDialog(
    initialTargetLocation: CityLocation,
    favoritePlaces: List<CityLocation>,
    searchQuery: String,
    searchResults: List<CityLocation>,
    onSearchQueryChange: (String) -> Unit,
    onSaveFavoritePlace: (location: CityLocation, customAlias: String, categoryTag: String, selectNow: Boolean) -> Unit,
    onSelectFavoritePlace: (CityLocation) -> Unit,
    onRemoveFavoritePlace: (CityLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalSkyMindColors.current

    // Check if initialTargetLocation matches an existing favorite to prefill its customAlias & categoryTag
    val matchingSaved = remember(initialTargetLocation, favoritePlaces) {
        favoritePlaces.firstOrNull {
            abs(it.latitude - initialTargetLocation.latitude) < 0.01 &&
                abs(it.longitude - initialTargetLocation.longitude) < 0.01
        }
    }

    var targetCity by remember(initialTargetLocation) {
        mutableStateOf(matchingSaved ?: initialTargetLocation)
    }
    var customAliasInput by remember(targetCity) {
        mutableStateOf(targetCity.customAlias.ifBlank { targetCity.name })
    }
    var selectedCategory by remember(targetCity) {
        mutableStateOf(FavoritePlaceCategory.fromTag(targetCity.categoryTag))
    }

    LaunchedEffect(targetCity) {
        customAliasInput = targetCity.customAlias.ifBlank { targetCity.name }
        selectedCategory = FavoritePlaceCategory.fromTag(targetCity.categoryTag)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SeaGlassSurface(
            cornerRadius = 28.dp,
            tintColor = if (tokens.isDark) Color(0xFF0C2233) else Color(0xFFE6FAF8),
            surfaceAlpha = if (tokens.isDark) 0.94f else 0.95f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("my_favorite_places_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Title Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SkyMindSunOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "나의 장소 즐겨찾기 관리",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "장소에 나만의 이름(예: 우리집, 회사, 단골 카페)을 붙여 저장하세요",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_my_places_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = SkyMindTextSecondary
                            )
                        }
                    }
                }

                // 1. Search Another City/Place to Add to Favorites
                item {
                    SeaGlassSurface(
                        cornerRadius = 18.dp,
                        tintColor = Color(0xFFCFFAFE),
                        surfaceAlpha = 0.28f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "1. 저장할 지역 선택 (현재 지역 또는 새 지역 검색)",
                                style = MaterialTheme.typography.labelLarge,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = SkyMindBlueAccent
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "다른 동네·도시 검색 (예: 판교, 강남, 해운대, 제주)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SkyMindTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0D9488),
                                    unfocusedBorderColor = tokens.dividerColor,
                                    focusedTextColor = SkyMindTextPrimary,
                                    unfocusedTextColor = SkyMindTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("favorite_modal_city_search_input")
                            )

                            if (searchResults.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    searchResults.take(4).forEach { resultCity ->
                                        SeaGlassSurface(
                                            cornerRadius = 12.dp,
                                            tintColor = Color(0xFF99F6E4),
                                            surfaceAlpha = 0.28f,
                                            onClick = {
                                                targetCity = resultCity
                                                onSearchQueryChange("")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = resultCity.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = SkyMindTextPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${resultCity.adminArea} ${resultCity.country}".trim(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SkyMindTextSecondary
                                                    )
                                                }
                                                Text(
                                                    text = "이 장소 선택",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SkyMindGreenGood,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Customize Place Name (Alias) & Category Icon
                item {
                    SeaGlassSurface(
                        cornerRadius = 20.dp,
                        tintColor = Color(0xFFD6F5F2),
                        surfaceAlpha = 0.34f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "2. 장소 이름(별칭) 및 아이콘 지정",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "실제 지역: ${targetCity.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindGreenGood,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Category Selector Row
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(FavoritePlaceCategory.entries) { cat ->
                                    val isSelected = selectedCategory == cat
                                    SeaGlassPill(
                                        isSelected = isSelected,
                                        activeColor = cat.accentColor,
                                        onClick = {
                                            selectedCategory = cat
                                            if (customAliasInput.isBlank() || customAliasInput == targetCity.name) {
                                                customAliasInput = cat.defaultAliasSuggestion
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else cat.accentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = cat.tagKo,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White else SkyMindTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Name Text Field
                            OutlinedTextField(
                                value = customAliasInput,
                                onValueChange = { customAliasInput = it },
                                leadingIcon = {
                                    Icon(
                                        imageVector = selectedCategory.icon,
                                        contentDescription = null,
                                        tint = selectedCategory.accentColor
                                    )
                                },
                                trailingIcon = {
                                    if (customAliasInput.isNotEmpty()) {
                                        IconButton(onClick = { customAliasInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "지우기",
                                                tint = SkyMindTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "장소 별칭 입력 (예: 우리집, 강남 회사, 단골 카페)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SkyMindTextSecondary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedCategory.accentColor,
                                    unfocusedBorderColor = tokens.dividerColor,
                                    focusedTextColor = SkyMindTextPrimary,
                                    unfocusedTextColor = SkyMindTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("favorite_place_alias_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Name Suggestion Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(QuickFavoriteAliasSuggestions) { (suggestedAlias, catTag) ->
                                    SeaGlassPill(
                                        isSelected = customAliasInput == suggestedAlias,
                                        activeColor = Color(0xFF0D9488),
                                        onClick = {
                                            customAliasInput = suggestedAlias
                                            selectedCategory = FavoritePlaceCategory.fromTag(catTag)
                                        }
                                    ) {
                                        Text(
                                            text = suggestedAlias,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (customAliasInput == suggestedAlias) Color.White else SkyMindTextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val finalAlias = customAliasInput.trim().ifBlank { targetCity.name }
                                    onSaveFavoritePlace(
                                        targetCity,
                                        finalAlias,
                                        selectedCategory.tagKo,
                                        true
                                    )
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_favorite_place_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "'${customAliasInput.trim().ifBlank { targetCity.name }}' 나의 장소로 저장",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 3. Existing Saved '나의 장소' List inside Modal
                if (favoritePlaces.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = tokens.dividerColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "저장된 나의 장소 (${favoritePlaces.size}곳) · 터치하여 이동 또는 이름 수정",
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(favoritePlaces) { savedPlace ->
                        val cat = FavoritePlaceCategory.fromTag(savedPlace.categoryTag)
                        SeaGlassSurface(
                            cornerRadius = 16.dp,
                            tintColor = Color(0xFFCFFAFE),
                            surfaceAlpha = 0.24f,
                            onClick = {
                                onSelectFavoritePlace(savedPlace)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = cat.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = savedPlace.displayWithOriginalName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SkyMindTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${cat.tagKo} · ${savedPlace.adminArea}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyMindTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SeaGlassPill(
                                        isSelected = false,
                                        onClick = { targetCity = savedPlace }
                                    ) {
                                        Text(
                                            text = "이름 수정",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyMindBlueAccent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onRemoveFavoritePlace(savedPlace) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "삭제",
                                            tint = SkyMindTextSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
