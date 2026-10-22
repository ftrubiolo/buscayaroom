package com.example.buscaya.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.buscaya.R
import com.example.buscaya.data.models.BusinessResult
import com.example.buscaya.data.models.SearchUiState
import com.example.buscaya.ui.theme.BuscayaTheme
import com.example.buscaya.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun HomeScreenContent(
    viewModel: SearchViewModel,
    onBusinessSelected: (BusinessResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefrescando by viewModel.refrescando.collectAsStateWithLifecycle()

    // Filtrado inmediato en Room (0ms) y auto-búsqueda remota si hay red (con debounce)
    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // Status bar space (edge-to-edge)
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // Encabezado superior fijo con logo y buscador
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoSection(showImage = false)

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Villa del Rosario",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Campo de búsqueda principal arriba de la lista
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = {
                    Text(
                        text = "Buscar comercios...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contenedor principal de la lista: VISIBLE DESDE EL INICIO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
            ) {
                // Fila de encabezado con botón "Actualizar"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Comercios disponibles" else "Resultados de búsqueda",
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Button(
                        onClick = { viewModel.refrescar(forzar = true) },
                        enabled = !isRefrescando,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        if (isRefrescando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = "Actualizar", fontSize = 12.sp)
                        }
                    }
                }

                // Banner de advertencia cuando se navega sin conexión pero hay datos guardados en Room
                if (uiState is SearchUiState.SinConexion) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEEBA))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = (uiState as SearchUiState.SinConexion).mensaje,
                                color = Color(0xFF664D03),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.refrescar(forzar = true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Reintentar",
                                    color = Color(0xFF664D03),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de resultados leyendo directamente de Room en tiempo real
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = uiState) {
                        is SearchUiState.Cargando -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is SearchUiState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "Ha ocurrido un problema",
                                    color = MaterialTheme.colorScheme.error,
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.mensaje,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(fontSize = 14.sp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.refrescar(forzar = true) }
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                        is SearchUiState.Exito -> {
                            if (state.results.isEmpty()) {
                                Text(
                                    text = if (searchQuery.isBlank()) {
                                        "No hay comercios guardados aún.\nTocá 'Actualizar' para cargar el catálogo."
                                    } else {
                                        "No se encontraron comercios para \"$searchQuery\"."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(fontSize = 14.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(state.results, key = { it.id }) { business ->
                                        BusinessItem(
                                            business = business,
                                            onClick = { onBusinessSelected(business) }
                                        )
                                    }
                                }
                            }
                        }
                        is SearchUiState.SinConexion -> {
                            if (state.results.isEmpty()) {
                                Text(
                                    text = "Sin conexión a internet y no hay comercios guardados.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(fontSize = 14.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 24.dp)
                                ) {
                                    items(state.results, key = { it.id }) { business ->
                                        BusinessItem(
                                            business = business,
                                            onClick = { onBusinessSelected(business) }
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

private val imageCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

@Composable
fun rememberAsyncImagePainter(url: String?): Painter? {
    if (url.isNullOrBlank()) return null
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(imageCache[url]) }
    LaunchedEffect(url) {
        if (bitmap != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val decoded = BitmapFactory.decodeStream(connection.inputStream)
                if (decoded != null) {
                    imageCache[url] = decoded
                    bitmap = decoded
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return bitmap?.let { BitmapPainter(it.asImageBitmap()) }
}

@Composable
fun rememberAppIconPainter(): Painter {
    val context = LocalContext.current
    return remember {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }
}


@Composable
fun LogoSection(
    showImage: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate brand text size dynamically (36.sp when idle, 27.sp when active/searching)
    val brandTextSize by animateFloatAsState(
        targetValue = if (showImage) 50f else 27f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "brandTextSize"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Toggle logo image visibility with expand/collapse animations
//        AnimatedVisibility(
//            visible = showImage,
//            enter = fadeIn() + expandVertically(),
//            exit = fadeOut() + shrinkVertically()
//        ) {
//            val appIconPainter = rememberAppIconPainter()
//            Image(
//                painter = appIconPainter,
//                contentDescription = "App Logo",
//                modifier = Modifier
//                    .size(83.dp) // 30% larger (originally 64.dp)
//                    .padding(bottom = 6.dp)
//            )
//        }

        // "BuscaYa" brand text (Permanently 20% larger: 36.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Busca",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = brandTextSize.sp
                )
            )
            Text(
                text = "Ya",
                style = TextStyle(
                    color = Color(0xFFE5A00D),
                    fontWeight = FontWeight.Bold,
                    fontSize = brandTextSize.sp
                )
            )
        }
    }
}

// Generate initials from business name
fun getInitials(name: String): String {
    if (name.isBlank()) return "BY"
    val words = name.trim().split("\\s+".toRegex())
    return if (words.size >= 2) {
        (words[0].take(1) + words[1].take(1)).uppercase()
    } else {
        name.take(2).uppercase()
    }
}

// Generate a dynamic consistent theme color for avatar background
@Composable
fun getAvatarColor(name: String): Color {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFE5A00D),
        Color(0xFF009688),
        Color(0xFF9C27B0),
        Color(0xFFE91E63),
        Color(0xFF4CAF50)
    )
    val index = Math.abs(name.hashCode()) % colors.size
    return colors[index]
}

@Composable
fun BusinessItem(
    business: BusinessResult,
    onClick: () -> Unit
) {
    val imagePainter = rememberAsyncImagePainter(business.imagen)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Avatar (Image or Initials Placeholder)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = getAvatarColor(business.nombre), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imagePainter != null) {
                    Image(
                        painter = imagePainter,
                        contentDescription = business.nombre,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = getInitials(business.nombre),
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Info Section
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = business.nombre,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Show rating badge if available
                    business.rating?.let { rating ->
                        Text(
                            text = "⭐ %.1f".format(rating),
                            style = TextStyle(
                                color = Color(0xFFE5A00D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = business.descripcion,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )
                
                business.ubicacion?.let { address ->
                    if (address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = address,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
                
                business.telefono?.let { phone ->
                    if (phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tel: $phone",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
                
                business.precio?.let { price ->
                    if (price.isNotBlank() && price != "null") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "Precio: ",
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = price,
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Item divider line
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}