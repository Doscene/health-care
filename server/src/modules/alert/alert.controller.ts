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

  @Post(':alertId/inquiry')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '提交问询回答' })
  async submitInquiry(
    @CurrentUser() user: UserPayload,
    @Param('alertId') alertId: string,
    @Body() body: { answer: string },
  ) {
    return {
      code: 0,
      data: await this.alertService.submitInquiry(user.id, alertId, body.answer),
      message: '回答已提交',
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

  @Post('contact/mutual')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '设置为互为紧急联系人' })
  async setMutualContact(
    @CurrentUser() user: UserPayload,
    @Body() body: { targetUserId: string },
  ) {
    return {
      code: 0,
      data: await this.alertService.setMutualContact(user.id, body.targetUserId),
      message: '已设置',
    };
  }

  // ==================== 急救包 ====================

  @Get('kit')
  @ApiOperation({ summary: '获取急救包物品列表' })
  async getFirstAidKit(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.alertService.getFirstAidKit(user.id),
      message: 'ok',
    };
  }

  @Post('kit')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加急救包物品' })
  async addFirstAidItem(
    @CurrentUser() user: UserPayload,
    @Body()
    body: {
      name: string;
      type: string;
      quantity?: number;
      expireDate?: string;
      notes?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.alertService.addFirstAidItem(user.id, body),
      message: '添加成功',
    };
  }

  @Put('kit/:itemId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新急救包物品' })
  async updateFirstAidItem(
    @CurrentUser() user: UserPayload,
    @Param('itemId') itemId: string,
    @Body()
    body: {
      name?: string;
      type?: string;
      quantity?: number;
      expireDate?: string;
      notes?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.alertService.updateFirstAidItem(user.id, itemId, body),
      message: '更新成功',
    };
  }

  @Delete('kit/:itemId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除急救包物品' })
  async deleteFirstAidItem(
    @CurrentUser() user: UserPayload,
    @Param('itemId') itemId: string,
  ) {
    return {
      code: 0,
      data: await this.alertService.deleteFirstAidItem(user.id, itemId),
      message: '已删除',
    };
  }

  // ==================== 急救指南 ====================

  @Get('guide')
  @ApiOperation({ summary: '获取急救指南列表' })
  async getEmergencyGuides(@Query('type') type?: string) {
    return {
      code: 0,
      data: await this.alertService.getEmergencyGuides(type),
      message: 'ok',
    };
  }
}
