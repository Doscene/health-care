import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { FamilyService } from './family.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';
import { Public } from '../../common/decorators/public.decorator.js';

@ApiTags('家庭圈')
@ApiBearerAuth()
@Controller('family')
export class FamilyController {
  constructor(private readonly familyService: FamilyService) {}

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
      data: await this.familyService.removeMember(
        familyId,
        memberId,
        user.id,
      ),
      message: '成员已移除',
    };
  }
}
