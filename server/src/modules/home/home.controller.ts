import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { HomeService } from './home.service.js';
import {
  CurrentUser,
  type UserPayload,
} from '../../common/decorators/current-user.decorator.js';

@ApiTags('首页')
@ApiBearerAuth()
@Controller('home')
export class HomeController {
  constructor(private readonly homeService: HomeService) {}

  @Get('patient')
  @ApiOperation({ summary: '获取患者首页数据（今日待办、最新指标、预警）' })
  async getPatientHome(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.homeService.getPatientHomeData(user.id),
      message: 'ok',
    };
  }

  @Get('family')
  @ApiOperation({ summary: '获取家庭成员健康概览（子女/照护者首页）' })
  async getFamilyHome(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.homeService.getFamilyHomeData(user.id),
      message: 'ok',
    };
  }
}
