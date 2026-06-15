import {
  Controller,
  Get,
  Post,
  Delete,
  Body,
  Param,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { AppointmentService } from './appointment.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';

@ApiTags('复诊计划')
@ApiBearerAuth()
@Controller('appointment')
export class AppointmentController {
  constructor(private readonly appointmentService: AppointmentService) {}

  @Get()
  @ApiOperation({ summary: '获取复诊计划列表' })
  async getPlans(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.appointmentService.getPlans(user.id),
      message: 'ok',
    };
  }

  @Post()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加复诊计划' })
  async addPlan(
    @CurrentUser() user: UserPayload,
    @Body()
    body: {
      hospital: string;
      department: string;
      date: string;
      reminderDays?: number;
      notes?: string;
    },
  ) {
    return {
      code: 0,
      data: await this.appointmentService.addPlan(user.id, body),
      message: '添加成功',
    };
  }

  @Delete(':planId')
  @ApiOperation({ summary: '删除复诊计划' })
  async deletePlan(
    @CurrentUser() user: UserPayload,
    @Param('planId') planId: string,
  ) {
    return {
      code: 0,
      data: await this.appointmentService.deletePlan(user.id, planId),
      message: '删除成功',
    };
  }
}
