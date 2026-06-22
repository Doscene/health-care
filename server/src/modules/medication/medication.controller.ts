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
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { MedicationService } from './medication.service.js';
import {
  CurrentUser,
  type UserPayload,
} from '../../common/decorators/current-user.decorator.js';

@ApiTags('用药管理')
@ApiBearerAuth()
@Controller('medication')
export class MedicationController {
  constructor(private readonly medicationService: MedicationService) {}

  @Post()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加用药计划' })
  async createMedication(
    @CurrentUser() user: UserPayload,
    @Body()
    body: {
      name: string;
      specification: string;
      dosagePerTime: number;
      frequencyPerDay: number;
      timeSlots: string[];
      remindTimes: string[];
      startDate: string;
      endDate?: string;
      notes?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.medicationService.createMedication(user.id, body),
      message: '添加成功',
    };
  }

  @Get()
  @ApiOperation({ summary: '获取用药列表' })
  async getMedications(
    @CurrentUser() user: UserPayload,
    @Query('status') status?: string,
  ) {
    return {
      code: 0,
      data: await this.medicationService.getMedications(user.id, status),
      message: 'ok',
    };
  }

  @Get(':medicationId')
  @ApiOperation({ summary: '获取用药详情' })
  async getMedication(
    @CurrentUser() user: UserPayload,
    @Param('medicationId') medicationId: string,
  ) {
    return {
      code: 0,
      data: await this.medicationService.getMedication(user.id, medicationId),
      message: 'ok',
    };
  }

  @Put(':medicationId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新用药计划' })
  async updateMedication(
    @CurrentUser() user: UserPayload,
    @Param('medicationId') medicationId: string,
    @Body()
    body: {
      name?: string;
      specification?: string;
      dosagePerTime?: number;
      frequencyPerDay?: number;
      timeSlots?: string[];
      remindTimes?: string[];
      endDate?: string;
      notes?: string;
      status?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.medicationService.updateMedication(
        user.id,
        medicationId,
        body,
      ),
      message: '更新成功',
    };
  }

  @Delete(':medicationId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除用药计划' })
  async deleteMedication(
    @CurrentUser() user: UserPayload,
    @Param('medicationId') medicationId: string,
  ) {
    return {
      code: 0,
      data: await this.medicationService.deleteMedication(
        user.id,
        medicationId,
      ),
      message: '已删除',
    };
  }

  @Post(':medicationId/confirm')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '确认服药' })
  async confirmMedication(
    @CurrentUser() user: UserPayload,
    @Param('medicationId') medicationId: string,
    @Body() body: { scheduledTime: string; status: string },
  ) {
    return {
      code: 0,
      data: await this.medicationService.confirmMedication(
        user.id,
        medicationId,
        body,
      ),
      message: '已确认',
    };
  }

  @Get('records/all')
  @ApiOperation({ summary: '获取服药记录' })
  async getMedicationRecords(
    @CurrentUser() user: UserPayload,
    @Query('medicationId') medicationId?: string,
    @Query('limit') limit?: string,
  ) {
    return {
      code: 0,
      data: await this.medicationService.getMedicationRecords(
        user.id,
        medicationId,
        limit ? parseInt(limit) : 30,
      ),
      message: 'ok',
    };
  }

  // ==================== 冲突检测 / 日历 / 依从性 ====================

  @Post('conflicts')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '检测多药冲突' })
  async checkConflicts(
    @CurrentUser() user: UserPayload,
    @Body() body: { medicationIds: string[] },
  ) {
    return {
      code: 0,
      data: await this.medicationService.checkConflicts(
        user.id,
        body.medicationIds,
      ),
      message: 'ok',
    };
  }

  @Get('calendar')
  @ApiOperation({ summary: '获取服药日历（月视图）' })
  async getCalendar(
    @CurrentUser() user: UserPayload,
    @Query('year') year?: string,
    @Query('month') month?: string,
    @Query('medicationId') medicationId?: string,
  ) {
    const now = new Date();
    return {
      code: 0,
      data: await this.medicationService.getCalendar(
        user.id,
        year ? parseInt(year) : now.getFullYear(),
        month ? parseInt(month) : now.getMonth() + 1,
        medicationId,
      ),
      message: 'ok',
    };
  }

  @Get('adherence')
  @ApiOperation({ summary: '获取用药依从性统计' })
  async getAdherence(
    @CurrentUser() user: UserPayload,
    @Query('days') days?: string,
  ) {
    return {
      code: 0,
      data: await this.medicationService.getAdherence(
        user.id,
        days ? parseInt(days) : 30,
      ),
      message: 'ok',
    };
  }
}
