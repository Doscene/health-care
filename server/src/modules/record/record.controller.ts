import {
  Controller,
  Get,
  Post,
  Delete,
  Body,
  Param,
  Query,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { RecordService } from './record.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';

@ApiTags('健康记录')
@ApiBearerAuth()
@Controller('record')
export class RecordController {
  constructor(private readonly recordService: RecordService) {}

  // ==================== 血压 ====================

  @Post('bp')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加血压记录' })
  async addBpRecord(
    @CurrentUser() user: UserPayload,
    @Body() body: { systolic: number; diastolic: number; heartRate?: number; inputMethod?: string },
  ) {
    return {
      code: 0,
      data: await this.recordService.addBpRecord(user.id, body),
      message: '记录成功',
    };
  }

  @Get('bp')
  @ApiOperation({ summary: '获取血压记录列表' })
  async getBpRecords(
    @CurrentUser() user: UserPayload,
    @Query('limit') limit?: string,
  ) {
    return {
      code: 0,
      data: await this.recordService.getBpRecords(user.id, limit ? parseInt(limit) : 30),
      message: 'ok',
    };
  }

  @Delete('bp/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除血压记录' })
  async deleteBpRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.recordService.deleteBpRecord(user.id, recordId),
      message: '已删除',
    };
  }

  // ==================== 血糖 ====================

  @Post('bg')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '添加血糖记录' })
  async addBgRecord(
    @CurrentUser() user: UserPayload,
    @Body() body: { type: string; value: number; inputMethod?: string },
  ) {
    return {
      code: 0,
      data: await this.recordService.addBgRecord(user.id, body),
      message: '记录成功',
    };
  }

  @Get('bg')
  @ApiOperation({ summary: '获取血糖记录列表' })
  async getBgRecords(
    @CurrentUser() user: UserPayload,
    @Query('limit') limit?: string,
  ) {
    return {
      code: 0,
      data: await this.recordService.getBgRecords(user.id, limit ? parseInt(limit) : 30),
      message: 'ok',
    };
  }

  @Delete('bg/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除血糖记录' })
  async deleteBgRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.recordService.deleteBgRecord(user.id, recordId),
      message: '已删除',
    };
  }
}
