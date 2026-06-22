import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiBearerAuth,
  ApiQuery,
} from '@nestjs/swagger';
import { FamilyService } from './family.service.js';
import { FamilySummaryService } from './family-summary.service.js';
import { MutualReminderService } from './mutual-reminder.service.js';
import { FamilyGoalService } from './family-goal.service.js';
import { FamilyChallengeService } from './family-challenge.service.js';
import {
  CurrentUser,
  type UserPayload,
} from '../../common/decorators/current-user.decorator.js';
import { Public } from '../../common/decorators/public.decorator.js';

@ApiTags('家庭圈')
@ApiBearerAuth()
@Controller('family')
export class FamilyController {
  constructor(
    private readonly familyService: FamilyService,
    private readonly summaryService: FamilySummaryService,
    private readonly reminderService: MutualReminderService,
    private readonly goalService: FamilyGoalService,
    private readonly challengeService: FamilyChallengeService,
  ) {}

  // ==================== 基础家庭圈 ====================

  @Post()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '创建家庭' })
  async createFamily(
    @CurrentUser() user: UserPayload,
    @Body() body: { name: string },
  ) {
    return {
      code: 0,
      data: await this.familyService.createFamily(user.id, body.name),
      message: '家庭创建成功',
    };
  }

  @Get('my')
  @ApiOperation({ summary: '获取我的家庭列表' })
  async getMyFamilies(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.familyService.getMyFamilies(user.id),
      message: 'ok',
    };
  }

  @Post('invite/verify')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '验证邀请码' })
  async verifyInviteCode(@Body() body: { code: string }) {
    return {
      code: 0,
      data: await this.familyService.verifyInviteCode(body.code),
      message: 'ok',
    };
  }

  @Post('join')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '加入家庭' })
  async joinFamily(
    @CurrentUser() user: UserPayload,
    @Body() body: { code: string; role: string },
  ) {
    return {
      code: 0,
      data: await this.familyService.joinFamily(user.id, body.code, body.role),
      message: '加入成功',
    };
  }

  @Get(':familyId/members')
  @ApiOperation({ summary: '获取家庭成员列表' })
  async getMembers(@Param('familyId') familyId: string) {
    return {
      code: 0,
      data: await this.familyService.getMembers(familyId),
      message: 'ok',
    };
  }

  @Post(':familyId/invite/regenerate')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '重新生成邀请码' })
  async regenerateInviteCode(@Param('familyId') familyId: string) {
    return {
      code: 0,
      data: await this.familyService.regenerateInviteCode(familyId),
      message: '邀请码已更新',
    };
  }

  @Put(':familyId/members/:memberId/role')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新成员角色' })
  async updateMemberRole(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('memberId') memberId: string,
    @Body() body: { role: string },
  ) {
    return {
      code: 0,
      data: await this.familyService.updateMemberRole(
        familyId,
        memberId,
        user.id,
        body.role,
      ),
      message: '角色更新成功',
    };
  }

  @Put(':familyId/members/:memberId/nickname')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新成员备注昵称' })
  async updateMemberNickname(
    @Param('familyId') familyId: string,
    @Param('memberId') memberId: string,
    @Body() body: { nickname: string },
  ) {
    return {
      code: 0,
      data: await this.familyService.updateMemberNickname(
        familyId,
        memberId,
        body.nickname,
      ),
      message: '昵称更新成功',
    };
  }

  @Delete(':familyId/members/:memberId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '移除家庭成员' })
  async removeMember(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('memberId') memberId: string,
  ) {
    return {
      code: 0,
      data: await this.familyService.removeMember(familyId, memberId, user.id),
      message: '成员已移除',
    };
  }

  // ==================== B3-1 家庭成员健康摘要 ====================

  @Put(':familyId/members/:memberId/visibility')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新成员数据可见性配置' })
  async updateMemberVisibility(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('memberId') memberId: string,
    @Body() body: { visibility: Record<string, string> },
  ) {
    return {
      code: 0,
      data: await this.familyService.updateMemberVisibility(
        familyId,
        memberId,
        user.id,
        body.visibility,
      ),
      message: '隐私设置已更新',
    };
  }

  @Get(':familyId/summary')
  @ApiOperation({ summary: '获取家庭成员健康摘要' })
  async getFamilySummary(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
  ): Promise<{ code: number; data: any; message: string }> {
    return {
      code: 0,
      data: await this.summaryService.getMembersSummary(familyId, user.id),
      message: 'ok',
    };
  }

  // ==================== B3-2 成员数据详情 ====================

  @Get(':familyId/members/:memberId/detail')
  @ApiOperation({ summary: '获取成员健康数据详情' })
  @ApiQuery({
    name: 'metric',
    required: false,
    description: '指标类型：bp/bg/medication/diet',
  })
  async getMemberDetail(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('memberId') memberId: string,
    @Query('metric') metric?: string,
  ): Promise<{ code: number; data: any; message: string }> {
    return {
      code: 0,
      data: await this.summaryService.getMemberDetail(
        familyId,
        memberId,
        user.id,
        (metric || 'bp') as any,
      ),
      message: 'ok',
    };
  }

  // ==================== B3-3 互相提醒 ====================

  @Post(':familyId/reminders')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '发送互相提醒' })
  async sendReminder(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Body() body: { targetUserId: string; type: string; message?: string },
  ) {
    return {
      code: 0,
      data: await this.reminderService.send(familyId, user.id, {
        toUserId: body.targetUserId,
        type: body.type,
        message: body.message,
      }),
      message: '提醒已发送',
    };
  }

  @Get(':familyId/reminders')
  @ApiOperation({ summary: '获取我的提醒列表' })
  @ApiQuery({ name: 'box', required: false, description: 'inbox/sent' })
  async getReminders(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Query('box') box?: string,
  ) {
    return {
      code: 0,
      data: await this.reminderService.list(
        familyId,
        user.id,
        (box || 'inbox') as 'inbox' | 'sent',
      ),
      message: 'ok',
    };
  }

  @Post(':familyId/reminders/:reminderId/complete')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '完成提醒' })
  async completeReminder(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('reminderId') reminderId: string,
  ) {
    return {
      code: 0,
      data: await this.reminderService.fulfill(reminderId, user.id),
      message: '已完成',
    };
  }

  // ==================== B3-4 默契值 ====================

  @Get(':familyId/chemistry')
  @ApiOperation({ summary: '获取夫妻默契值' })
  async getChemistry(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
  ) {
    return {
      code: 0,
      data: await this.summaryService.getSynergy(familyId, user.id),
      message: 'ok',
    };
  }

  // ==================== B3-5 家庭目标 ====================

  @Post(':familyId/goals')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '创建家庭目标' })
  async createGoal(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Body()
    body: {
      type: string;
      title: string;
      targetValue: number;
      unit: string;
      participantIds: string[];
      startDate: string;
      endDate: string;
    },
  ) {
    return {
      code: 0,
      data: await this.goalService.create(familyId, user.id, body),
      message: '目标创建成功',
    };
  }

  @Get(':familyId/goals')
  @ApiOperation({ summary: '获取家庭目标列表' })
  async getGoals(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
  ) {
    return {
      code: 0,
      data: await this.goalService.list(familyId, user.id),
      message: 'ok',
    };
  }

  @Get(':familyId/goals/:goalId')
  @ApiOperation({ summary: '获取目标详情' })
  async getGoal(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('goalId') goalId: string,
  ) {
    return {
      code: 0,
      data: await this.goalService.get(familyId, goalId, user.id),
      message: 'ok',
    };
  }

  @Put(':familyId/goals/:goalId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新目标' })
  async updateGoal(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('goalId') goalId: string,
    @Body()
    body: {
      title?: string;
      targetValue?: number;
      currentValue?: number;
      endDate?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.goalService.update(familyId, goalId, user.id, body),
      message: '更新成功',
    };
  }

  @Delete(':familyId/goals/:goalId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除目标' })
  async removeGoal(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('goalId') goalId: string,
  ) {
    return {
      code: 0,
      data: await this.goalService.remove(familyId, goalId, user.id),
      message: '已删除',
    };
  }

  // ==================== B3-6 家庭挑战 ====================

  @Post(':familyId/challenges')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '创建家庭挑战' })
  async createChallenge(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Body()
    body: {
      type: string;
      title: string;
      description: string;
      participantIds: string[];
      startDate: string;
      endDate?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.challengeService.create(familyId, user.id, {
        ...body,
        participantIds: body.participantIds || [user.id],
      }),
      message: '挑战创建成功',
    };
  }

  @Get(':familyId/challenges')
  @ApiOperation({ summary: '获取家庭挑战列表' })
  async getChallenges(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
  ) {
    return {
      code: 0,
      data: await this.challengeService.list(familyId, user.id),
      message: 'ok',
    };
  }

  @Post(':familyId/challenges/:challengeId/join')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '参加挑战' })
  async joinChallenge(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('challengeId') challengeId: string,
  ) {
    return {
      code: 0,
      data: await this.challengeService.join(familyId, challengeId, user.id),
      message: '已参加',
    };
  }

  @Get(':familyId/challenges/:challengeId')
  @ApiOperation({ summary: '获取挑战详情（含进度）' })
  async getChallengeDetail(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('challengeId') challengeId: string,
  ) {
    return {
      code: 0,
      data: await this.challengeService.detail(familyId, challengeId, user.id),
      message: 'ok',
    };
  }

  @Delete(':familyId/challenges/:challengeId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除挑战' })
  async removeChallenge(
    @CurrentUser() user: UserPayload,
    @Param('familyId') familyId: string,
    @Param('challengeId') challengeId: string,
  ) {
    return {
      code: 0,
      data: await this.challengeService.remove(familyId, challengeId, user.id),
      message: '已删除',
    };
  }
}
