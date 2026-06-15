package com.healthcare.family.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.ui.auth.LoginScreen
import com.healthcare.family.ui.family.CreateFamilyScreen
import com.healthcare.family.ui.family.JoinFamilyScreen
import com.healthcare.family.ui.onboarding.RoleSelectionScreen
import com.healthcare.family.ui.plan.AddPlanScreen
import com.healthcare.family.ui.plan.PlanListScreen
import com.healthcare.family.ui.profile.PrivacyScreen

/**
 * 全量导航图：登录 → 角色选择 → 主界面(底部Tab) → 详情页。
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    isLoggedIn: Boolean,
) {
    val startDestination = if (isLoggedIn) "main" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // 登录
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // 登录成功后直接进入主界面，角色选择改为可选引导
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }

        // 角色选择
        composable("role_selection") {
            RoleSelectionScreen(
                onRoleConfirmed = { _, _ ->
                    navController.navigate("main") {
                        popUpTo("role_selection") { inclusive = true }
                    }
                },
            )
        }

        // 主界面（底部Tab）
        composable("main") {
            MainScreen(
                tokenManager = tokenManager,
                onNavigateToDetail = { route ->
                    navController.navigate(route)
                },
            )
        }

        // 家庭圈详情
        composable("family/create") {
            CreateFamilyScreen(
                onBack = { navController.popBackStack() },
                onFamilyCreated = { navController.popBackStack() },
            )
        }
        composable("family/join") {
            JoinFamilyScreen(
                onBack = { navController.popBackStack() },
                onJoined = { navController.popBackStack() },
            )
        }
        composable("family/invite") {
            PlaceholderScreen("邀请成员")
        }

        // 快速记录
        composable("record") {
            PlaceholderScreen("快速记录")
        }

        // 用药表
        composable("profile/medications") {
            PlaceholderScreen("我的用药表")
        }

        // 复诊计划
        composable("profile/plans") {
            PlanListScreen(
                onBack = { navController.popBackStack() },
                onAddPlan = { navController.navigate("profile/plans/add") },
            )
        }
        composable("profile/plans/add") {
            AddPlanScreen(
                onBack = { navController.popBackStack() },
                onPlanAdded = { navController.popBackStack() },
            )
        }

        // 紧急联系人
        composable("profile/contacts") {
            PlaceholderScreen("紧急联系人")
        }

        // 隐私设置
        composable("profile/privacy") {
            PrivacyScreen(
                tokenManager = tokenManager,
                onBack = { navController.popBackStack() },
            )
        }

        // 健康周报
        composable("report/weekly") {
            PlaceholderScreen("健康周报")
        }

        // 饮食详情
        composable("diet/menu") {
            PlaceholderScreen("今日菜单")
        }
        composable("diet/substitution") {
            PlaceholderScreen("食材替换")
        }
        composable("diet/recipe/{id}") {
            PlaceholderScreen("食谱详情")
        }
    }
}
