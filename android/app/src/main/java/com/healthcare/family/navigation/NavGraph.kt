package com.healthcare.family.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.healthcare.family.MainActivity

/**
 * 全量导航图。
 * 当前Phase 0仅含占位，后续Phase逐页添加路由。
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Phase 1: 首页 (三角色)
        composable("home") {
            // TODO: Phase 1 实现 HomeScreen
        }

        // Phase 1: 家庭圈
        composable("family") {
            // TODO: Phase 1 实现 FamilyScreen
        }
        composable("family/member/{userId}") {
            // TODO: Phase 1 实现 MemberDetailScreen
        }

        // Phase 3: 饮食
        composable("diet") {
            // TODO: Phase 3 实现 DietScreen
        }
        composable("diet/recipe/{id}") {
            // TODO: Phase 3 实现 RecipeDetailScreen
        }
        composable("diet/menu") {
            // TODO: Phase 3 实现 DailyMenuScreen
        }

        // Phase 5: 发现 (仅年轻患者)
        composable("discover") {
            // TODO: Phase 5 实现 DiscoverScreen
        }
        composable("discover/article/{id}") {
            // TODO: Phase 5 实现 ArticleDetailScreen
        }
        composable("discover/group/{id}") {
            // TODO: Phase 5 实现 CommunityGroupScreen
        }

        // Phase 1: 我的
        composable("profile") {
            // TODO: Phase 1 实现 ProfileScreen
        }
        composable("profile/medications") {
            // TODO: Phase 1 实现 MedicationListScreen
        }
        composable("profile/medications/add") {
            // TODO: Phase 2 实现 AddMedicationScreen
        }
        composable("profile/privacy") {
            // TODO: Phase 1 实现 PrivacySettingsScreen
        }
        composable("profile/contacts") {
            // TODO: Phase 2 实现 EmergencyContactsScreen
        }

        // Phase 2: 快速记录 (全局，不在Tab内)
        composable("record") {
            // TODO: Phase 2 实现 RecordScreen
        }

        // Phase 4: 周报 (全局)
        composable("report/weekly") {
            // TODO: Phase 4 实现 WeeklyReportScreen
        }

        // Phase 6: 新手引导 (首次)
        composable("onboarding") {
            // TODO: Phase 6 实现 OnboardingScreen
        }
    }
}
