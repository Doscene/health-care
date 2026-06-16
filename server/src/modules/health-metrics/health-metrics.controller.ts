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
import { HealthMetricsService } from './health-metrics.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';

@ApiTags('健康指标记录')
@ApiBearerAuth()
@Controller('records')
export class HealthMetricsController {
  constructor(private readonly healthMetricsService: HealthMetricsService) {}

  // ==================== 血压记录 ====================

  @Post('blood-pressure')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加血压记录' })
  async createBpRecord(
    @CurrentUser() user: UserPayload,
    @Body()
    body: {
      systolic: number;
      diastolic: number;
      heartRate?: number;
      inputMethod: string;
      source?: string;
      recordedAt?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.createBpRecord(user.id, body),
      message: '记录成功',
    };
  }

  @Get('blood-pressure')
  @ApiOperation({ summary: '获取血压记录列表' })
  async getBpRecords(
    @CurrentUser() user: UserPayload,
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.getBpRecords(user.id, {
        startDate,
        endDate,
        page: page ? parseInt(page) : 1,
        pageSize: pageSize ? parseInt(pageSize) : 20,
      }),
      message: 'ok',
    };
  }

  @Get('blood-pressure/:recordId')
  @ApiOperation({ summary: '获取血压记录详情' })
  async getBpRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.getBpRecord(user.id, recordId),
      message: 'ok',
    };
  }

  @Put('blood-pressure/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新血压记录' })
  async updateBpRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
    @Body()
    body: {
      systolic?: number;
      diastolic?: number;
      heartRate?: number;
      inputMethod?: string;
      source?: string;
      recordedAt?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.updateBpRecord(user.id, recordId, body),
      message: '更新成功',
    };
  }

  @Delete('blood-pressure/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除血压记录' })
  async deleteBpRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.deleteBpRecord(user.id, recordId),
      message: '已删除',
    };
  }

  // ==================== 血糖记录 ====================

  @Post('blood-sugar')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加血糖记录' })
  async createBgRecord(
    @CurrentUser() user: UserPayload,
    @Body()
    body: {
      type: string;
      value: number;
      inputMethod: string;
      source?: string;
      recordedAt?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.createBgRecord(user.id, body),
      message: '记录成功',
    };
  }

  @Get('blood-sugar')
  @ApiOperation({ summary: '获取血糖记录列表' })
  async getBgRecords(
    @CurrentUser() user: UserPayload,
    @Query('type') type?: string,
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.getBgRecords(user.id, {
        type,
        startDate,
        endDate,
        page: page ? parseInt(page) : 1,
        pageSize: pageSize ? parseInt(pageSize) : 20,
      }),
      message: 'ok',
    };
  }

  @Get('blood-sugar/:recordId')
  @ApiOperation({ summary: '获取血糖记录详情' })
  async getBgRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.getBgRecord(user.id, recordId),
      message: 'ok',
    };
  }

  @Put('blood-sugar/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新血糖记录' })
  async updateBgRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
    @Body()
    body: {
      type?: string;
      value?: number;
      inputMethod?: string;
      source?: string;
      recordedAt?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.updateBgRecord(user.id, recordId, body),
      message: '更新成功',
    };
  }

  @Delete('blood-sugar/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除血糖记录' })
  async deleteBgRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.deleteBgRecord(user.id, recordId),
      message: '已删除',
    };
  }

  // ==================== 统计接口 ====================

  @Get('summary/today')
  @ApiOperation({ summary: '获取今日指标摘要' })
  async getTodaySummary(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.healthMetricsService.getTodaySummary(user.id),
      message: 'ok',
    };
  }

  @Get('trend/blood-pressure')
  @ApiOperation({ summary: '获取血压趋势数据' })
  async getBpTrend(
    @CurrentUser() user: UserPayload,
    @Query('days') days?: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.getBpTrend(
        user.id,
        days ? parseInt(days) : 7,
      ),
      message: 'ok',
    };
  }

  @Get('trend/blood-sugar')
  @ApiOperation({ summary: '获取血糖趋势数据' })
  async getBgTrend(
    @CurrentUser() user: UserPayload,
    @Query('days') days?: string,
  ) {
    return {
      code: 0,
      data: await this.healthMetricsService.getBgTrend(
        user.id,
        days ? parseInt(days) : 7,
      ),
      message: 'ok',
    };
  }
}
