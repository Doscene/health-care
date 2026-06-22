import {
  Controller,
  Get,
  Put,
  Body,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { UserService } from './user.service.js';
import {
  CurrentUser,
  type UserPayload,
} from '../../common/decorators/current-user.decorator.js';

@ApiTags('用户')
@ApiBearerAuth()
@Controller('user')
export class UserController {
  constructor(private readonly userService: UserService) {}

  @Get('me')
  @ApiOperation({ summary: '获取当前用户信息' })
  async getProfile(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.userService.getProfile(user.id),
      message: 'ok',
    };
  }

  @Put('role')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新用户角色' })
  async updateRole(
    @CurrentUser() user: UserPayload,
    @Body() body: { selfRole: string; diseases?: string[] },
  ) {
    return {
      code: 0,
      data: await this.userService.updateRole(
        user.id,
        body.selfRole,
        body.diseases,
      ),
      message: '角色更新成功',
    };
  }

  @Put('profile')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '更新用户资料' })
  async updateProfile(
    @CurrentUser() user: UserPayload,
    @Body() body: { name?: string; avatar?: string; age?: number },
  ) {
    return {
      code: 0,
      data: await this.userService.updateProfile(user.id, body),
      message: '资料更新成功',
    };
  }
}
