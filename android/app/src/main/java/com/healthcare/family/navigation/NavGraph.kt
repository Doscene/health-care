package com.healthcare.family.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.healthcare.family.data.local.TokenManager
import com.healthcare.family.ui.auth.LoginScreen
import com.healthcare.family.ui.camera.CameraMode
import com.healthcare.family.ui.camera.CameraScreen
import com.healthcare.family.ui.contacts.EmergencyContactScreen
import com.healthcare.family.ui.diet.MenuScreen
import com.healthcare.family.ui.diet.RecipeDetailScreen
import com.healthcare.family.ui.diet.SubstitutionScreen
import com.healthcare.family.ui.family.CreateFamilyScreen
import com.healthcare.family.ui.family.InviteMemberScreen
import com.healthcare.family.ui.family.JoinFamilyScreen
import com.healthcare.family.ui.kit.FirstAidKitScreen
import com.healthcare.family.ui.medication.AddMedicationScreen
import com.healthcare.family.ui.medication.MedicationCalendarScreen
import com.healthcare.family.ui.medication.MedicationScreen
import com.healthcare.family.ui.onboarding.RoleSelectionScreen
import com.healthcare.family.ui.plan.AddPlanScreen
import com.healthcare.family.ui.plan.PlanListScreen
import com.healthcare.family.ui.profile.PrivacyScreen
import com.healthcare.family.ui.record.RecordScreen
import com.healthcare.family.ui.report.WeeklyReportScreen

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
                    navController.navigate("role_selection") {
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
                onNavigateToDetail = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
            )
        }

        // 家庭圈
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
            InviteMemberScreen(onBack = { navController.popBackStack() })
        }

        // 快速记录
        composable("record") {
            RecordScreen(onBack = { navController.popBackStack() })
        }

        // 用药表
        composable("profile/medications") {
            MedicationScreen(onBack = { navController.popBackStack() })
        }

        // 添加药品
        composable("medication/add") {
            AddMedicationScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate("camera/MEDICINE_BOX") },
            )
        }

        // 服药日历
        composable("medication/calendar") {
            MedicationCalendarScreen(onBack = { navController.popBackStack() })
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
            EmergencyContactScreen(onBack = { navController.popBackStack() })
        }

        // 急救包管理
        composable("profile/kit") {
            FirstAidKitScreen(onBack = { navController.popBackStack() })
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
            WeeklyReportScreen(onBack = { navController.popBackStack() })
        }

        // 拍照
        composable("camera/{mode}") { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "GENERAL"
            val cameraMode = try {
                CameraMode.valueOf(mode)
            } catch (e: Exception) {
                CameraMode.GENERAL
            }
            CameraScreen(
                cameraMode = cameraMode,
                onBack = { navController.popBackStack() },
                onImageCaptured = { filePath ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("capturedImage", filePath)
                    navController.popBackStack()
                },
            )
        }

        // 饮食详情
        composable("diet/menu") {
            MenuScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable("diet/substitution") {
            SubstitutionScreen(onBack = { navController.popBackStack() })
        }
        composable("diet/recipe/{id}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("id") ?: ""
            RecipeDetailScreen(
                recipeId = recipeId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
