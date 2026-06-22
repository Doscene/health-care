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
import {
  ApiTags,
  ApiOperation,
  ApiBearerAuth,
  ApiQuery,
} from '@nestjs/swagger';
import { DietService } from './diet.service.js';
import { RecipeRecommendationService } from './recipe-recommendation.service.js';
import {
  CurrentUser,
  type UserPayload,
} from '../../common/decorators/current-user.decorator.js';

@ApiTags('饮食管理')
@ApiBearerAuth()
@Controller('diet')
export class DietController {
  constructor(
    private readonly dietService: DietService,
    private readonly recommendationService: RecipeRecommendationService,
  ) {}

  // ==================== B3-7 食谱推荐 ====================

  @Get('recipe')
  @ApiOperation({ summary: '获取食谱列表' })
  @ApiQuery({
    name: 'suitableFor',
    required: false,
    description: '适合人群：hypertension/diabetes',
  })
  @ApiQuery({ name: 'limit', required: false, description: '返回数量' })
  async getRecipes(
    @Query('suitableFor') suitableFor?: string,
    @Query('limit') limit?: string,
  ) {
    return {
      code: 0,
      data: await this.dietService.getRecipes(
        suitableFor,
        limit ? parseInt(limit) : 50,
      ),
      message: 'ok',
    };
  }

  @Get('recipe/recommended')
  @ApiOperation({ summary: '获取智能推荐食谱（根据病种+人数）' })
  @ApiQuery({
    name: 'diseases',
    required: false,
    description: '病种，逗号分隔：hypertension,diabetes',
  })
  @ApiQuery({ name: 'people', required: false, description: '用餐人数' })
  async getRecommendedRecipes(
    @Query('diseases') diseases?: string,
    @Query('people') people?: string,
  ) {
    const diseaseList = diseases ? diseases.split(',') : [];
    const peopleCount = people ? parseInt(people) : 2;
    return {
      code: 0,
      data: await this.recommendationService.getRecommendedRecipes(
        diseaseList,
        peopleCount,
      ),
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

  // ==================== B3-8 食材替换 ====================

  @Get('substitution')
  @ApiOperation({ summary: '查询食材替换建议' })
  @ApiQuery({ name: 'ingredient', required: false, description: '食材名称' })
  async getSubstitutions(@Query('ingredient') ingredient?: string) {
    return {
      code: 0,
      data: await this.dietService.getSubstitutions(ingredient || ''),
      message: 'ok',
    };
  }

  // ==================== B3-9 饮食拍照记录 ====================

  @Post('record')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '记录饮食（支持拍照）' })
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
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'date', required: false, description: '日期 YYYY-MM-DD' })
  async getDietRecords(
    @CurrentUser() user: UserPayload,
    @Query('limit') limit?: string,
    @Query('date') date?: string,
  ) {
    return {
      code: 0,
      data: await this.dietService.getDietRecords(user.id, {
        limit: limit ? parseInt(limit) : 20,
        date,
      }),
      message: 'ok',
    };
  }

  @Delete('record/:recordId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '删除饮食记录' })
  async deleteDietRecord(
    @CurrentUser() user: UserPayload,
    @Param('recordId') recordId: string,
  ) {
    return {
      code: 0,
      data: await this.dietService.deleteDietRecord(recordId, user.id),
      message: '已删除',
    };
  }

  // ==================== B3-10 今日菜单 + 买菜清单 ====================

  @Get('menu')
  @ApiOperation({ summary: '获取今日推荐菜单' })
  async getDailyMenu(@CurrentUser() user: UserPayload) {
    const diseases = (user as any).diseases ?? [];
    return {
      code: 0,
      data: await this.dietService.getDailyMenu(diseases),
      message: 'ok',
    };
  }

  @Get('menu/custom')
  @ApiOperation({ summary: '获取自定义今日菜单' })
  async getCustomMenu(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.dietService.getCustomMenu(user.id),
      message: 'ok',
    };
  }

  @Post('menu/custom')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '保存自定义今日菜单' })
  async saveCustomMenu(
    @CurrentUser() user: UserPayload,
    @Body() body: { recipeIds: string[] },
  ) {
    return {
      code: 0,
      data: await this.dietService.saveCustomMenu(user.id, body.recipeIds),
      message: '菜单已保存',
    };
  }

  @Get('shopping-list')
  @ApiOperation({ summary: '生成买菜清单（基于今日菜单）' })
  async getShoppingList(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.dietService.generateShoppingList(user.id),
      message: 'ok',
    };
  }

  @Get('shopping-list/saved')
  @ApiOperation({ summary: '获取已保存的买菜清单' })
  async getSavedShoppingList(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: await this.dietService.getSavedShoppingList(user.id),
      message: 'ok',
    };
  }

  @Post('shopping-list/save')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '保存买菜清单' })
  async saveShoppingList(
    @CurrentUser() user: UserPayload,
    @Body()
    body: {
      items: Array<{ ingredient: string; amount: string; unit: string }>;
    },
  ) {
    return {
      code: 0,
      data: await this.dietService.saveShoppingList(user.id, body.items),
      message: '清单已保存',
    };
  }

  @Put('shopping-list/:listId/item/:itemIndex/check')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '勾选买菜清单项目' })
  async checkShoppingItem(
    @CurrentUser() user: UserPayload,
    @Param('listId') listId: string,
    @Param('itemIndex') itemIndex: string,
  ) {
    return {
      code: 0,
      data: await this.dietService.checkShoppingItem(
        listId,
        parseInt(itemIndex),
        user.id,
      ),
      message: '已勾选',
    };
  }
}
