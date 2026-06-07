package cn.favouritesc.cookingshop.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import cn.favouritesc.cookingshop.ui.theme.Primary

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            val color by animateColorAsState(
                targetValue = if (selected) Primary else Color(0xFFBDBDBD),
                animationSpec = tween(300)
            )

            NavigationBarItem(
                icon = {
                    val iconToShow = if (selected && screen.selectedIcon != null) screen.selectedIcon
                        else screen.icon
                    iconToShow?.let {
                        Icon(imageVector = it, contentDescription = screen.title, tint = color)
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = color
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
