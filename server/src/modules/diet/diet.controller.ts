import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Query,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { DietService } from './diet.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';

@ApiTags('饮食管理')
@ApiBearerAuth()
@Controller('diet')
export class DietController {
  constructor(private readonly dietService: DietService) {}

  @Get('recipe')
  @ApiOperation({ summary: '获取食谱列表' })
  async getRecipes(@Query('suitableFor') suitableFor?: string) {
    return {
      code: 0,
      data: await this.dietService.getRecipes(suitableFor),
      message: 'ok',
    };
  }

  @Get('recipe/:recipeId')
  @ApiOperation({ summary: '获取食谱详情' })
  async getRecipe(@Param('recipeId') recipeId: string) {
    return {
      code: 0,
      data: await this.dietService.getRecipe(recipeId),
      message: 'ok',
    };
  }

  @Get('menu')
  @ApiOperation({ summary: '获取今日推荐菜单' })
  async getDailyMenu(@CurrentUser() user: UserPayload) {
    // 获取用户病种
    const diseases = (user as any).diseases ?? [];
    return {
      code: 0,
      data: await this.dietService.getDailyMenu(diseases),
      message: 'ok',
    };
  }

  @Get('substitution')
  @ApiOperation({ summary: '查询食材替换建议' })
  async getSubstitutions(@Query('ingredient') ingredient: string) {
    return {
      code: 0,
      data: await this.dietService.getSubstitutions(ingredient || ''),
      message: 'ok',
    };
  }

  @Post('record')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '记录饮食' })
  async addDietRecord(
    @CurrentUser() user: UserPayload,
    @Body() body: { mealType: string; description?: string; imageUrl?: string },
  ) {
    return {
      code: 0,
      data: await this.dietService.addDietRecord(user.id, body),
      message: '记录成功',
    };
  }

  @Get('record')
  @ApiOperation({ summary: '获取饮食记录' })
  async getDietRecords(
    @CurrentUser() user: UserPayload,
    @Query('limit') limit?: string,
  ) {
    return {
      code: 0,
      data: await this.dietService.getDietRecords(user.id, limit ? parseInt(limit) : 20),
      message: 'ok',
    };
  }
}
