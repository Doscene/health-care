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
import { AlertService } from './alert.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';

@ApiTags('风险预警')
@ApiBearerAuth()
@Controller('alert')
export class AlertController {
  constructor(private readonly alertService: AlertService) {}

  // ==================== 预警 ====================

  @Get()
  @ApiOperation({ summary: '获取风险预警列表' })
  async getAlerts(
    @CurrentUser() user: UserPayload,
    @Query('status') status?: string,
  ) {
    return {
      code: 0,
      data: await this.alertService.getAlerts(user.id, status),
      message: 'ok',
    };
  }

  @Put(':alertId/status')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新预警状态' })
  async updateAlertStatus(
    @CurrentUser() user: UserPayload,
    @Param('alertId') alertId: string,
    @Body() body: { status: string },
  ) {
    return {
      code: 0,
      data: await this.alertService.updateAlertStatus(user.id, alertId, body.status),
      message: '状态已更新',
    };
  }

  // ==================== 紧急联系人 ====================

  @Post('contact')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加紧急联系人' })
  async addEmergencyContact(
    @CurrentUser() user: UserPayload,
    @Body() body: { name: string; phone: string; relation: string; priority?: number },
  ) {
    return {
      code: 0,
      data: await this.alertService.addEmergencyContact(user.id, body),
      message: '添加成功',
    };
  }

  @Get('contact')
  @ApiOperation({ summary: '获取紧急联系人列表' })
  async getEmergencyContacts(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.alertService.getEmergencyContacts(user.id),
      message: 'ok',
    };
  }

  @Put('contact/:contactId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新紧急联系人' })
  async updateEmergencyContact(
    @CurrentUser() user: UserPayload,
    @Param('contactId') contactId: string,
    @Body() body: { name?: string; phone?: string; relation?: string; priority?: number },
  ) {
    return {
      code: 0,
      data: await this.alertService.updateEmergencyContact(user.id, contactId, body),
      message: '更新成功',
    };
  }

  @Delete('contact/:contactId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除紧急联系人' })
  async deleteEmergencyContact(
    @CurrentUser() user: UserPayload,
    @Param('contactId') contactId: string,
  ) {
    return {
      code: 0,
      data: await this.alertService.deleteEmergencyContact(user.id, contactId),
      message: '已删除',
    };
  }
}
